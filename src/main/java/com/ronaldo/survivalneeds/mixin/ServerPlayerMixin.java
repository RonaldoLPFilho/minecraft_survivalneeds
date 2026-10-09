package com.ronaldo.survivalneeds.mixin;

import com.ronaldo.survivalneeds.FatigueSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Cansado, o jogador pode se deitar na cama a qualquer hora, não só à noite. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Redirect(
		method = "startSleepInBed",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"))
	private boolean survivalneeds$fatiguedCanSleepAnytime(BedRule rule, Level level) {
		return rule.canSleep(level) || FatigueSystem.canSleepAnytime((ServerPlayer) (Object) this);
	}
}
