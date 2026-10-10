package com.ronaldo.survivalneeds.debuff;

import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Base dos efeitos que reduzem um atributo do jogador (dano, velocidade...). Vários efeitos ao mesmo tempo se
 * combinam multiplicando o que sobra: 50% e 50% dão 75% de redução, não 100%.
 */
abstract class AttributeDebuff implements DebuffHandler {
	private final Holder<Attribute> attribute;
	private final Identifier modifierId;
	private final Function<DebuffEffect, Double> reductionOf;

	/** @param reductionOf devolve a fração reduzida por um efeito, ou null se o efeito não é deste tipo */
	protected AttributeDebuff(Holder<Attribute> attribute, Identifier modifierId, Function<DebuffEffect, Double> reductionOf) {
		this.attribute = attribute;
		this.modifierId = modifierId;
		this.reductionOf = reductionOf;
	}

	@Override
	public void tick(ServerPlayer player, List<DebuffEffect> active) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}

		double remaining = 1.0;
		for (DebuffEffect effect : active) {
			Double reduction = reductionOf.apply(effect);
			if (reduction != null) {
				remaining *= 1.0 - Math.max(0.0, Math.min(1.0, reduction));
			}
		}
		double totalReduction = 1.0 - remaining;

		if (totalReduction > 0.0) {
			// ADD_MULTIPLIED_TOTAL: valor final = base * (1 + modificador)
			instance.addOrUpdateTransientModifier(new AttributeModifier(
				modifierId, -totalReduction, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else {
			instance.removeModifier(modifierId);
		}
	}
}
