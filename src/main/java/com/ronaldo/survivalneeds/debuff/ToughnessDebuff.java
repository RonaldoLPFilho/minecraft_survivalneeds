package com.ronaldo.survivalneeds.debuff;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Aplica {@link DebuffEffect.ArmorReduction} também na resistência da armadura (netherita, diamante). */
final class ToughnessDebuff extends AttributeDebuff {
	ToughnessDebuff() {
		super(Attributes.ARMOR_TOUGHNESS, SurvivalNeeds.id("debuff_toughness"),
			effect -> effect instanceof DebuffEffect.ArmorReduction reduction ? reduction.fraction() : null);
	}
}
