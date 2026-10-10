package com.ronaldo.survivalneeds.debuff;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;

/**
 * Aplica um tipo de efeito. É chamado uma vez por tick para cada jogador com a lista de <b>todos</b> os efeitos
 * ativos dele (de todas as fontes), mesmo vazia, para poder desfazer o que aplicou antes. O handler escolhe os
 * efeitos que lhe interessam e combina os repetidos.
 */
interface DebuffHandler {
	void tick(ServerPlayer player, List<DebuffEffect> active);

	/** O jogador saiu do servidor: limpa o que o handler guardava dele. */
	default void forget(java.util.UUID player) {}
}
