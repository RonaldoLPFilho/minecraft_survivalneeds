package com.ronaldo.survivalneeds;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModMenus {
	public static final ExtendedMenuType<PurifierMenu, BlockPos> WATER_PURIFIER = Registry.register(
		BuiltInRegistries.MENU,
		SurvivalNeeds.id("water_purifier"),
		new ExtendedMenuType<>((containerId, inventory, pos) -> new PurifierMenu(containerId, inventory), BlockPos.STREAM_CODEC));

	public static final ExtendedMenuType<AmuletTableMenu, BlockPos> AMULET_TABLE = Registry.register(
		BuiltInRegistries.MENU,
		SurvivalNeeds.id("amulet_table"),
		new ExtendedMenuType<>((containerId, inventory, pos) -> new AmuletTableMenu(containerId, inventory), BlockPos.STREAM_CODEC));

	/** A tela da Princess Brubin: o dado enviado ao cliente é o id da entidade. */
	public static final ExtendedMenuType<BrubinMenu, Integer> BRUBIN = Registry.register(
		BuiltInRegistries.MENU,
		SurvivalNeeds.id("brubin"),
		new ExtendedMenuType<>((containerId, inventory, entityId) -> new BrubinMenu(containerId, inventory), ByteBufCodecs.VAR_INT));

	private ModMenus() {}

	public static void init() {}
}
