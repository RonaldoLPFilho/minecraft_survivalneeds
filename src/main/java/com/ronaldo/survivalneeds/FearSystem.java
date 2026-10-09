package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Medo: com a situação desfavorável, o jogador causa menos dano. Regras (qualquer uma ativa o medo):
 * <ol>
 *   <li>Cercado por 3+ inimigos com arma inferior a ferro.</li>
 *   <li>Cercado por 10+ inimigos com arma inferior a diamante.</li>
 *   <li>Perto de um monstro forte, com pouco XP e pouca armadura.</li>
 *   <li>Perto de um chefe (Warden, Wither, Dragão), sem estar muito forte.</li>
 *   <li>Com pouca vida e 2+ inimigos por perto.</li>
 *   <li>No escuro, com 2+ inimigos por perto e arma fraca.</li>
 * </ol>
 * Depois de ativado, o medo dura um tempo mínimo para não ficar piscando.
 */
public final class FearSystem {
	// Alcances
	private static final double CROWD_RADIUS = 8.0;       // "cercado"
	private static final double STRONG_RADIUS = 16.0;     // monstro forte
	private static final double BOSS_RADIUS = 24.0;       // chefes

	// Quem conta como ameaça
	private static final float MIN_THREAT_HEALTH = 8.0f;  // ignora bichinhos (slime pequeno etc.)
	private static final float STRONG_MIN_HEALTH = 40.0f; // Ravager, Warden, Wither, Elder Guardian...
	private static final int STRONG_WEIGHT = 3;           // um monstro forte conta como 3 inimigos

	// Cercado
	private static final int SURROUNDED_FEW = 3;
	private static final int SURROUNDED_MANY = 10;
	private static final int TIER_IRON = 3;
	private static final int TIER_DIAMOND = 4;

	// Monstro forte / chefe: força do jogador que dispensa o medo
	private static final int BRAVE_XP_LEVEL = 15;
	private static final int BRAVE_ARMOR_VALUE = 10;
	private static final int BOSS_BRAVE_XP_LEVEL = 30;
	private static final int BOSS_BRAVE_ARMOR_VALUE = 15;

	// Pouca vida / escuro
	private static final float LOW_HEALTH = 6.0f; // 3 corações
	private static final int DARK_LIGHT_LEVEL = 4;
	private static final int TIER_STONE = 2;

	private static final double DAMAGE_PENALTY = -0.5;    // -50% de dano
	private static final int CHECK_INTERVAL_TICKS = 10;
	private static final int MIN_FEAR_DURATION_TICKS = 60; // 3 segundos

	private static final Identifier FEAR_ID = SurvivalNeeds.id("fear_damage");
	private static final AttributeModifier FEAR_MODIFIER =
		new AttributeModifier(FEAR_ID, DAMAGE_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

	private static final Map<Item, Integer> WEAPON_TIERS = new HashMap<>();

	static {
		tier(1, Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_SPEAR, Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_SPEAR);
		tier(2, Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_SPEAR, Items.COPPER_SWORD, Items.COPPER_AXE, Items.COPPER_SPEAR);
		tier(3, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SPEAR);
		tier(4, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_SPEAR, Items.MACE, Items.TRIDENT);
		tier(5, Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_SPEAR);
	}

	private static final Set<UUID> AFRAID = new HashSet<>();
	private static final Set<UUID> FORCED = new HashSet<>();          // ativado por comando
	private static final Map<UUID, Long> FEAR_UNTIL = new HashMap<>(); // tick de jogo em que o medo pode acabar

	private FearSystem() {}

	private static void tier(int tier, Item... items) {
		for (Item item : items) {
			WEAPON_TIERS.put(item, tier);
		}
	}

	public static boolean isAfraid(ServerPlayer player) {
		return AFRAID.contains(player.getUUID());
	}

	/** Liga ou desliga o medo à força (comando /fear), ignorando todas as regras. */
	public static void setForced(ServerPlayer player, boolean forced) {
		if (forced) {
			FORCED.add(player.getUUID());
		} else {
			FORCED.remove(player.getUUID());
		}
		refresh(player);
	}

	public static void tick(ServerPlayer player) {
		if (player.tickCount % CHECK_INTERVAL_TICKS == 0) {
			refresh(player);
		}
	}

	private static void refresh(ServerPlayer player) {
		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage == null) {
			return;
		}

		UUID id = player.getUUID();
		long now = player.level().getGameTime();

		String reason = null;
		if (FORCED.contains(id)) {
			reason = "Você está com medo!";
		} else if (!player.isCreative() && !player.isSpectator() && !player.isDeadOrDying()) {
			reason = findReason(player);
		}
		if (reason != null) {
			FEAR_UNTIL.put(id, now + MIN_FEAR_DURATION_TICKS);
		}

		boolean afraid = FORCED.contains(id) || FEAR_UNTIL.getOrDefault(id, 0L) > now;
		if (afraid) {
			boolean wasAfraid = AFRAID.contains(id);
			damage.addOrUpdateTransientModifier(FEAR_MODIFIER);
			AFRAID.add(id);
			if (!wasAfraid && reason != null) {
				player.sendSystemMessage(Component.literal(reason + " Seus golpes estão mais fracos."), true);
			}
		} else {
			damage.removeModifier(FEAR_ID);
			AFRAID.remove(id);
			FEAR_UNTIL.remove(id);
		}
	}

