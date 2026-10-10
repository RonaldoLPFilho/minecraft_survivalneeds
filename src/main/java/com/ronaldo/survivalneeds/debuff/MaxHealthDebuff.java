package com.ronaldo.survivalneeds.debuff;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Aplica {@link DebuffEffect.MaxHealthReduction}: a vida máxima do jogador cai (a cura não passa dela). */
final class MaxHealthDebuff extends AttributeDebuff {
	MaxHealthDebuff() {
		super(Attributes.MAX_HEALTH, SurvivalNeeds.id("debuff_max_health"),
			effect -> effect instanceof DebuffEffect.MaxHealthReduction reduction ? reduction.fraction() : null);
	}
}
