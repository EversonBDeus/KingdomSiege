package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import com.eversonbdeus.kingdomsiege.soldier.SoldierMode;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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

	private static final double OWNER_PROTECT_RANGE = 16.0D;
	private static final double OWNER_PROTECT_RANGE_SQR = OWNER_PROTECT_RANGE * OWNER_PROTECT_RANGE;

	private static final double FOLLOW_START_DISTANCE = 5.0D;
	private static final double FOLLOW_STOP_DISTANCE = 2.5D;
	private static final double FOLLOW_MOVE_SPEED = 1.15D;
	private static final double FOLLOW_REJOIN_DISTANCE = 12.0D;
	private static final double FOLLOW_REJOIN_DISTANCE_SQR = FOLLOW_REJOIN_DISTANCE * FOLLOW_REJOIN_DISTANCE;

	private SoldierClass soldierClass = SoldierClass.SWORDSMAN;
	private SoldierMode soldierMode = SoldierMode.GUARD;
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

	public SoldierMode getSoldierMode() {
		return soldierMode;
	}

	public void setSoldierMode(SoldierMode soldierMode) {
		SoldierMode resolvedMode = soldierMode != null ? soldierMode : SoldierMode.GUARD;
		this.soldierMode = resolvedMode;

		if (resolvedMode == SoldierMode.GUARD && getTarget() == null) {
			getNavigation().stop();
		}
	}

	public void toggleSoldierMode() {
		setSoldierMode(getSoldierMode().next());
	}

	public boolean isSwordsman() {
		return soldierClass == SoldierClass.SWORDSMAN;
	}

	public boolean isArcher() {
		return soldierClass == SoldierClass.ARCHER;
	}

	public boolean isGuardMode() {
		return soldierMode == SoldierMode.GUARD;
	}

	public boolean isFollowMode() {
		return soldierMode == SoldierMode.FOLLOW;
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

	public Player getOwnerPlayer() {
		if (ownerUuid == null) {
			return null;
		}

		if (!(level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		for (Player player : serverLevel.players()) {
			if (ownerUuid.equals(player.getUUID())) {
				return player;
			}
		}

		return null;
	}

	public boolean canAutoAcquireHostileTarget() {
		if (isGuardMode()) {
			return true;
		}

		if (!isFollowMode()) {
			return false;
		}

		Player owner = getOwnerPlayer();

		if (owner == null || !owner.isAlive()) {
			return false;
		}

		return distanceToSqr(owner) <= OWNER_PROTECT_RANGE_SQR;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new SwordsmanMeleeAttackGoal(this, SWORDSMAN_MELEE_SPEED, true));
		goalSelector.addGoal(1, new ArcherRangedAttackGoal(this, ARCHER_MOVE_SPEED, ARCHER_ATTACK_RANGE));
		goalSelector.addGoal(2, new FollowOwnerGoal(this, FOLLOW_MOVE_SPEED, FOLLOW_START_DISTANCE, FOLLOW_STOP_DISTANCE));
		goalSelector.addGoal(3, new GuardModeRandomStrollGoal(this, 1.0D));
		goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(5, new RandomLookAroundGoal(this));

		targetSelector.addGoal(0, new ProtectOwnerWhenHurtGoal(this));
		targetSelector.addGoal(1, new AssistOwnerAttackTargetGoal(this));
		targetSelector.addGoal(2, new SwordsmanNearestHostileTargetGoal(this));
		targetSelector.addGoal(2, new ArcherNearestHostileTargetGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float amount) {
		if (isOwnerDamage(damageSource)) {
			return false;
		}

		return super.hurtServer(serverLevel, damageSource, amount);
	}

	private boolean isOwnerDamage(DamageSource damageSource) {
		if (damageSource == null) {
			return false;
		}

		if (!(damageSource.getEntity() instanceof Player player)) {
			return false;
		}

		return isOwnedBy(player);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.store("SoldierClass", SoldierClass.CODEC, soldierClass);
		valueOutput.store("SoldierMode", SoldierMode.CODEC, soldierMode);
		valueOutput.storeNullable("OwnerUuid", UUIDUtil.STRING_CODEC, ownerUuid);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		setSoldierClass(valueInput.read("SoldierClass", SoldierClass.CODEC).orElse(SoldierClass.SWORDSMAN));
		setSoldierMode(valueInput.read("SoldierMode", SoldierMode.CODEC).orElse(SoldierMode.GUARD));
		setOwnerUuid(valueInput.read("OwnerUuid", UUIDUtil.STRING_CODEC).orElse(null));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (!isOwnedBy(player) || !player.isShiftKeyDown()) {
			return super.mobInteract(player, hand);
		}

		if (!level().isClientSide()) {
			toggleSoldierMode();
			getNavigation().stop();

			String modeName = isFollowMode() ? "SEGUIR" : "GUARDAR";
			player.sendSystemMessage(Component.literal("Modo do soldado: " + modeName));
		}

		return InteractionResult.SUCCESS;
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

	private static final class ProtectOwnerWhenHurtGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private LivingEntity ownerAttacker;
		private int lastOwnerAttackedTime;

		private ProtectOwnerWhenHurtGoal(CastleSoldierEntity soldier) {
			this.soldier = soldier;
			this.lastOwnerAttackedTime = 0;
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			Player owner = soldier.getOwnerPlayer();

			if (owner == null || !owner.isAlive()) {
				return false;
			}

			if (soldier.distanceToSqr(owner) > OWNER_PROTECT_RANGE_SQR) {
				return false;
			}

			LivingEntity attacker = owner.getLastHurtByMob();
			int attackedTime = owner.getLastHurtByMobTimestamp();

			if (attackedTime == lastOwnerAttackedTime) {
				return false;
			}

			if (!isValidAttacker(attacker, owner)) {
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
			Player owner = soldier.getOwnerPlayer();

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

		private boolean isValidAttacker(LivingEntity attacker, Player owner) {
			if (attacker == null || !attacker.isAlive()) {
				return false;
			}

			if (attacker == soldier || attacker == owner) {
				return false;
			}

			return attacker instanceof Monster;
		}
	}

	private static final class AssistOwnerAttackTargetGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private LivingEntity ownerTarget;
		private int lastOwnerAttackTime;

		private AssistOwnerAttackTargetGoal(CastleSoldierEntity soldier) {
			this.soldier = soldier;
			this.lastOwnerAttackTime = 0;
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			Player owner = soldier.getOwnerPlayer();

			if (owner == null || !owner.isAlive()) {
				return false;
			}

			if (soldier.distanceToSqr(owner) > OWNER_PROTECT_RANGE_SQR) {
				return false;
			}

			LivingEntity target = owner.getLastHurtMob();
			int attackTime = owner.getLastHurtMobTimestamp();

			if (attackTime == lastOwnerAttackTime) {
				return false;
			}

			if (!isValidTarget(target, owner)) {
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
			Player owner = soldier.getOwnerPlayer();

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

		private boolean isValidTarget(LivingEntity target, Player owner) {
			if (target == null || !target.isAlive()) {
				return false;
			}

			if (target == soldier || target == owner) {
				return false;
			}

			return target instanceof Monster;
		}
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
			return soldier.isSwordsman() && soldier.canAutoAcquireHostileTarget() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isSwordsman() && soldier.canAutoAcquireHostileTarget() && super.canContinueToUse();
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
			return soldier.isArcher() && soldier.canAutoAcquireHostileTarget() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isArcher() && soldier.canAutoAcquireHostileTarget() && super.canContinueToUse();
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

	private static final class GuardModeRandomStrollGoal extends RandomStrollGoal {
		private final CastleSoldierEntity soldier;

		private GuardModeRandomStrollGoal(CastleSoldierEntity soldier, double speedModifier) {
			super(soldier, speedModifier);
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.isGuardMode() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isGuardMode() && super.canContinueToUse();
		}
	}

	private static final class FollowOwnerGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private final double speedModifier;
		private final double startDistanceSqr;
		private final double stopDistanceSqr;
		private int timeToRecalculatePath;

		private FollowOwnerGoal(CastleSoldierEntity soldier, double speedModifier, double startDistance, double stopDistance) {
			this.soldier = soldier;
			this.speedModifier = speedModifier;
			this.startDistanceSqr = startDistance * startDistance;
			this.stopDistanceSqr = stopDistance * stopDistance;
			this.timeToRecalculatePath = 0;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			Player owner = soldier.getOwnerPlayer();

			if (!soldier.isFollowMode() || owner == null || !owner.isAlive() || owner.isSpectator()) {
				return false;
			}

			if (soldier.getTarget() != null) {
				return false;
			}

			return soldier.distanceToSqr(owner) > startDistanceSqr;
		}

		@Override
		public boolean canContinueToUse() {
			Player owner = soldier.getOwnerPlayer();

			if (!soldier.isFollowMode() || owner == null || !owner.isAlive() || owner.isSpectator()) {
				return false;
			}

			if (soldier.getTarget() != null) {
				return false;
			}

			return soldier.distanceToSqr(owner) > stopDistanceSqr;
		}

		@Override
		public void start() {
			timeToRecalculatePath = 0;
		}

		@Override
		public void stop() {
			soldier.getNavigation().stop();
		}

		@Override
		public void tick() {
			Player owner = soldier.getOwnerPlayer();

			if (owner == null) {
				return;
			}

			double distanceToOwnerSqr = soldier.distanceToSqr(owner);

			if (distanceToOwnerSqr >= FOLLOW_REJOIN_DISTANCE_SQR) {
				rejoinNearOwner(owner);
				return;
			}

			soldier.getLookControl().setLookAt(owner, 30.0F, 30.0F);

			if (--timeToRecalculatePath <= 0) {
				timeToRecalculatePath = 10;
				soldier.getNavigation().moveTo(owner, speedModifier);
			}
		}

		private void rejoinNearOwner(Player owner) {
			double targetX = owner.getX() - owner.getLookAngle().x * 1.5D;
			double targetY = owner.getY();
			double targetZ = owner.getZ() - owner.getLookAngle().z * 1.5D;

			soldier.getNavigation().stop();
			soldier.setPos(targetX, targetY, targetZ);
			soldier.setYRot(owner.getYRot());
			soldier.setXRot(owner.getXRot());
			soldier.setDeltaMovement(0.0D, 0.0D, 0.0D);
		}
	}
}