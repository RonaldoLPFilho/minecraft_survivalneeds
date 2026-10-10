package com.ronaldo.survivalneeds;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Morrer custa níveis. A fração perdida cai linearmente de {@code maxLoss} (no nível {@code fromLevel} ou menos)
 * até {@code minLoss} (no nível {@code toLevel} ou mais), e o jogador volta com o resto, com a barra zerada.
 * <pre>
 *   perda%   = maxLoss - (maxLoss - minLoss) x (nível - fromLevel) / (toLevel - fromLevel)
 *   perdidos = arredondar(nível x perda%)
 * </pre>
 * Vale também com keepInventory ligado. O XP solto que o jogo original deixa no local da morte não muda.
 */
public final class DeathPenalty {
	private DeathPenalty() {}

	public static void init() {
		// alive = false só na morte (voltar do Fim vem com alive = true e não custa nada)
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				apply(oldPlayer.experienceLevel, newPlayer);
			}
		});
	}

	private static void apply(int levelAtDeath, ServerPlayer player) {
		SurvivalNeedsConfig.Progression progression = SurvivalNeedsConfig.get().progression;
		if (!progression.enabled || !progression.death.enabled || levelAtDeath <= 0) {
			return;
		}
		int kept = levelAfterDeath(levelAtDeath);
		player.setExperienceLevels(kept);
		player.setExperiencePoints(0);
		player.sendSystemMessage(Component.literal("Você morreu e perdeu " + (levelAtDeath - kept)
			+ " níveis (nível " + levelAtDeath + " para " + kept + ")."), false);
	}

	/** Nível com que o jogador volta depois de morrer no nível dado. */
	static int levelAfterDeath(int level) {
		SurvivalNeedsConfig.Death death = SurvivalNeedsConfig.get().progression.death;
		double span = Math.max(1, death.toLevel - death.fromLevel);
		double progress = Math.max(0.0, Math.min(1.0, (level - death.fromLevel) / span));
		double loss = death.maxLoss - (death.maxLoss - death.minLoss) * progress;
		int lost = (int) Math.round(level * loss);
		return Math.max(0, Math.min(level, level - lost));
	}
}
