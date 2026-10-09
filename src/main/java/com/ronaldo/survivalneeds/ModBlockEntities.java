package com.ronaldo.survivalneeds;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<WaterPurifierBlockEntity> WATER_PURIFIER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		SurvivalNeeds.id("water_purifier"),
		new BlockEntityType<>(WaterPurifierBlockEntity::new, Set.of(ModBlocks.WATER_PURIFIER)));

	private ModBlockEntities() {}

	public static void init() {}
}
