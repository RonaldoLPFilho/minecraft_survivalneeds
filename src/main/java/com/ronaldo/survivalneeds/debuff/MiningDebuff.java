package com.ronaldo.survivalneeds.debuff;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Aplica {@link DebuffEffect.MiningSlowdown}: o jogador mina mais devagar. */
final class MiningDebuff extends AttributeDebuff {
	MiningDebuff() {
		super(Attributes.BLOCK_BREAK_SPEED, SurvivalNeeds.id("debuff_mining"),
			effect -> effect instanceof DebuffEffect.MiningSlowdown reduction ? reduction.fraction() : null);
	}
}
