package com.ronaldo.survivalneeds.debuff;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Aplica {@link DebuffEffect.ArmorReduction}: a armadura protege menos. */
final class ArmorDebuff extends AttributeDebuff {
	ArmorDebuff() {
		super(Attributes.ARMOR, SurvivalNeeds.id("debuff_armor"),
			effect -> effect instanceof DebuffEffect.ArmorReduction reduction ? reduction.fraction() : null);
	}
}
