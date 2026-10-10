package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.BrubinMenu;
import com.ronaldo.survivalneeds.Curse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tela da Princess Brubin: mostra a maldição, o preço da cura, quanto já foi oferecido (com uma barra) e os botões
 * de níveis de XP e de confirmar. O painel é desenhado aqui; os espaços usam o sprite de espaço do próprio jogo.
 */
public class BrubinScreen extends AbstractContainerScreen<BrubinMenu> {
	private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");

	private Button confirm;

	public BrubinScreen(BrubinMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
		this.inventoryLabelY = BrubinMenu.INV_Y - 11;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.literal("-"), button -> press(BrubinMenu.BUTTON_FEWER_LEVELS))
			.bounds(leftPos + 8, topPos + 108, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+"), button -> press(BrubinMenu.BUTTON_MORE_LEVELS))
			.bounds(leftPos + 30, topPos + 108, 20, 20).build());
		confirm = addRenderableWidget(Button.builder(Component.translatable("container.survivalneeds.brubin.confirm"),
				button -> press(BrubinMenu.BUTTON_CONFIRM))
			.bounds(leftPos + 108, topPos + 108, 60, 20).build());
	}

	private void press(int id) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (confirm != null) {
			confirm.active = menu.canConfirm();
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = leftPos;
		int y = topPos;

		// painel cinza com relevo, como as telas do jogo
		graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
		graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
		graphics.fill(x + 2, y + 2, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
		graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFC6C6C6);

		// barra de progresso do pagamento
		int barX = x + 8;
		int barY = y + 52;
		int barW = imageWidth - 16;
		graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF373737);
		graphics.fill(barX, barY, barX + barW, barY + 8, 0xFF8B8B8B);
		int price = Math.max(1, menu.getPrice());
		int filled = Math.min(barW, barW * menu.getOffered() / price);
		graphics.fill(barX, barY, barX + filled, barY + 8, menu.canConfirm() ? 0xFF55CC55 : 0xFFD9A441);

		// espaços (pagamento e inventário)
		for (var slot : menu.slots) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x + slot.x - 1, y + slot.y - 1, 18, 18);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		Curse curse = menu.getCurse();
		String name = curse == null ? "?" : curse.displayName;
		graphics.text(font, Component.translatable("container.survivalneeds.brubin.curse", name), 8, 18, 0xFF6A1B9A, false);
		graphics.text(font, Component.translatable("container.survivalneeds.brubin.price", menu.getPrice()), 8, 30, 0xFF404040, false);
		graphics.text(font, Component.translatable("container.survivalneeds.brubin.offered", menu.getOffered()), 8, 41, 0xFF404040, false);
		graphics.text(font, Component.translatable("container.survivalneeds.brubin.levels", menu.getLevels()), 54, 114, 0xFF404040, false);
	}
}