	/** Devolve o motivo do medo (texto para o aviso) ou null se o jogador está tranquilo. */
	private static String findReason(ServerPlayer player) {
		ServerLevel level = player.level();
		List<LivingEntity> threats = level.getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(BOSS_RADIUS),
			e -> e instanceof Enemy && e.isAlive() && e.getMaxHealth() >= MIN_THREAT_HEALTH);
		if (threats.isEmpty()) {
			return null;
		}

		int crowd = 0;          // ameaças ao redor, contando monstros fortes em dobro
		boolean strongNearby = false;
		boolean bossNearby = false;
		for (LivingEntity threat : threats) {
			double distance = threat.distanceToSqr(player);
			boolean strong = threat.getMaxHealth() >= STRONG_MIN_HEALTH;
			if (isBoss(threat)) {
				bossNearby = true;
			}
			if (strong && distance <= STRONG_RADIUS * STRONG_RADIUS) {
				strongNearby = true;
			}
			if (distance <= CROWD_RADIUS * CROWD_RADIUS) {
				crowd += strong ? STRONG_WEIGHT : 1;
			}
		}

		int tier = weaponTier(player.getMainHandItem());
		int xp = player.experienceLevel;
		int armor = player.getArmorValue();

		if (crowd >= SURROUNDED_MANY && tier < TIER_DIAMOND) {
			return "Você está cercado por uma horda!";
		}
		if (crowd >= SURROUNDED_FEW && tier < TIER_IRON) {
			return "Você está cercado e com uma arma fraca!";
		}
		if (strongNearby && xp < BRAVE_XP_LEVEL && armor < BRAVE_ARMOR_VALUE) {
			return "Você está com medo desse monstro!";
		}
		if (bossNearby && !(xp >= BOSS_BRAVE_XP_LEVEL && armor >= BOSS_BRAVE_ARMOR_VALUE)) {
			return "Uma presença aterrorizante está por perto!";
		}
		if (player.getHealth() <= LOW_HEALTH && crowd >= 2) {
			return "Você está ferido e em perigo!";
		}
		if (crowd >= 2 && tier < TIER_STONE
			&& level.getMaxLocalRawBrightness(player.blockPosition()) <= DARK_LIGHT_LEVEL) {
			return "No escuro, cercado e desarmado!";
		}
		return null;
	}

	private static boolean isBoss(LivingEntity entity) {
		return entity.getType() == EntityTypes.WARDEN
			|| entity.getType() == EntityTypes.WITHER
			|| entity.getType() == EntityTypes.ENDER_DRAGON;
	}

	/** 0 = sem arma; 1 madeira/ouro; 2 pedra/cobre; 3 ferro; 4 diamante (e maça/tridente); 5 netherita. */
	private static int weaponTier(ItemStack stack) {
		return WEAPON_TIERS.getOrDefault(stack.getItem(), 0);
	}
}
