package com.ronaldo.survivalneeds.entity.ai;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Combate de salto e golpe, guardado para um futuro monstro (hoje nenhum mob usa). Foi o combate do Espectro da
 * Maldição antes de ele passar a se teletransportar. O ciclo:
 * <ol>
 *   <li><b>Perseguição:</b> corre atrás do alvo e bate por {@code chaseTicks};</li>
 *   <li><b>pulo para trás:</b> salta para longe do alvo;</li>
 *   <li><b>tiro:</b> pousa, mira por {@code aimTicks} e dispara a ação de tiro ({@link Shot});</li>
 *   <li><b>pulo para frente:</b> salta sobre o alvo (com força proporcional à distância) e volta à perseguição.</li>
 * </ol>
 * {@link #requestRetreat()} adianta o pulo para trás (ex.: ao levar um golpe forte).
 * <p>
 * Para usar: {@code goalSelector.addGoal(1, new LeapStrikeGoal<>(this, new LeapStrikeGoal.Params(80, 0.8, 12, 0.9, 15), shot))}.
 */
public final class LeapStrikeGoal<T extends PathfinderMob> extends Goal {
	private static final double JUMP_HEIGHT = 0.5;

	/**
	 * @param chaseTicks         tempo de perseguição antes do pulo para trás (80 = 4 s)
	 * @param backJumpPower      força do pulo para trás (0.8 leva uns 6 blocos)
	 * @param aimTicks           tempo parado, mirando, antes do tiro
	 * @param leapPower          força máxima do pulo para frente (limitada pela distância)
	 * @param meleeCooldownTicks intervalo entre golpes de corpo a corpo
	 */
	public record Params(int chaseTicks, double backJumpPower, int aimTicks, double leapPower, int meleeCooldownTicks) {}

	/** O que o monstro faz na fase de tiro (disparar um dardo, uma flecha...). */
	@FunctionalInterface
	public interface Shot<T extends PathfinderMob> {
		void fire(ServerLevel level, T mob, LivingEntity target);
	}

	private enum Phase {
		CHASE, BACKFLIP, AIM, LUNGE
	}

	private final T mob;
	private final Params params;
	private final Shot<T> shot;
	private Phase phase = Phase.CHASE;
	private int phaseTicks;
	private int meleeCooldown;
	private boolean leftGround;
	private boolean retreatRequested;

	public LeapStrikeGoal(T mob, Params params, Shot<T> shot) {
		this.mob = mob;
		this.params = params;
		this.shot = shot;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	/** Pede o pulo para trás assim que o monstro estiver no chão. */
	public void requestRetreat() {
		retreatRequested = true;
	}

	@Override
	public boolean canUse() {
		LivingEntity target = mob.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		mob.setAggressive(true);
		enter(Phase.CHASE);
	}

	@Override
	public void stop() {
		mob.setAggressive(false);
		mob.getNavigation().stop();
	}

	private void enter(Phase next) {
		phase = next;
		phaseTicks = 0;
		leftGround = false;
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		mob.getLookControl().setLookAt(target, 60.0f, 60.0f);
		phaseTicks++;
		if (meleeCooldown > 0) {
			meleeCooldown--;
		}

		switch (phase) {
			case CHASE -> chase(level, target);
			case BACKFLIP -> backflip();
			case AIM -> aim(level, target);
			case LUNGE -> lunge(level, target);
		}
	}

	/** Corre atrás do alvo e bate; passado o tempo (ou pedido o recuo), pula para trás. */
	private void chase(ServerLevel level, LivingEntity target) {
		if ((retreatRequested || phaseTicks >= params.chaseTicks()) && mob.onGround()) {
			retreatRequested = false;
			Vec3 away = mob.position().subtract(target.position());
			Vec3 dir = new Vec3(away.x, 0.0, away.z).normalize();
			mob.getNavigation().stop();
			mob.setDeltaMovement(dir.x * params.backJumpPower(), JUMP_HEIGHT, dir.z * params.backJumpPower());
			mob.playSound(SoundEvents.RAVAGER_STEP, 1.0f, 0.7f);
			enter(Phase.BACKFLIP);
			return;
		}
		if (phaseTicks % 4 == 1) {
			mob.getNavigation().moveTo(target, 1.35);
		}
		melee(level, target);
	}

	/** No ar, para longe do alvo; ao pousar, passa a mirar. */
	private void backflip() {
		if (!mob.onGround()) {
			leftGround = true;
		}
		if ((leftGround && mob.onGround()) || phaseTicks > 40) {
			mob.getNavigation().stop();
			enter(Phase.AIM);
		}
	}

	/** Parado, mira por um instante e atira (só com o alvo à vista); depois salta para a frente. */
	private void aim(ServerLevel level, LivingEntity target) {
		mob.getNavigation().stop();
		if (phaseTicks < params.aimTicks()) {
			return;
		}
		if (mob.hasLineOfSight(target)) {
			shot.fire(level, mob, target);
			startLunge(target);
		} else if (phaseTicks > params.aimTicks() + 40) {
			startLunge(target); // não achou o alvo: parte para cima dele
		}
	}

	private void startLunge(LivingEntity target) {
		enter(Phase.LUNGE);
		if (!mob.onGround()) {
			return;
		}
		Vec3 toward = target.position().subtract(mob.position());
		double distance = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
		Vec3 dir = new Vec3(toward.x, 0.0, toward.z).normalize();
		// força proporcional à distância (o ar freia o pulo), limitada pelo máximo
		double power = Math.max(0.3, Math.min(params.leapPower(), distance * 0.125));
		mob.setDeltaMovement(dir.x * power, JUMP_HEIGHT, dir.z * power);
		mob.playSound(SoundEvents.RAVAGER_ROAR, 0.8f, 1.2f);
	}

	/** Salto sobre o alvo; pousando (ou passado um tempo), volta à perseguição. */
	private void lunge(ServerLevel level, LivingEntity target) {
		if (!mob.onGround()) {
			leftGround = true;
		}
		melee(level, target);
		if ((leftGround && mob.onGround()) || phaseTicks > 30) {
			enter(Phase.CHASE);
		}
	}

	private void melee(ServerLevel level, LivingEntity target) {
		double reach = mob.getBbWidth() * 1.6 + target.getBbWidth();
		if (mob.distanceToSqr(target) <= reach * reach && meleeCooldown <= 0) {
			meleeCooldown = params.meleeCooldownTicks();
			mob.swingForAttack(InteractionHand.MAIN_HAND);
			mob.doHurtTarget(level, target);
		}
	}
}
