package com.ronaldo.survivalneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Purificador: clique com uma garrafa de água suja e tenha carvão no inventário para obter água limpa. */
public class WaterPurifierBlock extends Block {
	public WaterPurifierBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!isDirtyWaterBottle(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!player.isCreative() && !takeOneCoal(player)) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendSystemMessage(Component.literal("Você precisa de carvão no inventário."), true);
			}
			return InteractionResult.CONSUME;
		}

		ItemStack purified = new ItemStack(ModItems.PURIFIED_WATER_BOTTLE);
		if (stack.getCount() == 1) {
			player.setItemInHand(hand, purified);
		} else {
			stack.shrink(1);
			if (!player.getInventory().add(purified)) {
				if (level instanceof ServerLevel serverLevel) {
					player.spawnAtLocation(serverLevel, purified);
				}
			}
		}

		level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
				12, 0.25, 0.1, 0.25, 0.1);
		}
		return InteractionResult.SUCCESS;
	}

	/** A água vanilla de garrafa (poção "water") é a água suja do nosso sistema. */
	public static boolean isDirtyWaterBottle(ItemStack stack) {
		if (!stack.is(Items.POTION)) {
			return false;
		}
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return contents != null && contents.is(Potions.WATER);
	}

	private static boolean takeOneCoal(Player player) {
		Container inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);
			if (slot.is(Items.COAL) || slot.is(Items.CHARCOAL)) {
				slot.shrink(1);
				return true;
			}
		}
		return false;
	}
}
