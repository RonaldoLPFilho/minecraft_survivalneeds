package com.ronaldo.survivalneeds;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Mesa de Amuletos: clicar abre a tela de 3x3 onde se fabricam os amuletos. */
public class AmuletTableBlock extends Block {
	public AmuletTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.openMenu(new ExtendedMenuProvider<BlockPos>() {
				@Override
				public Component getDisplayName() {
					return Component.translatable("container.survivalneeds.amulet_table");
				}

				@Override
				public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
					return new AmuletTableMenu(containerId, inventory, ContainerLevelAccess.create(level, pos));
				}

				@Override
				public BlockPos getScreenOpeningData(ServerPlayer serverPlayer) {
					return pos;
				}
			});
		}
		return InteractionResult.SUCCESS;
	}
}
