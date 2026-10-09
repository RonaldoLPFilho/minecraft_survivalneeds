package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import com.ronaldo.survivalneeds.ThirstSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Barra de sede acima da fome. Usa os sprites de bolha de ar do próprio jogo,
 * então herda o visual de qualquer pacote de texturas ativo (ex.: PureBDcraft).
 */
public final class ThirstHud {
	private static final Identifier FULL = Identifier.withDefaultNamespace("hud/air");
	private static final Identifier EMPTY = Identifier.withDefaultNamespace("hud/air_empty");
	private static final Identifier DRAINING = SurvivalNeeds.id("hud/thirst_draining");
	private static final int ARROW_SIZE = 11;
	private static final int ICON_SIZE = 9;
	private static final int ICONS = 10;

	private ThirstHud() {}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.isCreative() || player.isSpectator()) {
			return;
		}

		int thirst = ThirstSystem.get(player);
		int right = graphics.guiWidth() / 2 + 91;
		int y = graphics.guiHeight() - 39 - 10; // logo acima da barra de fome
		if (player.getAirSupply() < player.getMaxAirSupply()) {
			y -= 10; // desvia das bolhas de ar quando está debaixo d'água
		}

		for (int i = 0; i < ICONS; i++) {
			int x = right - i * 8 - ICON_SIZE;
			int value = thirst - i * 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, x, y, ICON_SIZE, ICON_SIZE);
			if (value >= 2) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FULL, x, y, ICON_SIZE, ICON_SIZE);
			} else if (value == 1) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FULL, x, y, ICON_SIZE, ICON_SIZE, 0.5f);
			}
		}

		// Seta ao lado da barra enquanto a corrida gasta sede
		if (player.isSprinting() && thirst > 0) {
			int bob = (player.tickCount / 4) % 2; // balança 1 pixel para chamar atenção
			int arrowX = right - (ICONS - 1) * 8 - ICON_SIZE - ARROW_SIZE - 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DRAINING, arrowX, y - 1 + bob, ARROW_SIZE, ARROW_SIZE);
		}
	}
}
