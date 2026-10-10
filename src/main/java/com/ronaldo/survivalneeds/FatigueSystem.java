package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import com.ronaldo.survivalneeds.debuff.DebuffEffect;
import com.ronaldo.survivalneeds.debuff.DebuffSystem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Cansaço. Passar noites seguidas sem dormir deixa o jogador cansado. Valores em {@code fatigue} na configuração.
 * <ul>
 *   <li><b>Efeitos</b> (pelo {@link DebuffSystem}): limite de construção, limite (ou bloqueio) de corrida e dano reduzido.</li>
 *   <li><b>Contador:</b> ao ficar cansado começa uma contagem. Se ela chegar a zero sem o jogador dormir, ele
 *       passa a perder vida aos poucos até dormir.</li>
 *   <li><b>Dormir:</b> cansado, o jogador consegue dormir de dia e mesmo com monstros por perto
 *       (ver PlayerMixin, ServerPlayerMixin e os eventos em SurvivalNeeds). Dormir cura o cansaço.</li>
 * </ul>
 * Uma "noite sem dormir" é uma noite que termina (amanhece) sem que o jogador tenha dormido nela.
 */
public final class FatigueSystem {
	private static final String SOURCE = "fatigue";
	private static final int SYNC_INTERVAL_TICKS = 20;

	// A noite do jogo, como na regra da cama: do fim da tarde até o amanhecer (em ticks do dia de 24000)
	private static final long NIGHT_START = 12542;
	private static final long NIGHT_END = 23460;

	/**
	 * Estado do cansaço, salvo no jogador e enviado ao cliente (ícone, contador e permissão de dormir):
	 * 0 = descansado; 1 = cansado e com o contador esgotado (perdendo vida); N maior que 1 = cansado, faltam N-1 ticks.
	 */
	public static final AttachmentType<Integer> STATE = AttachmentRegistry.<Integer>create(
		SurvivalNeeds.id("fatigue_state"),
		builder -> builder
			.persistent(Codec.INT)
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	/** Noites seguidas sem dormir. Só o servidor usa. */
	private static final AttachmentType<Integer> NIGHTS_AWAKE = AttachmentRegistry.createPersistent(
		SurvivalNeeds.id("nights_awake"), Codec.INT);

	private static final Set<UUID> ACTIVE = new HashSet<>();
	private static final Map<UUID, Integer> LEFT = new HashMap<>();        // ticks que faltam no contador (0 = esgotado)
	private static final Map<UUID, Boolean> LAST_NIGHT = new HashMap<>();  // se era noite na última checagem
	private static final Set<UUID> SLEPT_TONIGHT = new HashSet<>();        // dormiu na noite que está passando

	private FatigueSystem() {}

	public static void init() {
		// força o carregamento da classe (registra os attachments)
	}

	private static SurvivalNeedsConfig.Fatigue cfg() {
		return SurvivalNeedsConfig.get().fatigue;
	}

	/** Funciona nos dois lados (servidor e cliente), pois o estado é sincronizado. */
	public static boolean isFatigued(Player player) {
		return player.getAttachedOrElse(STATE, 0) > 0;
	}

	/** Cansado, o jogador pode dormir a qualquer hora e com monstros por perto. Usado pelos mixins de cama. */
	public static boolean canSleepAnytime(Player player) {
		return isFatigued(player);
	}

	/** Liga (cansado na hora) ou desliga (descansa na hora) o cansaço: comando /fatigue. */
	public static void setActive(ServerPlayer player, boolean active) {
		if (active) {
			player.setAttached(NIGHTS_AWAKE, cfg().nightsWithoutSleep);
			startFatigue(player);
			DebuffXp.abort(player, DebuffXp.Type.FATIGUE); // comando não dá XP
		} else {
			player.setAttached(NIGHTS_AWAKE, 0);
			DebuffXp.abort(player, DebuffXp.Type.FATIGUE);
			rest(player, true);
		}
	}

	public static void tick(ServerPlayer player) {
		UUID id = player.getUUID();
		if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) {
			if (ACTIVE.contains(id) || player.getAttachedOrElse(STATE, 0) > 0) {
				rest(player, false);
			}
			return;
		}

		// Ao entrar no mundo, retoma o estado salvo
		if (!LEFT.containsKey(id)) {
			int saved = player.getAttachedOrElse(STATE, 0);
			if (saved > 0) {
				ACTIVE.add(id);
				LEFT.put(id, saved - 1);
			} else {
				LEFT.put(id, 0);
			}
		}

		if (player.tickCount % 20 == 0) {
			trackNights(player);
		}

		// Dormiu o suficiente: descansou
		if (player.isSleeping() && player.getSleepTimer() >= Player.SLEEP_DURATION) {
			onSlept(player);
		}

		if (!ACTIVE.contains(id) && nightsAwake(player) >= cfg().nightsWithoutSleep) {
			startFatigue(player);
		}
		if (ACTIVE.contains(id)) {
			tickFatigue(player);
		}

		DebuffSystem.set(player, SOURCE, ACTIVE.contains(id), FatigueSystem::debuffs);
	}

