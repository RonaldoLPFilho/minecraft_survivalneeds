package com.ronaldo.survivalneeds;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Amuleto: guarda o minério e o monstro de que foi feito e dá um bônus de XP quando equipado. */
public class AmuletItem extends Item {
	public AmuletItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public Component getName(ItemStack stack) {
		Component ore = Amulets.itemName(stack.get(ModComponents.AMULET_ORE));
		String source = stack.get(ModComponents.HEART_SOURCE);
		Component monster = source == null ? null : Hearts.monsterName(source);
		if (ore == null) {
			return Component.translatable("item.survivalneeds.amulet");
		}
		if (monster == null) {
			return Component.translatable("item.survivalneeds.amulet.of", ore);
		}
		return Component.translatable("item.survivalneeds.amulet.of_with", ore, monster);
	}
}
