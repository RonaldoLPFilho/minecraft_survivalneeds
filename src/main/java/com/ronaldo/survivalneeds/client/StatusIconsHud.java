package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.StatusFlags;
import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

/** Mostra no canto superior direito o ícone de cada efeito ativo (frio, medo, sede). */
public final class StatusIconsHud {
	private static final Identifier COLD = SurvivalNeeds.id("hud/status_cold");
	private static final Identifier FEAR = SurvivalNeeds.id("hud/status_fear");
	private static final Identifier THIRST = SurvivalNeeds.id("hud/status_thirst");

	private static final int ICON_SIZE = 24;
	private static final int SPACING = 25;
	private static final int MARGIN = 1;
	private static final int EFFECT_ROW_HEIGHT = 26; // altura de cada linha de poções do jogo

	private StatusIconsHud() {}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int flags = player.getAttachedOrElse(StatusFlags.FLAGS, 0);
		if (flags == 0) {
			return;
		}

		// Fica logo abaixo das linhas de poções do jogo (benéficas e prejudiciais), que também ficam no topo direito.
		boolean beneficial = false;
		boolean harmful = false;
		for (MobEffectInstance effect : player.getActiveEffects()) {
			if (!effect.showIcon()) {
				continue;
			}
			if (effect.getEffect().value().isBeneficial()) {
				beneficial = true;
			} else {
				harmful = true;
			}
		}
		int y = MARGIN + EFFECT_ROW_HEIGHT * ((beneficial ? 1 : 0) + (harmful ? 1 : 0));

		int slot = 0;
		slot = draw(graphics, flags, StatusFlags.COLD, COLD, y, slot);
		slot = draw(graphics, flags, StatusFlags.FEAR, FEAR, y, slot);
		draw(graphics, flags, StatusFlags.THIRST, THIRST, y, slot);
	}

	/** Desenha o ícone se a flag estiver ativa e devolve o próximo slot livre (da direita para a esquerda). */
	private static int draw(GuiGraphicsExtractor graphics, int flags, int flag, Identifier sprite, int y, int slot) {
		if ((flags & flag) == 0) {
			return slot;
		}
		int x = graphics.guiWidth() - MARGIN - ICON_SIZE - slot * SPACING;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, ICON_SIZE, ICON_SIZE);
		return slot + 1;
	}
}
