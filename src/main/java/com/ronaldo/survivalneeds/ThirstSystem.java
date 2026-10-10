package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Sede: 20 pontos como a fome. Com poucos pontos o jogador não corre; com 0 sofre dano (sem morrer).
 * O valor é salvo no jogador e sincronizado com o cliente para o HUD. Valores em {@code thirst} na configuração.
 */
public final class ThirstSystem {
	public static final int MAX = 20;
	private static final String SOURCE = "thirst";

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

	private static SurvivalNeedsConfig.Thirst cfg() {
		return SurvivalNeedsConfig.get().thirst;
	}

	public static int get(Player player) {
		return player.getAttachedOrElse(THIRST, MAX);
	}

	public static boolean isTooThirstyToSprint(Player player) {
		return !player.isSpectator() && get(player) <= cfg().noSprintAtOrBelow;
	}

	/** Liga a sede (comando /thirst): deixa a barra no limite do efeito. Desligar enche a barra. */
	public static void setActive(ServerPlayer player, boolean active) {
		int target = active ? cfg().noSprintAtOrBelow : MAX;
		DebuffXp.abort(player, DebuffXp.Type.THIRST); // comando não dá XP
		add(player, target - get(player));
		DebuffXp.abort(player, DebuffXp.Type.THIRST);
	}

	/** Soma (ou subtrai) pontos de sede, limitado entre 0 e MAX. Só no servidor. */
	public static void add(ServerPlayer player, int amount) {
		int old = get(player);
		int now = Math.max(0, Math.min(MAX, old + amount));
		if (now == old) {
			return;
		}
		player.setAttached(THIRST, now);
		int limit = cfg().noSprintAtOrBelow;
		if (old > limit && now <= limit) {
			DebuffXp.begin(player, DebuffXp.Type.THIRST);
			player.sendSystemMessage(Component.literal("Você está com sede demais para correr!"), true);
		} else if (old <= limit && now > limit) {
			DebuffXp.recover(player, DebuffXp.Type.THIRST);
		}
	}

	/** Beber água suja (garrafa de água comum) intoxica: dá náusea e tira vida, sem matar. */
	private static void checkDirtyWater(ServerPlayer player) {
		if (!player.isUsingItem() || player.getUseItemRemainingTicks() != 1
			|| !WaterPurifierBlock.isDirtyWaterBottle(player.getUseItem())) {
			return;
		}
		SurvivalNeedsConfig.Thirst cfg = cfg();
		float damage = Math.min((float) cfg.dirtyWaterDamage, player.getHealth() - 1f);
		if (damage > 0f) {
			player.hurtServer(player.level(), player.level().damageSources().magic(), damage);
		}
		player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SurvivalNeedsConfig.ticks(cfg.dirtyWaterNauseaSeconds)));
		player.sendSystemMessage(Component.literal("Você bebeu água suja e passou mal! Purifique a água antes."), true);
	}

	public static void tick(ServerPlayer player) {
		// Sede baixa: não corre (vale também em criativo, para poder testar com /thirst)
		DebuffSystem.set(player, SOURCE, isTooThirstyToSprint(player), () -> List.of(new DebuffEffect.SprintBlock()));

		if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) {
			return;
		}

		checkDirtyWater(player);
		SurvivalNeedsConfig.Thirst cfg = cfg();

		// Gasto por tick: parado, e mais ainda correndo (e quanto mais tempo correndo, mais rápido)
		float rate = 1f / SurvivalNeedsConfig.ticks(cfg.secondsPerPointIdle);
		if (player.isSprinting()) {
			int sprintTicks = SPRINT_TICKS.merge(player.getUUID(), 1, Integer::sum);
			float ramp = (float) (sprintTicks / (double) SurvivalNeedsConfig.ticks(cfg.sprintRampSeconds));
			float boost = (float) Math.min(cfg.sprintMaxBoost, ramp * cfg.sprintMaxBoost);
			rate += (1f / SurvivalNeedsConfig.ticks(cfg.secondsPerPointSprinting)) * (1f + boost);
		} else {
			SPRINT_TICKS.remove(player.getUUID());
		}
		float drain = DRAIN.merge(player.getUUID(), rate, Float::sum);
		if (drain >= 1f) {
			DRAIN.put(player.getUUID(), drain - 1f);
			add(player, -1);
		}

		int damageInterval = SurvivalNeedsConfig.ticks(cfg.dryDamageIntervalSeconds);
		if (get(player) <= 0 && player.tickCount % damageInterval == 0 && player.getHealth() > cfg.dryMinHealth) {
			player.hurtServer(player.level(), player.level().damageSources().dryOut(), (float) cfg.dryDamage);
		}
	}
}
