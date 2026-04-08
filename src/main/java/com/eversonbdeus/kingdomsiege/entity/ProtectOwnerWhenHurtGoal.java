package com.eversonbdeus.kingdomsiege.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

// ─── Proteger dono quando atacado ────────────────────────────────────────

final class ProtectOwnerWhenHurtGoal extends Goal {
	private final CastleSoldierEntity soldier;
	private LivingEntity ownerAttacker;
	private int lastOwnerAttackedTime;

	ProtectOwnerWhenHurtGoal(CastleSoldierEntity soldier) {
		this.soldier = soldier;
		this.lastOwnerAttackedTime = 0;
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

		LivingEntity attacker = owner.getLastHurtByMob();
		int attackedTime = owner.getLastHurtByMobTimestamp();

		if (attackedTime == lastOwnerAttackedTime) {
			return false;
		}

		if (!soldier.isValidCombatTarget(attacker)) {
			return false;
		}

		ownerAttacker = attacker;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	@Override
	public void start() {
		Player owner = soldier.getValidOwnerPlayer();

		if (owner != null && ownerAttacker != null) {
			lastOwnerAttackedTime = owner.getLastHurtByMobTimestamp();
			soldier.setTarget(ownerAttacker);
		}

		super.start();
	}

	@Override
	public void stop() {
		ownerAttacker = null;
		super.stop();
	}
}
