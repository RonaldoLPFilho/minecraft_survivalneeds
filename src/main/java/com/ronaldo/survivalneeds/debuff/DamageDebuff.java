package com.ronaldo.survivalneeds.debuff;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Aplica {@link DebuffEffect.DamageReduction}: o jogador causa menos dano. */
final class DamageDebuff extends AttributeDebuff {
	DamageDebuff() {
		super(Attributes.ATTACK_DAMAGE, SurvivalNeeds.id("debuff_damage"),
			effect -> effect instanceof DebuffEffect.DamageReduction reduction ? reduction.fraction() : null);
	}
}
