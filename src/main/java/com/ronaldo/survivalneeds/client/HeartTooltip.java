package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.Hearts;
import com.ronaldo.survivalneeds.ModItems;
import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Dica do coração de monstro: raridade, XP ao consumir e bônus no amuleto. */
public final class HeartTooltip {
	private HeartTooltip() {}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!stack.is(ModItems.MONSTER_HEART)) {
				return;
			}
			String key = Hearts.tierOf(stack);
			SurvivalNeedsConfig.HeartTier tier = Hearts.tier(key);
			lines.add(Component.literal(Hearts.tierName(key)).withStyle(Hearts.color(key)));
			lines.add(Component.literal("Consumir: +" + tier.xp + " XP").withStyle(ChatFormatting.GRAY));
			lines.add(Component.literal("No amuleto: +" + Math.round(tier.bonus * 100) + "% de XP").withStyle(ChatFormatting.GRAY));
		});
	}
}
