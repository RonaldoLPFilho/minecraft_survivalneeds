package com.ronaldo.survivalneeds;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Impede o jogador de correr enquanto o frio ou a sede estiverem no limite. */
public final class SprintLock {
	// O sprint do jogo soma +30% de velocidade (multiplicador total); este modificador cancela isso.
	private static final Identifier NO_SPRINT_ID = SurvivalNeeds.id("no_sprint");
	private static final AttributeModifier NO_SPRINT =
		new AttributeModifier(NO_SPRINT_ID, -0.3 / 1.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

	private SprintLock() {}

	public static void tick(ServerPlayer player) {
		boolean locked = ColdSystem.isFreezing(player) || ThirstSystem.isTooThirstyToSprint(player);

		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if (locked && player.isSprinting()) {
			player.setSprinting(false);
			speed.addOrUpdateTransientModifier(NO_SPRINT);
		} else {
			speed.removeModifier(NO_SPRINT_ID);
		}
	}
}
