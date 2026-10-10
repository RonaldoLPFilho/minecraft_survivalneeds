package com.ronaldo.survivalneeds.entity;

import java.util.EnumSet;

import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Espectro da Maldição. Não bate: atira dardos e se teletransporta.
 * <ol>
 *   <li><b>Despedida:</b> joga um dardo e some;</li>
 *   <li><b>teletransporte:</b> reaparece perto do alvo, de frente para ele;</li>
 *   <li><b>tiro:</b> mira por um instante e atira outro dardo (dois seguidos abaixo de metade da vida);</li>
 *   <li><b>abertura:</b> depois de se teletransportar, fica {@code specterTeleportCooldownTicks} (5 s) sem poder
 *       fazê-lo de novo. É a janela em que o jogador pode alcançá-lo e bater.</li>
 * </ol>
 * Os tempos ficam em {@code curse} na configuração (lidos na hora, valem com /survivalneeds reload).
 * O combate antigo, de saltos e golpes, está guardado em {@link com.ronaldo.survivalneeds.entity.ai.LeapStrikeGoal}.
 */
public class CurseSpecter extends Monster {
	private boolean vanishing;

	public CurseSpecter(EntityType<? extends CurseSpecter> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 100.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 9.0)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
			.add(Attributes.ARMOR, 6.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new TeleportCombatGoal(this));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!level().isClientSide() && isAlive()) {
			playPresence();
		}
		if (level().isClientSide() && !isInvisible()) {
			if (random.nextInt(2) == 0) {
				level().addParticle(ParticleTypes.WITCH, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0.0, 0.0, 0.0);
			}
			if (random.nextInt(4) == 0) { // fumaça espectral subindo do corpo
				level().addParticle(ParticleTypes.SOUL, getRandomX(0.7), getY() + 0.5 + random.nextDouble() * 2.0, getRandomZ(0.7), 0.0, 0.03, 0.0);
			}
		}
	}

	/**
	 * Sons de presença, ouvidos por quem está por perto (uns 25 blocos): batimento grave e constante, sussurros
	 * de caverna de tempos em tempos e, de vez em quando, um lamento de alma.
	 */
	private void playPresence() {
		if (tickCount % 35 == 0) {
			playSound(SoundEvents.WARDEN_HEARTBEAT, 1.7f, 0.55f);
		}
		if (tickCount % 140 == 70 && random.nextInt(2) == 0) {
			playSound(SoundEvents.AMBIENT_CAVE.value(), 1.6f, 0.5f + random.nextFloat() * 0.2f);
		}
		if (tickCount % 220 == 110 && random.nextInt(3) == 0) {
			playSound(SoundEvents.SOUL_ESCAPE.value(), 1.4f, 0.6f);
		}
	}

	/** Desfaz-se em fumaça, sem morrer (não solta nada): é o que ele faz depois de amaldiçoar alguém. */
	public void fadeAway(ServerLevel level) {
		level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.2, getZ(), 60, 0.6, 1.2, 0.6, 0.3);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 20, 0.5, 1.0, 0.5, 0.08);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SKELETON_DEATH, getSoundSource(), 1.2f, 0.5f);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.4f, 0.4f);
		discard();
	}

	/** Some e reaparece sem poder ser ferido no meio do caminho. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return !vanishing && super.hurtServer(level, source, amount);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_SKELETON_DEATH;
	}

	private static SurvivalNeedsConfig.Curse cfg() {
		return SurvivalNeedsConfig.get().curse;
	}

	/** Dispara um dardo no alvo (dois, em rajada, com menos de metade da vida). */
	private void shoot(ServerLevel level, LivingEntity target) {
		SurvivalNeedsConfig.Curse cfg = cfg();
		swingForAttack(InteractionHand.MAIN_HAND);
		CursedDart.fireAt(level, this, target, cfg.specterDartSpeed, 0.0f);
		if (getHealth() < getMaxHealth() * cfg.specterVolleyBelowHealth) {
			CursedDart.fireAt(level, this, target, cfg.specterDartSpeed, 0.12f);
		}
		playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2f, 0.8f);
	}

	private enum Phase {
		ARRIVED, VANISHING
	}

	/** O ciclo: atirar, sumir, reaparecer perto do alvo e atirar; entre um teletransporte e outro, a janela de 5 s. */
	private static final class TeleportCombatGoal extends Goal {
		private final CurseSpecter mob;
		private Phase phase = Phase.ARRIVED;
		private int ticksSinceTeleport;
		private int vanishTicks;
		private boolean arrivalShotFired;

		TeleportCombatGoal(CurseSpecter mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = mob.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() || phase == Phase.VANISHING;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			mob.setAggressive(true);
			phase = Phase.ARRIVED;
			ticksSinceTeleport = cfg().specterTeleportCooldownTicks; // ao avistar o alvo, já pode começar pelo dardo e o sumiço
			arrivalShotFired = true;
		}

		@Override
		public void stop() {
			mob.setAggressive(false);
			mob.getNavigation().stop();
			mob.setInvisible(false);
			mob.vanishing = false;
		}

		@Override
		public void tick() {
			LivingEntity target = mob.getTarget();
			if (!(mob.level() instanceof ServerLevel level)) {
				return;
			}
			if (phase == Phase.VANISHING) {
				vanish(level, target);
				return;
			}
			if (target == null) {
				return;
			}
			SurvivalNeedsConfig.Curse cfg = cfg();
			mob.getNavigation().stop();
			mob.getLookControl().setLookAt(target, 60.0f, 60.0f);
			ticksSinceTeleport++;

			// Recém-chegado: mira por um instante e atira
			if (!arrivalShotFired && ticksSinceTeleport >= cfg.specterAimTicks && mob.hasLineOfSight(target)) {
				mob.shoot(level, target);
				arrivalShotFired = true;
			}

			// Passada a janela: despede-se com um dardo e some
			if (ticksSinceTeleport >= cfg.specterTeleportCooldownTicks && mob.hasLineOfSight(target)) {
				mob.shoot(level, target);
				startVanishing(level);
			}
		}

		private void startVanishing(ServerLevel level) {
			phase = Phase.VANISHING;
			vanishTicks = 0;
			mob.vanishing = true;
			mob.setInvisible(true);
			effects(level, mob.position());
		}

		/** Invisível por um instante; depois reaparece perto do alvo, ou espera um lugar bom. */
		private void vanish(ServerLevel level, LivingEntity target) {
			vanishTicks++;
			if (vanishTicks < cfg().specterVanishTicks) {
				return;
			}
			if (target == null || !target.isAlive()) {
				finishVanish(level); // o alvo se foi: reaparece onde está
				return;
			}
			Vec3 destination = findDestination(level, target);
			if (destination == null) {
				if (vanishTicks > 60) {
					finishVanish(level); // sem lugar para ir: volta onde estava
				}
				return;
			}
			mob.snapTo(destination.x, destination.y, destination.z);
			faceTarget(target);
			effects(level, destination);
			finishVanish(level);
		}

		private void finishVanish(ServerLevel level) {
			mob.vanishing = false;
			mob.setInvisible(false);
			phase = Phase.ARRIVED;
			ticksSinceTeleport = 0;
			arrivalShotFired = false;
		}

		private void faceTarget(LivingEntity target) {
			double dx = target.getX() - mob.getX();
			double dz = target.getZ() - mob.getZ();
			float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
			mob.setYRot(yaw);
			mob.setYHeadRot(yaw);
			mob.setYBodyRot(yaw);
		}

		/** Fumaça e som no ponto de partida e no de chegada. */
		private void effects(ServerLevel level, Vec3 at) {
			level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1.2, at.z, 40, 0.5, 1.0, 0.5, 0.2);
			level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 12, 0.4, 0.8, 0.4, 0.05);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, mob.getSoundSource(), 1.4f, 0.55f);
		}

		/** Um chão livre a {@code min}..{@code max} blocos do alvo, de onde se vê o alvo. */
		private Vec3 findDestination(ServerLevel level, LivingEntity target) {
			SurvivalNeedsConfig.Curse cfg = cfg();
			Vec3 targetEyes = target.getEyePosition();
			for (int attempt = 0; attempt < 24; attempt++) {
				double angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
				double distance = cfg.specterTeleportMinDistance
					+ mob.getRandom().nextDouble() * Math.max(0.0, cfg.specterTeleportMaxDistance - cfg.specterTeleportMinDistance);
				double x = target.getX() + Math.cos(angle) * distance;
				double z = target.getZ() + Math.sin(angle) * distance;
				Vec3 ground = findGround(level, x, target.getY(), z);
				if (ground == null) {
					continue;
				}
				AABB box = mob.getBoundingBox().move(ground.x - mob.getX(), ground.y - mob.getY(), ground.z - mob.getZ());
				if (!level.noCollision(mob, box)) {
					continue;
				}
				Vec3 eyes = new Vec3(ground.x, ground.y + mob.getEyeHeight(), ground.z);
				HitResult sight = level.clip(new ClipContext(eyes, targetEyes, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
				if (sight.getType() == HitResult.Type.MISS) {
					return ground;
				}
			}
			return null;
		}

		/** O chão em (x, z) perto da altura {@code aroundY}: sólido embaixo, espaço livre em cima, sem líquido. */
		private Vec3 findGround(ServerLevel level, double x, double aroundY, double z) {
			int bx = Mth.floor(x);
			int bz = Mth.floor(z);
			int startY = Mth.floor(aroundY) + 4;
			for (int y = startY; y > startY - 12; y--) {
				BlockPos below = new BlockPos(bx, y - 1, bz);
				BlockPos feet = new BlockPos(bx, y, bz);
				if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)
						&& level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
						&& level.getFluidState(feet).isEmpty()
						&& level.getFluidState(below).isEmpty()) {
					return new Vec3(bx + 0.5, y, bz + 0.5);
				}
			}
			return null;
		}
	}
}
