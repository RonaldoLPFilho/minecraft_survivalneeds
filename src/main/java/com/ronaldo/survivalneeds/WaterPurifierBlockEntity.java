package com.ronaldo.survivalneeds;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Inventário do purificador, no estilo da fornalha:
 * <ul>
 *   <li>slot 0: garrafa de água suja (entrada)</li>
 *   <li>slot 1: carvão. É consumido na hora e vira {@link #USES_PER_COAL} usos. Só aceita outro quando os usos acabam.</li>
 *   <li>slot 2: garrafa de água purificada (saída)</li>
 * </ul>
 * Cada garrafa purificada gasta 1 uso.
 */
public class WaterPurifierBlockEntity extends BaseContainerBlockEntity implements ExtendedMenuProvider<BlockPos> {
	public static final int SLOT_INPUT = 0;
	public static final int SLOT_COAL = 1;
	public static final int SLOT_OUTPUT = 2;
	public static final int SLOT_COUNT = 3;

	public static final int USES_PER_COAL = 5;
	public static final int PURIFY_TICKS = 80; // 4 segundos por garrafa

	public static final int DATA_USES = 0;
	public static final int DATA_PROGRESS = 1;
	public static final int DATA_COUNT = 2;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int uses;
	private int progress;

	/** Sincroniza usos e progresso com a tela aberta. */
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return index == DATA_USES ? uses : progress;
		}

		@Override
		public void set(int index, int value) {
			if (index == DATA_USES) {
				uses = value;
			} else {
				progress = value;
			}
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public WaterPurifierBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WATER_PURIFIER, pos, state);
	}

	public static boolean isCoal(ItemStack stack) {
		return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, WaterPurifierBlockEntity purifier) {
		boolean changed = false;

		// O carvão vira usos assim que entra, e só entra outro quando os usos acabam.
		ItemStack coal = purifier.items.get(SLOT_COAL);
		if (purifier.uses == 0 && isCoal(coal)) {
			coal.shrink(1);
			purifier.uses = USES_PER_COAL;
			changed = true;
		}

		boolean canWork = purifier.uses > 0
			&& WaterPurifierBlock.isDirtyWaterBottle(purifier.items.get(SLOT_INPUT))
			&& purifier.canOutput();

		if (canWork) {
			purifier.progress++;
			if (purifier.progress >= PURIFY_TICKS) {
				purifier.progress = 0;
				purifier.finishBottle();
			}
			changed = true;
		} else if (purifier.progress > 0) {
			purifier.progress = 0;
			changed = true;
		}

		if (changed) {
			purifier.setChanged();
		}
	}

	private boolean canOutput() {
		ItemStack output = items.get(SLOT_OUTPUT);
		Item purified = ModItems.PURIFIED_WATER_BOTTLE;
		return output.isEmpty() || (output.is(purified) && output.getCount() < output.getMaxStackSize());
	}

	private void finishBottle() {
		items.get(SLOT_INPUT).shrink(1);
		ItemStack output = items.get(SLOT_OUTPUT);
		if (output.isEmpty()) {
			items.set(SLOT_OUTPUT, new ItemStack(ModItems.PURIFIED_WATER_BOTTLE));
		} else {
			output.grow(1);
		}
		uses--;
	}

	// ---- Container ----

	@Override
	public int getContainerSize() {
		return SLOT_COUNT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return switch (slot) {
			case SLOT_INPUT -> WaterPurifierBlock.isDirtyWaterBottle(stack);
			// um carvão por vez: só entra com os usos zerados e o slot vazio
			case SLOT_COAL -> isCoal(stack) && uses == 0 && items.get(SLOT_COAL).isEmpty();
			default -> false;
		};
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	// ---- Menu ----

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.survivalneeds.water_purifier");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new PurifierMenu(containerId, inventory, this, data, ContainerLevelAccess.create(level, worldPosition));
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	// ---- Salvar e carregar ----

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		uses = input.getIntOr("Uses", 0);
		progress = input.getIntOr("Progress", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("Uses", uses);
		output.putInt("Progress", progress);
	}

	/** Ao quebrar o bloco, solta o que estiver dentro. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			Containers.dropContents(level, pos, this);
		}
	}
}
