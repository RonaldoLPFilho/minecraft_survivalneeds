package com.ronaldo.survivalneeds;

import com.ronaldo.survivalneeds.entity.PrincessBrubin;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A troca com a Princess Brubin. O jogador põe itens nos espaços e escolhe quantos níveis de XP dar; a tela mostra o
 * preço da cura e quanto já foi oferecido. Ao confirmar, gasta-se só o necessário (os itens que sobram voltam) e a
 * maldição some. XP só é cobrado na confirmação.
 */
public class BrubinMenu extends AbstractContainerMenu {
	public static final int BUTTON_MORE_LEVELS = 0;
	public static final int BUTTON_FEWER_LEVELS = 1;
	public static final int BUTTON_CONFIRM = 2;

	public static final int PAY_ROWS = 2;
	public static final int PAY_SLOTS = PAY_ROWS * 9;
	// índices: 0-17 pagamento, 18-44 inventário, 45-53 barra de atalhos
	private static final int INV_START = PAY_SLOTS;
	private static final int HOTBAR_START = INV_START + 27;
	private static final int END = HOTBAR_START + 9;

	/** Posição das coisas na tela (usadas também pela tela do cliente). */
	public static final int PAY_X = 8;
	public static final int PAY_Y = 66;
	public static final int INV_Y = 140;

	private static final int MAX_SHOWN = 30000; // os valores viajam como 16 bits

	private final PrincessBrubin brubin;
	private final Player player;
	private final Container pay = new SimpleContainer(PAY_SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			BrubinMenu.this.slotsChanged(this);
		}
	};
	private final DataSlot price = DataSlot.standalone();
	private final DataSlot offered = DataSlot.standalone();
	private final DataSlot levels = DataSlot.standalone();
	private final DataSlot curseCode = DataSlot.standalone();

	/** Lado do cliente: o conteúdo real chega do servidor. */
	public BrubinMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null);
	}

	public BrubinMenu(int containerId, Inventory inventory, PrincessBrubin brubin) {
		super(ModMenus.BRUBIN, containerId);
		this.brubin = brubin;
		this.player = inventory.player;

		for (int row = 0; row < PAY_ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(pay, col + row * 9, PAY_X + col * 18, PAY_Y + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return BrubinEconomy.unitValue(stack) > 0.0;
					}
				});
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
		}
		addDataSlot(price);
		addDataSlot(offered);
		addDataSlot(levels);
		addDataSlot(curseCode);

		if (!player.level().isClientSide()) {
			Curse curse = Curses.current(player);
			curseCode.set(curse == null ? 0 : curse.code());
			price.set(curse == null ? 0 : Math.min(MAX_SHOWN, BrubinEconomy.priceOf(curse)));
		}
	}

	// ---- leitura (cliente e servidor) ----

	public Curse getCurse() {
		return Curse.byCode(curseCode.get());
	}

	public int getPrice() {
		return price.get();
	}

	public int getOffered() {
		return offered.get();
	}

	public int getLevels() {
		return levels.get();
	}

	public boolean canConfirm() {
		return getPrice() > 0 && getOffered() >= getPrice();
	}

	// ---- contas ----

	private int itemPoints() {
		double sum = 0.0;
		for (int i = 0; i < pay.getContainerSize(); i++) {
			ItemStack stack = pay.getItem(i);
			sum += BrubinEconomy.unitValue(stack) * stack.getCount();
		}
		return (int) Math.floor(sum);
	}

	private void refresh() {
		int total = itemPoints() + BrubinEconomy.xpPoints(levels.get());
		offered.set(Math.min(MAX_SHOWN, total));
	}

	@Override
	public void slotsChanged(Container container) {
		if (!player.level().isClientSide()) {
			refresh();
			broadcastChanges();
		}
	}

	@Override
	public boolean clickMenuButton(Player clicker, int id) {
		if (!(clicker instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		switch (id) {
			case BUTTON_MORE_LEVELS -> {
				// não passa do que o jogador tem nem do que falta para o preço
				boolean has = levels.get() + 1 <= serverPlayer.experienceLevel;
				boolean needed = itemPoints() + BrubinEconomy.xpPoints(levels.get()) < price.get();
				if (has && needed) {
					levels.set(levels.get() + 1);
					refresh();
				}
			}
			case BUTTON_FEWER_LEVELS -> {
				if (levels.get() > 0) {
					levels.set(levels.get() - 1);
					refresh();
				}
			}
			case BUTTON_CONFIRM -> confirm(serverPlayer);
			default -> {
				return false;
			}
		}
		return true;
	}

	private void confirm(ServerPlayer serverPlayer) {
		Curse current = Curses.current(serverPlayer);
		if (current == null || curseCode.get() != current.code()) {
			serverPlayer.closeContainer();
			return;
		}
		int cost = BrubinEconomy.priceOf(current);
		int fromItems = itemPoints();
		int wantedLevels = Math.min(levels.get(), serverPlayer.experienceLevel);
		int xpPerLevel = BrubinEconomy.xpPoints(1);
		if (fromItems + wantedLevels * xpPerLevel < cost) {
			serverPlayer.sendSystemMessage(Component.literal("§6Princess Brubin: §fAinda falta um pouco para a cura."), true);
			refresh();
			return;
		}

		// Níveis primeiro (só os necessários), depois os itens, na ordem dos espaços, só o que falta
		int usedLevels = Math.min(wantedLevels, (int) Math.ceil(cost / (double) xpPerLevel));
		double remaining = cost - usedLevels * (double) xpPerLevel;
		for (int i = 0; i < pay.getContainerSize() && remaining > 0.0; i++) {
			ItemStack stack = pay.getItem(i);
			double unit = BrubinEconomy.unitValue(stack);
			if (unit <= 0.0) {
				continue;
			}
			int take = Math.min(stack.getCount(), (int) Math.ceil(remaining / unit));
			stack.shrink(take);
			remaining -= take * unit;
			pay.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
		}
		if (usedLevels > 0) {
			serverPlayer.giveExperienceLevels(-usedLevels);
		}

		Curses.clear(serverPlayer, true);
		serverPlayer.sendSystemMessage(Component.literal("§6Princess Brubin: §fPronto! A maldição se desfez. Cuide-se."), false);
		if (serverPlayer.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(),
				30, 0.5, 0.8, 0.5, 0.1);
			level.playSound(null, serverPlayer.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
		}
		serverPlayer.closeContainer(); // o que sobrou nos espaços volta ao jogador
	}

	@Override
	public void removed(Player closing) {
		super.removed(closing);
		clearContainer(closing, pay);
	}

	@Override
	public boolean stillValid(Player viewer) {
		return brubin == null || (brubin.isAlive() && viewer.distanceToSqr(brubin) < 64.0);
	}

	@Override
	public ItemStack quickMoveStack(Player mover, int index) {
		ItemStack original = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			original = stack.copy();
			if (index < PAY_SLOTS) {
				if (!moveItemStackTo(stack, INV_START, END, true)) {
					return ItemStack.EMPTY;
				}
			} else if (BrubinEconomy.unitValue(stack) > 0.0) {
				if (!moveItemStackTo(stack, 0, PAY_SLOTS, false)) {
					return ItemStack.EMPTY;
				}
			} else if (index < HOTBAR_START) {
				if (!moveItemStackTo(stack, HOTBAR_START, END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
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
			slot.onTake(mover, stack);
		}
		return original;
	}
}
