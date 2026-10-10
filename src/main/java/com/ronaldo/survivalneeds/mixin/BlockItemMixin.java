package com.ronaldo.survivalneeds.mixin;

import com.ronaldo.survivalneeds.debuff.DebuffSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Molhado: só dá para colocar 5 blocos seguidos; depois é preciso esperar 5 segundos. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void survivalneeds$limitPlacingWhenWet(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (context.getPlayer() instanceof ServerPlayer player && !DebuffSystem.canPlaceBlock(player)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "place", at = @At("RETURN"))
	private void survivalneeds$countPlacedWhenWet(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (context.getPlayer() instanceof ServerPlayer player && cir.getReturnValue().consumesAction()) {
			DebuffSystem.onBlockPlaced(player);
		}
	}
}
