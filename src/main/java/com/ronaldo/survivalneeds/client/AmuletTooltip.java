package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.Amulets;
import com.ronaldo.survivalneeds.ModComponents;
import com.ronaldo.survivalneeds.ModItems;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Dica do amuleto: o bônus de XP total e de onde ele vem (minério e coração). */
public final class AmuletTooltip {
	private AmuletTooltip() {}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!stack.is(ModItems.AMULET)) {
				return;
			}
			String ore = stack.get(ModComponents.AMULET_ORE);
			String heart = stack.get(ModComponents.HEART_SOURCE);
			lines.add(Component.literal("+" + Math.round(Amulets.bonusOf(stack) * 100) + "% de XP").withStyle(ChatFormatting.GREEN));
			lines.add(Component.literal("Minério: +" + Math.round(Amulets.oreBonus(ore) * 100) + "%  Coração: +"
				+ Math.round(Amulets.heartBonus(heart) * 100) + "%").withStyle(ChatFormatting.GRAY));
		});
	}
}
