package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.ModEntities;
import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.gui.screens.MenuScreens;
import com.ronaldo.survivalneeds.ModMenus;

public class SurvivalNeedsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, SurvivalNeeds.id("thirst"), ThirstHud::render);
		HudElementRegistry.attachElementAfter(VanillaHudElements.MOB_EFFECTS, SurvivalNeeds.id("status_icons"), StatusIconsHud::render);
		MenuScreens.register(ModMenus.WATER_PURIFIER, PurifierScreen::new);
		ModelLayerRegistry.registerModelLayer(CurseSpecterModel.LAYER, CurseSpecterModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.CURSE_SPECTER, CurseSpecterRenderer::new);
		ModelLayerRegistry.registerModelLayer(BrubinModel.LAYER, BrubinModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.PRINCESS_BRUBIN, BrubinRenderer::new);
		MenuScreens.register(ModMenus.BRUBIN, BrubinScreen::new);
		EntityRendererRegistry.register(ModEntities.CURSED_DART, context -> new ThrownItemRenderer<>(context, 1.5f, true));
		GearTooltip.register();
		HeartTooltip.register();
		AmuletTooltip.register();
		MenuScreens.register(ModMenus.AMULET_TABLE, AmuletTableScreen::new);
	}
}
