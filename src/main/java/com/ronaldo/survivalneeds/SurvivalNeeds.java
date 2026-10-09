package com.ronaldo.survivalneeds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SurvivalNeeds implements ModInitializer {
	public static final String MOD_ID = "survivalneeds";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModBlocks.init();
		ModItems.init();
		ThirstSystem.init();
		StatusFlags.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> StatusCommands.register(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(server ->
			server.getPlayerList().getPlayers().forEach(player -> {
				ColdSystem.tick(player);
				FearSystem.tick(player);
				ThirstSystem.tick(player);
				SprintLock.tick(player);
				StatusFlags.update(player);
			}));

		addToCreativeTab("food_and_drinks", ModItems.PURIFIED_WATER_BOTTLE);
		addToCreativeTab("functional_blocks", ModItems.WATER_PURIFIER);

		LOGGER.info("Survival Needs carregado!");
	}

	private static void addToCreativeTab(String tab, Item item) {
		ResourceKey<CreativeModeTab> key =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(tab));
		CreativeModeTabEvents.modifyOutputEvent(key).register(output -> output.accept(item));
	}
}
