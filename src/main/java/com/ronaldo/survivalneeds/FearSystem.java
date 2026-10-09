package com.ronaldo.survivalneeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import com.ronaldo.survivalneeds.debuff.DebuffEffect;
import com.ronaldo.survivalneeds.debuff.DebuffSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Medo: com a situação desfavorável, o jogador causa menos dano. Valores em {@code fear} na configuração.
 * Regras (qualquer uma ativa o medo):
 * <ol>
 *   <li>Cercado por poucos inimigos com arma fraca.</li>
 *   <li>Cercado por uma horda com arma que não é de topo.</li>
 *   <li>Perto de um monstro forte, com pouco XP e pouca armadura.</li>
 *   <li>Perto de um chefe (Warden, Wither, Dragão), sem estar muito forte.</li>
 *   <li>Com 1 coração de vida ou menos, sempre, sem olhar arma, XP nem inimigos.</li>
 *   <li>Com 2 corações de vida ou menos e qualquer inimigo na região.</li>
 *   <li>Com 3 corações de vida ou menos e alguns inimigos por perto.</li>
 *   <li>No escuro, com alguns inimigos por perto e arma fraca.</li>
 * </ol>
 * Depois de ativado, o medo dura um tempo mínimo para não ficar piscando.
 */
public final class FearSystem {
	private static final String SOURCE = "fear";
	private static final int CHECK_INTERVAL_TICKS = 10;

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

	private static SurvivalNeedsConfig.Fear cfg() {
		return SurvivalNeedsConfig.get().fear;
	}

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
		// Com medo o jogador causa menos dano
		DebuffSystem.set(player, SOURCE, isAfraid(player),
			() -> List.of(new DebuffEffect.DamageReduction(cfg().damageReduction)));
	}

	private static void refresh(ServerPlayer player) {
		UUID id = player.getUUID();
		long now = player.level().getGameTime();

		String reason = null;
		if (FORCED.contains(id)) {
			reason = "Você está com medo!";
		} else if (!player.isCreative() && !player.isSpectator() && !player.isDeadOrDying()) {
			reason = findReason(player);
		}
		if (reason != null) {
			FEAR_UNTIL.put(id, now + SurvivalNeedsConfig.ticks(cfg().minFearSeconds));
		}

		boolean afraid = FORCED.contains(id) || FEAR_UNTIL.getOrDefault(id, 0L) > now;
		if (afraid) {
			boolean wasAfraid = AFRAID.contains(id);
			AFRAID.add(id);
			if (!wasAfraid && reason != null) {
				player.sendSystemMessage(Component.literal(reason + " Seus golpes estão mais fracos."), true);
			}
		} else {
			AFRAID.remove(id);
			FEAR_UNTIL.remove(id);
		}
	}

	/** Devolve o motivo do medo (texto para o aviso) ou null se o jogador está tranquilo. */
	private static String findReason(ServerPlayer player) {
		SurvivalNeedsConfig.Fear cfg = cfg();
		float health = player.getHealth();

		// 1 coração: o medo vem sempre, mesmo sem inimigos por perto
		if (health <= cfg.lastHeartHealth) {
			return "Você está quase morrendo!";
		}

		// Com 2 corações a região de busca de inimigos se expande bastante (só então vale o custo da busca grande)
		boolean severe = health <= cfg.severeHealth;
		double searchRadius = severe ? Math.max(cfg.severeRadius, cfg.bossRadius) : cfg.bossRadius;

		ServerLevel level = player.level();
		List<LivingEntity> hostiles = level.getEntitiesOfClass(
			LivingEntity.class,
			player.getBoundingBox().inflate(searchRadius),
			e -> e instanceof Enemy && e.isAlive());

		// 2 corações: qualquer inimigo na região já causa medo
		if (severe && !hostiles.isEmpty()) {
			return "Você está muito ferido e há inimigos por perto!";
		}

		// Acima disso valem as regras de cerco, monstro forte, chefe etc., só com ameaças de verdade
		List<LivingEntity> threats = hostiles.stream()
			.filter(e -> e.getMaxHealth() >= cfg.minThreatHealth)
			.toList();
		if (threats.isEmpty()) {
			return null;
		}

		int crowd = 0;          // ameaças ao redor, contando monstros fortes em dobro
		boolean strongNearby = false;
		boolean bossNearby = false;
		for (LivingEntity threat : threats) {
			double distance = threat.distanceToSqr(player);
			boolean strong = threat.getMaxHealth() >= cfg.strongMinHealth;
			if (isBoss(threat) && distance <= cfg.bossRadius * cfg.bossRadius) {
				bossNearby = true;
			}
			if (strong && distance <= cfg.strongRadius * cfg.strongRadius) {
				strongNearby = true;
			}
			if (distance <= cfg.crowdRadius * cfg.crowdRadius) {
				crowd += strong ? cfg.strongWeight : 1;
			}
		}

		int tier = weaponTier(player.getMainHandItem());
		int xp = player.experienceLevel;
		int armor = player.getArmorValue();

		if (crowd >= cfg.surroundedMany && tier < cfg.manyMinWeaponTier) {
			return "Você está cercado por uma horda!";
		}
		if (crowd >= cfg.surroundedFew && tier < cfg.fewMinWeaponTier) {
			return "Você está cercado e com uma arma fraca!";
		}
		if (strongNearby && xp < cfg.braveXpLevel && armor < cfg.braveArmorValue) {
			return "Você está com medo desse monstro!";
		}
		if (bossNearby && !(xp >= cfg.bossBraveXpLevel && armor >= cfg.bossBraveArmorValue)) {
			return "Uma presença aterrorizante está por perto!";
		}
		if (health <= cfg.lowHealth && crowd >= cfg.lowHealthEnemies) {
			return "Você está ferido e em perigo!";
		}
		if (crowd >= cfg.darkEnemies && tier < cfg.darkMinWeaponTier
			&& level.getMaxLocalRawBrightness(player.blockPosition()) <= cfg.darkLightLevel) {
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
