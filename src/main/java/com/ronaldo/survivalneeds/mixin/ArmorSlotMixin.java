package com.ronaldo.survivalneeds.mixin;

import com.ronaldo.survivalneeds.EquipmentGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sem o nível, a armadura não entra nos espaços de armadura do inventário. Cobre clique, arrastar, tecla de troca
 * e shift+clique (que também confere o espaço antes de mover).
 */
@Mixin(ArmorSlot.class)
public abstract class ArmorSlotMixin {
	@Shadow
	@Final
	private LivingEntity owner;

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void survivalneeds$requireLevel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (owner instanceof Player player && !EquipmentGate.canEquip(player, stack)) {
			if (player instanceof ServerPlayer serverPlayer) {
				EquipmentGate.warn(serverPlayer, stack);
			}
			cir.setReturnValue(false);
		}
	}
}
