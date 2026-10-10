package com.ronaldo.survivalneeds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Mesa de Amuletos: 3x3 mais resultado, com a disposição da bancada de criação. Só uma receita funciona:
 * 8 minérios iguais em volta e 1 coração de monstro no centro. A conta é feita aqui (e não por receita em JSON)
 * para aceitar qualquer minério da configuração e qualquer coração. Ao fechar, o que sobrou volta ao jogador.
 */
public class AmuletTableMenu extends AbstractContainerMenu {
	// índices dos slots: 0 resultado, 1-9 grade, 10-36 inventário, 37-45 barra de atalhos
	private static final int GRID_START = 1;
	private static final int GRID_END = 10;
	private static final int INV_END = 37;
	private static final int HOTBAR_END = 46;
	private static final int CENTER = 4; // posição do coração na grade (0 a 8)

	private final ContainerLevelAccess access;
	private final Player player;
	private final Container result = new SimpleContainer(1);
	private final Container grid = new SimpleContainer(9) {
		@Override
		public void setChanged() {
			super.setChanged();
			AmuletTableMenu.this.slotsChanged(this);
		}
	};

	/** Lado do cliente: o conteúdo real chega do servidor. */
	public AmuletTableMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public AmuletTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(ModMenus.AMULET_TABLE, containerId);
		this.access = access;
		this.player = inventory.player;

		addSlot(new Slot(result, 0, 124, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player taker, ItemStack stack) {
				for (int i = 0; i < 9; i++) {
					grid.removeItem(i, 1); // gasta um de cada: 8 minérios e o coração
				}
				slotsChanged(grid);
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				addSlot(new Slot(grid, col + row * 3, 30 + col * 18, 17 + row * 18));
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, 142));
		}
	}

	/** O amuleto que a grade forma, ou vazio se ela não for a receita. */
	private ItemStack recipeResult() {
		ItemStack heart = grid.getItem(CENTER);
		if (!heart.is(ModItems.MONSTER_HEART)) {
			return ItemStack.EMPTY;
		}
		String ore = null;
		for (int i = 0; i < 9; i++) {
			if (i == CENTER) {
				continue;
			}
			ItemStack stack = grid.getItem(i);
			if (stack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			if (Amulets.oreBonus(id) <= 0.0 || (ore != null && !ore.equals(id))) {
				return ItemStack.EMPTY;
			}
			ore = id;
		}
		String source = heart.get(ModComponents.HEART_SOURCE);
		return Amulets.create(ore, source == null ? "minecraft:zombie" : source);
	}

	@Override
	public void slotsChanged(Container container) {
		if (!player.level().isClientSide()) {
			result.setItem(0, recipeResult());
			broadcastChanges();
		}
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, pos) -> clearContainer(player, grid));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModBlocks.AMULET_TABLE);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack original = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			original = stack.copy();
			if (index == 0) {
				if (!moveItemStackTo(stack, GRID_END, HOTBAR_END, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, original);
			} else if (index >= GRID_END) {
				if (!moveItemStackTo(stack, GRID_START, GRID_END, false)) {
					if (index < INV_END) {
						if (!moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
							return ItemStack.EMPTY;
						}
					} else if (!moveItemStackTo(stack, GRID_END, INV_END, false)) {
						return ItemStack.EMPTY;
					}
				}
			} else if (!moveItemStackTo(stack, GRID_END, HOTBAR_END, false)) {
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
			if (index == 0) {
				player.drop(stack, false, net.minecraft.util.Prediction.PREDICTED);
			}
		}
		return original;
	}
}
