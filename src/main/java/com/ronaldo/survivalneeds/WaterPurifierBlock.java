package com.ronaldo.survivalneeds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Purificador de água. Clicar abre uma tela como a da fornalha: garrafa de água suja de um lado,
 * carvão embaixo (1 carvão = 5 usos) e a garrafa purificada na saída.
 */
public class WaterPurifierBlock extends Block implements EntityBlock {
	public WaterPurifierBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WaterPurifierBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != ModBlockEntities.WATER_PURIFIER) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) ->
			WaterPurifierBlockEntity.serverTick(tickLevel, pos, tickState, (WaterPurifierBlockEntity) entity);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WaterPurifierBlockEntity purifier) {
			player.openMenu(purifier);
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
}
