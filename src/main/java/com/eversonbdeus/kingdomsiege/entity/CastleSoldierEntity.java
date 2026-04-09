package com.eversonbdeus.kingdomsiege.entity;


import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.ArmorTier;
import com.eversonbdeus.kingdomsiege.soldier.CatalystType;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import com.eversonbdeus.kingdomsiege.soldier.SoldierIdentityData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierMode;
import com.eversonbdeus.kingdomsiege.soldier.SoldierRank;
import com.eversonbdeus.kingdomsiege.soldier.WeaponClass;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;

import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.HumanoidArm;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class CastleSoldierEntity extends PathfinderMob implements RangedAttackMob {

	// ─── Atributos base ───────────────────────────────────────────────────────

	private static final double BASE_MOVEMENT_SPEED = 0.28D;
	private static final double BASE_ATTACK_DAMAGE = 4.0D;
	private static final double BASE_FOLLOW_RANGE = 24.0D;
	private static final double BASE_MAX_HEALTH = 20.0D;
	private static final double BASE_ARMOR = 0.0D;
	private static final double BASE_ARMOR_TOUGHNESS = 0.0D;
	private static final double BASE_KNOCKBACK_RESISTANCE = 0.0D;

	// ─── Constantes de herança de encantamentos ────────────────────────────────

	private static final double INHERITED_PROTECTION_REDUCTION_PER_LEVEL = 0.04D;
	private static final double INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL = 0.08D;
	private static final float THORNS_CHANCE_PER_LEVEL = 0.15F;
	private static final double INHERITED_SHARPNESS_DAMAGE_PER_LEVEL = 0.5D;
	private static final double INHERITED_SHARPNESS_BASE_BONUS = 0.5D;
	private static final int FIRE_ASPECT_SECONDS_PER_LEVEL = 4;
	private static final int FLAME_SECONDS_PER_LEVEL = 5;
	private static final double KNOCKBACK_STRENGTH_PER_LEVEL = 0.5D;
	private static final double INHERITED_POWER_DAMAGE_PER_LEVEL = 0.5D;
	private static final double INHERITED_POWER_BASE_BONUS = 0.5D;

	// ─── Constantes de combate ────────────────────────────────────────────────

	private static final double SWORDSMAN_MELEE_SPEED = 1.15D;
	private static final double COMBAT_THREAT_SCAN_RANGE = 24.0D;
	private static final double COMBAT_THREAT_SCAN_RANGE_SQR = COMBAT_THREAT_SCAN_RANGE * COMBAT_THREAT_SCAN_RANGE;
	private static final double COMBAT_SWITCH_MARGIN = 4.0D;
	private static final double COMBAT_FACE_MAX_ANGLE = 70.0D;
	private static final double LOW_HEALTH_RETREAT_THRESHOLD = 0.35D;
	private static final double CRITICAL_HEALTH_RETREAT_THRESHOLD = 0.20D;
	private static final double RETREAT_TRIGGER_DISTANCE = 9.0D;
	private static final double RETREAT_TRIGGER_DISTANCE_SQR = RETREAT_TRIGGER_DISTANCE * RETREAT_TRIGGER_DISTANCE;
	private static final double RETREAT_MOVE_SPEED = 1.28D;

	private static final int TARGET_LOST_SIGHT_DISENGAGE_TICKS = 30;
	private static final int TARGET_UNREACHABLE_DISENGAGE_TICKS = 30;
	private static final int TARGET_PATH_RECHECK_TICKS = 10;
	private static final double TARGET_VERTICAL_BLOCK_CHECK_DISTANCE = 2.75D;
	private static final double TARGET_VERTICAL_BLOCK_CHECK_DISTANCE_SQR =
			TARGET_VERTICAL_BLOCK_CHECK_DISTANCE * TARGET_VERTICAL_BLOCK_CHECK_DISTANCE;
	private static final double TARGET_VERTICAL_BLOCK_MIN_HEIGHT = 1.20D;

	// ─── Constantes de animação visual ───────────────────────────────────────

	private static final int VISUAL_MELEE_SWING_DURATION_TICKS = 6;
	private static final int VISUAL_ARCHER_COMBAT_READY_LINGER_TICKS = 8;

	private static final EntityDataAccessor<Integer> DATA_VISUAL_MELEE_SWING_TICKS =
			SynchedEntityData.defineId(CastleSoldierEntity.class, EntityDataSerializers.INT);

	private static final EntityDataAccessor<Boolean> DATA_VISUAL_ARCHER_BOW_POSE =
			SynchedEntityData.defineId(CastleSoldierEntity.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<Boolean> DATA_VISUAL_ARCHER_COMBAT_READY =
			SynchedEntityData.defineId(CastleSoldierEntity.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<Integer> DATA_VISUAL_SOLDIER_CLASS =
			SynchedEntityData.defineId(CastleSoldierEntity.class, EntityDataSerializers.INT);

	// ─── Constantes de GUARD ──────────────────────────────────────────────────

	private static final double GUARD_MOVE_SPEED = 1.0D;
	private static final double GUARD_RETURN_BUFFER = 1.5D;
	private static final double GUARD_CHASE_LEASH = 12.0D;

	// ─── [FASE 5] Limite de seguidores por classe ─────────────────────────────

	private static final int MAX_FOLLOW_SWORDSMEN = 3;
	private static final int MAX_FOLLOW_ARCHERS = 2;

	private static final double GUARD_HOME_HOLD_DISTANCE = 1.75D;
	private static final int GUARD_HOME_HOLD_MIN_TICKS = 30;
	private static final int GUARD_HOME_HOLD_MAX_TICKS = 70;

	private static final double GUARD_ALLY_CALL_RANGE = 20.0D;
	private static final double GUARD_ALLY_CALL_RANGE_SQR = GUARD_ALLY_CALL_RANGE * GUARD_ALLY_CALL_RANGE;
	private static final int GUARD_ALLY_CALL_COOLDOWN_TICKS = 120;
	private static final double GUARD_ALLY_CALL_HEALTH_THRESHOLD = 0.5D;
	private static final int GUARD_ALLY_CALL_MIN_ENEMIES = 2;

	// ─── Constantes do arqueiro ───────────────────────────────────────────────

	private static final double ARCHER_MOVE_SPEED = 1.0D;
	private static final double ARCHER_ATTACK_RANGE = 12.0D;
	private static final double ARCHER_COMFORT_DISTANCE = 8.0D;
	private static final double ARCHER_RETREAT_DISTANCE = 5.5D;
	private static final double ARCHER_LATERAL_OFFSET = 2.0D;
	private static final int ARCHER_REPOSITION_INTERVAL_TICKS = 12;
	private static final int ARCHER_ATTACK_INTERVAL_TICKS = 30;
	private static final float ARCHER_PROJECTILE_VELOCITY = 1.6F;
	private static final int ARCHER_BOW_DRAW_TICKS = 12;
	private static final int ARCHER_STABLE_SIGHT_TICKS = 2;
	private static final int ARCHER_CLOSE_STABLE_SIGHT_TICKS = 1;
	private static final double ARCHER_MIN_DRAW_DISTANCE = 2.25D;
	private static final double ARCHER_SPIDER_MIN_DRAW_DISTANCE = 3.10D;
	private static final double ARCHER_CLOSE_REACTION_DISTANCE = 2.90D;
	private static final double ARCHER_SPIDER_CLOSE_REACTION_DISTANCE = 3.60D;
	private static final double ARCHER_PROJECTILE_FORWARD_OFFSET = 0.55D;
	private static final double ARCHER_PROJECTILE_SIDE_OFFSET = 0.28D;
	private static final double ARCHER_PROJECTILE_HEIGHT_OFFSET = 0.18D;
	private static final double ARCHER_DRAW_FACE_MAX_ANGLE = 40.0D;
	private static final int ARCHER_HIT_RETREAT_TICKS = 14;
	private static final int ARCHER_REAIM_LOCK_TICKS = 10;
	private static final double ARCHER_HIT_RETREAT_DISTANCE = 9.5D;
	private static final double ARCHER_HIT_RETREAT_SPEED = 1.4D;
	// ─── Modo pânico: disparo de emergência quando mob chega muito perto ───
	private static final double ARCHER_PANIC_DISTANCE = 3.8D;
	private static final int ARCHER_PANIC_BOW_DRAW_TICKS = 4;
	private static final int ARCHER_PANIC_ATTACK_COOLDOWN = 8;
	private static final double ARCHER_KITE_SPEED = 1.38D;

	// ─── [COMBATE] Progressão por rank ─────────────────────────────────────
	// Inaccuracy corrigida: valores menores = maior precisão.
	// Recruit (4.0) é comparável ao Skeleton no modo normal (6.0 = pior que Recruit).
	private static final float[] RANK_ARCHER_INACCURACY  = { 4.0F, 3.0F, 2.2F, 1.4F, 0.6F };
	private static final int[]   RANK_ARCHER_INTERVAL    = { 30, 26, 22, 18, 14 };
	private static final float[] RANK_ARCHER_VELOCITY    = { 1.6F, 1.75F, 1.95F, 2.15F, 2.4F };
	private static final double[] RANK_SWORD_CHASE_SPEED = { 1.0D, 1.08D, 1.16D, 1.24D, 1.32D };

	// ─── Constantes de proteção ao dono ──────────────────────────────────────

	private static final double OWNER_PROTECT_RANGE = 16.0D;
	static final double OWNER_PROTECT_RANGE_SQR = OWNER_PROTECT_RANGE * OWNER_PROTECT_RANGE;

	// ─── Constantes de FOLLOW ─────────────────────────────────────────────────

	private static final double FOLLOW_START_DISTANCE = 6.0D;
	private static final double FOLLOW_STOP_DISTANCE = 3.5D;
	private static final double FOLLOW_DESIRED_DISTANCE = 4.0D;
	private static final double FOLLOW_MOVE_SPEED = 1.15D;
	private static final double FOLLOW_REJOIN_DISTANCE = 24.0D;
	static final double FOLLOW_REJOIN_DISTANCE_SQR = FOLLOW_REJOIN_DISTANCE * FOLLOW_REJOIN_DISTANCE;

	private static final int FOLLOW_OWNER_STATIONARY_TICKS = 80;

	private static final double FOLLOW_OWNER_MOVEMENT_TOLERANCE = 0.04D;
	private static final double FOLLOW_OWNER_MOVEMENT_TOLERANCE_SQR =
			FOLLOW_OWNER_MOVEMENT_TOLERANCE * FOLLOW_OWNER_MOVEMENT_TOLERANCE;

	private static final double FOLLOW_ROAM_MAX_DISTANCE = 12.0D;
	private static final double FOLLOW_ROAM_MAX_DISTANCE_SQR = FOLLOW_ROAM_MAX_DISTANCE * FOLLOW_ROAM_MAX_DISTANCE;

	private static final double FOLLOW_ROAM_MIN_RADIUS = 3.5D;
	private static final double FOLLOW_ROAM_MAX_RADIUS = 5.5D;

	static final int FOLLOW_ROAM_RECALCULATE_TICKS = 80;
	static final int FOLLOW_ROAM_REST_MIN_TICKS = 60;
	static final int FOLLOW_ROAM_REST_MAX_TICKS = 120;
	static final double FOLLOW_ROAM_ARRIVAL_THRESHOLD_SQR = 4.0D;

	// ─── [FASE 6] Regeneração lenta ─────────────────────────────────────────

	private static final int REGEN_INTERVAL_TICKS = 40;
	private static final float REGEN_AMOUNT = 0.5F;
	private static final int REGEN_COMBAT_COOLDOWN_TICKS = 100;

	// ─── Constantes de navegação ──────────────────────────────────────────────

	private static final int DEFAULT_GUARD_RADIUS = 8;

	private static final double NAVIGATION_PROGRESS_TOLERANCE = 0.04D;
	private static final double NAVIGATION_PROGRESS_TOLERANCE_SQR =
			NAVIGATION_PROGRESS_TOLERANCE * NAVIGATION_PROGRESS_TOLERANCE;
	private static final int NAVIGATION_REPATH_TICKS = 15;
	private static final int NAVIGATION_STUCK_TICKS = 22;
	private static final int NAVIGATION_ASSIST_COOLDOWN_TICKS = 18;
	private static final double NAVIGATION_CLIMB_ASCENT_SPEED = 0.12D;
	private static final double NAVIGATION_SWIM_ASCENT_SPEED = 0.08D;
	private static final double NAVIGATION_JUMP_MIN_TARGET_HEIGHT = 0.25D;
	private static final double NAVIGATION_JUMP_MAX_TARGET_HEIGHT = 1.25D;
	private static final double NAVIGATION_JUMP_MIN_TARGET_DISTANCE = 0.75D;
	private static final double NAVIGATION_JUMP_MIN_TARGET_DISTANCE_SQR =
			NAVIGATION_JUMP_MIN_TARGET_DISTANCE * NAVIGATION_JUMP_MIN_TARGET_DISTANCE;

	// ─── Constantes de salto crítico (espadachim) ────────────────────────────

	private static final int MELEE_CRIT_JUMP_COOLDOWN_TICKS = 24;
	private static final float MELEE_CRIT_JUMP_CHANCE = 0.16F;
	private static final double MELEE_CRIT_JUMP_MIN_DISTANCE = 1.75D;
	private static final double MELEE_CRIT_JUMP_MAX_DISTANCE = 3.25D;
	private static final double MELEE_CRIT_JUMP_MIN_DISTANCE_SQR =
			MELEE_CRIT_JUMP_MIN_DISTANCE * MELEE_CRIT_JUMP_MIN_DISTANCE;
	private static final double MELEE_CRIT_JUMP_MAX_DISTANCE_SQR =
			MELEE_CRIT_JUMP_MAX_DISTANCE * MELEE_CRIT_JUMP_MAX_DISTANCE;

	// ─── Estado persistido ────────────────────────────────────────────────────

	private SoldierBlueprintData soldierBlueprint = SoldierBlueprintData.defaultRecruit();
	private SoldierMode soldierMode = SoldierMode.GUARD;
	private UUID ownerUuid;
	private BlockPos homePos;
	private int guardRadius = DEFAULT_GUARD_RADIUS;

	private SoldierIdentityData soldierIdentity = SoldierIdentityData.defaultRecruit();

	private final CastleSoldierStatus statusView = new CastleSoldierStatus(this);
	private final CastleSoldierProgression progressionView = new CastleSoldierProgression(this);

	// ─── Estado interno de navegação/follow ───────────────────────────────────

	private Vec3 lastOwnerFollowSample;
	private int ownerStillTicks;
	private Vec3 lastNavigationProgressSample;
	private int stationaryNavigationTicks;
	private int movementAssistCooldown;
	private int meleeCritJumpCooldown;

	private int allyCallCooldown = 0;
	private int visualArcherCombatReadyTicks = 0;
	private int retreatTicks = 0;
	private int targetLostSightTicks = 0;
	private int targetUnreachableTicks = 0;
	private int targetPathRecheckCooldown = 0;

	private int regenTickCounter = 0;
	private int combatCooldownTicks = 0;
	private int archerHitRetreatTicks = 0;
	private int archerReaimLockTicks = 0;
	private boolean battleRegisteredThisCombat = false;

	// ─── Construtor ───────────────────────────────────────────────────────────

	public CastleSoldierEntity(Level level) {
		this(ModEntities.CASTLE_SOLDIER, level);
	}

	public CastleSoldierEntity(EntityType<? extends CastleSoldierEntity> entityType, Level level) {
		super(entityType, level);
		xpReward = 0;
		configurePathfindingPreferences();
		refreshDerivedAttributes();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VISUAL_MELEE_SWING_TICKS, 0);
		builder.define(DATA_VISUAL_ARCHER_BOW_POSE, false);
		builder.define(DATA_VISUAL_ARCHER_COMBAT_READY, false);
		builder.define(DATA_VISUAL_SOLDIER_CLASS, SoldierClass.SWORDSMAN.ordinal());
	}

	public float getVisualMeleeSwingProgress(float partialTick) {
		int remainingTicks = this.entityData.get(DATA_VISUAL_MELEE_SWING_TICKS);

		if (remainingTicks <= 0) {
			return 0.0F;
		}

		float elapsedTicks = VISUAL_MELEE_SWING_DURATION_TICKS
				- Math.max(0.0F, remainingTicks - partialTick);

		return Mth.clamp(
				elapsedTicks / (float) VISUAL_MELEE_SWING_DURATION_TICKS,
				0.0F,
				1.0F
		);
	}

	private void triggerVisualMeleeSwing() {
		this.entityData.set(DATA_VISUAL_MELEE_SWING_TICKS, VISUAL_MELEE_SWING_DURATION_TICKS);
	}

	private void tickVisualAnimationSync() {
		if (this.level().isClientSide()) {
			return;
		}

		int remainingTicks = this.entityData.get(DATA_VISUAL_MELEE_SWING_TICKS);

		if (remainingTicks > 0) {
			this.entityData.set(DATA_VISUAL_MELEE_SWING_TICKS, remainingTicks - 1);
		}
	}

	public boolean isVisualArcherBowPoseActive() {
		return this.entityData.get(DATA_VISUAL_ARCHER_BOW_POSE);
	}

	private void setVisualArcherBowPose(boolean active) {
		this.entityData.set(DATA_VISUAL_ARCHER_BOW_POSE, active);
	}

	public boolean isVisualArcherCombatReadyActive() {
		return this.entityData.get(DATA_VISUAL_ARCHER_COMBAT_READY);
	}

	private void setVisualArcherCombatReady(boolean active) {
		this.entityData.set(DATA_VISUAL_ARCHER_COMBAT_READY, active);
	}

	// ─── Atributos estáticos ─────────────────────────────────────────────────

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
				.add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
				.add(Attributes.FOLLOW_RANGE, BASE_FOLLOW_RANGE)
				.add(Attributes.ARMOR, BASE_ARMOR)
				.add(Attributes.ARMOR_TOUGHNESS, BASE_ARMOR_TOUGHNESS)
				.add(Attributes.KNOCKBACK_RESISTANCE, BASE_KNOCKBACK_RESISTANCE);
	}

	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	// ─── Navegação ────────────────────────────────────────────────────────────

	@Override
	protected PathNavigation createNavigation(Level level) {
		GroundPathNavigation navigation = new GroundPathNavigation(this, level);
		navigation.setCanOpenDoors(true);
		navigation.setCanFloat(true);
		return navigation;
	}

	private void configurePathfindingPreferences() {
		setPathfindingMalus(PathType.DOOR_OPEN, 0.0F);
		setPathfindingMalus(PathType.WALKABLE_DOOR, 0.0F);
		setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, 0.0F);
		setPathfindingMalus(PathType.WATER, 0.0F);
		setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
	}

	// ─── Blueprint ───────────────────────────────────────────────────────────

	public SoldierBlueprintData getSoldierBlueprint() {
		return soldierBlueprint;
	}

	public void applyBlueprint(SoldierBlueprintData blueprint) {
		soldierBlueprint = blueprint != null ? blueprint : SoldierBlueprintData.defaultRecruit();
		this.entityData.set(DATA_VISUAL_SOLDIER_CLASS, soldierBlueprint.soldierClass().ordinal());
		refreshDerivedAttributes();
	}

	public void initializeFromBlueprint(SoldierBlueprintData blueprint, UUID ownerUuid, BlockPos homePos) {
		applyBlueprint(blueprint);
		setOwnerUuid(ownerUuid);
		setHomePos(homePos);
		setGuardRadius(DEFAULT_GUARD_RADIUS);
		setSoldierMode(SoldierMode.GUARD);
	}

	// ─── HomePos ─────────────────────────────────────────────────────────────

	public BlockPos getHomePos() {
		return homePos;
	}

	public void setHomePos(BlockPos homePos) {
		this.homePos = homePos != null ? homePos.immutable() : null;
	}

	public boolean hasHomePos() {
		return homePos != null;
	}

	public void clearHomePos() {
		homePos = null;
	}

	// ─── Raio de guarda ──────────────────────────────────────────────────────

	public int getGuardRadius() {
		return guardRadius;
	}

	public void setGuardRadius(int guardRadius) {
		this.guardRadius = Math.max(1, guardRadius);
	}

	// ─── Classe e equipamento ────────────────────────────────────────────────

	public SoldierClass getSoldierClass() {
		return soldierBlueprint.soldierClass();
	}

	public SoldierClass getVisualSoldierClass() {
		int ordinal = this.entityData.get(DATA_VISUAL_SOLDIER_CLASS);
		SoldierClass[] values = SoldierClass.values();

		if (ordinal < 0 || ordinal >= values.length) {
			return SoldierClass.SWORDSMAN;
		}

		return values[ordinal];
	}

	public void setSoldierClass(SoldierClass soldierClass) {
		SoldierBlueprintData currentBlueprint = getSoldierBlueprint();
		SoldierBlueprintData defaultBlueprint = SoldierBlueprintData.of(soldierClass, getArmorTier());

		applyBlueprint(new SoldierBlueprintData(
				soldierClass,
				getArmorTier(),
				WeaponClass.fromSoldierClass(soldierClass),
				getCatalystType(),
				defaultBlueprint.weaponStack(),
				currentBlueprint.chestplateStack(),
				currentBlueprint.inheritedProtectionLevel(),
				currentBlueprint.inheritedProjectileProtectionLevel(),
				currentBlueprint.inheritedBlastProtectionLevel(),
				currentBlueprint.inheritedFireProtectionLevel(),
				currentBlueprint.inheritedThornsLevel(),
				currentBlueprint.inheritedSharpnessLevel(),
				currentBlueprint.inheritedFireAspectLevel(),
				currentBlueprint.inheritedKnockbackLevel(),
				currentBlueprint.inheritedPowerLevel(),
				currentBlueprint.inheritedPunchLevel(),
				currentBlueprint.inheritedFlameLevel()
		));
	}

	public ArmorTier getArmorTier() {
		return soldierBlueprint.armorTier();
	}

	public WeaponClass getWeaponClass() {
		return soldierBlueprint.weaponClass();
	}

	public CatalystType getCatalystType() {
		return soldierBlueprint.catalystType();
	}

	// ─── Modo de comando ─────────────────────────────────────────────────────

	public SoldierMode getSoldierMode() {
		return soldierMode;
	}

	public void setSoldierMode(SoldierMode soldierMode) {
		SoldierMode resolvedMode = soldierMode != null ? soldierMode : SoldierMode.GUARD;

		if (this.soldierMode == resolvedMode) {
			if (resolvedMode == SoldierMode.GUARD) {
				getNavigation().stop();
			}
			return;
		}

		if (resolvedMode == SoldierMode.FOLLOW && !isFollowSlotAvailable()) {
			notifyOwnerFollowLimitReached();
			return;
		}

		this.soldierMode = resolvedMode;
		clearCurrentTarget();
		getNavigation().stop();

		if (resolvedMode == SoldierMode.GUARD && homePos == null) {
			setHomePos(blockPosition());
		}
	}

	public void toggleSoldierMode() {
		setSoldierMode(getSoldierMode().next());
	}

	public boolean isSwordsman() {
		return getSoldierClass() == SoldierClass.SWORDSMAN;
	}

	public boolean isArcher() {
		return getSoldierClass() == SoldierClass.ARCHER;
	}

	public boolean isGuardMode() {
		return soldierMode == SoldierMode.GUARD;
	}

	public boolean isFollowMode() {
		return soldierMode == SoldierMode.FOLLOW;
	}

	// ─── Dono ────────────────────────────────────────────────────────────────

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

	public boolean hasSameOwner(CastleSoldierEntity other) {
		return other != null
				&& ownerUuid != null
				&& other.getOwnerUuid() != null
				&& ownerUuid.equals(other.getOwnerUuid());
	}

	// ─── Identidade ──────────────────────────────────────────────────────────

	public SoldierIdentityData getSoldierIdentity() {
		return soldierIdentity;
	}

	public void setSoldierIdentity(SoldierIdentityData identity) {
		this.soldierIdentity = identity != null ? identity : SoldierIdentityData.defaultRecruit();
	}

	public Component getSoldierDisplayName() {
		return soldierIdentity.customName()
				.map(Component::literal)
				.orElseGet(() -> super.getDisplayName().copy());
	}

	public SoldierRank getSoldierRank() {
		return soldierIdentity.rank();
	}

	public int getMilitaryXp() {
		return soldierIdentity.militaryXp();
	}

	public int getKillCount() {
		return soldierIdentity.killCount();
	}

	public int getBattlesCount() {
		return soldierIdentity.battlesCount();
	}

	void clearBattleRegistrationForNextCombat() {
		battleRegisteredThisCombat = false;
	}

	// ─── [COMBATE] Parâmetros de combate escalados por rank ─────────────────

	public float getRankArcherInaccuracy() {
		int ordinal = Math.min(getSoldierRank().ordinal(), RANK_ARCHER_INACCURACY.length - 1);
		return RANK_ARCHER_INACCURACY[ordinal];
	}

	public int getRankArcherIntervalTicks() {
		int ordinal = Math.min(getSoldierRank().ordinal(), RANK_ARCHER_INTERVAL.length - 1);
		return RANK_ARCHER_INTERVAL[ordinal];
	}

	public float getRankArcherVelocity() {
		int ordinal = Math.min(getSoldierRank().ordinal(), RANK_ARCHER_VELOCITY.length - 1);
		return RANK_ARCHER_VELOCITY[ordinal];
	}

	public double getRankSwordChaseSpeed() {
		int ordinal = Math.min(getSoldierRank().ordinal(), RANK_SWORD_CHASE_SPEED.length - 1);
		return RANK_SWORD_CHASE_SPEED[ordinal];
	}

	// ─── Localização do dono no ServerLevel ──────────────────────────────────

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

	// ─── Lógica de aquisição automática de alvo ───────────────────────────────

	public boolean canAutoAcquireHostileTarget() {
		if (isGuardMode()) {
			return true;
		}

		if (!isFollowMode()) {
			return false;
		}

		Player owner = getValidOwnerPlayer();
		return owner != null && distanceToSqr(owner) <= OWNER_PROTECT_RANGE_SQR;
	}

	private boolean canEngageThreatInCurrentMode(LivingEntity threat) {
		if (!isValidCombatTarget(threat)) {
			return false;
		}

		if (isGuardMode()) {
			return !isOutsideGuardChaseLimit(threat);
		}

		if (!isFollowMode()) {
			return false;
		}

		Player owner = getValidOwnerPlayer();
		if (owner == null) {
			return false;
		}

		if (distanceToSqr(owner) > OWNER_PROTECT_RANGE_SQR) {
			return false;
		}

		if (threat.distanceToSqr(owner) <= OWNER_PROTECT_RANGE_SQR) {
			return true;
		}

		if (threat instanceof Mob hostile) {
			LivingEntity hostileTarget = hostile.getTarget();
			return hostileTarget == owner || hostileTarget == this;
		}

		return false;
	}

	// ─── Goals ───────────────────────────────────────────────────────────────

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new SwordsmanMeleeAttackGoal(this, SWORDSMAN_MELEE_SPEED, true));
		goalSelector.addGoal(1, new ArcherRangedAttackGoal(this, ARCHER_MOVE_SPEED, ARCHER_ATTACK_RANGE));
		goalSelector.addGoal(2, new FollowOwnerGoal(this, FOLLOW_MOVE_SPEED, FOLLOW_START_DISTANCE, FOLLOW_STOP_DISTANCE));
		goalSelector.addGoal(3, new ReturnToHomePosGoal(this, GUARD_MOVE_SPEED));
		goalSelector.addGoal(4, new GuardHomePauseGoal(this));
		goalSelector.addGoal(5, new GuardModeRandomStrollGoal(this, GUARD_MOVE_SPEED));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));

		targetSelector.addGoal(0, new ProtectOwnerWhenHurtGoal(this));
		targetSelector.addGoal(1, new AssistOwnerAttackTargetGoal(this));
		targetSelector.addGoal(2, new SwordsmanNearestHostileTargetGoal(this));
		targetSelector.addGoal(2, new ArcherNearestHostileTargetGoal(this));
	}

	// ─── Remoção por distância ────────────────────────────────────────────────

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	// ─── Tick principal ───────────────────────────────────────────────────────

	@Override
	public void tick() {
		super.tick();

		if (!this.level().isClientSide()) {
			this.tickVisualAnimationSync();
			this.tickArcherVisualCombatReadyState();
			this.tickArcherDamageRecoveryState();
			this.updateFollowOwnerMotionState();
			this.tickAdvancedMovementSupport();
			this.tickCombatCoordination();
			this.tickMeleeCriticalJumpSupport();
			this.validateOwnerState();
			this.validateCurrentTarget();
			this.tickSlowRegeneration();
		}
	}

	// ─── Dano recebido ────────────────────────────────────────────────────────

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isBlockedDamageFromOwner(source)) {
			return false;
		}

		boolean damaged = super.hurtServer(level, source, amount);

		if (damaged) {
			combatCooldownTicks = REGEN_COMBAT_COOLDOWN_TICKS;

			if (!battleRegisteredThisCombat) {
				battleRegisteredThisCombat = true;
				setSoldierIdentity(soldierIdentity.withBattle());
			}

			LivingEntity attacker = source.getEntity() instanceof LivingEntity livingAttacker
					&& isValidCombatTarget(livingAttacker)
					? livingAttacker
					: null;

			if (attacker != null) {
				setTarget(attacker);
				encourageThreatToAttackSoldier(attacker);

				if (canUseBowCombat()) {
					triggerArcherDamageRecovery();
					stopUsingItem();
					setVisualArcherBowPose(true);
					visualArcherCombatReadyTicks = VISUAL_ARCHER_COMBAT_READY_LINGER_TICKS;
					setVisualArcherCombatReady(true);
				}
			}

			tryCallForAllyHelp(level);
		}

		return damaged;
	}

	private void tryCallForAllyHelp(ServerLevel level) {
		if (allyCallCooldown > 0) {
			return;
		}

		if (getHealth() / getMaxHealth() > GUARD_ALLY_CALL_HEALTH_THRESHOLD) {
			return;
		}

		double scanRadius = guardRadius + 4.0D;
		List<Mob> nearbyHostiles = level.getEntitiesOfClass(
				Mob.class,
				getBoundingBox().inflate(scanRadius),
				e -> e instanceof Enemy && isValidCombatTarget(e)
		);

		if (nearbyHostiles.size() < GUARD_ALLY_CALL_MIN_ENEMIES) {
			return;
		}

		callNearestIdleAlly(level);
		allyCallCooldown = GUARD_ALLY_CALL_COOLDOWN_TICKS;
	}

	private void callNearestIdleAlly(ServerLevel level) {
		LivingEntity currentTarget = getTarget();
		if (currentTarget == null) {
			return;
		}

		List<CastleSoldierEntity> candidates = level.getEntitiesOfClass(
				CastleSoldierEntity.class,
				getBoundingBox().inflate(GUARD_ALLY_CALL_RANGE),
				e -> e != this && hasSameOwner(e) && e.getTarget() == null
		);

		if (candidates.isEmpty()) {
			return;
		}

		CastleSoldierEntity closest = null;
		double closestDistSqr = GUARD_ALLY_CALL_RANGE_SQR;

		for (CastleSoldierEntity ally : candidates) {
			double distSqr = distanceToSqr(ally);
			if (distSqr < closestDistSqr) {
				closestDistSqr = distSqr;
				closest = ally;
			}
		}

		if (closest != null) {
			closest.setTarget(currentTarget);
		}
	}

	private boolean isBlockedDamageFromOwner(DamageSource source) {
		return isOwnerEntity(source.getEntity()) || isOwnerEntity(source.getDirectEntity());
	}

	private boolean isOwnerEntity(Entity entity) {
		return entity != null
				&& this.ownerUuid != null
				&& this.ownerUuid.equals(entity.getUUID());
	}

	@Override
	public void setTarget(LivingEntity target) {
		LivingEntity previousTarget = getTarget();
		super.setTarget(target);

		if (previousTarget != target) {
			resetCombatTargetValidationState();
			if (target != null && canUseBowCombat()) {
				visualArcherCombatReadyTicks = VISUAL_ARCHER_COMBAT_READY_LINGER_TICKS;
				setVisualArcherCombatReady(true);
			}
		}
	}

	// ─── Dano causado ────────────────────────────────────────────────────────

	@Override
	public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
		triggerVisualMeleeSwing();
		swing(InteractionHand.MAIN_HAND, true);

		boolean damaged = super.doHurtTarget(serverLevel, target);

		if (!damaged || !(target instanceof LivingEntity livingTarget)) {
			return damaged;
		}

		applyInheritedMeleeEnchantments(livingTarget);
		return true;
	}

	private void applyInheritedMeleeEnchantments(LivingEntity livingTarget) {
		applyInheritedFireAspect(livingTarget);
		applyInheritedKnockback(livingTarget);
	}

	private void applyInheritedProjectileEnchantments(Arrow arrow) {
		applyInheritedFlame(arrow);
	}

	private Vec3 getBowProjectileSpawnPos() {
		double yawRadians = Math.toRadians(getYRot());

		double spawnX = getX()
				- Math.sin(yawRadians) * ARCHER_PROJECTILE_FORWARD_OFFSET
				- Math.cos(yawRadians) * ARCHER_PROJECTILE_SIDE_OFFSET;

		double spawnY = getEyeY() - ARCHER_PROJECTILE_HEIGHT_OFFSET;

		double spawnZ = getZ()
				+ Math.cos(yawRadians) * ARCHER_PROJECTILE_FORWARD_OFFSET
				- Math.sin(yawRadians) * ARCHER_PROJECTILE_SIDE_OFFSET;

		return new Vec3(spawnX, spawnY, spawnZ);
	}

	private void applyInheritedFlame(Arrow arrow) {
		int flameLevel = getEffectiveInheritedFlameLevel();

		if (flameLevel <= 0) {
			return;
		}

		int fireTicks = flameLevel * FLAME_SECONDS_PER_LEVEL * 20;
		float fireSeconds = (float) (flameLevel * FLAME_SECONDS_PER_LEVEL);

		arrow.setRemainingFireTicks(fireTicks);
		arrow.igniteForSeconds(fireSeconds);
	}

	private void applyInheritedFireAspect(LivingEntity livingTarget) {
		int fireAspectLevel = getEffectiveInheritedFireAspectLevel();

		if (fireAspectLevel <= 0) {
			return;
		}

		livingTarget.setRemainingFireTicks(fireAspectLevel * FIRE_ASPECT_SECONDS_PER_LEVEL * 20);
	}

	private void applyInheritedKnockback(LivingEntity livingTarget) {
		int knockbackLevel = getEffectiveInheritedKnockbackLevel();

		if (knockbackLevel <= 0) {
			return;
		}

		double yawRadians = Math.toRadians(getYRot());
		livingTarget.knockback(
				knockbackLevel * KNOCKBACK_STRENGTH_PER_LEVEL,
				Math.sin(yawRadians),
				-Math.cos(yawRadians)
		);
	}

	// ─── Dano recebido: defesa e espinhos ────────────────────────────────────

	private boolean isFriendlyDamageSource(DamageSource damageSource) {
		if (damageSource == null) {
			return false;
		}

		return isFriendlyDamageEntity(damageSource.getEntity())
				|| isFriendlyDamageEntity(damageSource.getDirectEntity())
				|| isFriendlyIndirectDamageEntity(damageSource.getEntity())
				|| isFriendlyIndirectDamageEntity(damageSource.getDirectEntity());
	}

	private float applyInheritedDefensiveEnchantments(DamageSource damageSource, float amount) {
		float adjustedAmount = amount;

		adjustedAmount = applyDamageReduction(
				adjustedAmount,
				soldierBlueprint.inheritedProtectionLevel(),
				INHERITED_PROTECTION_REDUCTION_PER_LEVEL
		);

		if (damageSource != null && damageSource.is(DamageTypeTags.IS_PROJECTILE)) {
			adjustedAmount = applyDamageReduction(
					adjustedAmount,
					soldierBlueprint.inheritedProjectileProtectionLevel(),
					INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL
			);
		}

		if (damageSource != null && damageSource.is(DamageTypeTags.IS_EXPLOSION)) {
			adjustedAmount = applyDamageReduction(
					adjustedAmount,
					soldierBlueprint.inheritedBlastProtectionLevel(),
					INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL
			);
		}

		if (damageSource != null && damageSource.is(DamageTypeTags.IS_FIRE)) {
			adjustedAmount = applyDamageReduction(
					adjustedAmount,
					soldierBlueprint.inheritedFireProtectionLevel(),
					INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL
			);
		}

		return Math.max(0.0F, adjustedAmount);
	}

	private float applyDamageReduction(float amount, int level, double reductionPerLevel) {
		if (level <= 0) {
			return amount;
		}

		double totalReduction = Math.min(0.80D, level * reductionPerLevel);
		return (float) (amount * (1.0D - totalReduction));
	}

	private void applyInheritedThorns(ServerLevel serverLevel, DamageSource damageSource) {
		int thornsLevel = soldierBlueprint.inheritedThornsLevel();

		if (thornsLevel <= 0) {
			return;
		}

		Entity attacker = damageSource != null ? damageSource.getEntity() : null;

		if (!(attacker instanceof LivingEntity livingAttacker) || isFriendlyDamageEntity(attacker)) {
			return;
		}

		if (getRandom().nextFloat() > Math.min(0.75F, THORNS_CHANCE_PER_LEVEL * thornsLevel)) {
			return;
		}

		float reflectedDamage = 1.0F + getRandom().nextInt(thornsLevel + 1);
		livingAttacker.hurtServer(serverLevel, damageSources().thorns(this), reflectedDamage);
	}

	private boolean isFriendlyIndirectDamageEntity(Entity entity) {
		if (!(entity instanceof Projectile projectile)) {
			return false;
		}

		return isFriendlyDamageEntity(projectile.getOwner());
	}

	private boolean isFriendlyDamageEntity(Entity entity) {
		if (entity == null) {
			return false;
		}

		if (entity instanceof Player player) {
			return isOwnedBy(player);
		}

		if (entity instanceof CastleSoldierEntity otherSoldier) {
			return hasSameOwner(otherSoldier);
		}

		return false;
	}

	// ─── Coordenação de combate ───────────────────────────────────────────────

	private void tickCombatCoordination() {
		if (allyCallCooldown > 0) {
			allyCallCooldown--;
		}

		if (retreatTicks > 0) {
			retreatTicks--;
		}

		LivingEntity priorityThreat = findPriorityCombatThreat();

		if (priorityThreat == null || !canEngageThreatInCurrentMode(priorityThreat)) {
			return;
		}

		if (shouldSwitchTargetTo(priorityThreat)) {
			setTarget(priorityThreat);
		}

		encourageThreatToAttackSoldier(priorityThreat);

		if (shouldRetreatFromThreat(priorityThreat)) {
			retreatTicks = 10;
			navigateAwayFromThreat(priorityThreat);

			if (level() instanceof ServerLevel serverLevel) {
				tryCallForAllyHelp(serverLevel);
			}
		}
	}

	private LivingEntity findPriorityCombatThreat() {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return getTarget();
		}

		Player owner = getValidOwnerPlayer();
		List<Mob> nearbyHostiles = serverLevel.getEntitiesOfClass(
				Mob.class,
				getBoundingBox().inflate(COMBAT_THREAT_SCAN_RANGE),
				e -> e instanceof Enemy && canEngageThreatInCurrentMode(e)
		);

		if (nearbyHostiles.isEmpty()) {
			return getTarget();
		}

		LivingEntity bestThreat = null;
		double bestScore = Double.MAX_VALUE;

		for (Mob hostile : nearbyHostiles) {
			double score = computeThreatScore(hostile, owner);
			if (score < bestScore) {
				bestScore = score;
				bestThreat = hostile;
			}
		}

		return bestThreat;
	}

	private double computeThreatScore(LivingEntity threat, Player owner) {
		double score = distanceToSqr(threat);

		if (threat instanceof Mob hostile) {
			LivingEntity hostileTarget = hostile.getTarget();

			if (hostileTarget == this) {
				score -= 16.0D;
			}

			if (owner != null && hostileTarget == owner) {
				score -= 12.0D;
			}
		}

		if (!getSensing().hasLineOfSight(threat)) {
			score += 8.0D;
		}

		if (isCombatTargetVerticallyBlocked(threat, false)) {
			score += 16.0D;
		}

		if (getTarget() == threat) {
			score -= 4.0D;
		}

		return score;
	}

	private boolean shouldKeepCurrentCombatTarget(LivingEntity currentTarget) {
		if (!isValidCombatTarget(currentTarget)) {
			return false;
		}

		double currentDistanceSqr = distanceToSqr(currentTarget);

		if (canUseSwordCombat()) {
			return currentDistanceSqr <= 9.0D;
		}

		if (canUseBowCombat()) {
			double panicDistanceSqr = ARCHER_PANIC_DISTANCE * ARCHER_PANIC_DISTANCE;
			return isUsingItem()
					|| isVisualArcherBowPoseActive()
					|| currentDistanceSqr <= panicDistanceSqr
					|| (currentDistanceSqr <= ARCHER_ATTACK_RANGE * ARCHER_ATTACK_RANGE && getSensing().hasLineOfSight(currentTarget));
		}

		return false;
	}

	private boolean shouldSwitchTargetTo(LivingEntity candidate) {
		if (!canEngageThreatInCurrentMode(candidate)) {
			return false;
		}

		LivingEntity currentTarget = getTarget();
		if (!canEngageThreatInCurrentMode(currentTarget)) {
			return true;
		}

		if (currentTarget == candidate) {
			return false;
		}

		if (shouldKeepCurrentCombatTarget(currentTarget)) {
			return false;
		}

		Player owner = getValidOwnerPlayer();
		double candidateScore = computeThreatScore(candidate, owner);
		double currentScore = computeThreatScore(currentTarget, owner);
		return candidateScore + COMBAT_SWITCH_MARGIN < currentScore;
	}

	private void encourageThreatToAttackSoldier(LivingEntity threat) {
		if (!(threat instanceof Mob hostile) || !threat.isAlive()) {
			return;
		}

		LivingEntity hostileTarget = hostile.getTarget();
		Player owner = getValidOwnerPlayer();

		if (hostileTarget == null || hostileTarget == owner || hostileTarget == this) {
			hostile.setTarget(this);
		}
	}

	private boolean shouldRetreatFromThreat(LivingEntity threat) {
		if (!isValidCombatTarget(threat)) {
			return false;
		}

		double healthRatio = getHealth() / Math.max(1.0F, getMaxHealth());
		if (healthRatio > LOW_HEALTH_RETREAT_THRESHOLD) {
			return false;
		}

		int nearbyEnemies = countNearbyHostiles(COMBAT_THREAT_SCAN_RANGE);
		if (healthRatio <= CRITICAL_HEALTH_RETREAT_THRESHOLD) {
			return nearbyEnemies >= 1;
		}

		return nearbyEnemies >= 2 || distanceToSqr(threat) <= RETREAT_TRIGGER_DISTANCE_SQR;
	}

	private int countNearbyHostiles(double range) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return 0;
		}

		return serverLevel.getEntitiesOfClass(
				Mob.class,
				getBoundingBox().inflate(range),
				e -> e instanceof Enemy && isValidCombatTarget(e)
		).size();
	}

	private boolean isRetreatingFromThreat() {
		LivingEntity currentTarget = getTarget();
		return retreatTicks > 0 && isValidCombatTarget(currentTarget) && shouldRetreatFromThreat(currentTarget);
	}

	private void navigateAwayFromThreat(LivingEntity threat) {
		Vec3 awayDirection = position().subtract(threat.position());
		awayDirection = new Vec3(awayDirection.x, 0.0D, awayDirection.z);

		if (awayDirection.lengthSqr() < 1.0E-4D) {
			awayDirection = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			awayDirection = awayDirection.normalize();
		}

		double retreatDistance = canUseBowCombat() ? ARCHER_COMFORT_DISTANCE + 3.0D : 6.5D;
		Vec3 retreatAnchor = clampToGuardArea(position().add(awayDirection.scale(retreatDistance)));

		if (canUseBowCombat()) {
			getLookControl().setLookAt(retreatAnchor.x, retreatAnchor.y, retreatAnchor.z, 25.0F, 25.0F);
			visualArcherCombatReadyTicks = 0;
			setVisualArcherCombatReady(false);
		} else {
			getLookControl().setLookAt(threat, 30.0F, 30.0F);
		}

		getNavigation().moveTo(retreatAnchor.x, retreatAnchor.y, retreatAnchor.z, RETREAT_MOVE_SPEED);
		stopUsingItem();
		setVisualArcherBowPose(false);
	}
	private void faceTargetHard(LivingEntity target, float maxTurnDegrees) {
		if (target == null) {
			return;
		}

		double deltaX = target.getX() - getX();
		double deltaZ = target.getZ() - getZ();
		if (deltaX * deltaX + deltaZ * deltaZ < 1.0E-4D) {
			return;
		}

		float desiredYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
		float yawDelta = Mth.wrapDegrees(desiredYaw - getYRot());
		setYRot(getYRot() + Mth.clamp(yawDelta, -maxTurnDegrees, maxTurnDegrees));
	}

	private boolean isFacingTarget(LivingEntity target, double maxAngleDegrees) {
		if (target == null) {
			return false;
		}

		Vec3 toTarget = target.position().subtract(position());
		toTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
		if (toTarget.lengthSqr() < 1.0E-4D) {
			return true;
		}

		toTarget = toTarget.normalize();
		Vec3 lookDirection = getLookAngle();
		lookDirection = new Vec3(lookDirection.x, 0.0D, lookDirection.z);
		if (lookDirection.lengthSqr() < 1.0E-4D) {
			return true;
		}

		lookDirection = lookDirection.normalize();
		double dot = Mth.clamp(lookDirection.dot(toTarget), -1.0D, 1.0D);
		double angle = Math.toDegrees(Math.acos(dot));
		return angle <= maxAngleDegrees;
	}

	// ─── Validação de estado ──────────────────────────────────────────────────

	private void validateOwnerState() {
		if (!isFollowMode()) {
			return;
		}

		if (getValidOwnerPlayer() == null) {
			setSoldierMode(SoldierMode.GUARD);
		}
	}

	private void validateCurrentTarget() {
		LivingEntity currentTarget = getTarget();

		if (currentTarget == null) {
			resetCombatTargetValidationState();
			return;
		}

		if (!canEngageThreatInCurrentMode(currentTarget)) {
			clearCurrentTarget();
			return;
		}

		if (!isValidCombatTarget(currentTarget)) {
			clearCurrentTarget();
			return;
		}

		if (shouldDisengageFromTarget(currentTarget)
				|| shouldDropCurrentTargetForCombatValidity(currentTarget)) {
			clearCurrentTarget();
		}
	}
	private boolean shouldDropCurrentTargetForCombatValidity(LivingEntity target) {
		boolean hasLineOfSight = getSensing().hasLineOfSight(target);

		if (hasLineOfSight) {
			targetLostSightTicks = 0;
		} else {
			targetLostSightTicks++;
		}

		if (targetPathRecheckCooldown > 0) {
			targetPathRecheckCooldown--;
		}

		if (targetPathRecheckCooldown <= 0) {
			targetPathRecheckCooldown = TARGET_PATH_RECHECK_TICKS;

			if (hasCombatPathToTarget(target)) {
				targetUnreachableTicks = 0;
			} else {
				targetUnreachableTicks += TARGET_PATH_RECHECK_TICKS;
			}
		}

		if (isCombatTargetVerticallyBlocked(target, hasLineOfSight)) {
			return true;
		}

		if (hasLowHeadClearance() && !hasLineOfSight && distanceToSqr(target) <= 9.0D) {
			return true;
		}

		if (targetLostSightTicks >= TARGET_LOST_SIGHT_DISENGAGE_TICKS
				&& targetUnreachableTicks >= TARGET_UNREACHABLE_DISENGAGE_TICKS) {
			return true;
		}

		return getNavigation().isStuck()
				&& targetUnreachableTicks >= TARGET_UNREACHABLE_DISENGAGE_TICKS;
	}

	private void resetCombatTargetValidationState() {
		targetLostSightTicks = 0;
		targetUnreachableTicks = 0;
		targetPathRecheckCooldown = 0;
	}

	private boolean hasCombatPathToTarget(LivingEntity target) {
		if (target == null) {
			return false;
		}

		if (distanceToSqr(target) <= 4.0D) {
			return true;
		}

		Path path = getNavigation().createPath(target, 0);
		return path != null && path.canReach();
	}

	private boolean isCombatTargetVerticallyBlocked(LivingEntity target, boolean hasLineOfSight) {
		if (target == null || hasLineOfSight) {
			return false;
		}

		double verticalDelta = Math.abs(target.getY() - getY());
		double horizontalDistanceSqr = horizontalDistanceSqr(position(), target.position());

		return verticalDelta >= TARGET_VERTICAL_BLOCK_MIN_HEIGHT
				&& horizontalDistanceSqr <= TARGET_VERTICAL_BLOCK_CHECK_DISTANCE_SQR;
	}

	private boolean shouldDisengageFromTarget(LivingEntity target) {
		if (isGuardMode()) {
			return isOutsideGuardChaseLimit(target);
		}

		if (!isFollowMode()) {
			return false;
		}

		Player owner = getValidOwnerPlayer();

		if (owner == null) {
			return true;
		}

		if (distanceToSqr(owner) > FOLLOW_REJOIN_DISTANCE_SQR) {
			return true;
		}

		return target.distanceToSqr(owner) > OWNER_PROTECT_RANGE_SQR;
	}

	private boolean shouldReturnToHomePos() {
		return isGuardMode()
				&& getTarget() == null
				&& hasHomePos()
				&& position().distanceToSqr(getHomeAnchor()) > getGuardReturnDistanceSqr();
	}

	private boolean isOutsideGuardChaseLimit(LivingEntity target) {
		if (!isGuardMode() || target == null || !hasHomePos()) {
			return false;
		}

		return target.position().distanceToSqr(getHomeAnchor()) > getGuardChaseLimitSqr();
	}

	private double getGuardReturnDistanceSqr() {
		double returnDistance = guardRadius + GUARD_RETURN_BUFFER;
		return returnDistance * returnDistance;
	}

	private double getGuardChaseLimitSqr() {
		double chaseLimit = guardRadius + GUARD_CHASE_LEASH;
		return chaseLimit * chaseLimit;
	}

	private Vec3 getHomeAnchor() {
		if (!hasHomePos()) {
			return position();
		}

		return Vec3.atBottomCenterOf(homePos);
	}

	// ─── Posicionamento de combate ────────────────────────────────────────────

	private Vec3 clampToGuardArea(Vec3 desiredPosition) {
		if (!isGuardMode() || !hasHomePos()) {
			return desiredPosition;
		}

		Vec3 homeAnchor = getHomeAnchor();
		double maxDistance = guardRadius + GUARD_CHASE_LEASH;
		double deltaX = desiredPosition.x - homeAnchor.x;
		double deltaZ = desiredPosition.z - homeAnchor.z;
		double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

		if (horizontalDistance <= maxDistance || horizontalDistance < 1.0E-4D) {
			return desiredPosition;
		}

		double scale = maxDistance / horizontalDistance;
		return new Vec3(
				homeAnchor.x + deltaX * scale,
				desiredPosition.y,
				homeAnchor.z + deltaZ * scale
		);
	}

	private Vec3 getCombatKiteAnchor(LivingEntity target, double desiredDistance, double lateralOffset) {
		Vec3 horizontalAway = position().subtract(target.position());
		horizontalAway = new Vec3(horizontalAway.x, 0.0D, horizontalAway.z);

		if (horizontalAway.lengthSqr() < 1.0E-4D) {
			horizontalAway = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			horizontalAway = horizontalAway.normalize();
		}

		Vec3 lateral = new Vec3(-horizontalAway.z, 0.0D, horizontalAway.x);
		Vec3 desiredPosition = target.position()
				.add(horizontalAway.scale(desiredDistance))
				.add(lateral.scale(lateralOffset));

		return clampToGuardArea(desiredPosition);
	}

	// ─── Posicionamento de FOLLOW ─────────────────────────────────────────────

	Vec3 getFollowAnchor(Player owner) {
		Vec3 lookDirection = owner.getLookAngle();
		Vec3 horizontalLookDirection = new Vec3(lookDirection.x, 0.0D, lookDirection.z);

		if (horizontalLookDirection.lengthSqr() < 1.0E-4D) {
			horizontalLookDirection = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			horizontalLookDirection = horizontalLookDirection.normalize();
		}

		return owner.position().subtract(horizontalLookDirection.scale(FOLLOW_DESIRED_DISTANCE));
	}

	Vec3 getFollowRoamAnchor(Player owner) {
		Vec3 ownerPosition = owner.position();

		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = getRandom().nextDouble() * Math.PI * 2.0D;
			double radius = FOLLOW_ROAM_MIN_RADIUS
					+ getRandom().nextDouble() * (FOLLOW_ROAM_MAX_RADIUS - FOLLOW_ROAM_MIN_RADIUS);
			Vec3 candidate = ownerPosition.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);

			if (candidate.distanceToSqr(position()) > 2.0D) {
				return candidate;
			}
		}

		return ownerPosition.add(0.0D, 0.0D, FOLLOW_ROAM_MIN_RADIUS + 0.5D);
	}

	// ─── Estado de movimento do dono ──────────────────────────────────────────

	boolean hasOwnerBeenStillLongEnough() {
		return ownerStillTicks >= FOLLOW_OWNER_STATIONARY_TICKS;
	}

	boolean shouldRoamAroundStoppedOwner(Player owner) {
		return isFollowMode()
				&& getTarget() == null
				&& owner != null
				&& hasOwnerBeenStillLongEnough()
				&& distanceToSqr(owner) <= FOLLOW_ROAM_MAX_DISTANCE_SQR;
	}

	private void updateFollowOwnerMotionState() {
		Player owner = getValidOwnerPlayer();

		if (!isFollowMode() || owner == null) {
			resetFollowOwnerMotionState();
			return;
		}

		Vec3 currentOwnerPosition = owner.position();

		if (lastOwnerFollowSample == null) {
			lastOwnerFollowSample = currentOwnerPosition;
			ownerStillTicks = 0;
			return;
		}

		double movementDistanceSqr = horizontalDistanceSqr(currentOwnerPosition, lastOwnerFollowSample);
		lastOwnerFollowSample = currentOwnerPosition;

		if (movementDistanceSqr <= FOLLOW_OWNER_MOVEMENT_TOLERANCE_SQR) {
			ownerStillTicks++;
			return;
		}

		ownerStillTicks = 0;
	}

	private void resetFollowOwnerMotionState() {
		lastOwnerFollowSample = null;
		ownerStillTicks = 0;
	}

	// ─── Suporte avançado de movimento ────────────────────────────────────────

	private void tickAdvancedMovementSupport() {
		if (movementAssistCooldown > 0) {
			movementAssistCooldown--;
		}

		if (!hasActiveNavigationTarget()) {
			resetMovementSupportState();
			return;
		}

		Vec3 currentPosition = position();

		if (lastNavigationProgressSample == null) {
			lastNavigationProgressSample = currentPosition;
			stationaryNavigationTicks = 0;
		} else {
			double movementDistanceSqr = horizontalDistanceSqr(currentPosition, lastNavigationProgressSample);

			if (movementDistanceSqr <= NAVIGATION_PROGRESS_TOLERANCE_SQR) {
				stationaryNavigationTicks++;
			} else {
				stationaryNavigationTicks = 0;
			}

			lastNavigationProgressSample = currentPosition;
		}

		if (stationaryNavigationTicks == NAVIGATION_REPATH_TICKS && movementAssistCooldown <= 0) {
			getNavigation().recomputePath();
		}

		if (hasLowHeadClearance()) {
			return;
		}

		if (shouldTryClimbAssist()) {
			Vec3 currentDeltaMovement = getDeltaMovement();
			setDeltaMovement(currentDeltaMovement.x, Math.max(currentDeltaMovement.y, NAVIGATION_CLIMB_ASCENT_SPEED), currentDeltaMovement.z);
		}

		if (shouldTryWaterAssist()) {
			Vec3 currentDeltaMovement = getDeltaMovement();
			setDeltaMovement(currentDeltaMovement.x, Math.max(currentDeltaMovement.y, NAVIGATION_SWIM_ASCENT_SPEED), currentDeltaMovement.z);
		}

		if (shouldTryJumpAssist()) {
			getJumpControl().jump();
			movementAssistCooldown = NAVIGATION_ASSIST_COOLDOWN_TICKS;
			stationaryNavigationTicks = 0;
			lastNavigationProgressSample = currentPosition;
		}
	}

	private boolean hasActiveNavigationTarget() {
		return !getNavigation().isDone() || getNavigation().getTargetPos() != null;
	}

	private void resetMovementSupportState() {
		lastNavigationProgressSample = null;
		stationaryNavigationTicks = 0;
	}

	private boolean hasLowHeadClearance() {
		return !level().noCollision(this, getBoundingBox().move(0.0D, 0.20D, 0.0D));
	}

	private boolean shouldTryJumpAssist() {
		if (movementAssistCooldown > 0 || !onGround() || isInWater() || onClimbable() || hasLowHeadClearance()) {
			return false;
		}

		if (!horizontalCollision && !getNavigation().isStuck()) {
			return false;
		}

		Vec3 navigationTarget = getNavigationTargetCenter();

		if (navigationTarget == null) {
			return false;
		}

		double verticalDelta = navigationTarget.y - getY();
		double horizontalTargetDistanceSqr = horizontalDistanceSqr(position(), navigationTarget);

		return verticalDelta >= NAVIGATION_JUMP_MIN_TARGET_HEIGHT
				&& verticalDelta <= NAVIGATION_JUMP_MAX_TARGET_HEIGHT
				&& horizontalTargetDistanceSqr >= NAVIGATION_JUMP_MIN_TARGET_DISTANCE_SQR;
	}

	private boolean shouldTryClimbAssist() {
		Vec3 navigationTarget = getNavigationTargetCenter();

		return navigationTarget != null
				&& onClimbable()
				&& navigationTarget.y > getY() + 0.4D;
	}

	private boolean shouldTryWaterAssist() {
		Vec3 navigationTarget = getNavigationTargetCenter();

		return navigationTarget != null
				&& isInWater()
				&& navigationTarget.y > getY() + 0.4D;
	}

	private boolean shouldForceEmergencyRetreatFromCorner(LivingEntity threat, double closeReactionDistance) {
		if (!isValidCombatTarget(threat)) {
			return false;
		}

		double closeReactionDistanceSqr = closeReactionDistance * closeReactionDistance;
		return distanceToSqr(threat) <= closeReactionDistanceSqr
				&& (horizontalCollision || getNavigation().isStuck() || stationaryNavigationTicks >= NAVIGATION_STUCK_TICKS);
	}

	// ─── Salto crítico do espadachim ──────────────────────────────────────────

	private void tickMeleeCriticalJumpSupport() {
		if (meleeCritJumpCooldown > 0) {
			meleeCritJumpCooldown--;
		}

		if (meleeCritJumpCooldown > 0 || !canUseSwordCombat() || !onGround() || isInWater() || onClimbable()) {
			return;
		}

		LivingEntity target = getTarget();

		if (!isValidCombatTarget(target) || !getSensing().hasLineOfSight(target)) {
			return;
		}

		double distanceToTargetSqr = distanceToSqr(target);

		if (distanceToTargetSqr < MELEE_CRIT_JUMP_MIN_DISTANCE_SQR || distanceToTargetSqr > MELEE_CRIT_JUMP_MAX_DISTANCE_SQR) {
			return;
		}

		if (horizontalCollision || getNavigation().isStuck()) {
			return;
		}

		if (getRandom().nextFloat() > MELEE_CRIT_JUMP_CHANCE) {
			return;
		}

		Vec3 directionToTarget = target.position().subtract(position());
		directionToTarget = new Vec3(directionToTarget.x, 0.0D, directionToTarget.z);

		if (directionToTarget.lengthSqr() < 1.0E-4D) {
			return;
		}

		directionToTarget = directionToTarget.normalize();
		Vec3 currentDeltaMovement = getDeltaMovement();
		setDeltaMovement(
				currentDeltaMovement.x + directionToTarget.x * 0.08D,
				Math.max(currentDeltaMovement.y, 0.42D),
				currentDeltaMovement.z + directionToTarget.z * 0.08D
		);
		getJumpControl().jump();
		meleeCritJumpCooldown = MELEE_CRIT_JUMP_COOLDOWN_TICKS;
	}

	// ─── Utilitários ─────────────────────────────────────────────────────────

	private Vec3 getNavigationTargetCenter() {
		BlockPos navigationTargetPos = getNavigation().getTargetPos();
		return navigationTargetPos != null ? Vec3.atBottomCenterOf(navigationTargetPos) : null;
	}

	private double horizontalDistanceSqr(Vec3 first, Vec3 second) {
		double deltaX = first.x - second.x;
		double deltaZ = first.z - second.z;
		return deltaX * deltaX + deltaZ * deltaZ;
	}

	private void clearCurrentTarget() {
		setTarget(null);
		stopUsingItem();
		setAggressive(false);
		setVisualArcherBowPose(false);
		visualArcherCombatReadyTicks = 0;
		setVisualArcherCombatReady(false);
		resetCombatTargetValidationState();
		getNavigation().stop();
		movementAssistCooldown = 0;
		resetMovementSupportState();
	}

	// ─── [FASE 5] Controle de limite de seguidores ───────────────────────────

	private int countFollowersOfClass(SoldierClass targetClass) {
		if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
			return 0;
		}

		List<CastleSoldierEntity> followers = serverLevel.getEntitiesOfClass(
				CastleSoldierEntity.class,
				getBoundingBox().inflate(256.0D),
				e -> e != this
						&& ownerUuid.equals(e.getOwnerUuid())
						&& e.isFollowMode()
						&& e.getSoldierClass() == targetClass
		);

		return followers.size();
	}

	private int getMaxFollowersForMyClass() {
		return isSwordsman() ? MAX_FOLLOW_SWORDSMEN : MAX_FOLLOW_ARCHERS;
	}

	private boolean isFollowSlotAvailable() {
		return countFollowersOfClass(getSoldierClass()) < getMaxFollowersForMyClass();
	}

	private void notifyOwnerFollowLimitReached() {
		Player owner = getValidOwnerPlayer();
		if (owner == null) {
			return;
		}
		owner.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.follow_limit_reached",
				Component.translatable(getSoldierClass().getTranslationKey()),
				getMaxFollowersForMyClass()
		));
	}

	Player getValidOwnerPlayer() {
		Player owner = getOwnerPlayer();

		if (owner == null || !owner.isAlive() || owner.isRemoved() || owner.isSpectator()) {
			return null;
		}

		return owner;
	}

	boolean isValidCombatTarget(LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}

		if (isFriendlyEntity(target)) {
			return false;
		}

		return target instanceof Enemy;
	}

	private boolean isFriendlyEntity(LivingEntity entity) {
		if (entity == null) {
			return false;
		}

		if (entity == this) {
			return true;
		}

		if (entity instanceof Player player) {
			return isOwnedBy(player);
		}

		if (entity instanceof CastleSoldierEntity otherSoldier) {
			return hasSameOwner(otherSoldier);
		}

		return false;
	}

	private boolean canUseSwordCombat() {
		return isSwordsman() && getWeaponClass() == WeaponClass.SWORD;
	}

	private boolean canUseBowCombat() {
		return isArcher() && getWeaponClass() == WeaponClass.BOW;
	}

	// ─── Atributos derivados ──────────────────────────────────────────────────

	private void refreshDerivedAttributes() {
		double previousMaxHealth = getAttributeValue(Attributes.MAX_HEALTH);
		float previousHealth = getHealth();

		setBaseAttribute(Attributes.MAX_HEALTH, BASE_MAX_HEALTH + soldierBlueprint.getBonusHealth());
		setBaseAttribute(Attributes.ATTACK_DAMAGE, getEffectiveMeleeAttackDamage());
		setBaseAttribute(Attributes.ARMOR, BASE_ARMOR + soldierBlueprint.getArmorBonus());
		setBaseAttribute(Attributes.ARMOR_TOUGHNESS, BASE_ARMOR_TOUGHNESS + soldierBlueprint.getToughnessBonus());
		setBaseAttribute(Attributes.KNOCKBACK_RESISTANCE, BASE_KNOCKBACK_RESISTANCE + soldierBlueprint.getKnockbackResistanceBonus());

		refreshVisualWeapon();
		refreshHealthAfterBlueprintChange(previousMaxHealth, previousHealth);
	}

	private void refreshHealthAfterBlueprintChange(double previousMaxHealth, float previousHealth) {
		if (!isAlive()) {
			return;
		}

		double resolvedPreviousMaxHealth = previousMaxHealth > 0.0D ? previousMaxHealth : BASE_MAX_HEALTH;
		double healthRatio = previousHealth > 0.0F ? previousHealth / resolvedPreviousMaxHealth : 1.0D;

		healthRatio = Math.max(0.0D, Math.min(1.0D, healthRatio));

		setHealth((float) (getMaxHealth() * healthRatio));
	}

	private void setBaseAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance attributeInstance = getAttribute(attribute);

		if (attributeInstance != null) {
			attributeInstance.setBaseValue(value);
		}
	}

	private void refreshVisualWeapon() {
		ItemStack visualWeaponStack = soldierBlueprint.weaponStack().isEmpty()
				? switch (getWeaponClass()) {
			case BOW -> new ItemStack(Items.BOW);
			case SWORD -> new ItemStack(Items.IRON_SWORD);
			default -> ItemStack.EMPTY;
		}
				: soldierBlueprint.weaponStack().copy();

		if (!visualWeaponStack.isEmpty()) {
			visualWeaponStack.setCount(1);
		}

		setItemSlot(EquipmentSlot.MAINHAND, visualWeaponStack);
		setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		setDropChance(EquipmentSlot.OFFHAND, 0.0F);
		setDropChance(EquipmentSlot.CHEST, 0.0F);
	}

	// ─── Cálculo de dano ─────────────────────────────────────────────────────

	private double getEffectiveMeleeAttackDamage() {
		return soldierBlueprint.getBaseAttackDamage() + getInheritedSharpnessBonus();
	}

	private double getInheritedSharpnessBonus() {
		int sharpnessLevel = getEffectiveInheritedSharpnessLevel();

		if (sharpnessLevel <= 0) {
			return 0.0D;
		}

		return INHERITED_SHARPNESS_BASE_BONUS + sharpnessLevel * INHERITED_SHARPNESS_DAMAGE_PER_LEVEL;
	}

	private double getEffectiveProjectileBaseDamage() {
		return soldierBlueprint.getProjectileBaseDamage() + getInheritedPowerBonus();
	}

	private double getInheritedPowerBonus() {
		int powerLevel = getEffectiveInheritedPowerLevel();

		if (powerLevel <= 0) {
			return 0.0D;
		}

		return INHERITED_POWER_BASE_BONUS + powerLevel * INHERITED_POWER_DAMAGE_PER_LEVEL;
	}

	// ─── Encantamentos herdados ───────────────────────────────────────────────

	private int getEffectiveInheritedSharpnessLevel() {
		return resolveEffectiveWeaponEnchantmentLevel(
				soldierBlueprint.inheritedSharpnessLevel(),
				Enchantments.SHARPNESS
		);
	}

	private int getEffectiveInheritedFireAspectLevel() {
		return resolveEffectiveWeaponEnchantmentLevel(
				soldierBlueprint.inheritedFireAspectLevel(),
				Enchantments.FIRE_ASPECT
		);
	}

	private int getEffectiveInheritedKnockbackLevel() {
		return resolveEffectiveWeaponEnchantmentLevel(
				soldierBlueprint.inheritedKnockbackLevel(),
				Enchantments.KNOCKBACK
		);
	}

	private int getEffectiveInheritedPowerLevel() {
		return resolveEffectiveWeaponEnchantmentLevel(
				soldierBlueprint.inheritedPowerLevel(),
				Enchantments.POWER
		);
	}

	private int getEffectiveInheritedPunchLevel() {
		return resolveEffectiveWeaponEnchantmentLevel(
				soldierBlueprint.inheritedPunchLevel(),
				Enchantments.PUNCH
		);
	}

	private int getEffectiveInheritedFlameLevel() {
		int flameLevel = resolveEffectiveWeaponEnchantmentLevel(
				soldierBlueprint.inheritedFlameLevel(),
				Enchantments.FLAME
		);

		if (flameLevel > 0) {
			return flameLevel;
		}

		if (canUseBowCombat()) {
			return resolveWeaponEnchantmentLevel(Enchantments.FIRE_ASPECT);
		}

		return 0;
	}

	private int resolveEffectiveWeaponEnchantmentLevel(int storedLevel, ResourceKey<Enchantment> enchantmentKey) {
		return Math.max(storedLevel, resolveWeaponEnchantmentLevel(enchantmentKey));
	}

	private int resolveWeaponEnchantmentLevel(ResourceKey<Enchantment> enchantmentKey) {
		ItemStack weaponStack = soldierBlueprint.weaponStack();

		if (weaponStack == null || weaponStack.isEmpty()) {
			return 0;
		}

		ItemEnchantments enchantments = weaponStack.getEnchantments();

		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
			if (entry.getKey().is(enchantmentKey)) {
				return Math.max(0, entry.getIntValue());
			}
		}

		return 0;
	}

	// ─── Componentes de UI de status ──────────────────────────────────────────

	private void sendBasicStatusTo(Player player) {
		statusView.sendBasicStatusTo(player);
	}

	boolean canUseBowCombatForStatus() {
		return canUseBowCombat();
	}

	double getEffectiveProjectileBaseDamageForStatus() {
		return getEffectiveProjectileBaseDamage();
	}

	int getEffectiveInheritedSharpnessLevelForStatus() {
		return getEffectiveInheritedSharpnessLevel();
	}

	int getEffectiveInheritedFireAspectLevelForStatus() {
		return getEffectiveInheritedFireAspectLevel();
	}

	int getEffectiveInheritedKnockbackLevelForStatus() {
		return getEffectiveInheritedKnockbackLevel();
	}

	int getEffectiveInheritedPowerLevelForStatus() {
		return getEffectiveInheritedPowerLevel();
	}

	int getEffectiveInheritedPunchLevelForStatus() {
		return getEffectiveInheritedPunchLevel();
	}

	int getEffectiveInheritedFlameLevelForStatus() {
		return getEffectiveInheritedFlameLevel();
	}

	// ─── [FASE 6] Morte com peso real ───────────────────────────────────────

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);

		if (level() instanceof ServerLevel serverLevel) {
			Player owner = getValidOwnerPlayer();
			if (owner != null) {
				owner.sendSystemMessage(Component.translatable(
						"message.kingdomsiege.soldier_died",
						getSoldierDisplayName(),
						Component.translatable(getSoldierRank().getTranslationKey()),
						getKillCount(),
						getBattlesCount()
				));
			}
		}
	}
	private void tickArcherVisualCombatReadyState() {
		if (!canUseBowCombat()) {
			visualArcherCombatReadyTicks = 0;
			setVisualArcherCombatReady(false);
			return;
		}

		LivingEntity target = getTarget();
		boolean shouldStayCombatReady = isValidCombatTarget(target)
				|| isUsingItem()
				|| isArcherRecoveringFromHit()
				|| isVisualArcherBowPoseActive();

		if (shouldStayCombatReady) {
			visualArcherCombatReadyTicks = VISUAL_ARCHER_COMBAT_READY_LINGER_TICKS;
		} else if (visualArcherCombatReadyTicks > 0) {
			visualArcherCombatReadyTicks--;
		}

		setVisualArcherCombatReady(visualArcherCombatReadyTicks > 0);
	}

	private void tickArcherDamageRecoveryState() {
		if (archerHitRetreatTicks > 0) {
			archerHitRetreatTicks--;
		}

		if (archerReaimLockTicks > 0) {
			archerReaimLockTicks--;
		}
	}

	private void triggerArcherDamageRecovery() {
		archerHitRetreatTicks = ARCHER_HIT_RETREAT_TICKS;
		archerReaimLockTicks = ARCHER_REAIM_LOCK_TICKS;
	}

	private boolean isArcherRecoveringFromHit() {
		return canUseBowCombat() && archerHitRetreatTicks > 0;
	}

	private Vec3 getDirectRetreatAnchor(LivingEntity target, double desiredDistance) {
		Vec3 awayDirection = position().subtract(target.position());
		awayDirection = new Vec3(awayDirection.x, 0.0D, awayDirection.z);

		if (awayDirection.lengthSqr() < 1.0E-4D) {
			awayDirection = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			awayDirection = awayDirection.normalize();
		}

		return clampToGuardArea(position().add(awayDirection.scale(desiredDistance)));
	}
	// ─── [FASE 6] Regeneração lenta ───────────────────────────────────────────

	private void tickSlowRegeneration() {
		if (combatCooldownTicks > 0) {
			combatCooldownTicks--;
			return;
		}

		if (getHealth() >= getMaxHealth()) {
			regenTickCounter = 0;
			return;
		}

		regenTickCounter++;
		if (regenTickCounter >= REGEN_INTERVAL_TICKS) {
			regenTickCounter = 0;
			heal(REGEN_AMOUNT);
		}
	}

	// ─── Serialização ─────────────────────────────────────────────────────────

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.store("SoldierBlueprint", SoldierBlueprintData.CODEC, soldierBlueprint);
		valueOutput.store("SoldierMode", SoldierMode.CODEC, soldierMode);
		valueOutput.storeNullable("OwnerUuid", UUIDUtil.STRING_CODEC, ownerUuid);
		valueOutput.store("GuardRadius", Codec.INT, guardRadius);
		valueOutput.store("SoldierIdentity", SoldierIdentityData.CODEC, soldierIdentity);
		valueOutput.storeNullable("HomePosX", Codec.INT, homePos != null ? homePos.getX() : null);
		valueOutput.storeNullable("HomePosY", Codec.INT, homePos != null ? homePos.getY() : null);
		valueOutput.storeNullable("HomePosZ", Codec.INT, homePos != null ? homePos.getZ() : null);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		applyBlueprint(valueInput.read("SoldierBlueprint", SoldierBlueprintData.CODEC)
				.orElse(SoldierBlueprintData.defaultRecruit()));
		setSoldierMode(valueInput.read("SoldierMode", SoldierMode.CODEC).orElse(SoldierMode.GUARD));
		setOwnerUuid(valueInput.read("OwnerUuid", UUIDUtil.STRING_CODEC).orElse(null));
		setGuardRadius(valueInput.read("GuardRadius", Codec.INT).orElse(DEFAULT_GUARD_RADIUS));
		setSoldierIdentity(valueInput.read("SoldierIdentity", SoldierIdentityData.CODEC)
				.orElseGet(SoldierIdentityData::defaultRecruit));

		Integer homePosX = valueInput.read("HomePosX", Codec.INT).orElse(null);
		Integer homePosY = valueInput.read("HomePosY", Codec.INT).orElse(null);
		Integer homePosZ = valueInput.read("HomePosZ", Codec.INT).orElse(null);

		if (homePosX != null && homePosY != null && homePosZ != null) {
			setHomePos(new BlockPos(homePosX, homePosY, homePosZ));
		} else {
			clearHomePos();
		}
	}

	// ─── Interação com o dono ─────────────────────────────────────────────────

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (!isOwnedBy(player)) {
			return super.mobInteract(player, hand);
		}

		if (player.isShiftKeyDown()) {
			if (!level().isClientSide()) {
				toggleSoldierMode();
				player.sendSystemMessage(Component.translatable(
						"message.kingdomsiege.soldier_mode_changed",
						Component.translatable(getSoldierMode().getTranslationKey())
				));
			}

			return InteractionResult.SUCCESS;
		}

		ItemStack heldItem = player.getItemInHand(hand);
		if (heldItem.getItem() instanceof NameTagItem && heldItem.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
			if (!level().isClientSide()) {
				String newName = heldItem.getHoverName().getString();
				setSoldierIdentity(soldierIdentity.withCustomName(newName));
				player.sendSystemMessage(Component.translatable(
						"message.kingdomsiege.soldier_named",
						getSoldierDisplayName()
				));
				if (!player.getAbilities().instabuild) {
					heldItem.shrink(1);
				}
			}
			return InteractionResult.SUCCESS;
		}

		if (heldItem.isEmpty()) {
			if (!level().isClientSide()) {
				sendBasicStatusTo(player);
			}

			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	// ─── Progressão militar ───────────────────────────────────────────────────

	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity killedEntity, DamageSource damageSource) {
		boolean result = super.killedEntity(level, killedEntity, damageSource);
		progressionView.handleKill(level, killedEntity);
		return result;
	}

	// ─── Ataque ranged (arqueiro) ─────────────────────────────────────────────
	//
	// CORREÇÃO DO DISPARO:
	//
	// Problema 1 — deltaY calculado de target.getEyeY():
	//   O olho do zombie fica quase no topo do hitbox (1.67/1.95 blocos).
	//   Somando o arco (horizontalDistance * 0.2), a flecha passava acima
	//   da cabeça do alvo na maioria dos ranges, especialmente 5-10 blocos.
	//
	// Problema 2 — arco de 0.2 era o dobro do necessário:
	//   Na fórmula do Minecraft, 0.2 * distância gera trajetória que compensa
	//   gravity ok para mobs de olho baixo (Skeleton), mas não para arqueiros
	//   que atiram de cima (olho a ~1.66 blocos de altura).
	//
	// Solução aplicada:
	//   - Mirar em 55% da altura do hitbox do alvo (centro de massa real).
	//     Funciona tanto para zombies (1.95 blocos) quanto para aranhas (0.9 blocos).
	//   - Reduzir o arco para 0.10 * horizontalDistance.
	//     Compensa a gravidade sem ultrapassar o alvo em nenhum range típico.
	//   - Calcular o vetor de direção a partir do centro do olho (getX/getEyeY/getZ),
	//     não do spawn offset lateral — evita que flechas passem pelo lado do hitbox.
	//
	@Override
	public void performRangedAttack(LivingEntity target, float velocity) {
		if (!canUseBowCombat() || !isValidCombatTarget(target)) {
			stopUsingItem();
			return;
		}

		// Checagens mínimas de segurança — as demais já foram validadas no goal
		// antes de iniciar o draw, e re-checar aqui causava cancelamentos indevidos
		// quando o alvo se movia levemente durante os 12 ticks de draw.
		//
		// CORREÇÃO DO PÂNICO:
		// Quando o mob está dentro de ARCHER_PANIC_DISTANCE (~3.8 blocos), ele está
		// literalmente na cara do arqueiro. Nesse caso:
		// - hasLineOfSight() pode falhar por sobreposição de hitbox — ignoramos.
		// - A distância mínima de draw não se aplica — atiramos de qualquer jeito.
		double panicDistSqr = ARCHER_PANIC_DISTANCE * ARCHER_PANIC_DISTANCE;
		boolean isPanicRange = distanceToSqr(target) <= panicDistSqr;

		if (!isPanicRange) {
			if (!getSensing().hasLineOfSight(target)) {
				stopUsingItem();
				return;
			}
			if (distanceToSqr(target) <= ARCHER_MIN_DRAW_DISTANCE * ARCHER_MIN_DRAW_DISTANCE) {
				stopUsingItem();
				return;
			}
		}


		ItemStack arrowStack = new ItemStack(Items.ARROW);
		ItemStack weaponStack = soldierBlueprint.weaponStack().isEmpty()
				? new ItemStack(Items.BOW)
				: soldierBlueprint.weaponStack().copy();

		// Spawn visual: mantém o offset lateral para a pose correta do arco.
		Vec3 spawnPos = getBowProjectileSpawnPos();
		Arrow arrow = new Arrow(level(), this, arrowStack, weaponStack);
		arrow.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

		// Direção de disparo: calculada a partir do CENTRO do olho (sem offset lateral)
		// para garantir que a trajetória aponte para o centro do hitbox do alvo.
		Vec3 aimOrigin = new Vec3(getX(), getEyeY(), getZ());

		// Mira em 55% da altura do hitbox — centro de massa real do mob.
		// Funciona para zombies (1.95 blocos), aranhas (0.9 blocos) e mobs genéricos.
		double targetAimY = target.getY() + target.getBbHeight() * 0.55D;

		double deltaX = target.getX() - aimOrigin.x;
		double deltaY = targetAimY - aimOrigin.y;
		double deltaZ = target.getZ() - aimOrigin.z;
		double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

		float resolvedVelocity = velocity > 0.0F ? velocity : getRankArcherVelocity();

		arrow.setBaseDamage(getEffectiveProjectileBaseDamage());

		// Arc de 0.10 compensa a gravidade sem elevar demais a trajetória.
		// Valor testado para ranges de 3 a 12 blocos com a altura de olho deste mob.
		arrow.shoot(
				deltaX,
				deltaY + horizontalDistance * 0.10D,
				deltaZ,
				resolvedVelocity,
				getRankArcherInaccuracy()
		);

		applyInheritedProjectileEnchantments(arrow);
		stopUsingItem();

		playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
		level().addFreshEntity(arrow);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// Goals internos
	// ═════════════════════════════════════════════════════════════════════════

	// ─── Espadachim: Ataque Corpo a Corpo ─────────────────────────────────────
	private static final class SwordsmanMeleeAttackGoal extends MeleeAttackGoal {
		private final CastleSoldierEntity soldier;
		private int stuckAttackTicks = 0;

		private SwordsmanMeleeAttackGoal(CastleSoldierEntity soldier, double speedModifier, boolean followingTargetEvenIfNotSeen) {
			super(soldier, speedModifier, followingTargetEvenIfNotSeen);
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.canUseSwordCombat()
					&& !soldier.isRetreatingFromThreat()
					&& super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.canUseSwordCombat()
					&& !soldier.isRetreatingFromThreat()
					&& super.canContinueToUse();
		}

		@Override
		public void start() {
			super.start();
			stuckAttackTicks = 0;
		}

		@Override
		public void stop() {
			super.stop();
			stuckAttackTicks = 0;
		}

		@Override
		public void tick() {
			super.tick();

			LivingEntity target = soldier.getTarget();
			if (target == null || !target.isAlive()) {
				stuckAttackTicks = 0;
				return;
			}

			soldier.getLookControl().setLookAt(target, 65.0F, 65.0F);

			double distSqr = soldier.distanceToSqr(target);
			double meleeReach = soldier.getBbWidth() + target.getBbWidth() + 1.10D;
			double forcedAttackRangeSqr = Math.max(4.0D, meleeReach * meleeReach);

			if (distSqr <= forcedAttackRangeSqr) {
				soldier.getNavigation().stop();
				stuckAttackTicks++;

				if (stuckAttackTicks >= 10
						&& !soldier.level().isClientSide()
						&& soldier.getSensing().hasLineOfSight(target)) {
					if (soldier.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
						soldier.faceTargetHard(target, 35.0F);
						soldier.doHurtTarget(serverLevel, target);
					}
					stuckAttackTicks = 0;
				}
			} else {
				stuckAttackTicks = 0;
			}
		}
	}

	// ─── Alvo hostil do espadachim ────────────────────────────────────────────

	private static final class SwordsmanNearestHostileTargetGoal extends NearestAttackableTargetGoal<Mob> {
		private final CastleSoldierEntity soldier;

		private SwordsmanNearestHostileTargetGoal(CastleSoldierEntity soldier) {
			// randomInterval=3: verifica a cada ~3 ticks (era 10) para detecção muito mais rápida.
			// mustSee=false: detecta mobs sem linha de visão direta — corrige o bug onde o
			// zombie se aproximava por trás ou de lado sem ser notado. A validação de LOS
			// para perseguição contínua já é feita em shouldDropCurrentTargetForCombatValidity.
			super(soldier, Mob.class, 3, false, false, (target, level) -> soldier.isValidCombatTarget(target));
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.canUseSwordCombat() && soldier.canAutoAcquireHostileTarget() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.canUseSwordCombat() && soldier.canAutoAcquireHostileTarget() && super.canContinueToUse();
		}
	}

	// ─── Alvo hostil do arqueiro ──────────────────────────────────────────────

	private static final class ArcherNearestHostileTargetGoal extends NearestAttackableTargetGoal<Mob> {
		private final CastleSoldierEntity soldier;

		private ArcherNearestHostileTargetGoal(CastleSoldierEntity soldier) {
			// randomInterval=3: detecção mais rápida (era 10).
			// mustSee=false: arqueiro também detecta mobs sem LOS direta para adquirir o alvo.
			// O combate em si exige LOS para atirar, mas a aquisição do alvo não.
			super(soldier, Mob.class, 3, false, false, (target, level) -> soldier.isValidCombatTarget(target));
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.canUseBowCombat() && soldier.canAutoAcquireHostileTarget() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.canUseBowCombat() && soldier.canAutoAcquireHostileTarget() && super.canContinueToUse();
		}
	}

	// ─── Arqueiro: Ataque à Distância ─────────────────────────────────────────
	//
	// MELHORIAS DE COMBATE (v2):
	//
	// Problema 1 — Não atirava quando mob chegava perto:
	//   O performRangedAttack cancelava o disparo se hasLineOfSight() falhava,
	//   o que ocorre frequentemente a 1-3 blocos (hitbox do zombie sobrepõe).
	//   Solução: cancelamento de LOS ignorado dentro de ARCHER_PANIC_DISTANCE.
	//
	// Problema 2 — Não mantinha distância:
	//   shouldRetreatFromClosePressure só ativava com hurtTime > 0 ou isStuck().
	//   O zombie podia chegar até ~2.9 blocos sem o arqueiro recuar.
	//   Solução: recuo ativo a partir de ARCHER_RETREAT_DISTANCE (5.5 blocos).
	//
	// Problema 3 — Modo pânico (< 3.8 blocos):
	//   Sem nenhum mecanismo de tiro rápido quando mob está na cara do arqueiro.
	//   Solução: draw de 4 ticks + cooldown de 8 ticks no modo pânico.
	//
	// Problema 4 — Flecha não saía quando muito longe:
	//   O arqueiro ficava correndo atrás do mob enquanto o mob se aproximava,
	//   resultando em um loop de "quase atirou". Solução: a lógica de draw
	//   tem prioridade sobre o movimento tático — se pode atirar, para e atira.
	//
	private static final class ArcherRangedAttackGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private final double speedModifier;
		private final double attackRangeSqr;
		private int attackCooldown;
		private int repositionCooldown;
		private int lateralDirection;
		private int drawTicksRemaining;
		private int stableSightTicks;
		private int drawCancelGraceTicks;
		private boolean panicMode;

		private ArcherRangedAttackGoal(CastleSoldierEntity soldier, double speedModifier, double attackRange) {
			this.soldier = soldier;
			this.speedModifier = speedModifier;
			this.attackRangeSqr = attackRange * attackRange;
			this.attackCooldown = 0;
			this.repositionCooldown = 0;
			this.lateralDirection = 1;
			this.drawTicksRemaining = 0;
			this.stableSightTicks = 0;
			this.drawCancelGraceTicks = 0;
			this.panicMode = false;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = soldier.getTarget();
			return soldier.canUseBowCombat()
					&& target != null
					&& target.isAlive()
					&& !soldier.isRetreatingFromThreat();
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity target = soldier.getTarget();
			return soldier.canUseBowCombat()
					&& target != null
					&& target.isAlive()
					&& !soldier.isRetreatingFromThreat();
		}

		@Override
		public void start() {
			attackCooldown = 0;
			repositionCooldown = 0;
			lateralDirection = soldier.getRandom().nextBoolean() ? 1 : -1;
			drawTicksRemaining = 0;
			stableSightTicks = 0;
			drawCancelGraceTicks = 0;
			panicMode = false;
			soldier.stopUsingItem();
			soldier.setAggressive(true);
			soldier.setVisualArcherBowPose(false);
			soldier.visualArcherCombatReadyTicks = VISUAL_ARCHER_COMBAT_READY_LINGER_TICKS;
			soldier.setVisualArcherCombatReady(true);
		}

		@Override
		public void stop() {
			attackCooldown = 0;
			repositionCooldown = 0;
			drawTicksRemaining = 0;
			stableSightTicks = 0;
			drawCancelGraceTicks = 0;
			panicMode = false;
			soldier.stopUsingItem();
			soldier.getNavigation().stop();
			soldier.setAggressive(false);
			soldier.setVisualArcherBowPose(false);
			soldier.visualArcherCombatReadyTicks = 0;
			soldier.setVisualArcherCombatReady(false);
			soldier.movementAssistCooldown = 0;
			soldier.resetMovementSupportState();
		}

		private void setAimReadyVisualState(LivingEntity target, float yawLimit) {
			soldier.visualArcherCombatReadyTicks = VISUAL_ARCHER_COMBAT_READY_LINGER_TICKS;
			soldier.setVisualArcherCombatReady(true);
			soldier.getLookControl().setLookAt(target, yawLimit, yawLimit);
		}

		private void moveInRetreatRunState(Vec3 retreatAnchor, double speed) {
			soldier.stopUsingItem();
			soldier.setVisualArcherBowPose(false);
			soldier.visualArcherCombatReadyTicks = 0;
			soldier.setVisualArcherCombatReady(false);
			soldier.getLookControl().setLookAt(retreatAnchor.x, retreatAnchor.y, retreatAnchor.z, 25.0F, 25.0F);
			soldier.getNavigation().moveTo(retreatAnchor.x, retreatAnchor.y, retreatAnchor.z, speed);
		}

		@Override
		public void tick() {
			LivingEntity target = soldier.getTarget();

			if (!soldier.isValidCombatTarget(target)) {
				soldier.clearCurrentTarget();
				soldier.setAggressive(false);
				drawTicksRemaining = 0;
				stableSightTicks = 0;
				drawCancelGraceTicks = 0;
				panicMode = false;
				return;
			}

			soldier.setVisualArcherBowPose(false);

			double distanceToTargetSqr = soldier.distanceToSqr(target);
			double distanceToTarget = Math.sqrt(distanceToTargetSqr);
			boolean hasLineOfSight = soldier.getSensing().hasLineOfSight(target);
			boolean isSpiderTarget = target.getType() == EntityType.SPIDER
					|| target.getType() == EntityType.CAVE_SPIDER;

			double closeReactionDistance = isSpiderTarget
					? ARCHER_SPIDER_CLOSE_REACTION_DISTANCE
					: ARCHER_CLOSE_REACTION_DISTANCE;

			double minDrawDistance = isSpiderTarget
					? ARCHER_SPIDER_MIN_DRAW_DISTANCE
					: ARCHER_MIN_DRAW_DISTANCE;

			// LOS a curta distância (≤ ARCHER_PANIC_DISTANCE) é considerada como verdadeira:
			// o mob está na cara do arqueiro e hasLineOfSight() pode falhar por sobreposição.
			boolean effectiveLos = hasLineOfSight || distanceToTarget <= ARCHER_PANIC_DISTANCE;

			if (effectiveLos) {
				stableSightTicks++;
			} else {
				stableSightTicks = 0;
			}

			int requiredStableSightTicks = distanceToTarget <= closeReactionDistance
					? ARCHER_CLOSE_STABLE_SIGHT_TICKS
					: ARCHER_STABLE_SIGHT_TICKS;

			// ── 1. RECUPERAÇÃO DE DANO: recua primeiro, atira depois ─────────────────
			if (soldier.isArcherRecoveringFromHit()) {
				drawTicksRemaining = 0;
				stableSightTicks = 0;
				panicMode = false;
				Vec3 retreatAnchor = soldier.getDirectRetreatAnchor(target, ARCHER_HIT_RETREAT_DISTANCE);
				moveInRetreatRunState(retreatAnchor, ARCHER_HIT_RETREAT_SPEED);
				return;
			}

			// ── 2. MODO PÂNICO: mob chegou muito perto — draw rápido + tiro imediato ─
			if (distanceToTarget <= ARCHER_PANIC_DISTANCE) {
				panicMode = true;

				if (attackCooldown > 0) {
					attackCooldown--;
					Vec3 retreatAnchor = soldier.getDirectRetreatAnchor(target, ARCHER_COMFORT_DISTANCE);
					moveInRetreatRunState(retreatAnchor, ARCHER_KITE_SPEED);
					return;
				}

				setAimReadyVisualState(target, 70.0F);

				if (drawTicksRemaining <= 0) {
					if (!soldier.isUsingItem()) {
						soldier.startUsingItem(InteractionHand.MAIN_HAND);
					}
					soldier.setVisualArcherBowPose(true);
					drawCancelGraceTicks = 0;
					drawTicksRemaining = ARCHER_PANIC_BOW_DRAW_TICKS;
					soldier.getNavigation().stop();
				} else {
					soldier.setVisualArcherBowPose(true);
					drawTicksRemaining--;
					if (drawTicksRemaining <= 0) {
						soldier.faceTargetHard(target, 45.0F);
						soldier.performRangedAttack(target, soldier.getRankArcherVelocity());
						attackCooldown = ARCHER_PANIC_ATTACK_COOLDOWN;
						Vec3 retreatAnchor = soldier.getDirectRetreatAnchor(target, ARCHER_COMFORT_DISTANCE);
						moveInRetreatRunState(retreatAnchor, ARCHER_KITE_SPEED);
					}
				}
				return;
			}

			panicMode = false;

			// ── 3. RETREAT DE EMERGÊNCIA: encurralado ou preso ───────────────────────
			if (soldier.shouldForceEmergencyRetreatFromCorner(target, closeReactionDistance)) {
				drawTicksRemaining = 0;
				stableSightTicks = 0;
				lateralDirection *= -1;
				Vec3 retreatAnchor = soldier.getDirectRetreatAnchor(target, ARCHER_COMFORT_DISTANCE + 1.5D);
				moveInRetreatRunState(retreatAnchor, RETREAT_MOVE_SPEED);
				attackCooldown = Math.max(attackCooldown, 8);
				repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
				return;
			}

			setAimReadyVisualState(target, 65.0F);

			boolean canBeginDraw = effectiveLos
					&& distanceToTargetSqr <= attackRangeSqr
					&& distanceToTarget > minDrawDistance
					&& stableSightTicks >= requiredStableSightTicks
					&& soldier.isFacingTarget(target, ARCHER_DRAW_FACE_MAX_ANGLE)
					&& soldier.archerReaimLockTicks <= 0;

			if (drawTicksRemaining > 0) {
				setAimReadyVisualState(target, 65.0F);
				soldier.setVisualArcherBowPose(true);
				soldier.getNavigation().stop();

				if (!canBeginDraw || soldier.hurtTime > 0) {
					drawCancelGraceTicks++;

					if (drawCancelGraceTicks >= 2) {
						drawCancelGraceTicks = 0;
						drawTicksRemaining = 0;
						soldier.stopUsingItem();
					}
					return;
				} else {
					drawCancelGraceTicks = 0;
				}

				drawTicksRemaining--;

				if (drawTicksRemaining <= 0) {
					soldier.faceTargetHard(target, 45.0F);
					soldier.performRangedAttack(target, soldier.getRankArcherVelocity());
					attackCooldown = Math.max(4, soldier.getRankArcherIntervalTicks() - ARCHER_BOW_DRAW_TICKS);
				}
				return;
			}

			if (attackCooldown > 0) {
				attackCooldown--;
			}

			if (canBeginDraw && attackCooldown <= 0) {
				setAimReadyVisualState(target, 65.0F);
				if (!soldier.isUsingItem()) {
					soldier.startUsingItem(InteractionHand.MAIN_HAND);
				}
				soldier.setVisualArcherBowPose(true);
				drawCancelGraceTicks = 0;
				drawTicksRemaining = ARCHER_BOW_DRAW_TICKS;
				soldier.getNavigation().stop();
				return;
			}

			if (soldier.isUsingItem()) {
				soldier.stopUsingItem();
			}

			boolean shouldActivelyRetreat = distanceToTarget < ARCHER_RETREAT_DISTANCE && effectiveLos;

			if (shouldActivelyRetreat) {
				stableSightTicks = Math.max(0, stableSightTicks - 1);
				Vec3 retreatAnchor = soldier.getDirectRetreatAnchor(target, ARCHER_COMFORT_DISTANCE);
				moveInRetreatRunState(retreatAnchor, ARCHER_KITE_SPEED);
				repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
			} else if (distanceToTargetSqr > attackRangeSqr || !effectiveLos) {
				setAimReadyVisualState(target, 45.0F);
				soldier.getNavigation().moveTo(target, speedModifier);
				repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
			} else if (distanceToTarget > ARCHER_COMFORT_DISTANCE + 0.5D) {
				setAimReadyVisualState(target, 45.0F);
				soldier.getNavigation().moveTo(target, speedModifier * 0.90D);
				repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
			} else {
				setAimReadyVisualState(target, 55.0F);
				if (--repositionCooldown <= 0) {
					repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
					lateralDirection = soldier.getRandom().nextBoolean() ? 1 : -1;
					Vec3 repositionAnchor = soldier.getCombatKiteAnchor(
							target,
							ARCHER_COMFORT_DISTANCE,
							lateralDirection * ARCHER_LATERAL_OFFSET
					);
					soldier.getNavigation().moveTo(
							repositionAnchor.x, repositionAnchor.y, repositionAnchor.z,
							speedModifier * 0.9D
					);
				}
			}
		}
	}

	// ─── Retorno ao posto (GUARD) ─────────────────────────────────────────────

	private static final class ReturnToHomePosGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private final double speedModifier;
		private int timeToRecalculatePath;

		private ReturnToHomePosGoal(CastleSoldierEntity soldier, double speedModifier) {
			this.soldier = soldier;
			this.speedModifier = speedModifier;
			this.timeToRecalculatePath = 0;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return soldier.shouldReturnToHomePos();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.shouldReturnToHomePos();
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
			Vec3 homeAnchor = soldier.getHomeAnchor();
			soldier.getLookControl().setLookAt(homeAnchor.x, homeAnchor.y, homeAnchor.z, 30.0F, 30.0F);

			if (--timeToRecalculatePath <= 0) {
				timeToRecalculatePath = 10;
				soldier.getNavigation().moveTo(homeAnchor.x, homeAnchor.y, homeAnchor.z, speedModifier);
			}
		}
	}

	// ─── Pausa no posto (GUARD) ───────────────────────────────────────────────

	private static final class GuardHomePauseGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private int remainingTicks;

		private GuardHomePauseGoal(CastleSoldierEntity soldier) {
			this.soldier = soldier;
			this.remainingTicks = 0;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!soldier.isGuardMode() || soldier.getTarget() != null || !soldier.hasHomePos()) {
				return false;
			}

			if (!soldier.getNavigation().isDone()) {
				return false;
			}

			double maxDistanceSqr = GUARD_HOME_HOLD_DISTANCE * GUARD_HOME_HOLD_DISTANCE;
			return soldier.position().distanceToSqr(soldier.getHomeAnchor()) <= maxDistanceSqr
					&& soldier.getRandom().nextInt(80) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isGuardMode()
					&& soldier.getTarget() == null
					&& remainingTicks > 0;
		}

		@Override
		public void start() {
			remainingTicks = GUARD_HOME_HOLD_MIN_TICKS
					+ soldier.getRandom().nextInt(GUARD_HOME_HOLD_MAX_TICKS - GUARD_HOME_HOLD_MIN_TICKS + 1);
			soldier.getNavigation().stop();
		}

		@Override
		public void stop() {
			remainingTicks = 0;
		}

		@Override
		public void tick() {
			remainingTicks--;
			Vec3 homeAnchor = soldier.getHomeAnchor();
			soldier.getNavigation().stop();
			soldier.getLookControl().setLookAt(homeAnchor.x, homeAnchor.y, homeAnchor.z, 30.0F, 30.0F);
		}
	}

	// ─── Patrulha aleatória centrada no homePos (GUARD) ──────────────────────

	private static final class GuardModeRandomStrollGoal extends RandomStrollGoal {
		private final CastleSoldierEntity soldier;

		private GuardModeRandomStrollGoal(CastleSoldierEntity soldier, double speedModifier) {
			super(soldier, speedModifier);
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.isGuardMode()
					&& soldier.getTarget() == null
					&& !soldier.shouldReturnToHomePos()
					&& super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.isGuardMode()
					&& soldier.getTarget() == null
					&& !soldier.shouldReturnToHomePos()
					&& super.canContinueToUse();
		}

		@Override
		protected Vec3 getPosition() {
			if (!soldier.hasHomePos()) {
				return super.getPosition();
			}

			Vec3 home = soldier.getHomeAnchor();
			double radius = soldier.getGuardRadius();

			for (int attempt = 0; attempt < 10; attempt++) {
				double angle = soldier.getRandom().nextDouble() * Math.PI * 2.0D;
				double dist = radius * (0.35D + soldier.getRandom().nextDouble() * 0.65D);
				Vec3 candidate = home.add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist);

				BlockPos blockPos = BlockPos.containing(candidate);
				if (soldier.level().getBlockState(blockPos.below()).isSolid()) {
					return candidate;
				}
			}

			return super.getPosition();
		}
	}

}
