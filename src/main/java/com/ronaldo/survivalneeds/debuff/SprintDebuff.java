package com.ronaldo.survivalneeds.debuff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Aplica {@link DebuffEffect.SprintBlock} (não corre) e {@link DebuffEffect.SprintLimit} (corre por um tempo e
 * depois descansa). Se houver vários limites, vale o mais restrito.
 */
final class SprintDebuff implements DebuffHandler {
	// O sprint do jogo soma +30% de velocidade (multiplicador total); este modificador cancela isso.
	private static final Identifier CANCEL_ID = SurvivalNeeds.id("debuff_no_sprint");
	private static final AttributeModifier CANCEL =
		new AttributeModifier(CANCEL_ID, -0.3 / 1.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

	private final Map<UUID, Integer> sprintTicks = new HashMap<>();   // há quanto tempo corre seguido
	private final Map<UUID, Integer> cooldown = new HashMap<>();      // tempo que falta de descanso
	private final Map<UUID, Long> lastSprint = new HashMap<>();

	@Override
	public void tick(ServerPlayer player, List<DebuffEffect> active) {
		boolean blocked = false;
		DebuffEffect.SprintLimit limit = null;
		for (DebuffEffect effect : active) {
			if (effect instanceof DebuffEffect.SprintBlock) {
				blocked = true;
			} else if (effect instanceof DebuffEffect.SprintLimit candidate) {
				limit = stricter(limit, candidate);
			}
		}

		boolean resting = false;
		if (limit != null) {
			resting = tickLimit(player, limit);
		} else {
			forget(player.getUUID());
		}

		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if ((blocked || resting) && player.isSprinting()) {
			player.setSprinting(false);
			speed.addOrUpdateTransientModifier(CANCEL);
		} else {
			speed.removeModifier(CANCEL_ID);
		}
	}

	/** Conta a corrida seguida. Devolve true enquanto o jogador está no descanso e não pode correr. */
	private boolean tickLimit(ServerPlayer player, DebuffEffect.SprintLimit limit) {
		UUID id = player.getUUID();
		long now = player.level().getGameTime();

		int rest = cooldown.getOrDefault(id, 0);
		if (rest > 0) {
			rest--;
			if (rest == 0) {
				cooldown.remove(id);
				if (limit.readyMessage() != null) {
					player.sendSystemMessage(Component.literal(limit.readyMessage()), true);
				}
				return false;
			}
			cooldown.put(id, rest);
			return true;
		}

		if (player.isSprinting()) {
			lastSprint.put(id, now);
			int sprinted = sprintTicks.merge(id, 1, Integer::sum);
			if (sprinted >= limit.limitTicks()) {
				sprintTicks.remove(id);
				cooldown.put(id, limit.cooldownTicks());
				if (limit.lockedMessage() != null) {
					player.sendSystemMessage(Component.literal(limit.lockedMessage()), true);
				}
				return true;
			}
		} else if (now - lastSprint.getOrDefault(id, now) > limit.graceTicks()) {
			sprintTicks.remove(id);
		}
		return false;
	}

	private static DebuffEffect.SprintLimit stricter(DebuffEffect.SprintLimit current, DebuffEffect.SprintLimit other) {
		if (current == null) {
			return other;
		}
		if (other.limitTicks() != current.limitTicks()) {
			return other.limitTicks() < current.limitTicks() ? other : current;
		}
		return other.cooldownTicks() > current.cooldownTicks() ? other : current;
	}

	@Override
	public void forget(UUID player) {
		sprintTicks.remove(player);
		cooldown.remove(player);
		lastSprint.remove(player);
	}
}
