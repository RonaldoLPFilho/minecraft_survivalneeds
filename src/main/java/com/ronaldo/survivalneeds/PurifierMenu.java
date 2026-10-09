package com.ronaldo.survivalneeds;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela do purificador, com a mesma disposição da fornalha. */
public class PurifierMenu extends AbstractContainerMenu {
	// índices dos slots: 0-2 do purificador, 3-29 inventário, 30-38 barra de atalhos
	private static final int PLAYER_START = 3;
	private static final int PLAYER_END = 30;
	private static final int HOTBAR_START = 30;
	private static final int HOTBAR_END = 39;

	private final ContainerLevelAccess access;
	private final ContainerData data;

	/** Lado do cliente: o conteúdo real chega do servidor. */
	public PurifierMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(WaterPurifierBlockEntity.SLOT_COUNT),
			new SimpleContainerData(WaterPurifierBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL);
	}

	public PurifierMenu(int containerId, Inventory inventory, Container container, ContainerData data,
			ContainerLevelAccess access) {
		super(ModMenus.WATER_PURIFIER, containerId);
		this.access = access;
		this.data = data;

		addSlot(new Slot(container, WaterPurifierBlockEntity.SLOT_INPUT, 56, 17) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return WaterPurifierBlock.isDirtyWaterBottle(stack);
			}
		});
		addSlot(new Slot(container, WaterPurifierBlockEntity.SLOT_COAL, 56, 53) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				// um carvão por vez
				return WaterPurifierBlockEntity.isCoal(stack) && PurifierMenu.this.getUses() == 0 && !hasItem();
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addSlot(new Slot(container, WaterPurifierBlockEntity.SLOT_OUTPUT, 116, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});

		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	public int getUses() {
		return data.get(WaterPurifierBlockEntity.DATA_USES);
	}

	/** Quantos usos 1 carvão dá (vem do servidor, pela configuração dele). */
	public int getMaxUses() {
		return Math.max(1, data.get(WaterPurifierBlockEntity.DATA_MAX_USES));
	}

	/** Progresso da garrafa atual, de 0.0 a 1.0. */
	public float getProgress() {
		int total = Math.max(1, data.get(WaterPurifierBlockEntity.DATA_PURIFY_TICKS));
		return Math.min(1f, (float) data.get(WaterPurifierBlockEntity.DATA_PROGRESS) / total);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index < PLAYER_START) {
			// do purificador para o inventário
			if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (WaterPurifierBlock.isDirtyWaterBottle(stack)) {
			if (!moveItemStackTo(stack, WaterPurifierBlockEntity.SLOT_INPUT, WaterPurifierBlockEntity.SLOT_INPUT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (WaterPurifierBlockEntity.isCoal(stack) && getUses() == 0) {
			if (!moveItemStackTo(stack, WaterPurifierBlockEntity.SLOT_COAL, WaterPurifierBlockEntity.SLOT_COAL + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (index < PLAYER_END) {
			if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModBlocks.WATER_PURIFIER);
	}
}
