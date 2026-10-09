package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import com.ronaldo.survivalneeds.debuff.DebuffEffect;
import com.ronaldo.survivalneeds.debuff.DebuffSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Molhado. A chuva e entrar na água molham o jogador. Valores em {@code wet} na configuração.
 * Enquanto molhado o jogador recebe, pelo {@link DebuffSystem}:
 * <ul>
 *   <li>limite de corrida: corre por um tempo e depois descansa;</li>
 *   <li>limite de construção: só coloca alguns blocos seguidos e depois espera.</li>
 * </ul>
 * E também: a vida não regenera (ver HungerSystem) e não consegue dormir (ver SurvivalNeeds).
 * <p>
 * Para secar: tempo sob um teto ou, mais longo, ao relento, e mais rápido perto de uma fonte de calor.
 * A contagem só corre quando não está chovendo sobre o jogador nem ele está na água.
 */
public final class WetSystem {
	private static final String SOURCE = "wet";
	private static final int CHECK_INTERVAL_TICKS = 20;

	private static final Set<UUID> WET = new HashSet<>();
	private static final Set<UUID> FORCED = new HashSet<>();            // ativado por comando (não seca sozinho)
	private static final Set<UUID> HEAT = new HashSet<>();              // molhado e perto de uma fonte de calor
	private static final Set<UUID> DRYING = new HashSet<>();            // a contagem para secar está correndo
	private static final Set<UUID> SHELTERED = new HashSet<>();         // secando em lugar coberto (senão, ao relento)
	private static final Map<UUID, Float> DRY_PROGRESS = new HashMap<>(); // de 0 (molhado) a 1 (seco)

	private static final Set<Block> HEAT_BLOCKS = Set.of(
		Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
		Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH, Blocks.LANTERN, Blocks.SOUL_LANTERN,
		Blocks.MAGMA_BLOCK);
	private static final Set<Block> LIT_BLOCKS = Set.of(Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER);

	private WetSystem() {}

	private static SurvivalNeedsConfig.Wet cfg() {
		return SurvivalNeedsConfig.get().wet;
	}

	public static boolean isWet(ServerPlayer player) {
		return WET.contains(player.getUUID());
	}

	/** Liga (molhado, sem secar sozinho) ou desliga (seca na hora) o estado molhado: comando /wet. */
	public static void setForced(ServerPlayer player, boolean forced) {
		if (forced) {
			FORCED.add(player.getUUID());
			becomeWet(player);
		} else {
			FORCED.remove(player.getUUID());
			dry(player, true);
		}
	}

	public static void tick(ServerPlayer player) {
		if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) {
			if (WET.contains(player.getUUID()) && !FORCED.contains(player.getUUID())) {
				dry(player, false);
			}
			return;
		}
		if (player.isInWater()) {
			// entrar na água molha na hora, a cada tick, para um mergulho rápido também contar
			becomeWet(player);
			DRY_PROGRESS.put(player.getUUID(), 0f);
		}
		tickWetness(player);
		if (player.tickCount % CHECK_INTERVAL_TICKS == 0) {
			updateHeat(player);
		}

