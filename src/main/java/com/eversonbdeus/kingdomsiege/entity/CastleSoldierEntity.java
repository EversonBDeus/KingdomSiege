package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CastleSoldierEntity extends PathfinderMob {
	private static final double BASE_MOVEMENT_SPEED = 0.28D;
	private static final double BASE_ATTACK_DAMAGE = 4.0D;
	private static final double BASE_FOLLOW_RANGE = 16.0D;
	private static final double SWORDSMAN_MELEE_SPEED = 1.15D;

	private SoldierClass soldierClass = SoldierClass.SWORDSMAN;

	public CastleSoldierEntity(Level level) {
		this(ModEntities.CASTLE_SOLDIER, level);
	}

	public CastleSoldierEntity(EntityType<? extends CastleSoldierEntity> entityType, Level level) {
		super(entityType, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
				.add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
				.add(Attributes.FOLLOW_RANGE, BASE_FOLLOW_RANGE);
	}

	public SoldierClass getSoldierClass() {
		return soldierClass;
	}

	public void setSoldierClass(SoldierClass soldierClass) {
		this.soldierClass = soldierClass != null ? soldierClass : SoldierClass.SWORDSMAN;
	}

	public boolean isSwordsman() {
		return soldierClass == SoldierClass.SWORDSMAN;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new SwordsmanMeleeAttackGoal(this, SWORDSMAN_MELEE_SPEED, true));
		goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
		goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(4, new RandomLookAroundGoal(this));

		targetSelector.addGoal(1, new SwordsmanNearestHostileTargetGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.store("SoldierClass", SoldierClass.CODEC, soldierClass);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		setSoldierClass(valueInput.read("SoldierClass", SoldierClass.CODEC).orElse(SoldierClass.SWORDSMAN));
	}

	private static final class SwordsmanMeleeAttackGoal extends MeleeAttackGoal {
		private final CastleSoldierEntity soldier;

		private SwordsmanMeleeAttackGoal(CastleSoldierEntity soldier, double speedModifier, boolean followingTargetEvenIfNotSeen) {
			super(soldier, speedModifier, followingTargetEvenIfNotSeen);
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.isSwordsman() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isSwordsman() && super.canContinueToUse();
		}
	}

	private static final class SwordsmanNearestHostileTargetGoal extends NearestAttackableTargetGoal<Monster> {
		private final CastleSoldierEntity soldier;

		private SwordsmanNearestHostileTargetGoal(CastleSoldierEntity soldier) {
			super(soldier, Monster.class, true);
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.isSwordsman() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isSwordsman() && super.canContinueToUse();
		}
	}
}