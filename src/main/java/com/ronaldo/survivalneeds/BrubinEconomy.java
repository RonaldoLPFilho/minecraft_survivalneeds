package com.ronaldo.survivalneeds;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * As contas da Princess Brubin: quanto custa curar cada maldição e quanto vale cada coisa que se oferece a ela.
 * Tudo em pontos, ajustável em {@code curse} na configuração. O jogador combina minérios, níveis de XP, corações,
 * amuletos e fragmentos até chegar ao preço.
 */
public final class BrubinEconomy {
	private BrubinEconomy() {}

	private static SurvivalNeedsConfig.Curse cfg() {
		return SurvivalNeedsConfig.get().curse;
	}

	/** O preço da cura de uma maldição: o específico dela, se houver, senão o da gravidade. */
	public static int priceOf(Curse curse) {
		Integer override = cfg().brubinPriceOverrides.get(curse.id);
		if (override != null) {
			return Math.max(1, override);
		}
		return Math.max(1, cfg().brubinPrice.getOrDefault(curse.severity.name().toLowerCase(), 100));
	}

	/** Pontos que valem os níveis de XP oferecidos. */
	public static int xpPoints(int levels) {
		return levels * Math.max(1, cfg().brubinXpLevelValue);
	}

	/** O valor, em pontos, de UMA unidade do item (0 se ela não aceita). */
	public static double unitValue(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0.0;
		}
		SurvivalNeedsConfig.Curse cfg = cfg();
		if (stack.is(ModItems.CURSE_FRAGMENT)) {
			return cfg.brubinFragmentValue;
		}
		if (stack.is(ModItems.MONSTER_HEART)) {
			return heartValue(stack);
		}
		if (stack.is(ModItems.AMULET)) {
			// um amuleto custou 8 minérios e um coração
			String ore = stack.get(ModComponents.AMULET_ORE);
			double oreValue = ore == null ? 0.0 : cfg.brubinOreValue.getOrDefault(ore, 0);
			return Math.round(cfg.brubinAmuletFactor * (8.0 * oreValue + heartValue(stack)));
		}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		return cfg.brubinOreValue.getOrDefault(id, 0);
	}

	private static double heartValue(ItemStack stack) {
		return cfg().brubinHeartValue.getOrDefault(Hearts.tierOf(stack), 0);
	}
}
