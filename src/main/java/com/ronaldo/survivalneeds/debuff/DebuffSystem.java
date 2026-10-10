package com.ronaldo.survivalneeds.debuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.server.level.ServerPlayer;

/**
 * Central dos debuffs. Os sistemas (frio, sede, fome, molhado, cansaço...) não mexem em velocidade, corrida,
 * dano ou construção por conta própria: eles <b>pedem</b> os efeitos aqui, identificando-se por uma "fonte"
 * (um texto como {@code "wet"}), e o {@code DebuffSystem} cuida de aplicar, combinar e desfazer.
 * <pre>
 *   // dentro de WetSystem.tick: enquanto molhado, pede dois efeitos; quando seca, os retira
 *   DebuffSystem.set(player, "wet", wet, () -> List.of(
 *       new DebuffEffect.SprintLimit(...), new DebuffEffect.BuildLimit(...)));
 * </pre>
 * Vantagens: dois sistemas pedindo o mesmo tipo de efeito não duplicam código nem se atrapalham (valem o mais
 * restrito ou a soma combinada, conforme o tipo), e um sistema novo só precisa escolher os efeitos que quer.
 * <p>
 * Ordem do tick: cada sistema chama {@link #set}, depois {@link #tick} aplica tudo de uma vez.
 */
public final class DebuffSystem {
	private static final SprintDebuff SPRINT = new SprintDebuff();
	private static final BuildDebuff BUILD = new BuildDebuff();
	private static final List<DebuffHandler> HANDLERS = List.of(SPRINT, BUILD, new DamageDebuff(), new SpeedDebuff(),
		new MiningDebuff(), new ArmorDebuff(), new ToughnessDebuff(), new MaxHealthDebuff());

	/** Efeitos pedidos por jogador: fonte -> efeitos. */
	private static final Map<UUID, Map<String, List<DebuffEffect>>> REQUESTS = new HashMap<>();

	private DebuffSystem() {}

	/**
	 * Liga ou desliga os efeitos de uma fonte. Se {@code active}, os efeitos devolvidos por {@code effects}
	 * passam a valer (substituindo os que a fonte já tinha); senão, a fonte deixa de ter efeitos.
	 * O {@code Supplier} só é chamado quando ativo, então pode ler a configuração na hora.
	 */
	public static void set(ServerPlayer player, String source, boolean active, Supplier<List<DebuffEffect>> effects) {
		if (active) {
			apply(player, source, effects.get());
		} else {
			clear(player, source);
		}
	}

	public static void apply(ServerPlayer player, String source, List<DebuffEffect> effects) {
		REQUESTS.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(source, List.copyOf(effects));
	}

	public static void clear(ServerPlayer player, String source) {
		Map<String, List<DebuffEffect>> sources = REQUESTS.get(player.getUUID());
		if (sources != null) {
			sources.remove(source);
		}
	}

	/** Aplica os efeitos de todas as fontes. Roda uma vez por tick para cada jogador, depois dos sistemas. */
	public static void tick(ServerPlayer player) {
		List<DebuffEffect> all = new ArrayList<>();
		Map<String, List<DebuffEffect>> sources = REQUESTS.get(player.getUUID());
		if (sources != null) {
			sources.values().forEach(all::addAll);
		}
		for (DebuffHandler handler : HANDLERS) {
			handler.tick(player, all);
		}
	}

	/** Chamado antes de colocar um bloco (BlockItemMixin). Falso se algum debuff de construção barra o jogador. */
	public static boolean canPlaceBlock(ServerPlayer player) {
		return BUILD.canPlace(player);
	}

	/** Chamado depois que um bloco foi colocado (BlockItemMixin). */
	public static void onBlockPlaced(ServerPlayer player) {
		BUILD.onPlaced(player);
	}

	/** O jogador saiu do servidor: esquece os pedidos e o estado dele. */
	public static void forget(UUID player) {
		REQUESTS.remove(player);
		HANDLERS.forEach(handler -> handler.forget(player));
	}
}
