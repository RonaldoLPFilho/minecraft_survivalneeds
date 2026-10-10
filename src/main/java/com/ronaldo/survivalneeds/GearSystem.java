package com.ronaldo.survivalneeds;

import java.util.ArrayList;
import java.util.List;

import com.ronaldo.survivalneeds.debuff.DebuffEffect;
import com.ronaldo.survivalneeds.debuff.DebuffSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Equipamento forte demais para o nível do jogador. A cada tick olha a mão principal e as quatro peças de armadura
 * e pede ao {@link DebuffSystem} (fonte {@code "gear"}) as penalidades que valem:
 * arma na mão reduz o dano, ferramenta reduz a mineração e a armadura vestida protege menos.
 */
public final class GearSystem {
	private static final String SOURCE = "gear";
	private static final EquipmentSlot[] ARMOR_SLOTS = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

	private GearSystem() {}

	public static void tick(ServerPlayer player) {
		List<DebuffEffect> effects = new ArrayList<>();
		if (!EquipmentGate.ignores(player)) {
			addHeldItem(player, effects);
			addArmor(player, effects);
		}
		DebuffSystem.set(player, SOURCE, !effects.isEmpty(), () -> effects);
	}

	private static void addHeldItem(ServerPlayer player, List<DebuffEffect> effects) {
		ItemStack held = player.getMainHandItem();
		EquipmentGate.Kind kind = EquipmentGate.kindOf(held);
		double penalty = EquipmentGate.penalty(player, held);
		if (penalty <= 0.0) {
			return;
		}
		if (kind == EquipmentGate.Kind.WEAPON || kind == EquipmentGate.Kind.WEAPON_TOOL) {
			effects.add(new DebuffEffect.DamageReduction(penalty));
		}
		if (kind == EquipmentGate.Kind.TOOL || kind == EquipmentGate.Kind.WEAPON_TOOL) {
			effects.add(new DebuffEffect.MiningSlowdown(penalty));
		}
	}

	/**
	 * A proteção é um atributo só do jogador, não de cada peça. A penalidade de cada peça entra pesada pela
	 * armadura que ela dá: uma bota fraca de diamante pesa menos que um peitoral.
	 */
	private static void addArmor(ServerPlayer player, List<DebuffEffect> effects) {
		double total = 0.0;
		double lost = 0.0;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			double armor = armorValue(stack, slot);
			total += armor;
			lost += armor * EquipmentGate.penalty(player, stack);
		}
		if (total > 0.0 && lost > 0.0) {
			effects.add(new DebuffEffect.ArmorReduction(lost / total));
		}
	}

	private static double armorValue(ItemStack stack, EquipmentSlot slot) {
		if (stack.isEmpty()) {
			return 0.0;
		}
		ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		double[] value = {0.0};
		modifiers.forEach(slot, (attribute, modifier) -> {
			if (attribute.equals(Attributes.ARMOR) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				value[0] += modifier.amount();
			}
		});
		return value[0];
	}
}
