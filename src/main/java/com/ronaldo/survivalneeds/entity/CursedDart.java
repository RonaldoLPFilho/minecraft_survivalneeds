package com.ronaldo.survivalneeds.entity;

import com.ronaldo.survivalneeds.Curses;
import com.ronaldo.survivalneeds.ModEntities;
import com.ronaldo.survivalneeds.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * O dardo do Espectro da Maldição. Voa reto e rápido. Ao acertar um jogador, o escudo de frente segura; sem escudo,
 * o amuleto equipado se parte (primeiro dardo) ou a maldição é aplicada (ver {@link Curses#onDartHit}).
 */
public class CursedDart extends ThrowableItemProjectile {
	public static final float DAMAGE = 2.0f;
	private static final int LIFETIME_TICKS = 100;

	public CursedDart(EntityType<? extends CursedDart> type, Level level) {
		super(type, level);
	}

	public CursedDart(Level level, LivingEntity owner) {
		super(ModEntities.CURSED_DART, owner, level, new ItemStack(ModItems.CURSED_DART));
	}

	/**
	 * Dispara um dardo de {@code shooter} no peito de {@code target}, adiantando-se ao movimento dele (o dardo voa
	 * reto, sem queda). {@code spread} desvia um pouco, para uma rajada não ser um só ponto.
	 */
	public static void fireAt(ServerLevel level, LivingEntity shooter, LivingEntity target, double speed, float spread) {
		CursedDart dart = new CursedDart(level, shooter);
		double vx = target.getX() - target.xo;
		double vy = target.onGround() ? 0.0 : target.getY() - target.yo;
		double vz = target.getZ() - target.zo;
		Vec3 aim = new Vec3(target.getX(), target.getY(0.5), target.getZ());
		for (int i = 0; i < 3; i++) {
			double flight = aim.distanceTo(dart.position()) / speed;
			aim = new Vec3(target.getX() + vx * flight, target.getY(0.5) + vy * flight, target.getZ() + vz * flight);
		}
		double dx = aim.x - dart.getX() + (shooter.getRandom().nextDouble() - 0.5) * spread * 8.0;
		double dy = aim.y - dart.getY();
		double dz = aim.z - dart.getZ() + (shooter.getRandom().nextDouble() - 0.5) * spread * 8.0;
		Projectile.spawnProjectileUsingShoot(dart, level, new ItemStack(ModItems.CURSED_DART), dx, dy, dz, (float) speed, 0.0f);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.CURSED_DART;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0; // voa reto
	}

	@Override
	protected float getAirDrag() {
		return 1.0f; // não perde velocidade no ar: mira mais precisa
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			level().addParticle(ParticleTypes.WITCH, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
			if (random.nextInt(3) == 0) {
				level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
			}
		} else if (tickCount > LIFETIME_TICKS) {
			discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		Entity target = result.getEntity();
		DamageSource source = server.damageSources().thrown(this, getOwner());

		if (target instanceof ServerPlayer player) {
			boolean blocked = isBlockedByShield(player);
			player.hurtServer(server, source, DAMAGE); // com escudo, o jogo cuida do desgaste e do som
			if (!blocked) {
				// Cumpriu a missão: o Espectro que atirou some. Enquanto não amaldiçoa, continua atirando.
				if (Curses.onDartHit(player) && getOwner() instanceof CurseSpecter specter) {
					specter.fadeAway(server);
				}
			}
		} else {
			target.hurtServer(server, source, DAMAGE);
		}
	}

	/** O jogador está com o escudo erguido e de frente para o dardo (meia-volta à frente dele). */
	private boolean isBlockedByShield(ServerPlayer player) {
		if (!player.isBlocking()) {
			return false;
		}
		Vec3 toDart = position().subtract(player.position());
		Vec3 view = player.getViewVector(1.0f);
		Vec3 flatDart = new Vec3(toDart.x, 0.0, toDart.z).normalize();
		Vec3 flatView = new Vec3(view.x, 0.0, view.z).normalize();
		return flatDart.dot(flatView) > 0.0;
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!level().isClientSide()) {
			discard();
		}
	}
}
