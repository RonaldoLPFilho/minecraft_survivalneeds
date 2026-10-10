package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.EquipmentGate;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Acrescenta "Requer nível X" ao texto das peças que pedem nível: vermelho se o jogador ainda não o tem. */
public final class GearTooltip {
	private GearTooltip() {}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null || EquipmentGate.ignores(player)) {
				return;
			}
			int required = EquipmentGate.requiredLevel(stack);
			if (required <= 0) {
				return;
			}
			ChatFormatting color = player.experienceLevel >= required ? ChatFormatting.GRAY : ChatFormatting.RED;
			lines.add(Component.literal("Requer nível " + required).withStyle(color));
		});
	}
}
