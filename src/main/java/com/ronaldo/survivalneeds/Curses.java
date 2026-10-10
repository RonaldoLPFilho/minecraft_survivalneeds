package com.ronaldo.survivalneeds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import com.ronaldo.survivalneeds.debuff.DebuffEffect;
import com.ronaldo.survivalneeds.debuff.DebuffSystem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;

/**
 * Maldição: um debuff aleatório. O jogador tem no máximo uma. Valores em {@code curse} na configuração.
 * <ul>
 *   <li><b>Efeitos</b> de corrida, construção, velocidade, mineração, vida máxima: pelo {@link DebuffSystem}.</li>
 *   <li><b>Proibições</b> (atacar, comer, dormir): eventos; os dois primeiros valem nos dois lados, pois o estado é sincronizado.</li>
 *   <li><b>Contador</b> (afogamento, sentença, explosão): salvo no jogador; corre só com ele online; avisa aos poucos minutos.</li>
 *   <li><b>Imunidade</b> depois de curado ou de morrer, para não encadear maldições.</li>
 * </ul>
 */
public final class Curses {
	private static final String SOURCE = "curse";
	private static final int SYNC_INTERVAL_TICKS = 20;
	private static final int DROWNING = -1; // valor de TICKS depois que o afogamento começou

	/** Maldição atual: 0 = nenhuma, senão {@link Curse#code()}. Salva no jogador e enviada ao cliente. */
	public static final AttachmentType<Integer> CURRENT = AttachmentRegistry.<Integer>create(
		SurvivalNeeds.id("curse"),
		builder -> builder
			.persistent(Codec.INT)
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	/** Ticks que faltam no contador (0 = a maldição não tem contador; {@value #DROWNING} = afogamento em curso). */
	public static final AttachmentType<Integer> TICKS = AttachmentRegistry.<Integer>create(
		SurvivalNeeds.id("curse_ticks"),
		builder -> builder
			.persistent(Codec.INT)
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	/** Ticks de imunidade que restam. Só o servidor usa. */
	private static final AttachmentType<Integer> IMMUNE = AttachmentRegistry.createPersistent(
		SurvivalNeeds.id("curse_immunity"), Codec.INT);

	private static final Map<UUID, Integer> LEFT = new HashMap<>(); // ticks exatos que faltam no contador
	private static final Random RANDOM = new Random();

	private Curses() {}

	private static SurvivalNeedsConfig.Curse cfg() {
		return SurvivalNeedsConfig.get().curse;
	}

	public static void init() {
		// Morrer tira a maldição e dá um tempo de imunidade
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				// a maldição não é copiada na morte; só a imunidade é dada (mexer nos outros avisaria o cliente cedo demais)
				newPlayer.setAttached(IMMUNE, SurvivalNeedsConfig.ticks(cfg().immunityMinutes * 60.0));
			}
		});

