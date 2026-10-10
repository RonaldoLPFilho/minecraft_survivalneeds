package com.ronaldo.survivalneeds;

import net.minecraft.server.level.ServerPlayer;

/**
 * Ponto único para dar XP extra ao jogador (debuffs superados, corações...). O XP original do jogo (orbes,
 * mineração, fundição) não passa por aqui. Quando existirem amuletos, o bônus deles entra neste método.
 */
public final class Progression {
	private Progression() {}

	public static void giveXp(ServerPlayer player, int points) {
		if (points > 0) {
			player.giveExperiencePoints(points);
		}
	}
}
