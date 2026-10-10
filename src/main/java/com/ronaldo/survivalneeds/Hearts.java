package com.ronaldo.survivalneeds;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Coração de monstro: quem dropa, com que chance e o que cada raridade vale. Só mortes causadas por jogador
 * dropam. Para as raridades comum, incomum e rara há proteção contra azar: depois de {@code pityKills} mortes sem
 * coração, a próxima dá um garantido.
 */
public final class Hearts {
	/** Mortes seguidas de monstros comuns, incomuns ou raros sem nenhum coração. Salvo no jogador. */
	private static final AttachmentType<Integer> DRY_KILLS = AttachmentRegistry.create(
		SurvivalNeeds.id("heart_dry_kills"),
		builder -> builder.persistent(Codec.INT).initializer(() -> 0).copyOnDeath());

	private Hearts() {}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && entity.level() instanceof ServerLevel level
					&& !(entity instanceof Player)) {
				onKill(player, entity, level);
			}
		});
	}

	private static SurvivalNeedsConfig.Hearts cfg() {
		return SurvivalNeedsConfig.get().progression.hearts;
	}

	private static boolean enabled() {
		return SurvivalNeedsConfig.get().progression.enabled && cfg().enabled;
	}

	// ---- raridades ----

	/** A raridade de um monstro (id do tipo), ou null se ele não dropa coração. */
	public static String tierOfMonster(String entityId) {
		return cfg().monsters.get(entityId);
	}

	/** A raridade de um coração (sem origem conhecida vale como comum). */
	public static String tierOf(ItemStack heart) {
		String source = heart.get(ModComponents.HEART_SOURCE);
		String tier = source == null ? null : tierOfMonster(source);
		return tier == null ? "common" : tier;
	}

	public static SurvivalNeedsConfig.HeartTier tier(String key) {
		Map<String, SurvivalNeedsConfig.HeartTier> tiers = cfg().tiers;
		SurvivalNeedsConfig.HeartTier tier = tiers.get(key);
		return tier != null ? tier : tiers.getOrDefault("common", new SurvivalNeedsConfig.HeartTier(0.06, 10, 0.03));
	}

	public static ChatFormatting color(String tier) {
		return switch (tier) {
			case "uncommon" -> ChatFormatting.YELLOW;
			case "rare" -> ChatFormatting.AQUA;
			case "epic" -> ChatFormatting.LIGHT_PURPLE;
			case "legendary" -> ChatFormatting.GOLD;
			default -> ChatFormatting.WHITE;
		};
	}

	public static String tierName(String tier) {
		return switch (tier) {
			case "uncommon" -> "Incomum";
			case "rare" -> "Raro";
			case "epic" -> "Épico";
			case "legendary" -> "Lendário";
			default -> "Comum";
		};
	}

	/** Um coração do monstro com o id dado. */
	public static ItemStack create(String entityId) {
		ItemStack stack = new ItemStack(ModItems.MONSTER_HEART);
		stack.set(ModComponents.HEART_SOURCE, entityId);
		return stack;
	}

	// ---- drops ----

	private static void onKill(ServerPlayer player, LivingEntity victim, ServerLevel level) {
		if (!enabled() || player.isCreative() || player.isSpectator()) {
			return;
		}
		String entityId = EntityType.getKey(victim.getType()).toString();
		String tierKey = tierOfMonster(entityId);
		if (tierKey == null) {
			return;
		}
		SurvivalNeedsConfig.Hearts cfg = cfg();
		boolean guaranteedTier = tier(tierKey).chance >= 1.0;

		double chance = tier(tierKey).chance + cfg.lootingBonus * lootingLevel(player, level);
		int dry = player.getAttachedOrElse(DRY_KILLS, 0);
		boolean pity = !guaranteedTier && cfg.pityKills > 0 && dry + 1 >= cfg.pityKills;

		if (guaranteedTier || pity || victim.getRandom().nextDouble() < chance) {
			victim.spawnAtLocation(level, create(entityId));
			if (!guaranteedTier) {
				player.setAttached(DRY_KILLS, 0);
			}
		} else if (!guaranteedTier) {
			player.setAttached(DRY_KILLS, dry + 1);
		}
	}

	private static int lootingLevel(ServerPlayer player, ServerLevel level) {
		Holder<Enchantment> looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.LOOTING);
		return EnchantmentHelper.getItemEnchantmentLevel(looting, player.getMainHandItem());
	}

	/** Nome do tipo de entidade a partir do id, ou null se não existir. */
	public static net.minecraft.network.chat.Component monsterName(String entityId) {
		Identifier id = Identifier.tryParse(entityId);
		if (id == null) {
			return null;
		}
		return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id)
			.map(EntityType::getDescription).orElse(null);
	}
}