	// ---- noites sem dormir ----

	/** Detecta o fim de cada noite e conta as que passaram sem o jogador dormir. */
	private static void trackNights(ServerPlayer player) {
		UUID id = player.getUUID();
		ServerLevel level = player.level();
		long timeOfDay = level.getOverworldClockTime() % 24000L;
		boolean night = timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;

		Boolean wasNight = LAST_NIGHT.put(id, night);
		if (wasNight == null || !wasNight || night) {
			return;
		}
		// A noite acabou: se o jogador não dormiu nela, é mais uma noite acordado
		if (!SLEPT_TONIGHT.remove(id)) {
			player.setAttached(NIGHTS_AWAKE, nightsAwake(player) + 1);
		}
	}

	private static int nightsAwake(ServerPlayer player) {
		return player.getAttachedOrElse(NIGHTS_AWAKE, 0);
	}

	private static void onSlept(ServerPlayer player) {
		UUID id = player.getUUID();
		if (Boolean.TRUE.equals(LAST_NIGHT.get(id))) {
			SLEPT_TONIGHT.add(id); // esta noite conta como dormida
		}
		if (nightsAwake(player) != 0) {
			player.setAttached(NIGHTS_AWAKE, 0);
		}
		if (ACTIVE.contains(id)) {
			DebuffXp.recover(player, DebuffXp.Type.FATIGUE);
			rest(player, true);
		}
	}

	// ---- estar cansado ----

	private static void startFatigue(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!ACTIVE.add(id)) {
			return;
		}
		int ticks = SurvivalNeedsConfig.ticks(cfg().counterSeconds);
		LEFT.put(id, ticks);
		DebuffXp.begin(player, DebuffXp.Type.FATIGUE);
		player.setAttached(STATE, ticks + 1);
		player.sendSystemMessage(Component.literal("Você está exausto! Durma em até "
			+ Math.round(cfg().counterSeconds / 60.0) + " minutos ou começará a perder vida."), true);
	}

	/** Roda a cada tick enquanto cansado: desce o contador e, esgotado, tira vida aos poucos. */
	private static void tickFatigue(ServerPlayer player) {
		UUID id = player.getUUID();
		SurvivalNeedsConfig.Fatigue cfg = cfg();
		int left = LEFT.getOrDefault(id, 0);

		if (left > 0) {
			left--;
			LEFT.put(id, left);
			if (left == 0) {
				player.setAttached(STATE, 1);
				player.sendSystemMessage(Component.literal("Você está esgotado e perdendo vida! Durma agora."), true);
			} else if (left % SYNC_INTERVAL_TICKS == 0) {
				player.setAttached(STATE, left + 1);
			}
			return;
		}

		// Contador esgotado: perde vida aos poucos até dormir
		if (player.tickCount % SurvivalNeedsConfig.ticks(cfg.damageIntervalSeconds) == 0
			&& player.getHealth() > cfg.minHealth) {
			player.hurtServer(player.level(), player.level().damageSources().generic(), (float) cfg.damage);
		}
	}

	/** Cansaço passou (dormiu, comando ou mudou de modo de jogo). */
	private static void rest(ServerPlayer player, boolean notify) {
		UUID id = player.getUUID();
		DebuffXp.abort(player, DebuffXp.Type.FATIGUE); // dormir já chamou recover antes
		boolean wasActive = ACTIVE.remove(id);
		LEFT.put(id, 0);
		player.setAttached(STATE, 0);
		DebuffSystem.clear(player, SOURCE);
		if (wasActive && notify) {
			player.sendSystemMessage(Component.literal("Você descansou e o cansaço passou."), true);
		}
	}

	/** Os efeitos que o cansaço pede ao DebuffSystem. */
	private static List<DebuffEffect> debuffs() {
		SurvivalNeedsConfig.Fatigue cfg = cfg();
		DebuffEffect sprint = cfg.blockSprint
			? new DebuffEffect.SprintBlock()
			: new DebuffEffect.SprintLimit(
				SurvivalNeedsConfig.ticks(cfg.sprintLimitSeconds),
				SurvivalNeedsConfig.ticks(cfg.sprintCooldownSeconds),
				SurvivalNeedsConfig.ticks(cfg.sprintGraceSeconds),
				"Cansado e sem fôlego! Você não pode correr por " + Math.round(cfg.sprintCooldownSeconds) + " segundos.",
				"Você recuperou o fôlego e pode correr de novo.");
		return List.of(
			new DebuffEffect.BuildLimit(
				cfg.placeLimit,
				SurvivalNeedsConfig.ticks(cfg.placeResetSeconds),
				"Cansado: seus braços não obedecem."),
			sprint,
			new DebuffEffect.DamageReduction(cfg.damageReduction));
	}
}