		DebuffSystem.set(player, SOURCE, isWet(player), WetSystem::debuffs);
	}

	/** Os efeitos que o molhado pede ao DebuffSystem. */
	private static List<DebuffEffect> debuffs() {
		SurvivalNeedsConfig.Wet cfg = cfg();
		return List.of(
			new DebuffEffect.SprintLimit(
				SurvivalNeedsConfig.ticks(cfg.sprintLimitSeconds),
				SurvivalNeedsConfig.ticks(cfg.sprintCooldownSeconds),
				SurvivalNeedsConfig.ticks(cfg.sprintGraceSeconds),
				"Molhado e sem fôlego! Você não pode correr por " + Math.round(cfg.sprintCooldownSeconds) + " segundos.",
				"Você recuperou o fôlego e pode correr de novo."),
			new DebuffEffect.BuildLimit(
				cfg.placeLimit,
				SurvivalNeedsConfig.ticks(cfg.placeResetSeconds),
				"Molhado: suas mãos escorregam."));
	}

	/** Verdadeiro se o jogador molhado está perto de uma fonte de calor (o fogo acelera a secagem). */
	public static boolean isNearHeat(ServerPlayer player) {
		return HEAT.contains(player.getUUID());
	}

	/**
	 * Segundos que faltam para secar nas condições de agora (coberto ou ao relento, cada um com seu tempo).
	 * Perto do fogo a contagem desce mais rápido, e o contador acompanha.
	 * Devolve 0 quando a contagem não está correndo (na chuva, na água ou sem estar molhado).
	 */
	public static int secondsToDry(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!DRYING.contains(id)) {
			return 0;
		}
		SurvivalNeedsConfig.Wet cfg = cfg();
		double fullSeconds = SHELTERED.contains(id) ? cfg.coveredDrySeconds : cfg.openAirDrySeconds;
		double remaining = (1.0 - DRY_PROGRESS.getOrDefault(id, 0f)) * fullSeconds;
		return Math.max(1, (int) Math.ceil(remaining));
	}

	// ---- molhar e secar ----

	/** Roda a cada tick: a chuva molha e reinicia a contagem; sem chuva e fora da água, a contagem corre. */
	private static void tickWetness(ServerPlayer player) {
		UUID id = player.getUUID();
		ServerLevel level = player.level();
		BlockPos head = BlockPos.containing(player.getEyePosition());

		if (level.isRainingAt(head)) {
			becomeWet(player);
			DRY_PROGRESS.put(id, 0f); // a chuva reinicia a contagem para secar
			DRYING.remove(id);
			return;
		}
		if (!WET.contains(id) || FORCED.contains(id) || player.isInWater()) {
			DRYING.remove(id);
			return;
		}

		// A contagem corre sempre que não chove sobre o jogador nem ele está na água:
		// em lugar coberto (qualquer bloco sobre a cabeça) seca mais rápido que ao relento.
		SurvivalNeedsConfig.Wet cfg = cfg();
		boolean covered = !level.canSeeSky(head);
		DRYING.add(id);
		if (covered) {
			SHELTERED.add(id);
		} else {
			SHELTERED.remove(id);
		}
		float rate = 1f / SurvivalNeedsConfig.ticks(covered ? cfg.coveredDrySeconds : cfg.openAirDrySeconds);
		if (HEAT.contains(id)) {
			rate *= (float) cfg.heatDryMultiplier;
		}
		float progress = DRY_PROGRESS.getOrDefault(id, 0f) + rate;
		DRY_PROGRESS.put(id, progress);
		if (progress >= 1f) {
			dry(player, true);
		}
	}

	/** Roda uma vez por segundo: procura fonte de calor por perto (só para quem está molhado). */
	private static void updateHeat(ServerPlayer player) {
		UUID id = player.getUUID();
		if (WET.contains(id) && scanForHeat(player.level(), player.blockPosition())) {
			HEAT.add(id);
		} else {
			HEAT.remove(id);
		}
	}

	private static void becomeWet(ServerPlayer player) {
		if (WET.add(player.getUUID())) {
			player.sendSystemMessage(Component.literal("Você está molhado! Procure um lugar coberto para secar."), true);
		}
	}

	private static void dry(ServerPlayer player, boolean notify) {
		UUID id = player.getUUID();
		boolean wasWet = WET.remove(id);
		HEAT.remove(id);
		DRYING.remove(id);
		SHELTERED.remove(id);
		DRY_PROGRESS.remove(id);
		DebuffSystem.clear(player, SOURCE);
		if (wasWet && notify) {
			player.sendSystemMessage(Component.literal("Você secou."), true);
		}
	}

	private static boolean scanForHeat(ServerLevel level, BlockPos center) {
		SurvivalNeedsConfig.Wet cfg = cfg();
		for (BlockPos pos : BlockPos.betweenClosed(
				center.offset(-cfg.heatRadius, -cfg.heatBelow, -cfg.heatRadius),
				center.offset(cfg.heatRadius, cfg.heatAbove, cfg.heatRadius))) {
			if (isHeatSource(level.getBlockState(pos))) {
				return true;
			}
		}
		return false;
	}

	/** Fogo, lava, tochas, lanternas, fogueiras e velas acesas, fornalhas acesas e blocos de magma. */
	private static boolean isHeatSource(BlockState state) {
		if (state.is(BlockTags.FIRE) || HEAT_BLOCKS.contains(state.getBlock())
			|| state.getFluidState().is(FluidTags.LAVA)) {
			return true;
		}
		boolean canBeLit = state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.CANDLES)
			|| LIT_BLOCKS.contains(state.getBlock());
		return canBeLit && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
	}
}
