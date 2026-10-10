package com.ronaldo.survivalneeds;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Coração de monstro: um item só, que guarda de qual monstro veio. Consumir dá XP conforme a raridade. */
public class MonsterHeartItem extends Item {
	public MonsterHeartItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		String source = stack.get(ModComponents.HEART_SOURCE);
		Component monster = source == null ? null : Hearts.monsterName(source);
		if (monster == null) {
			return Component.translatable("item.survivalneeds.monster_heart").withStyle(Hearts.color(Hearts.tierOf(stack)));
		}
		return Component.translatable("item.survivalneeds.monster_heart.of", monster)
			.withStyle(Hearts.color(Hearts.tierOf(stack)));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		int xp = Hearts.tier(Hearts.tierOf(stack)).xp; // lido antes de o item ser consumido
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (!level.isClientSide() && entity instanceof ServerPlayer player && !player.isCreative()) {
			// XP direto, sem bônus de amuleto
			Amulets.withoutBonus(() -> player.giveExperiencePoints(xp));
			player.sendSystemMessage(Component.literal("+" + xp + " XP (coração consumido)"), true);
		}
		return result;
	}
}
