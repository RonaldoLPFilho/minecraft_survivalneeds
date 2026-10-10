package com.ronaldo.survivalneeds.entity;

import com.ronaldo.survivalneeds.BrubinMenu;
import com.ronaldo.survivalneeds.Curse;
import com.ronaldo.survivalneeds.Curses;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

/**
 * Princess Brubin: gentil, parada onde nasceu, e invulnerável. Quem está amaldiçoado clica nela e oferece minérios,
 * XP, corações, amuletos ou fragmentos até chegar ao preço da cura (ver {@link BrubinMenu}).
 */
public class PrincessBrubin extends PathfinderMob {
	private static final String[] GREETINGS = {
		"Que a luz acompanhe você, viajante!",
		"Fico feliz em ver você bem. Não encontrei nenhuma maldição em você.",
		"Cuide-se por aí. As sombras andam inquietas.",
		"Se um dia for amaldiçoado, venha me ver.",
	};

	public PrincessBrubin(EntityType<? extends PrincessBrubin> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0f));
		goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceSqr) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** Só o vazio e o comando /kill a atingem. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		Curse curse = Curses.current(serverPlayer);
		if (curse == null) {
			serverPlayer.sendSystemMessage(Component.literal("§6Princess Brubin: §f" + GREETINGS[random.nextInt(GREETINGS.length)]), false);
			return InteractionResult.SUCCESS;
		}

		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
		int entityId = getId();
		serverPlayer.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Component getDisplayName() {
				return Component.translatable("container.survivalneeds.brubin");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
				return new BrubinMenu(containerId, inventory, PrincessBrubin.this);
			}

			@Override
			public Integer getScreenOpeningData(ServerPlayer opening) {
				return entityId;
			}
		});
		return InteractionResult.SUCCESS;
	}
}
