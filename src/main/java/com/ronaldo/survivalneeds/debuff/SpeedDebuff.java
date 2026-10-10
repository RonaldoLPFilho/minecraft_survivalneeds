package com.ronaldo.survivalneeds.debuff;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Aplica {@link DebuffEffect.Slowdown}: o jogador anda mais devagar. */
final class SpeedDebuff extends AttributeDebuff {
	SpeedDebuff() {
		super(Attributes.MOVEMENT_SPEED, SurvivalNeeds.id("debuff_slowdown"),
			effect -> effect instanceof DebuffEffect.Slowdown slowdown ? slowdown.fraction() : null);
	}
}
