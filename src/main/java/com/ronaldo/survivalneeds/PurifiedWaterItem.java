package com.ronaldo.survivalneeds;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Garrafa de água purificada: ao beber, devolve pontos de sede. */
public class PurifiedWaterItem extends Item {
	public PurifiedWaterItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			ThirstSystem.add(player, SurvivalNeedsConfig.get().thirst.purifiedWaterRestores);
		}
		return result;
	}
}
