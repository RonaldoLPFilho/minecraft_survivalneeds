package com.ronaldo.survivalneeds.client;

import com.ronaldo.survivalneeds.FatigueSystem;
import com.ronaldo.survivalneeds.HungerSystem;
import com.ronaldo.survivalneeds.StatusFlags;
import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

/** Mostra no canto superior direito o ícone de cada efeito ativo (frio, medo, sede, fome, cansaço, molhado). */
public final class StatusIconsHud {
	private static final Identifier COLD = SurvivalNeeds.id("hud/status_cold");
	private static final Identifier FEAR = SurvivalNeeds.id("hud/status_fear");
	private static final Identifier THIRST = SurvivalNeeds.id("hud/status_thirst");
	private static final Identifier HUNGER = SurvivalNeeds.id("hud/status_hunger");
	private static final Identifier FATIGUE = SurvivalNeeds.id("hud/status_fatigue");
	private static final Identifier WET = SurvivalNeeds.id("hud/status_wet");
	private static final Identifier HEAT = SurvivalNeeds.id("hud/status_heat");

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
		slot = draw(graphics, flags, StatusFlags.THIRST, THIRST, y, slot);

		int hungerSlot = slot;
		slot = draw(graphics, flags, StatusFlags.HUNGRY, HUNGER, y, slot);

		int fatigueSlot = slot;
		slot = draw(graphics, flags, StatusFlags.FATIGUED, FATIGUE, y, slot);

		int wetSlot = slot;
		slot = draw(graphics, flags, StatusFlags.WET, WET, y, slot);
		draw(graphics, flags, StatusFlags.HEAT, HEAT, y, slot); // fogueira ao lado do molhado

		// Contador de fome (10:00 até 0:00): tempo que resta para morrer de fome
		int hungerSeconds = (player.getAttachedOrElse(HungerSystem.TICKS_LEFT, 0) + 19) / 20;
		if ((flags & StatusFlags.HUNGRY) != 0 && hungerSeconds > 0) {
			drawTimer(graphics, hungerSlot, y, hungerSeconds, 0xFFFF6B6B);
		}

		// Contador de cansaço (5:00 até 0:00): tempo para dormir antes de começar a perder vida.
		// Estado: 0 = descansado, 1 = contador esgotado (sem contador), N maior que 1 = faltam N-1 ticks.
		int fatigueState = player.getAttachedOrElse(FatigueSystem.STATE, 0);
		if ((flags & StatusFlags.FATIGUED) != 0 && fatigueState > 1) {
			drawTimer(graphics, fatigueSlot, y, (fatigueState - 1 + 19) / 20, 0xFFC8B6FF);
		}

		// Contador (2:00 ou 4:00 até 0:00) logo abaixo do ícone de molhado, enquanto ele está secando
		int wetSeconds = player.getAttachedOrElse(StatusFlags.WET_SECONDS, 0);
		if ((flags & StatusFlags.WET) != 0 && wetSeconds > 0) {
			drawTimer(graphics, wetSlot, y, wetSeconds, 0xFFFFFFFF);
		}
	}

	/** Escreve "m:ss" centralizado logo abaixo do ícone que está no slot indicado. */
	private static void drawTimer(GuiGraphicsExtractor graphics, int slot, int y, int seconds, int color) {
		String time = String.format("%d:%02d", seconds / 60, seconds % 60);
		Font font = Minecraft.getInstance().font;
		int iconX = graphics.guiWidth() - MARGIN - ICON_SIZE - slot * SPACING;
		int textX = iconX + ICON_SIZE / 2 - font.width(time) / 2;
		graphics.text(font, time, textX, y + ICON_SIZE + 2, color, true);
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
