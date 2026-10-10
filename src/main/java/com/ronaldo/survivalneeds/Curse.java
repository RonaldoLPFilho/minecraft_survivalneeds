package com.ronaldo.survivalneeds;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;

/**
 * As maldições que o jogador pode receber. O sorteio ({@link Curses#roll}) usa os pesos da configuração:
 * quanto mais punitiva a maldição, menor o peso. A gravidade define o preço da cura (fase 3).
 */
public enum Curse {
	NO_SPRINT("no_sprint", "Pernas pesadas", "Você não consegue mais correr.", Severity.LIGHT, 14),
	NO_ATTACK("no_attack", "Mãos de pedra", "Você não consegue mais atacar.", Severity.LIGHT, 10),
	NO_SLEEP("no_sleep", "Insônia", "Você não consegue mais dormir.", Severity.LIGHT, 10),
	BUILD_LIMIT("build_limit", "Mãos trêmulas", "Você só consegue construir alguns blocos por vez.", Severity.LIGHT, 10),
	FRAGILE("fragile", "Fragilidade", "Sua vida máxima foi reduzida e você não cura além dela.", Severity.LIGHT, 12),
	NO_EAT("no_eat", "Estômago selado", "Você não consegue mais comer (beber ainda pode).", Severity.LIGHT, 8),
	HALF_LIFE("half_life", "Vida em ruínas", "Sua vida máxima despencou.", Severity.MEDIUM, 6),
	HEAVY_BODY("heavy_body", "Corpo de chumbo", "Você anda muito mais devagar.", Severity.MEDIUM, 6),
	PAPER_SKIN("paper_skin", "Pele de papel", "Você recebe muito mais dano.", Severity.MEDIUM, 6),
	BLIND_HANDS("blind_hands", "Mãos cegas", "Você mina muito mais devagar.", Severity.MEDIUM, 5),
	PARCHED("parched", "Ressecado", "Sede e fome chegam muito mais rápido.", Severity.MEDIUM, 5),
	MIND_FOG("mind_fog", "Mente turva", "Você não ganha mais experiência.", Severity.MEDIUM, 2),
	DROWNING("drowning", "Afogamento", "Você vai se afogar, mesmo fora da água.", Severity.GRAVE, 5),
	DOOM("doom", "Sentença", "A maldição vai tirar sua vida.", Severity.GRAVE, 5),
	EXPLOSION("explosion", "Explosão", "Você vai explodir, e tudo ao redor sofrerá.", Severity.GRAVE, 3);

	public enum Severity {
		LIGHT, MEDIUM, GRAVE
	}

	public final String id;
	public final String displayName;
	public final String description;
	public final Severity severity;
	public final int defaultWeight;

	Curse(String id, String displayName, String description, Severity severity, int defaultWeight) {
		this.id = id;
		this.displayName = displayName;
		this.description = description;
		this.severity = severity;
		this.defaultWeight = defaultWeight;
	}

	/** Minutos até o efeito final, ou 0 se a maldição não tem contador. */
	public double timerMinutes() {
		SurvivalNeedsConfig.Curse cfg = SurvivalNeedsConfig.get().curse;
		return switch (this) {
			case DROWNING -> cfg.drowningMinutes;
			case DOOM -> cfg.doomMinutes;
			case EXPLOSION -> cfg.explosionMinutes;
			default -> 0.0;
		};
	}

	public static Curse byId(String id) {
		for (Curse curse : values()) {
			if (curse.id.equals(id)) {
				return curse;
			}
		}
		return null;
	}

	/** Posição na lista mais 1 (0 fica livre para "sem maldição"), como é guardada e enviada ao cliente. */
	public int code() {
		return ordinal() + 1;
	}

	public static Curse byCode(int code) {
		Curse[] all = values();
		return code >= 1 && code <= all.length ? all[code - 1] : null;
	}
}
