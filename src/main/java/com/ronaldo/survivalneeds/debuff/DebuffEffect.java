package com.ronaldo.survivalneeds.debuff;

/**
 * Os efeitos genéricos que qualquer sistema (frio, sede, fome, molhado, cansaço...) pode pedir ao
 * {@link DebuffSystem}. Cada um é só uma descrição, sem lógica: quem aplica é o {@link DebuffHandler} do tipo.
 * <p>
 * Para criar um novo tipo de efeito: adicione um record aqui e um handler que o aplique.
 */
public sealed interface DebuffEffect {
	/** O jogador não consegue correr. */
	record SprintBlock() implements DebuffEffect {}

	/**
	 * O jogador corre no máximo {@code limitTicks} seguidos; depois fica {@code cooldownTicks} sem poder correr.
	 * Parar de correr por menos de {@code graceTicks} não zera a contagem.
	 */
	record SprintLimit(int limitTicks, int cooldownTicks, int graceTicks,
					   String lockedMessage, String readyMessage) implements DebuffEffect {}

	/** O jogador coloca no máximo {@code maxBlocks} seguidos; depois espera {@code resetTicks} sem colocar nenhum. */
	record BuildLimit(int maxBlocks, int resetTicks, String reason) implements DebuffEffect {}

	/** O dano causado pelo jogador cai {@code fraction} (0.5 = 50% menos dano). */
	record DamageReduction(double fraction) implements DebuffEffect {}

	/** A velocidade de andar do jogador cai {@code fraction} (0.5 = metade da velocidade). */
	record Slowdown(double fraction) implements DebuffEffect {}

	/** A velocidade de mineração do jogador cai {@code fraction} (0.5 = metade da velocidade). */
	record MiningSlowdown(double fraction) implements DebuffEffect {}

	/** A armadura (e a resistência) do jogador cai {@code fraction} (0.5 = metade da proteção). */
	record ArmorReduction(double fraction) implements DebuffEffect {}
}
