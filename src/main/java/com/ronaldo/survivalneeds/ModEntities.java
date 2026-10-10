package com.ronaldo.survivalneeds;

import com.ronaldo.survivalneeds.entity.CurseSpecter;
import com.ronaldo.survivalneeds.entity.CursedDart;
import com.ronaldo.survivalneeds.entity.PrincessBrubin;
import com.ronaldo.survivalneeds.mixin.SpawnPlacementsInvoker;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final EntityType<CurseSpecter> CURSE_SPECTER = register("curse_specter",
		EntityType.Builder.of(CurseSpecter::new, MobCategory.MONSTER)
			.sized(1.5f, 2.8f)
			.eyeHeight(2.4f)
			.clientTrackingRange(10));

	public static final EntityType<CursedDart> CURSED_DART = register("cursed_dart",
		EntityType.Builder.<CursedDart>of(CursedDart::new, MobCategory.MISC)
			.sized(0.4f, 0.4f)
			.clientTrackingRange(4)
			.updateInterval(5));

	public static final EntityType<PrincessBrubin> PRINCESS_BRUBIN = register("princess_brubin",
		EntityType.Builder.of(PrincessBrubin::new, MobCategory.MISC)
			.sized(0.6f, 1.95f)
			.eyeHeight(1.7f)
			.clientTrackingRange(10));

	private ModEntities() {}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SurvivalNeeds.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CURSE_SPECTER, CurseSpecter.createAttributes());
		FabricDefaultAttributeRegistry.register(PRINCESS_BRUBIN, PrincessBrubin.createAttributes());

		// Raro: só em escuridão (as regras de monstro do jogo), peso baixo perto dos 100 do zumbi
		SpawnPlacementsInvoker.survivalneeds$register(CURSE_SPECTER, SpawnPlacementTypes.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, CURSE_SPECTER, 3, 1, 1);
	}
}
