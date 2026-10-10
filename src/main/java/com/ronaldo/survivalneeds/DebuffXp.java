package com.ronaldo.survivalneeds;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * XP extra por superar um debuff. Cada sistema avisa quando o debuff <b>começa</b> ({@link #begin}) e quando ele
 * termina <b>por recuperação</b> ({@link #recover}); terminar de outro jeito (morte, comando, modo criativo) é
 * {@link #abort} e não dá nada.
 * <p>
 * Proteção contra farm: (1) duração mínima no debuff; (2) tolerância por tipo, com
 * {@code xp = base x (1 - recente / tolerância)}, em que "recente" sobe 1 a cada ganho e cai com o tempo;
 * (3) teto de XP de debuffs por hora. Os contadores ficam em memória (reiniciam com o servidor).
 */
public final class DebuffXp {
	public enum Type {
		WET("wet", "secou"),
		COLD("cold", "superou o frio"),
		FEAR("fear", "superou o medo"),
		THIRST("thirst", "matou a sede"),
		FATIGUE("fatigue", "superou o cansaço"),
		HUNGER("hunger", "superou a fome");

		final String key;
		final String label;

		Type(String key, String label) {
			this.key = key;
			this.label = label;
		}
	}

	private static final long HOUR_TICKS = 72000L;

	private static final Map<UUID, EnumMap<Type, Long>> STARTED = new HashMap<>();
	private static final Map<UUID, EnumMap<Type, Double>> RECENT = new HashMap<>();
	private static final Map<UUID, EnumMap<Type, Long>> RECENT_AT = new HashMap<>();
	private static final Map<UUID, Deque<long[]>> GAINS = new HashMap<>(); // {tick, xp} da última hora

	private DebuffXp() {}

	private static SurvivalNeedsConfig.DebuffXp cfg() {
		return SurvivalNeedsConfig.get().progression.debuffXp;
	}

	private static boolean enabled() {
		return SurvivalNeedsConfig.get().progression.enabled && cfg().enabled;
	}

	/** O debuff começou (se já estava contando, continua contando do início). */
	public static void begin(ServerPlayer player, Type type) {
		STARTED.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Type.class))
			.putIfAbsent(type, player.level().getGameTime());
	}

	/** O debuff terminou sem recuperação (morte, comando, modo criativo...): não dá XP. */
	public static void abort(ServerPlayer player, Type type) {
		EnumMap<Type, Long> started = STARTED.get(player.getUUID());
		if (started != null) {
			started.remove(type);
		}
	}

	public static void abortAll(ServerPlayer player) {
		STARTED.remove(player.getUUID());
	}

	/** Roda a cada tick: quem morreu ou está em criativo/espectador perde as contagens em andamento. */
	public static void tick(ServerPlayer player) {
		if (player.isDeadOrDying() || player.isCreative() || player.isSpectator()) {
			abortAll(player);
		}
	}

	/** O jogador se livrou do debuff: dá o XP se valer (duração mínima, tolerância e teto por hora). */
	public static void recover(ServerPlayer player, Type type) {
		EnumMap<Type, Long> started = STARTED.get(player.getUUID());
		Long start = started == null ? null : started.remove(type);
		if (start == null || !enabled() || player.isCreative() || player.isSpectator()) {
			return;
		}
		SurvivalNeedsConfig.DebuffXp cfg = cfg();
		SurvivalNeedsConfig.DebuffXpType settings = cfg.types.get(type.key);
		if (settings == null) {
			return;
		}
		long now = player.level().getGameTime();
		if (now - start < SurvivalNeedsConfig.ticks(settings.minSeconds)) {
			return; // foi só um susto: não vale
		}

		UUID id = player.getUUID();
		double recent = decayedRecent(id, type, now, cfg);
		double factor = Math.max(0.0, 1.0 - recent / Math.max(0.0001, cfg.tolerancePerType));
		int xp = (int) Math.round(settings.xp * factor);
		RECENT.computeIfAbsent(id, k -> new EnumMap<>(Type.class)).put(type, Math.min(cfg.tolerancePerType, recent + 1.0));
		RECENT_AT.computeIfAbsent(id, k -> new EnumMap<>(Type.class)).put(type, now);

		xp = Math.min(xp, cfg.hourlyCap - gainedLastHour(id, now));
		if (xp <= 0) {
			return;
		}
		GAINS.computeIfAbsent(id, k -> new ArrayDeque<>()).add(new long[] {now, xp});
		Progression.giveXp(player, xp);
		player.sendSystemMessage(Component.literal("+" + xp + " XP (" + type.label + ")"), false);
	}

	/** O "recente" do tipo, já descontado o tempo passado desde a última vez. */
	private static double decayedRecent(UUID id, Type type, long now, SurvivalNeedsConfig.DebuffXp cfg) {
		EnumMap<Type, Double> recents = RECENT.get(id);
		EnumMap<Type, Long> times = RECENT_AT.get(id);
		if (recents == null || times == null || !recents.containsKey(type)) {
			return 0.0;
		}
		double decayTicks = Math.max(1.0, cfg.decayMinutes * 60.0 * 20.0);
		return Math.max(0.0, recents.get(type) - (now - times.get(type)) / decayTicks);
	}

	private static int gainedLastHour(UUID id, long now) {
		Deque<long[]> gains = GAINS.get(id);
		if (gains == null) {
			return 0;
		}
		while (!gains.isEmpty() && now - gains.peekFirst()[0] >= HOUR_TICKS) {
			gains.pollFirst();
		}
		int total = 0;
		for (long[] gain : gains) {
			total += (int) gain[1];
		}
		return total;
	}
}
