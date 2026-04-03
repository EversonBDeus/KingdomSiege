package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.core.UUIDUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;
import java.util.UUID;

public class CastleSoldierEntity extends PathfinderMob implements RangedAttackMob {
	private static final double BASE_MOVEMENT_SPEED = 0.28D;
	private static final double BASE_ATTACK_DAMAGE = 4.0D;
	private static final double BASE_FOLLOW_RANGE = 16.0D;

	private static final double SWORDSMAN_MELEE_SPEED = 1.15D;

	private static final double ARCHER_MOVE_SPEED = 1.0D;
	private static final double ARCHER_ATTACK_RANGE = 12.0D;
	private static final int ARCHER_ATTACK_INTERVAL_TICKS = 30;
	private static final float ARCHER_PROJECTILE_VELOCITY = 1.6F;
	private static final double ARCHER_ARROW_DAMAGE = 2.5D;

	private SoldierClass soldierClass = SoldierClass.SWORDSMAN;
	private UUID ownerUuid;

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

	public boolean isArcher() {
		return soldierClass == SoldierClass.ARCHER;
	}

	public UUID getOwnerUuid() {
		return ownerUuid;
	}

	public void setOwnerUuid(UUID ownerUuid) {
		this.ownerUuid = ownerUuid;
	}

	public boolean hasOwner() {
		return ownerUuid != null;
	}

	public boolean isOwnedBy(Player player) {
		return player != null && ownerUuid != null && ownerUuid.equals(player.getUUID());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new SwordsmanMeleeAttackGoal(this, SWORDSMAN_MELEE_SPEED, true));
		goalSelector.addGoal(1, new ArcherRangedAttackGoal(this, ARCHER_MOVE_SPEED, ARCHER_ATTACK_RANGE));
		goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
		goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(4, new RandomLookAroundGoal(this));

		targetSelector.addGoal(1, new SwordsmanNearestHostileTargetGoal(this));
		targetSelector.addGoal(1, new ArcherNearestHostileTargetGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.store("SoldierClass", SoldierClass.CODEC, soldierClass);
		valueOutput.storeNullable("OwnerUuid", UUIDUtil.STRING_CODEC, ownerUuid);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		setSoldierClass(valueInput.read("SoldierClass", SoldierClass.CODEC).orElse(SoldierClass.SWORDSMAN));
		setOwnerUuid(valueInput.read("OwnerUuid", UUIDUtil.STRING_CODEC).orElse(null));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float velocity) {
		if (target == null || !target.isAlive()) {
			return;
		}

		ItemStack arrowStack = new ItemStack(Items.ARROW);
		ItemStack weaponStack = new ItemStack(Items.BOW);

		Arrow arrow = new Arrow(level(), this, arrowStack, weaponStack);

		double deltaX = target.getX() - getX();
		double deltaY = target.getEyeY() - arrow.getY();
		double deltaZ = target.getZ() - getZ();
		double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

		arrow.setBaseDamage(ARCHER_ARROW_DAMAGE);
		arrow.shoot(deltaX, deltaY + horizontalDistance * 0.2D, deltaZ, velocity, 8.0F);

		playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
		level().addFreshEntity(arrow);
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

	private static final class ArcherNearestHostileTargetGoal extends NearestAttackableTargetGoal<Monster> {
		private final CastleSoldierEntity soldier;

		private ArcherNearestHostileTargetGoal(CastleSoldierEntity soldier) {
			super(soldier, Monster.class, true);
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.isArcher() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isArcher() && super.canContinueToUse();
		}
	}

	private static final class ArcherRangedAttackGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private final double speedModifier;
		private final double attackRangeSqr;
		private int attackCooldown;

		private ArcherRangedAttackGoal(CastleSoldierEntity soldier, double speedModifier, double attackRange) {
			this.soldier = soldier;
			this.speedModifier = speedModifier;
			this.attackRangeSqr = attackRange * attackRange;
			this.attackCooldown = 0;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = soldier.getTarget();
			return soldier.isArcher() && target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity target = soldier.getTarget();
			return soldier.isArcher() && target != null && target.isAlive();
		}

		@Override
		public void start() {
			attackCooldown = 0;
		}

		@Override
		public void stop() {
			attackCooldown = 0;
			soldier.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = soldier.getTarget();

			if (target == null) {
				return;
			}

			double distanceToTargetSqr = soldier.distanceToSqr(target);
			boolean hasLineOfSight = soldier.getSensing().hasLineOfSight(target);

			soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);

			if (distanceToTargetSqr > attackRangeSqr || !hasLineOfSight) {
				soldier.getNavigation().moveTo(target, speedModifier);
			} else {
				soldier.getNavigation().stop();
			}

			if (attackCooldown > 0) {
				attackCooldown--;
			}

			if (hasLineOfSight && distanceToTargetSqr <= attackRangeSqr && attackCooldown <= 0) {
				soldier.performRangedAttack(target, ARCHER_PROJECTILE_VELOCITY);
				attackCooldown = ARCHER_ATTACK_INTERVAL_TICKS;
			}
		}
	}
}