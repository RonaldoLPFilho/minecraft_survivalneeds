package com.ronaldo.survivalneeds.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Abre o registro de regras de aparição do jogo (privado), para o Espectro só nascer onde deve. */
@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsInvoker {
	@Invoker("register")
	static <T extends Mob> void survivalneeds$register(EntityType<T> type, SpawnPlacementType placement,
			Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
		throw new AssertionError();
	}
}
