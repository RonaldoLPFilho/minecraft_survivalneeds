package com.ronaldo.survivalneeds.mixin;

import com.ronaldo.survivalneeds.Amulets;
import com.ronaldo.survivalneeds.Curses;
import com.ronaldo.survivalneeds.FatigueSystem;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cansado, o jogador não é acordado quando amanhece enquanto dorme: a checagem periódica "ainda dá para dormir
 * agora?" passa a aceitar qualquer hora para ele.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	/** Amuletos equipados aumentam todo XP ganho (orbes, mineração, fundição, debuffs). */
	@ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
	private int survivalneeds$amuletBonus(int points) {
		if (!((Object) this instanceof ServerPlayer player)) {
			return points;
		}
		return points > 0 && Curses.blocksXp(player) ? 0 : Amulets.boost(player, points);
	}

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"))
	private boolean survivalneeds$fatiguedKeepsSleeping(BedRule rule, Level level) {
		return rule.canSleep(level) || FatigueSystem.canSleepAnytime((Player) (Object) this);
	}
}
