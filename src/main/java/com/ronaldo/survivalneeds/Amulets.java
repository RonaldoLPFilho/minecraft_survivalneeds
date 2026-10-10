package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Amuletos: o bônus de cada um (minério + coração), a soma dos equipados (pelo Trinkets) e a aplicação no XP.
 * O bônus vale para todo XP que passa por {@code Player.giveExperiencePoints} (orbes, mineração, fundição, XP de
 * debuffs). O XP do coração consumido usa {@link #withoutBonus} e fica de fora.
 */
public final class Amulets {
	private static boolean bypass;
	private static final Map<UUID, Double> REMAINDER = new HashMap<>(); // fração de XP ainda não entregue

	private Amulets() {}

	private static SurvivalNeedsConfig.Amulets cfg() {
		return SurvivalNeedsConfig.get().progression.amulets;
	}

	private static boolean enabled() {
		return SurvivalNeedsConfig.get().progression.enabled && cfg().enabled;
	}

	// ---- bônus de um amuleto ----

	public static double oreBonus(String oreId) {
		Double bonus = oreId == null ? null : cfg().oreBonus.get(oreId);
		return bonus == null ? 0.0 : bonus;
	}

	public static double heartBonus(String monsterId) {
		String tier = monsterId == null ? null : Hearts.tierOfMonster(monsterId);
		return Hearts.tier(tier == null ? "common" : tier).bonus;
	}

	/** Bônus do amuleto: o do minério mais o do coração. */
	public static double bonusOf(ItemStack amulet) {
		return oreBonus(amulet.get(ModComponents.AMULET_ORE)) + heartBonus(amulet.get(ModComponents.HEART_SOURCE));
	}

	public static ItemStack create(String oreId, String monsterId) {
		ItemStack stack = new ItemStack(ModItems.AMULET);
		stack.set(ModComponents.AMULET_ORE, oreId);
		stack.set(ModComponents.HEART_SOURCE, monsterId);
		return stack;
	}

	/** Nome do item de um id (ex.: "minecraft:diamond" vira "Diamante"), ou null. */
	public static Component itemName(String itemId) {
		Identifier id = itemId == null ? null : Identifier.tryParse(itemId);
		if (id == null) {
			return null;
		}
		return BuiltInRegistries.ITEM.getOptional(id).map(item -> new ItemStack(item).getHoverName()).orElse(null);
	}

	// ---- soma dos equipados ----

	/** Soma dos bônus dos amuletos equipados, limitada pelo teto da configuração. */
	public static double totalBonus(ServerPlayer player) {
		double[] sum = {0.0};
		TrinketsApi.getAttachment(player).forEach((access, stack) -> {
			if (stack.is(ModItems.AMULET)) {
				sum[0] += bonusOf(stack);
			}
		});
		return Math.min(cfg().maxTotalBonus, sum[0]);
	}

	/** Parte o amuleto equipado de menor bônus (protege do primeiro dardo). Falso se não há nenhum equipado. */
	public static boolean breakWeakest(ServerPlayer player) {
		TrinketSlotAccess[] weakest = {null};
		double[] lowest = {Double.MAX_VALUE};
		TrinketsApi.getAttachment(player).forEach((access, stack) -> {
			if (stack.is(ModItems.AMULET) && bonusOf(stack) < lowest[0]) {
				lowest[0] = bonusOf(stack);
				weakest[0] = access;
			}
		});
		if (weakest[0] == null) {
			return false;
		}
		weakest[0].set(ItemStack.EMPTY);
		return true;
	}

	// ---- aplicação no XP ----

	/** Quanto XP o jogador de fato recebe ao ganhar {@code points}. Guarda a fração que sobra para a próxima vez. */
	public static int boost(ServerPlayer player, int points) {
		if (bypass || points <= 0 || !enabled()) {
			return points;
		}
		double bonus = totalBonus(player);
		if (bonus <= 0.0) {
			return points;
		}
		double exact = points * (1.0 + bonus) + REMAINDER.getOrDefault(player.getUUID(), 0.0);
		int whole = (int) Math.floor(exact);
		REMAINDER.put(player.getUUID(), exact - whole);
		return whole;
	}

	/** Dá XP sem o bônus dos amuletos (coração consumido). */
	public static void withoutBonus(Runnable action) {
		bypass = true;
		try {
			action.run();
		} finally {
			bypass = false;
		}
	}

	public static void forget(UUID player) {
		REMAINDER.remove(player);
	}
}
