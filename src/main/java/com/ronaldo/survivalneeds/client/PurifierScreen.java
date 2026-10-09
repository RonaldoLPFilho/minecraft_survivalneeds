package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.PurifierMenu;
import com.ronaldo.survivalneeds.WaterPurifierBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tela do purificador. Usa a textura e os sprites da fornalha do próprio jogo,
 * então herda o visual de qualquer pacote de texturas ativo. A chama mostra os usos
 * de carvão restantes e a seta mostra o progresso da garrafa.
 */
public class PurifierScreen extends AbstractContainerScreen<PurifierMenu> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");

	public PurifierScreen(PurifierMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256);

		// Chama: altura proporcional aos usos de carvão que restam
		int uses = menu.getUses();
		if (uses > 0) {
			int height = Mth.ceil((float) uses / menu.getMaxUses() * 13f) + 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 14 - height,
				leftPos + 56, topPos + 36 + 14 - height, 14, height);
		}

		// Seta: progresso da garrafa em purificação
		int width = Mth.ceil(menu.getProgress() * 24f);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + 79, topPos + 34, width, 16);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		Component uses = Component.translatable("container.survivalneeds.water_purifier.uses",
			menu.getUses(), menu.getMaxUses());
		graphics.text(font, uses, 79, 58, 4210752, false);
	}
}
