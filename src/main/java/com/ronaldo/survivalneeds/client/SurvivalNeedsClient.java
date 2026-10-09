package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class SurvivalNeedsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, SurvivalNeeds.id("thirst"), ThirstHud::render);
		HudElementRegistry.attachElementAfter(VanillaHudElements.MOB_EFFECTS, SurvivalNeeds.id("status_icons"), StatusIconsHud::render);
	}
}
