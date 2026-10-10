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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;

/**
 * Frio. Em qualquer dos casos o jogador não corre e o ícone aparece. Valores em {@code cold} na configuração.
 * <ul>
 *   <li><b>Bioma de neve</b>, sem nenhuma peça de armadura: dano forte, por segundo.</li>
 *   <li><b>Noite ao relento</b>: depois de muito tempo à noite, com o céu totalmente aberto e sem armadura,
 *       o frio chega, mas com dano bem leve e lento.</li>
 * </ul>
 */
public final class ColdSystem {
	private static final String SOURCE = "cold";
	private static final int CHECK_INTERVAL_TICKS = 20;

	private static final Set<UUID> FREEZING = new HashSet<>();
	private static final Set<UUID> FORCED = new HashSet<>();                  // ativado por comando
	private static final Map<UUID, Integer> NIGHT_EXPOSURE = new HashMap<>(); // ticks de exposição acumulada

	private ColdSystem() {}

	private static SurvivalNeedsConfig.Cold cfg() {
		return SurvivalNeedsConfig.get().cold;
	}

	public static boolean isFreezing(ServerPlayer player) {
		return FREEZING.contains(player.getUUID());
	}

	/** Liga ou desliga o frio à força (comando /cold), ignorando bioma, noite e armadura. */
	public static void setForced(ServerPlayer player, boolean forced) {
		DebuffXp.abort(player, DebuffXp.Type.COLD); // comando não dá XP, nem ao ligar nem ao desligar
		if (forced) {
			FORCED.add(player.getUUID());
		} else {
			FORCED.remove(player.getUUID());
		}
		refresh(player);
	}

	public static void tick(ServerPlayer player) {
		if (player.tickCount % CHECK_INTERVAL_TICKS == 0) {
			refresh(player);
		}
		// Com frio o jogador não corre
		DebuffSystem.set(player, SOURCE, isFreezing(player), () -> List.of(new DebuffEffect.SprintBlock()));
	}

	private static void refresh(ServerPlayer player) {
		SurvivalNeedsConfig.Cold cfg = cfg();
		UUID id = player.getUUID();
		boolean active = !player.isCreative() && !player.isSpectator() && !player.isDeadOrDying();
		boolean unarmored = !hasAnyArmor(player);

		boolean biomeCold = active && unarmored && isInColdBiome(player);
		boolean nightCold = active && updateNightExposure(player, unarmored);
		boolean forced = FORCED.contains(id);

		boolean wasCold = FREEZING.contains(id);
		if (biomeCold || nightCold || forced) {
			FREEZING.add(id);

			if (biomeCold || forced) {
				player.hurtServer(player.level(), player.level().damageSources().freeze(), (float) cfg.biomeDamage);
			} else if (player.tickCount % SurvivalNeedsConfig.ticks(cfg.nightDamageIntervalSeconds) == 0
				&& player.getHealth() > cfg.nightMinHealth) {
				player.hurtServer(player.level(), player.level().damageSources().freeze(), (float) cfg.nightDamage);
			}

			if (!wasCold) {
				if (!forced) {
					DebuffXp.begin(player, DebuffXp.Type.COLD);
				}
				String message = biomeCold || forced
					? "Você está com frio! Vista uma armadura."
					: "O frio da noite te alcançou! Vista uma armadura ou procure abrigo.";
				player.sendSystemMessage(Component.literal(message), true);
			}
		} else {
			FREEZING.remove(id);
			if (wasCold) {
				if (active) {
					DebuffXp.recover(player, DebuffXp.Type.COLD);
				} else {
					DebuffXp.abort(player, DebuffXp.Type.COLD);
				}
			}
		}
	}

	/**
	 * Acumula a exposição ao frio da noite e diz se o frio já chegou.
	 * Só conta à noite, no Overworld, com o céu totalmente aberto sobre a cabeça e sem armadura.
	 * De dia a exposição fica parada; à noite, abrigado ou de armadura, ela diminui.
	 */
	private static boolean updateNightExposure(ServerPlayer player, boolean unarmored) {
		SurvivalNeedsConfig.Cold cfg = cfg();
		UUID id = player.getUUID();
		ServerLevel level = player.level();
		if (level.dimension() != Level.OVERWORLD || !level.isDarkOutside()) {
			return false; // de dia (ou em outra dimensão) a contagem fica parada
		}

		int needed = SurvivalNeedsConfig.ticks(cfg.nightExposureSeconds);
		boolean openSky = level.canSeeSky(BlockPos.containing(player.getEyePosition()));
		int exposure = NIGHT_EXPOSURE.getOrDefault(id, 0);
		if (openSky && unarmored) {
			exposure = Math.min(needed, exposure + CHECK_INTERVAL_TICKS);
		} else {
			exposure = Math.max(0, exposure - (int) Math.round(CHECK_INTERVAL_TICKS * cfg.nightShelterRecovery));
		}
		NIGHT_EXPOSURE.put(id, exposure);

		return openSky && unarmored && exposure >= needed;
	}

	private static boolean isInColdBiome(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos pos = player.blockPosition();
		return level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel());
	}

	private static boolean hasAnyArmor(ServerPlayer player) {
		return !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
			|| !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
			|| !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
			|| !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
	}
}
