package com.eversonbdeus.kingdomsiege.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// ─── Seguir dono (FOLLOW) ─────────────────────────────────────────────────

final class FollowOwnerGoal extends Goal {
	private final CastleSoldierEntity soldier;
	private final double speedModifier;
	private final double startDistanceSqr;
	private final double stopDistanceSqr;
	private int timeToRecalculatePath;
	private Vec3 roamAnchor;
	private int roamRecalculateTicks;
	private int roamRestTicks;

	FollowOwnerGoal(CastleSoldierEntity soldier, double speedModifier, double startDistance, double stopDistance) {
		this.soldier = soldier;
		this.speedModifier = speedModifier;
		this.startDistanceSqr = startDistance * startDistance;
		this.stopDistanceSqr = stopDistance * stopDistance;
		this.timeToRecalculatePath = 0;
		this.roamAnchor = null;
		this.roamRecalculateTicks = 0;
		this.roamRestTicks = 0;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		Player owner = soldier.getValidOwnerPlayer();

		if (!soldier.isFollowMode() || owner == null) {
			return false;
		}

		if (soldier.getTarget() != null) {
			return false;
		}

		double distanceToOwnerSqr = soldier.distanceToSqr(owner);
		return soldier.shouldRoamAroundStoppedOwner(owner)
				|| distanceToOwnerSqr > startDistanceSqr
				|| (!soldier.hasOwnerBeenStillLongEnough() && distanceToOwnerSqr > stopDistanceSqr);
	}

	@Override
	public boolean canContinueToUse() {
		Player owner = soldier.getValidOwnerPlayer();

		if (!soldier.isFollowMode() || owner == null) {
			return false;
		}

		if (soldier.getTarget() != null) {
			return false;
		}

		double distanceToOwnerSqr = soldier.distanceToSqr(owner);

		// Dono voltou a andar: cancela descanso e só continua se precisar seguir.
		if (!soldier.hasOwnerBeenStillLongEnough()) {
			roamRestTicks = 0;
			return distanceToOwnerSqr > stopDistanceSqr;
		}

		return soldier.shouldRoamAroundStoppedOwner(owner) || distanceToOwnerSqr > stopDistanceSqr;
	}

	@Override
	public void start() {
		timeToRecalculatePath = 0;
		roamAnchor = null;
		roamRecalculateTicks = 0;
		roamRestTicks = 0;
	}

	@Override
	public void stop() {
		roamAnchor = null;
		roamRecalculateTicks = 0;
		roamRestTicks = 0;
		soldier.getNavigation().stop();
	}

	@Override
	public void tick() {
		Player owner = soldier.getValidOwnerPlayer();

		if (owner == null) {
			return;
		}

		double distanceToOwnerSqr = soldier.distanceToSqr(owner);

		if (distanceToOwnerSqr >= CastleSoldierEntity.FOLLOW_REJOIN_DISTANCE_SQR) {
			rejoinNearOwner(owner);
			return;
		}

		soldier.getLookControl().setLookAt(owner, 30.0F, 30.0F);

		if (soldier.shouldRoamAroundStoppedOwner(owner)) {
			tickRoamAroundOwner(owner);
			return;
		}

		// Dono voltou a andar — limpa estado de vagar.
		roamAnchor = null;
		roamRecalculateTicks = 0;
		roamRestTicks = 0;

		if (distanceToOwnerSqr <= stopDistanceSqr) {
			soldier.getNavigation().stop();
			return;
		}

		Vec3 followAnchor = soldier.getFollowAnchor(owner);

		if (--timeToRecalculatePath <= 0) {
			timeToRecalculatePath = 10;
			soldier.getNavigation().moveTo(followAnchor.x, followAnchor.y, followAnchor.z, speedModifier);
		}
	}

	/**
	 * Vaga ao redor do dono parado.
	 * [FASE 4] Usa FOLLOW_ROAM_ARRIVAL_THRESHOLD_SQR (4.0D = 2 blocos) para
	 * registrar chegada com folga, evitando oscilação no mesmo ponto.
	 */
	private void tickRoamAroundOwner(Player owner) {
		if (!soldier.hasOwnerBeenStillLongEnough()) {
			roamAnchor = null;
			roamRestTicks = 0;
			soldier.getNavigation().stop();
			return;
		}

		// Fase de repouso: chegou ao destino, descansa antes de ir ao próximo.
		if (roamRestTicks > 0) {
			roamRestTicks--;
			soldier.getNavigation().stop();
			return;
		}

		// [FASE 4] Threshold de chegada ajustado para 2 blocos (era 1.41).
		if (roamAnchor != null && soldier.position().distanceToSqr(roamAnchor) <= CastleSoldierEntity.FOLLOW_ROAM_ARRIVAL_THRESHOLD_SQR) {
			roamRestTicks = CastleSoldierEntity.FOLLOW_ROAM_REST_MIN_TICKS
					+ soldier.getRandom().nextInt(CastleSoldierEntity.FOLLOW_ROAM_REST_MAX_TICKS - CastleSoldierEntity.FOLLOW_ROAM_REST_MIN_TICKS + 1);
			soldier.getNavigation().stop();
			return;
		}

		boolean needsNewAnchor = roamAnchor == null
				|| --roamRecalculateTicks <= 0
				|| soldier.getNavigation().isStuck();

		if (needsNewAnchor) {
			roamRecalculateTicks = CastleSoldierEntity.FOLLOW_ROAM_RECALCULATE_TICKS;
			roamAnchor = soldier.getFollowRoamAnchor(owner);
			// Velocidade reduzida para parecer mais relaxado.
			soldier.getNavigation().moveTo(roamAnchor.x, roamAnchor.y, roamAnchor.z, speedModifier * 0.65D);
		}
	}

	/** Teleporta o soldado para perto do dono quando a distância é absurda (>24 blocos). */
	private void rejoinNearOwner(Player owner) {
		Vec3 followAnchor = soldier.getFollowAnchor(owner);

		soldier.getNavigation().stop();
		soldier.setPos(followAnchor.x, followAnchor.y, followAnchor.z);
		soldier.setYRot(owner.getYRot());
		soldier.setXRot(owner.getXRot());
		soldier.setDeltaMovement(0.0D, 0.0D, 0.0D);
	}
}
