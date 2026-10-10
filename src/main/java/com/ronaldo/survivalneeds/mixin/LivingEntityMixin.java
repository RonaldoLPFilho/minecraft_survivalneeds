package com.ronaldo.survivalneeds.mixin;

import com.ronaldo.survivalneeds.EquipmentGate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** O dispensador também não veste armadura em quem não tem o nível. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "canEquipWithDispenser", at = @At("HEAD"), cancellable = true)
	private void survivalneeds$requireLevel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && !EquipmentGate.canEquip(player, stack)) {
			cir.setReturnValue(false);
		}
	}
}
