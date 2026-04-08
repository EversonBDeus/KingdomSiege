package com.eversonbdeus.kingdomsiege.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

// ─── Assistir alvo atacado pelo dono ─────────────────────────────────────

final class AssistOwnerAttackTargetGoal extends Goal {
	private final CastleSoldierEntity soldier;
	private LivingEntity ownerTarget;
	private int lastOwnerAttackTime;

	AssistOwnerAttackTargetGoal(CastleSoldierEntity soldier) {
		this.soldier = soldier;
		this.lastOwnerAttackTime = 0;
		setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		Player owner = soldier.getValidOwnerPlayer();

		if (owner == null) {
			return false;
		}

		if (soldier.distanceToSqr(owner) > CastleSoldierEntity.OWNER_PROTECT_RANGE_SQR) {
			return false;
		}

		LivingEntity target = owner.getLastHurtMob();
		int attackTime = owner.getLastHurtMobTimestamp();

		if (attackTime == lastOwnerAttackTime) {
			return false;
		}

		if (!soldier.isValidCombatTarget(target)) {
			return false;
		}

		ownerTarget = target;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	@Override
	public void start() {
		Player owner = soldier.getValidOwnerPlayer();

		if (owner != null && ownerTarget != null) {
			lastOwnerAttackTime = owner.getLastHurtMobTimestamp();
			soldier.setTarget(ownerTarget);
		}

		super.start();
	}

	@Override
	public void stop() {
		ownerTarget = null;
		super.stop();
	}
}
