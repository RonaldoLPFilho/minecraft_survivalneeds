package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Regras de nível do equipamento: qual nível cada peça pede e quanto ela perde de rendimento para quem não o tem.
 * <pre>
 *   déficit    = nível pedido - nível do jogador          (se menor ou igual a 0, não há penalidade)
 *   penalidade = mínimo(maxPenalty, penaltyStrength x déficit / nível pedido)
 * </pre>
 * Quem usa: {@link GearSystem} (aplica as penalidades), os mixins que barram vestir armadura e a dica do item.
 */
public final class EquipmentGate {
	private static final long WARN_INTERVAL_TICKS = 20;
	private static final Map<UUID, Long> LAST_WARNING = new HashMap<>();

	/** O que a peça é, para saber o que a penalidade reduz. */
	public enum Kind {
		NONE,       // não pede nível
		ARMOR,      // capacete, peitoral, calça, bota (e elitra): reduz a proteção; não pode ser vestida sem o nível
		WEAPON,     // espada, lança, maça, tridente: reduz o dano
		TOOL,       // picareta, pá, enxada: reduz a velocidade de mineração
		WEAPON_TOOL // machado: reduz as duas coisas
	}

	private EquipmentGate() {}

	private static SurvivalNeedsConfig.Progression config() {
		return SurvivalNeedsConfig.get().progression;
	}

	/** Criativo e espectador ignoram todas as regras, e a seção pode ser desligada na configuração. */
	public static boolean ignores(Player player) {
		return !config().enabled || player.isCreative() || player.isSpectator();
	}

	public static Kind kindOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return Kind.NONE;
		}
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null) {
			EquipmentSlot slot = equippable.slot();
			if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
				return Kind.ARMOR;
			}
		}
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (path.endsWith("_axe")) {
			return Kind.WEAPON_TOOL;
		}
		if (path.endsWith("_sword") || path.endsWith("_spear") || path.equals("mace") || path.equals("trident")) {
			return Kind.WEAPON;
		}
		if (path.endsWith("_pickaxe") || path.endsWith("_shovel") || path.endsWith("_hoe")) {
			return Kind.TOOL;
		}
		// Item de outro mod com nível configurado à mão, mas que não dá para classificar: vale como arma e ferramenta
		if (config().equipment.itemOverrides.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
			return Kind.WEAPON_TOOL;
		}
		return Kind.NONE;
	}

	/** Nível que a peça pede (0 se não pede nenhum). */
	public static int requiredLevel(ItemStack stack) {
		if (kindOf(stack) == Kind.NONE) {
			return 0;
		}
		SurvivalNeedsConfig.Equipment equipment = config().equipment;
		var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Integer override = equipment.itemOverrides.get(key.toString());
		if (override != null) {
			return Math.max(0, override);
		}
		return Math.max(0, equipment.requirements.getOrDefault(materialOf(key.getPath()), 0));
	}

	/** O material pelo nome do item: diamond_sword vira diamond, golden_pickaxe vira gold, mace vira mace. */
	private static String materialOf(String path) {
		if (path.equals("trident") || path.equals("mace") || path.equals("elytra")) {
			return path;
		}
		int underscore = path.indexOf('_');
		String prefix = underscore < 0 ? path : path.substring(0, underscore);
		return switch (prefix) {
			case "wooden" -> "wood";
			case "golden" -> "gold";
			default -> prefix;
		};
	}

	/** Fração (de 0 a maxPenalty) que a peça perde para quem tem este nível. */
	public static double penalty(int required, int level) {
		int deficit = required - level;
		if (required <= 0 || deficit <= 0) {
			return 0.0;
		}
		SurvivalNeedsConfig.Equipment equipment = config().equipment;
		return Math.min(equipment.maxPenalty, equipment.penaltyStrength * deficit / required);
	}

	public static double penalty(Player player, ItemStack stack) {
		return ignores(player) ? 0.0 : penalty(requiredLevel(stack), player.experienceLevel);
	}

	/** Só armadura é barrada: arma e ferramenta podem ser seguradas, mas rendem menos. */
	public static boolean canEquip(Player player, ItemStack stack) {
		if (ignores(player) || kindOf(stack) != Kind.ARMOR) {
			return true;
		}
		return player.experienceLevel >= requiredLevel(stack);
	}

	/** Avisa o jogador (no máximo uma vez por segundo) que ainda não tem o nível da peça. */
	public static void warn(ServerPlayer player, ItemStack stack) {
		long now = player.level().getGameTime();
		Long last = LAST_WARNING.get(player.getUUID());
		if (last != null && now - last < WARN_INTERVAL_TICKS) {
			return;
		}
		LAST_WARNING.put(player.getUUID(), now);
		player.sendSystemMessage(Component.literal("Você precisa do nível " + requiredLevel(stack)
			+ " para vestir isto (você está no nível " + player.experienceLevel + ")."), true);
		player.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
	}

	public static void forget(UUID player) {
		LAST_WARNING.remove(player);
	}
}
