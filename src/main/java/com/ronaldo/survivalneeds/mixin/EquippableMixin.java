package com.ronaldo.survivalneeds.mixin;

import com.ronaldo.survivalneeds.EquipmentGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Botão direito com a peça na mão: sem o nível, ela não troca com a que está vestida. */
@Mixin(Equippable.class)
public abstract class EquippableMixin {
	@Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
	private void survivalneeds$requireLevel(ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
		if (!EquipmentGate.canEquip(player, stack)) {
			if (player instanceof ServerPlayer serverPlayer) {
				EquipmentGate.warn(serverPlayer, stack);
			}
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
