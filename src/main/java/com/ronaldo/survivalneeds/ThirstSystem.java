package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Sede: 20 pontos como a fome. Com 5 ou menos o jogador não corre; com 0 sofre dano (sem morrer).
 * O valor é salvo no jogador e sincronizado com o cliente para o HUD.
 */
public final class ThirstSystem {
	public static final int MAX = 20;
	public static final int NO_SPRINT_AT_OR_BELOW = 5;

	private static final float BASE_DRAIN_PER_TICK = 1f / 2500f;   // ~1 ponto a cada 2 minutos parado
	private static final float SPRINT_DRAIN_PER_TICK = 1f / 800f;  // corrida seca mais rápido...
	private static final int SPRINT_RAMP_TICKS = 400;              // ...e quanto mais tempo correndo, mais rápido
	private static final float SPRINT_MAX_BOOST = 2f;              // até 3x o gasto da corrida (1 + 2)
	private static final int DRY_DAMAGE_INTERVAL_TICKS = 80;

	// Água suja (garrafa de água comum): intoxica
	private static final float DIRTY_WATER_DAMAGE = 6.0f; // 3 corações
	private static final int DIRTY_WATER_NAUSEA_TICKS = 200; // 10 segundos

	public static final AttachmentType<Integer> THIRST = AttachmentRegistry.<Integer>create(
		SurvivalNeeds.id("thirst"),
		builder -> builder
			.persistent(Codec.INT)
			.initializer(() -> MAX)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	private static final Map<UUID, Float> DRAIN = new HashMap<>();
	private static final Map<UUID, Integer> SPRINT_TICKS = new HashMap<>(); // há quanto tempo está correndo

	private ThirstSystem() {}

	public static void init() {
		// força o carregamento da classe (registra o attachment)
	}

	public static int get(Player player) {
		return player.getAttachedOrElse(THIRST, MAX);
	}

	public static boolean isTooThirstyToSprint(Player player) {
		return !player.isSpectator() && get(player) <= NO_SPRINT_AT_OR_BELOW;
	}

	/** Liga a sede (comando /thirst): deixa a barra no limite do efeito. Desligar enche a barra. */
	public static void setActive(ServerPlayer player, boolean active) {
		int target = active ? NO_SPRINT_AT_OR_BELOW : MAX;
		add(player, target - get(player));
	}

	/** Soma (ou subtrai) pontos de sede, limitado entre 0 e MAX. Só no servidor. */
	public static void add(ServerPlayer player, int amount) {
		int old = get(player);
		int now = Math.max(0, Math.min(MAX, old + amount));
		if (now == old) {
			return;
		}
		player.setAttached(THIRST, now);
		if (old > NO_SPRINT_AT_OR_BELOW && now <= NO_SPRINT_AT_OR_BELOW) {
			player.sendSystemMessage(Component.literal("Você está com sede demais para correr!"), true);
		}
	}

	/** Beber água suja (garrafa de água comum) intoxica: dá náusea e tira vida, sem matar. */
	private static void checkDirtyWater(ServerPlayer player) {
		if (!player.isUsingItem() || player.getUseItemRemainingTicks() != 1
			|| !WaterPurifierBlock.isDirtyWaterBottle(player.getUseItem())) {
			return;
		}
		float damage = Math.min(DIRTY_WATER_DAMAGE, player.getHealth() - 1f);
		if (damage > 0f) {
			player.hurtServer(player.level(), player.level().damageSources().magic(), damage);
		}
		player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, DIRTY_WATER_NAUSEA_TICKS));
		player.sendSystemMessage(Component.literal("Você bebeu água suja e passou mal! Purifique a água antes."), true);
	}

	public static void tick(ServerPlayer player) {
		if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) {
			return;
		}

		checkDirtyWater(player);

		float rate = BASE_DRAIN_PER_TICK;
		if (player.isSprinting()) {
			int sprintTicks = SPRINT_TICKS.merge(player.getUUID(), 1, Integer::sum);
			float boost = Math.min(SPRINT_MAX_BOOST, (float) sprintTicks / SPRINT_RAMP_TICKS * SPRINT_MAX_BOOST);
			rate += SPRINT_DRAIN_PER_TICK * (1f + boost);
		} else {
			SPRINT_TICKS.remove(player.getUUID());
		}
		float drain = DRAIN.merge(player.getUUID(), rate, Float::sum);
		if (drain >= 1f) {
			DRAIN.put(player.getUUID(), drain - 1f);
			add(player, -1);
		}

		if (get(player) <= 0 && player.tickCount % DRY_DAMAGE_INTERVAL_TICKS == 0 && player.getHealth() > 2f) {
			player.hurtServer(player.level(), player.level().damageSources().dryOut(), 1.0f);
		}
	}
}
