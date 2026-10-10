package com.ronaldo.survivalneeds;

import net.fabricmc.api.ModInitializer;
import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import com.ronaldo.survivalneeds.debuff.DebuffSystem;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.EventResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
		SurvivalNeedsConfig.load();
		ModBlocks.init();
		ModComponents.init();
		ModItems.init();
		ModEntities.init();
		ModBlockEntities.init();
		ModMenus.init();
		ThirstSystem.init();
		HungerSystem.init();
		FatigueSystem.init();
		StatusFlags.init();
		DeathPenalty.init();
		Hearts.init();
		Curses.init();
		BrubinSpawner.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> StatusCommands.register(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(server ->
			server.getPlayerList().getPlayers().forEach(player -> {
				DebuffXp.tick(player);
				ColdSystem.tick(player);
				FearSystem.tick(player);
				WetSystem.tick(player);
				HungerSystem.tick(player);
				FatigueSystem.tick(player);
				ThirstSystem.tick(player);
				GearSystem.tick(player);
				Curses.tick(player);
				DebuffSystem.tick(player); // depois de todos os sistemas: aplica os efeitos que eles pediram
				StatusFlags.update(player);
			}));

		// Quem sai do servidor deixa de ter efeitos guardados
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			DebuffSystem.forget(handler.getPlayer().getUUID());
			EquipmentGate.forget(handler.getPlayer().getUUID());
			Amulets.forget(handler.getPlayer().getUUID());
			Curses.forget(handler.getPlayer().getUUID());
		});

		// Molhado ou amaldiçoado com insônia não consegue dormir
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return null;
			}
			if (Curses.has(serverPlayer, Curse.NO_SLEEP) && !serverPlayer.isCreative()) {
				return new Player.BedSleepingProblem(Component.literal("A maldição não deixa você dormir."));
			}
			return WetSystem.isWet(serverPlayer)
				? new Player.BedSleepingProblem(Component.literal("Você está molhado demais para dormir! Seque-se primeiro."))
				: null;
		});

		// Cansado dorme mesmo com monstros por perto
		EntitySleepEvents.ALLOW_NEARBY_MONSTERS.register((player, pos, vanillaResult) ->
			FatigueSystem.canSleepAnytime(player) ? EventResult.ALLOW : EventResult.PASS);

		// Cansado também dorme de dia, mas dormir de dia não pode pular o tempo do mundo
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(player ->
			!(FatigueSystem.isFatigued(player) && player.level().isBrightOutside()));

		addToCreativeTab("food_and_drinks", ModItems.PURIFIED_WATER_BOTTLE);
		addToCreativeTab("functional_blocks", ModItems.WATER_PURIFIER);
		addToCreativeTab("functional_blocks", ModItems.AMULET_TABLE);
		addToCreativeTab("spawn_eggs", ModItems.CURSE_SPECTER_SPAWN_EGG);
		addToCreativeTab("spawn_eggs", ModItems.PRINCESS_BRUBIN_SPAWN_EGG);
		addToCreativeTab("ingredients", ModItems.CURSE_FRAGMENT);

		// Um coração de cada monstro na aba de ingredientes
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients")))
			.register(output -> SurvivalNeedsConfig.get().progression.hearts.monsters.keySet()
				.forEach(monster -> output.accept(Hearts.create(monster))));

		LOGGER.info("Survival Needs carregado!");
	}

	private static void addToCreativeTab(String tab, Item item) {
		ResourceKey<CreativeModeTab> key =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(tab));
		CreativeModeTabEvents.modifyOutputEvent(key).register(output -> output.accept(item));
	}
}
