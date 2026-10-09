package com.ronaldo.survivalneeds.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Desliga a cura natural e a inanição do jogo. Quem cura (e gasta a carne) e quem mata de fome é o HungerSystem.
 * Também desliga o gasto de comida que acompanhava a cura natural, para a carne não cair sem curar.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
	private void survivalneeds$noVanillaHeal(ServerPlayer player, float amount) {
		// a cura é feita pelo HungerSystem
	}

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
	private void survivalneeds$noVanillaHealExhaustion(FoodData data, float amount) {
		// o gasto de comida da cura natural do jogo não existe mais
	}

	@Redirect(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean survivalneeds$noVanillaStarvation(ServerPlayer player, ServerLevel level, DamageSource source, float amount) {
		return false; // a morte por fome é decidida pela contagem de 10 minutos do HungerSystem
	}
}
