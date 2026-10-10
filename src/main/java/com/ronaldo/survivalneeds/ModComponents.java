package com.ronaldo.survivalneeds;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModComponents {
	/** De qual monstro veio o coração (id do tipo de entidade, como "minecraft:zombie"). */
	public static final DataComponentType<String> HEART_SOURCE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE, SurvivalNeeds.id("heart_source"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	/** De qual minério é o amuleto (id do item, como "minecraft:diamond"). O monstro fica em HEART_SOURCE. */
	public static final DataComponentType<String> AMULET_ORE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE, SurvivalNeeds.id("amulet_ore"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	private ModComponents() {}

	public static void init() {}
}
