package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;

/**
 * Frio. Em qualquer dos casos o jogador não corre (ver SprintLock) e o ícone aparece.
 * <ul>
 *   <li><b>Bioma de neve</b>, sem nenhuma peça de armadura: dano forte, meio coração por segundo.</li>
 *   <li><b>Noite ao relento</b>: depois de muito tempo (10 min) à noite, com o céu totalmente aberto e sem
 *       armadura, o frio chega, mas com dano bem leve e lento.</li>
 * </ul>
 */
public final class ColdSystem {
	private static final int CHECK_INTERVAL_TICKS = 20;

	// Bioma de neve
	private static final float BIOME_DAMAGE = 1.0f;          // meio coração, a cada verificação (1 s)

	// Noite ao relento
	private static final int NIGHT_EXPOSURE_TICKS = 10 * 60 * 20; // 10 minutos de exposição
	private static final int NIGHT_SHELTER_RECOVERY = 2;     // abrigado/de armadura, a exposição some 2x mais rápido
	private static final float NIGHT_DAMAGE = 1.0f;          // meio coração...
	private static final int NIGHT_DAMAGE_INTERVAL_TICKS = 200; // ...a cada 10 segundos
	private static final float NIGHT_MIN_HEALTH = 2.0f;      // o frio da noite nunca mata (deixa 1 coração)

	private static final Set<UUID> FREEZING = new HashSet<>();
	private static final Set<UUID> FORCED = new HashSet<>();           // ativado por comando
	private static final Map<UUID, Integer> NIGHT_EXPOSURE = new HashMap<>(); // ticks de exposição acumulada

	private ColdSystem() {}

	public static boolean isFreezing(ServerPlayer player) {
		return FREEZING.contains(player.getUUID());
	}

	/** Liga ou desliga o frio à força (comando /cold), ignorando bioma, noite e armadura. */
	public static void setForced(ServerPlayer player, boolean forced) {
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
	}

	private static void refresh(ServerPlayer player) {
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
				player.hurtServer(player.level(), player.level().damageSources().freeze(), BIOME_DAMAGE);
			} else if (player.tickCount % NIGHT_DAMAGE_INTERVAL_TICKS == 0
				&& player.getHealth() > NIGHT_MIN_HEALTH) {
				player.hurtServer(player.level(), player.level().damageSources().freeze(), NIGHT_DAMAGE);
			}

			if (!wasCold) {
				String message = biomeCold || forced
					? "Você está com frio! Vista uma armadura."
					: "O frio da noite te alcançou! Vista uma armadura ou procure abrigo.";
				player.sendSystemMessage(Component.literal(message), true);
			}
		} else {
			FREEZING.remove(id);
		}
	}

	/**
	 * Acumula a exposição ao frio da noite e diz se o frio já chegou.
	 * Só conta à noite, no Overworld, com o céu totalmente aberto sobre a cabeça e sem armadura.
	 * De dia a exposição fica parada; à noite, abrigado ou de armadura, ela diminui.
	 */
	private static boolean updateNightExposure(ServerPlayer player, boolean unarmored) {
		UUID id = player.getUUID();
		ServerLevel level = player.level();
		if (level.dimension() != Level.OVERWORLD || !level.isDarkOutside()) {
			return false; // de dia (ou em outra dimensão) a contagem fica parada
		}

		boolean openSky = level.canSeeSky(BlockPos.containing(player.getEyePosition()));
		int exposure = NIGHT_EXPOSURE.getOrDefault(id, 0);
		if (openSky && unarmored) {
			exposure = Math.min(NIGHT_EXPOSURE_TICKS, exposure + CHECK_INTERVAL_TICKS);
		} else {
			exposure = Math.max(0, exposure - CHECK_INTERVAL_TICKS * NIGHT_SHELTER_RECOVERY);
		}
		NIGHT_EXPOSURE.put(id, exposure);

		return openSky && unarmored && exposure >= NIGHT_EXPOSURE_TICKS;
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
