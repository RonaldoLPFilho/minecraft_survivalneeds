package com.ronaldo.survivalneeds;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	public static final Block WATER_PURIFIER = register("water_purifier", props ->
		new WaterPurifierBlock(props.strength(2.5f).sound(SoundType.WOOD)));

	public static final Block AMULET_TABLE = register("amulet_table", props ->
		new AmuletTableBlock(props.strength(2.5f).sound(SoundType.WOOD).noOcclusion().lightLevel(state -> 7)));

	private ModBlocks() {}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SurvivalNeeds.id(name));
		Block block = factory.apply(BlockBehaviour.Properties.of().setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}

	public static void init() {}
}
