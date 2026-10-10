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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * Fome. A comida do jogo é a "barra de carne": 10 ícones, cada um com 2 pontos internos.
 * Valores em {@code hunger} na configuração.
 * <ul>
 *   <li><b>Cura:</b> enquanto houver carne e o jogador estiver ferido, ele se cura. Cada ponto de comida gasto
 *       cura 1 ponto de vida (1 ícone de carne = 1 coração). A carne também continua caindo normalmente com as
 *       ações. A cura natural e a inanição do jogo foram desligadas (ver FoodDataMixin).</li>
 *   <li><b>Faminto:</b> ao zerar a carne o jogador recebe, pelo {@link DebuffSystem}, lentidão, bloqueio de corrida
 *       e limite de construção. Começa uma contagem: se não comer até a metade da barra, ele morre.</li>
 * </ul>
 */
public final class HungerSystem {
	private static final String SOURCE = "hunger";
	private static final int SYNC_INTERVAL_TICKS = 20;     // o contador é salvo e enviado ao cliente a cada segundo

	/** Ticks que faltam para morrer de fome (0 = não está faminto). Salvo no jogador e enviado ao cliente. */
	public static final AttachmentType<Integer> TICKS_LEFT = AttachmentRegistry.<Integer>create(
		SurvivalNeeds.id("starvation_ticks"),
		builder -> builder
			.persistent(Codec.INT)
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	private static final Map<UUID, Integer> LEFT = new HashMap<>();         // contagem exata, em memória
	private static final Map<UUID, Integer> HEAL_COUNTER = new HashMap<>();
	private static final Set<UUID> FORCED = new HashSet<>(); // fome ligada por comando que ainda não começou

	private HungerSystem() {}

	public static void init() {
		// força o carregamento da classe (registra o attachment)
	}

	private static SurvivalNeedsConfig.Hunger cfg() {
		return SurvivalNeedsConfig.get().hunger;
	}

	public static boolean isStarving(ServerPlayer player) {
		return LEFT.getOrDefault(player.getUUID(), 0) > 0;
	}

	/** Liga (carne zerada, começa a contagem) ou desliga (barra cheia) a fome: comando /hunger. */
	public static void setActive(ServerPlayer player, boolean active) {
		FoodData food = player.getFoodData();
		FORCED.remove(player.getUUID());
		if (active) {
			FORCED.add(player.getUUID()); // a fome por comando não dá XP ao passar
			food.setFoodLevel(0);
		} else {
			food.setFoodLevel(20);
			stopStarving(player, false);
		}
	}

	public static void tick(ServerPlayer player) {
		UUID id = player.getUUID();
		if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) {
			if (LEFT.getOrDefault(id, player.getAttachedOrElse(TICKS_LEFT, 0)) > 0) {
				stopStarving(player, false);
			}
			return;
		}

		SurvivalNeedsConfig.Hunger cfg = cfg();
		FoodData food = player.getFoodData();
		int left = LEFT.computeIfAbsent(id, key -> player.getAttachedOrElse(TICKS_LEFT, 0));

		if (left == 0 && food.getFoodLevel() == 0) {
			left = SurvivalNeedsConfig.ticks(cfg.starveSeconds);
			LEFT.put(id, left);
			player.setAttached(TICKS_LEFT, left);
			if (!FORCED.remove(id)) {
				DebuffXp.begin(player, DebuffXp.Type.HUNGER);
			}
			player.sendSystemMessage(Component.literal("Você está faminto! Coma até a metade da barra ou morrerá em "
				+ Math.round(cfg.starveSeconds / 60.0) + " minutos."), true);
		} else if (left > 0 && food.getFoodLevel() >= cfg.recoverFoodLevel) {
			stopStarving(player, true);
			left = 0;
		} else if (left > 0) {
			left--;
			LEFT.put(id, left);
			if (left <= 0) {
				// Acabou o tempo: morre de fome. Se sobreviver (totem), com a carne zerada a contagem recomeça.
				player.setAttached(TICKS_LEFT, 0);
				player.hurtServer(player.level(), player.level().damageSources().starve(), 1_000_000f);
				return;
			}
			if (left % SYNC_INTERVAL_TICKS == 0) {
				player.setAttached(TICKS_LEFT, left);
			}
		}

		// Faminto: mais lento, não corre e só constrói um pouco
		DebuffSystem.set(player, SOURCE, left > 0, HungerSystem::debuffs);
		healFromFood(player, food);
	}

	/** Os efeitos que a fome pede ao DebuffSystem. */
	private static List<DebuffEffect> debuffs() {
		SurvivalNeedsConfig.Hunger cfg = cfg();
		return List.of(
			new DebuffEffect.Slowdown(cfg.slowdown),
			new DebuffEffect.SprintBlock(),
			new DebuffEffect.BuildLimit(
				cfg.placeLimit,
				SurvivalNeedsConfig.ticks(cfg.placeResetSeconds),
				"Com fome: você não tem forças."));
	}

	/** Cura um "passo" de vida por intervalo, gastando 1 ponto de comida, até a comida acabar. */
	private static void healFromFood(ServerPlayer player, FoodData food) {
		SurvivalNeedsConfig.Hunger cfg = cfg();
		UUID id = player.getUUID();
		if (WetSystem.isWet(player) || food.getFoodLevel() <= 0 || player.getHealth() >= player.getMaxHealth()) {
			HEAL_COUNTER.remove(id);
			return;
		}
		int counter = HEAL_COUNTER.merge(id, 1, Integer::sum);
		if (counter >= SurvivalNeedsConfig.ticks(cfg.healIntervalSeconds)) {
			HEAL_COUNTER.remove(id);
			player.heal((float) cfg.healAmount);
			food.setFoodLevel(food.getFoodLevel() - 1);
		}
	}

	private static void stopStarving(ServerPlayer player, boolean notify) {
		UUID id = player.getUUID();
		LEFT.put(id, 0);
		HEAL_COUNTER.remove(id);
		player.setAttached(TICKS_LEFT, 0);
		DebuffSystem.clear(player, SOURCE);
		if (notify) {
			DebuffXp.recover(player, DebuffXp.Type.HUNGER);
		} else {
			DebuffXp.abort(player, DebuffXp.Type.HUNGER);
		}
		if (notify) {
			player.sendSystemMessage(Component.literal("Você se alimentou e a fome passou."), true);
		}
	}
}