		// Mãos de pedra: não ataca
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (has(player, Curse.NO_ATTACK) && !ignores(player)) {
				warn(player, "Suas mãos de pedra não obedecem.");
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		// Estômago selado: não come (beber pode)
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (has(player, Curse.NO_EAT) && !ignores(player) && isEdible(player.getItemInHand(hand))) {
				warn(player, "Seu estômago está selado. Você não consegue comer.");
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (has(player, Curse.NO_EAT) && !ignores(player) && level.getBlockState(hit.getBlockPos()).getBlock() instanceof CakeBlock) {
				warn(player, "Seu estômago está selado. Você não consegue comer.");
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}

	private static boolean isEdible(ItemStack stack) {
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		return consumable != null && consumable.animation() == ItemUseAnimation.EAT;
	}

	/** Só avisa no servidor, para a mensagem não aparecer duas vezes. */
	private static void warn(Player player, String text) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.literal(text), true);
		}
	}

	private static boolean ignores(Player player) {
		return !cfg().enabled || player.isCreative() || player.isSpectator();
	}

	// ---- consulta (vale nos dois lados) ----

	/** A maldição do jogador, ou null. Funciona também no cliente, pois o estado é sincronizado. */
	public static Curse current(Player player) {
		return Curse.byCode(player.getAttachedOrElse(CURRENT, 0));
	}

	public static boolean has(Player player, Curse curse) {
		return current(player) == curse;
	}

	public static boolean isCursed(Player player) {
		return current(player) != null;
	}

	/** Pele de papel: o dano recebido sobe. Usado pelo mixin de dano. */
	public static float scaleDamageTaken(ServerPlayer player, float amount) {
		if (has(player, Curse.PAPER_SKIN) && !ignores(player)) {
			return amount * (1.0f + (float) cfg().paperSkinExtraDamage);
		}
		return amount;
	}

	/** Ressecado: a sede gasta mais rápido. */
	public static double thirstMultiplier(ServerPlayer player) {
		return has(player, Curse.PARCHED) && !ignores(player) ? Math.max(1.0, cfg().parchedThirstMultiplier) : 1.0;
	}

	/** Mente turva: o jogador não ganha XP. */
	public static boolean blocksXp(ServerPlayer player) {
		return has(player, Curse.MIND_FOG) && !ignores(player);
	}

	// ---- sorteio e aplicação ----

	/** Sorteia uma maldição pelos pesos da configuração, ou null se todos os pesos forem zero. */
	public static Curse roll() {
		List<Curse> options = new ArrayList<>();
		List<Integer> weights = new ArrayList<>();
		int total = 0;
		for (Curse curse : Curse.values()) {
			int weight = cfg().weights.getOrDefault(curse.id, curse.defaultWeight);
			if (weight > 0) {
				options.add(curse);
				weights.add(weight);
				total += weight;
			}
		}
		if (total <= 0) {
			return null;
		}
		int pick = RANDOM.nextInt(total);
		for (int i = 0; i < options.size(); i++) {
			pick -= weights.get(i);
			if (pick < 0) {
				return options.get(i);
			}
		}
		return options.get(options.size() - 1);
	}

	/** O jogador pode ser amaldiçoado agora? (sem maldição, sem imunidade, fora de criativo) */
	public static boolean canBeCursed(ServerPlayer player) {
		return !ignores(player) && !player.isDeadOrDying() && !isCursed(player) && player.getAttachedOrElse(IMMUNE, 0) <= 0;
	}

	/** Sorteia e aplica uma maldição, se o jogador puder receber. Devolve a maldição aplicada, ou null. */
	public static Curse afflict(ServerPlayer player) {
		if (!canBeCursed(player)) {
			return null;
		}
		Curse curse = roll();
		if (curse != null) {
			give(player, curse);
		}
		return curse;
	}

	/**
	 * Um dardo do Espectro acertou o jogador (e o escudo não segurou). O primeiro dardo parte um amuleto equipado e
	 * não amaldiçoa; sem amuleto, o dardo amaldiçoa. Quem já está amaldiçoado ou imune não perde amuleto nenhum. Devolve true se a maldição foi aplicada.
	 */
	public static boolean onDartHit(ServerPlayer player) {
		if (!canBeCursed(player)) {
			int immune = player.getAttachedOrElse(IMMUNE, 0);
			if (!ignores(player) && !isCursed(player) && immune > 0) {
				player.sendSystemMessage(Component.literal("§7A imunidade ainda protege você da maldição ("
					+ ((immune + 1199) / 1200) + " min)."), true);
			}
			return false;
		}
		if (Amulets.breakWeakest(player)) {
			player.sendSystemMessage(Component.literal("§eSeu amuleto se partiu e absorveu a maldição!"), false);
			player.level().playSound(null, player.blockPosition(),
				net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.7f);
			return false;
		}
		return afflict(player) != null;
	}

	/** Aplica uma maldição específica, sem sorteio nem imunidade (comando /curse give). */
	public static void give(ServerPlayer player, Curse curse) {
		DebuffSystem.clear(player, SOURCE);
		LEFT.remove(player.getUUID());
		player.setAttached(CURRENT, curse.code());
		int ticks = curse.timerMinutes() > 0.0 ? SurvivalNeedsConfig.ticks(curse.timerMinutes() * 60.0) : 0;
		player.setAttached(TICKS, ticks);

		String timer = ticks > 0 ? " Você tem " + formatMinutes(curse.timerMinutes()) + "." : "";
		player.sendSystemMessage(Component.literal("§5Você foi amaldiçoado: " + curse.displayName + ". §r" + curse.description + timer), false);
		player.level().playSound(null, player.blockPosition(),
			net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.6f);
	}

	/** Tira a maldição. {@code immune}: dá o tempo de imunidade da configuração (cura de verdade). */
	public static void clear(ServerPlayer player, boolean immune) {
		LEFT.remove(player.getUUID());
		player.setAttached(CURRENT, 0);
		player.setAttached(TICKS, 0);
		if (immune) {
			player.setAttached(IMMUNE, SurvivalNeedsConfig.ticks(cfg().immunityMinutes * 60.0));
		} else {
			player.setAttached(IMMUNE, 0); // comando /curse clear: também tira a imunidade, para poder testar de novo
		}
		DebuffSystem.clear(player, SOURCE);
	}

	private static String formatMinutes(double minutes) {
		long rounded = Math.round(minutes);
		return rounded + (rounded == 1 ? " minuto" : " minutos");
	}

	public static void forget(UUID player) {
		LEFT.remove(player);
	}

	// ---- tick ----

	public static void tick(ServerPlayer player) {
		int immune = player.getAttachedOrElse(IMMUNE, 0);
		if (immune > 0) {
			player.setAttached(IMMUNE, immune - 1);
		}

		Curse curse = current(player);
		if (curse == null || ignores(player) || player.isDeadOrDying()) {
			DebuffSystem.clear(player, SOURCE);
			return;
		}

		DebuffSystem.apply(player, SOURCE, effectsOf(curse));
		if (curse == Curse.PARCHED) {
			player.causeFoodExhaustion((float) cfg().parchedHungerPerSecond / 20.0f);
		}
		tickTimer(player, curse);
	}

	private static List<DebuffEffect> effectsOf(Curse curse) {
		SurvivalNeedsConfig.Curse cfg = cfg();
		return switch (curse) {
			case NO_SPRINT -> List.of(new DebuffEffect.SprintBlock());
			case BUILD_LIMIT -> List.of(new DebuffEffect.BuildLimit(
				cfg.buildLimit, SurvivalNeedsConfig.ticks(cfg.buildResetSeconds), "Maldição: suas mãos tremem."));
			case FRAGILE -> List.of(new DebuffEffect.MaxHealthReduction(cfg.fragileHealthLoss));
			case HALF_LIFE -> List.of(new DebuffEffect.MaxHealthReduction(cfg.halfLifeHealthLoss));
			case HEAVY_BODY -> List.of(new DebuffEffect.Slowdown(cfg.heavyBodySlowdown));
			case BLIND_HANDS -> List.of(new DebuffEffect.MiningSlowdown(cfg.blindHandsSlowdown));
			default -> List.of(); // as demais agem por eventos, mixins ou contador
		};
	}

	private static void tickTimer(ServerPlayer player, Curse curse) {
		UUID id = player.getUUID();
		int stored = player.getAttachedOrElse(TICKS, 0);

		if (stored == DROWNING) {
			tickDrowning(player);
			return;
		}
		if (stored <= 0) {
			LEFT.remove(id);
			return;
		}

		// A contagem exata fica na memória; só a cada segundo ela é salva e enviada ao cliente
		int ticks = LEFT.getOrDefault(id, stored) - 1;
		if (ticks <= 0) {
			LEFT.remove(id);
			finish(player, curse);
			return;
		}
		LEFT.put(id, ticks);
		if (ticks % SYNC_INTERVAL_TICKS == 0) {
			player.setAttached(TICKS, ticks);
		}
		for (int minutes : cfg().warnMinutes) {
			if (ticks == minutes * 60 * 20) {
				player.sendSystemMessage(Component.literal("§5Maldição (" + curse.displayName + "): faltam "
					+ formatMinutes(minutes) + "."), true);
			}
		}
	}

	/** O contador chegou a zero: dispara o efeito final. */
	private static void finish(ServerPlayer player, Curse curse) {
		ServerLevel level = player.level();
		switch (curse) {
			case DOOM -> {
				player.sendSystemMessage(Component.literal("§4A maldição cobrou a sua vida."), false);
				clear(player, false);
				player.kill(level);
			}
			case EXPLOSION -> {
				player.sendSystemMessage(Component.literal("§4A maldição explodiu!"), false);
				clear(player, true);
				level.explode(null, player.getX(), player.getY(), player.getZ(), cfg().explosionPower,
					cfg().explosionBreaksBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
			}
			case DROWNING -> {
				player.setAttached(TICKS, DROWNING);
				player.sendSystemMessage(Component.literal("§4Você está se afogando! Só a cura salva você."), false);
			}
			default -> player.setAttached(TICKS, 0);
		}
	}

	/** Afogamento em curso: falta ar mesmo fora da água, e o dano se repete até a cura ou a morte. */
	private static void tickDrowning(ServerPlayer player) {
		player.setAirSupply(0);
		int interval = SurvivalNeedsConfig.ticks(cfg().drowningIntervalSeconds);
		if (player.tickCount % interval == 0) {
			player.hurtServer(player.level(), player.level().damageSources().drown(), (float) cfg().drowningDamage);
		}
	}
}
