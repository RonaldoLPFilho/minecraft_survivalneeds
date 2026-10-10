package com.ronaldo.survivalneeds.debuff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Aplica {@link DebuffEffect.BuildLimit}: só dá para colocar alguns blocos seguidos, depois é preciso esperar.
 * Se houver vários limites, vale o mais restrito. A checagem em si ({@link #canPlace} e {@link #onPlaced}) é
 * chamada pelo BlockItemMixin.
 */
final class BuildDebuff implements DebuffHandler {
	private final Map<UUID, DebuffEffect.BuildLimit> limits = new HashMap<>();
	private final Map<UUID, Integer> placed = new HashMap<>();
	private final Map<UUID, Long> lastPlaced = new HashMap<>();

	@Override
	public void tick(ServerPlayer player, List<DebuffEffect> active) {
		DebuffEffect.BuildLimit strictest = null;
		for (DebuffEffect effect : active) {
			if (effect instanceof DebuffEffect.BuildLimit candidate) {
				strictest = stricter(strictest, candidate);
			}
		}
		UUID id = player.getUUID();
		if (strictest == null) {
			forget(id);
		} else {
			limits.put(id, strictest);
		}
	}

	/** Chamado antes de colocar um bloco. Falso se o jogador já passou do limite. */
	boolean canPlace(ServerPlayer player) {
		UUID id = player.getUUID();
		DebuffEffect.BuildLimit limit = limits.get(id);
		if (limit == null) {
			return true;
		}

		long now = player.level().getGameTime();
		long since = now - lastPlaced.getOrDefault(id, now - limit.resetTicks());
		if (since >= limit.resetTicks()) {
			placed.remove(id); // passou o tempo de espera sem colocar nada: zera a contagem
			return true;
		}
		if (placed.getOrDefault(id, 0) < limit.maxBlocks()) {
			return true;
		}

		long secondsLeft = (limit.resetTicks() - since + 19) / 20;
		player.sendSystemMessage(
			Component.literal(limit.reason() + " Espere " + secondsLeft + "s para colocar mais blocos."), true);
		return false;
	}

	/** Chamado depois que um bloco foi colocado com sucesso. */
	void onPlaced(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!limits.containsKey(id)) {
			return;
		}
		placed.merge(id, 1, Integer::sum);
		lastPlaced.put(id, player.level().getGameTime());
	}

	private static DebuffEffect.BuildLimit stricter(DebuffEffect.BuildLimit current, DebuffEffect.BuildLimit other) {
		if (current == null) {
			return other;
		}
		if (other.maxBlocks() != current.maxBlocks()) {
			return other.maxBlocks() < current.maxBlocks() ? other : current;
		}
		return other.resetTicks() > current.resetTicks() ? other : current;
	}

	@Override
	public void forget(UUID player) {
		limits.remove(player);
		placed.remove(player);
		lastPlaced.remove(player);
	}
}
