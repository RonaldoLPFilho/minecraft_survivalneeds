package com.ronaldo.survivalneeds;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;

public final class ModItems {
	public static final Item PURIFIED_WATER_BOTTLE = register("purified_water_bottle", props ->
		new PurifiedWaterItem(props
			.stacksTo(16)
			.usingConvertsTo(Items.GLASS_BOTTLE)
			.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)));

	public static final Item WATER_PURIFIER = register("water_purifier", props ->
		new BlockItem(ModBlocks.WATER_PURIFIER, props.useBlockDescriptionPrefix()));

	private ModItems() {}

	private static Item register(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SurvivalNeeds.id(name));
		Item item = factory.apply(new Item.Properties().setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {}
}
