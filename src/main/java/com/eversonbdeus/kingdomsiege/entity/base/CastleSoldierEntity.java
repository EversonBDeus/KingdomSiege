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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class CastleSoldierEntity extends PathfinderMob implements RangedAttackMob {

	// ─── Atributos base ───────────────────────────────────────────────────────

	private static final double BASE_MOVEMENT_SPEED = 0.28D;
	private static final double BASE_ATTACK_DAMAGE = 4.0D;
	private static final double BASE_FOLLOW_RANGE = 16.0D;
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

	// ─── Constantes de GUARD ──────────────────────────────────────────────────

	private static final double GUARD_MOVE_SPEED = 1.0D;
	private static final double GUARD_RETURN_BUFFER = 1.5D;

	// O soldado persegue até guardRadius + 12 blocos antes de abandonar o alvo.
	private static final double GUARD_CHASE_LEASH = 12.0D;

	// ─── [FASE 5] Limite de seguidores por classe ─────────────────────────────

	// Máximo de espadachins em modo FOLLOW simultaneamente por dono.
	private static final int MAX_FOLLOW_SWORDSMEN = 3;

	// Máximo de arqueiros em modo FOLLOW simultaneamente por dono.
	private static final int MAX_FOLLOW_ARCHERS = 2;

	private static final double GUARD_HOME_HOLD_DISTANCE = 1.75D;
	private static final int GUARD_HOME_HOLD_MIN_TICKS = 30;
	private static final int GUARD_HOME_HOLD_MAX_TICKS = 70;

	// Sistema de chamada de aliados: ativa quando há vários inimigos e vida baixa.
	private static final double GUARD_ALLY_CALL_RANGE = 20.0D;
	private static final double GUARD_ALLY_CALL_RANGE_SQR = GUARD_ALLY_CALL_RANGE * GUARD_ALLY_CALL_RANGE;
	private static final int GUARD_ALLY_CALL_COOLDOWN_TICKS = 120;
	private static final double GUARD_ALLY_CALL_HEALTH_THRESHOLD = 0.5D; // abaixo de 50% de vida
	private static final int GUARD_ALLY_CALL_MIN_ENEMIES = 2;            // 2+ inimigos para acionar

	// ─── Constantes do arqueiro ───────────────────────────────────────────────

	private static final double ARCHER_MOVE_SPEED = 1.0D;
	private static final double ARCHER_ATTACK_RANGE = 12.0D;
	private static final double ARCHER_COMFORT_DISTANCE = 7.0D;
	private static final double ARCHER_RETREAT_DISTANCE = 4.5D;
	private static final double ARCHER_LATERAL_OFFSET = 2.0D;
	private static final int ARCHER_REPOSITION_INTERVAL_TICKS = 12;
	private static final int ARCHER_ATTACK_INTERVAL_TICKS = 30;
	private static final float ARCHER_PROJECTILE_VELOCITY = 1.6F;

	// ─── Constantes de proteção ao dono ──────────────────────────────────────

	private static final double OWNER_PROTECT_RANGE = 16.0D;
	private static final double OWNER_PROTECT_RANGE_SQR = OWNER_PROTECT_RANGE * OWNER_PROTECT_RANGE;

	// ─── Constantes de FOLLOW ─────────────────────────────────────────────────

	private static final double FOLLOW_START_DISTANCE = 6.0D;
	private static final double FOLLOW_STOP_DISTANCE = 3.5D;
	private static final double FOLLOW_DESIRED_DISTANCE = 4.0D;
	private static final double FOLLOW_MOVE_SPEED = 1.15D;
	private static final double FOLLOW_REJOIN_DISTANCE = 24.0D;
	private static final double FOLLOW_REJOIN_DISTANCE_SQR = FOLLOW_REJOIN_DISTANCE * FOLLOW_REJOIN_DISTANCE;

	// Espera 4 segundos parado antes de começar a vagar.
	private static final int FOLLOW_OWNER_STATIONARY_TICKS = 80;

	private static final double FOLLOW_OWNER_MOVEMENT_TOLERANCE = 0.04D;
	private static final double FOLLOW_OWNER_MOVEMENT_TOLERANCE_SQR =
			FOLLOW_OWNER_MOVEMENT_TOLERANCE * FOLLOW_OWNER_MOVEMENT_TOLERANCE;

	// Raio máximo de vagar em torno do dono parado.
	private static final double FOLLOW_ROAM_MAX_DISTANCE = 12.0D;
	private static final double FOLLOW_ROAM_MAX_DISTANCE_SQR = FOLLOW_ROAM_MAX_DISTANCE * FOLLOW_ROAM_MAX_DISTANCE;

	// [FASE 4] Mínimo 3.5 blocos — nunca entra no espaço do player.
	private static final double FOLLOW_ROAM_MIN_RADIUS = 3.5D;

	// [FASE 4] Máximo 5.5 blocos — mantém distância respeitosa.
	private static final double FOLLOW_ROAM_MAX_RADIUS = 5.5D;

	// Recalcula destino de vagar a cada 80 ticks (4 s) no máximo.
	private static final int FOLLOW_ROAM_RECALCULATE_TICKS = 80;

	// [FASE 4] Repouso de 3–6 s após chegar ao ponto de vagar, antes de escolher o próximo.
	private static final int FOLLOW_ROAM_REST_MIN_TICKS = 60;
	private static final int FOLLOW_ROAM_REST_MAX_TICKS = 120;

	// [FASE 4] Threshold de "chegou ao ponto de vagar": 4 blocos² (= 2 blocos).
	// Valor maior que o antigo (2.0) evita que o soldado oscile sem registrar chegada.
	private static final double FOLLOW_ROAM_ARRIVAL_THRESHOLD_SQR = 4.0D;

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

	// [FASE 6] Identidade persistida — usada agora para rank e XP.
	private SoldierIdentityData soldierIdentity = SoldierIdentityData.defaultRecruit();

	// ─── Estado interno de navegação/follow ───────────────────────────────────

	private Vec3 lastOwnerFollowSample;
	private int ownerStillTicks;
	private Vec3 lastNavigationProgressSample;
	private int stationaryNavigationTicks;
	private int movementAssistCooldown;
	private int meleeCritJumpCooldown;

	// Cooldown para chamada de aliados — evita chamar toda hora.
	private int allyCallCooldown = 0;

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

	/**
	 * Define o modo de comando do soldado.
	 *
	 * [FASE 4] Ao mudar para GUARD sem homePos definido, usa a posição atual
	 * como posto de guarda. Isso evita que o soldado fique parado sem território
	 * ao ser liberado do modo FOLLOW.
	 */
	public void setSoldierMode(SoldierMode soldierMode) {
		SoldierMode resolvedMode = soldierMode != null ? soldierMode : SoldierMode.GUARD;

		if (this.soldierMode == resolvedMode) {
			if (resolvedMode == SoldierMode.GUARD) {
				getNavigation().stop();
			}
			return;
		}

		// [FASE 5] Bloqueia FOLLOW se o limite de seguidores da classe foi atingido.
		if (resolvedMode == SoldierMode.FOLLOW && !isFollowSlotAvailable()) {
			notifyOwnerFollowLimitReached();
			return;
		}

		this.soldierMode = resolvedMode;
		clearCurrentTarget();
		getNavigation().stop();

		// [FASE 4] Ao entrar em GUARD sem homePos, define o posto atual na posição corrente.
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

	/**
	 * [FASE 4] Bug corrigido: a atribuição estava triplicada no código anterior.
	 */
	public void setSoldierIdentity(SoldierIdentityData identity) {
		this.soldierIdentity = identity != null ? identity : SoldierIdentityData.defaultRecruit();
	}

	/** Retorna o nome de exibição: nome custom se definido, senão o nome padrão da entidade. */
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

		if (!level().isClientSide()) {
			if (allyCallCooldown > 0) {
				allyCallCooldown--;
			}

			updateFollowOwnerMotionState();
			validateOwnerState();
			validateCurrentTarget();
			tickAdvancedMovementSupport();
			tickMeleeCriticalJumpSupport();
		}
	}

	// ─── Dano recebido ────────────────────────────────────────────────────────

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isBlockedDamageFromOwner(source)) {
			return false;
		}

		boolean damaged = super.hurtServer(level, source, amount);

		// Em GUARD, com vida baixa e vários inimigos, chama aliado próximo.
		if (damaged && isGuardMode()) {
			tryCallForAllyHelp(level);
		}

		return damaged;
	}

	/** Verifica condições para chamar aliados e aciona se necessário. */
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

	/** Encontra o aliado ocioso mais próximo e manda ele ajudar. */
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

	// ─── Dano causado ────────────────────────────────────────────────────────

	@Override
	public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
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
			return;
		}

		if (!isValidCombatTarget(currentTarget)) {
			clearCurrentTarget();
			return;
		}

		if (shouldDisengageFromTarget(currentTarget)) {
			clearCurrentTarget();
		}
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

	private Vec3 getFollowAnchor(Player owner) {
		Vec3 lookDirection = owner.getLookAngle();
		Vec3 horizontalLookDirection = new Vec3(lookDirection.x, 0.0D, lookDirection.z);

		if (horizontalLookDirection.lengthSqr() < 1.0E-4D) {
			horizontalLookDirection = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			horizontalLookDirection = horizontalLookDirection.normalize();
		}

		return owner.position().subtract(horizontalLookDirection.scale(FOLLOW_DESIRED_DISTANCE));
	}

	private Vec3 getFollowRoamAnchor(Player owner) {
		Vec3 ownerPosition = owner.position();

		// Tenta 8 vezes encontrar posição diferente da atual.
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = getRandom().nextDouble() * Math.PI * 2.0D;
			double radius = FOLLOW_ROAM_MIN_RADIUS
					+ getRandom().nextDouble() * (FOLLOW_ROAM_MAX_RADIUS - FOLLOW_ROAM_MIN_RADIUS);
			Vec3 candidate = ownerPosition.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);

			if (candidate.distanceToSqr(position()) > 2.0D) {
				return candidate;
			}
		}

		// Fallback seguro.
		return ownerPosition.add(0.0D, 0.0D, FOLLOW_ROAM_MIN_RADIUS + 0.5D);
	}

	// ─── Estado de movimento do dono ──────────────────────────────────────────

	private boolean hasOwnerBeenStillLongEnough() {
		return ownerStillTicks >= FOLLOW_OWNER_STATIONARY_TICKS;
	}

	private boolean shouldRoamAroundStoppedOwner(Player owner) {
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

		if (stationaryNavigationTicks >= NAVIGATION_REPATH_TICKS) {
			getNavigation().recomputePath();
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

	private boolean shouldTryJumpAssist() {
		if (movementAssistCooldown > 0 || !onGround() || isInWater() || onClimbable()) {
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
				&& horizontalTargetDistanceSqr >= NAVIGATION_JUMP_MIN_TARGET_DISTANCE_SQR
				&& stationaryNavigationTicks >= 2;
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
		getNavigation().stop();
	}

	// ─── [FASE 5] Controle de limite de seguidores ───────────────────────────

	/**
	 * Conta quantos soldados do mesmo dono e da mesma classe
	 * estão atualmente em modo FOLLOW no nível (não conta a si mesmo).
	 */
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

	/** Limite máximo de seguidores permitido para a classe deste soldado. */
	private int getMaxFollowersForMyClass() {
		return isSwordsman() ? MAX_FOLLOW_SWORDSMEN : MAX_FOLLOW_ARCHERS;
	}

	/** Retorna true se ainda há vaga para este soldado entrar em FOLLOW. */
	private boolean isFollowSlotAvailable() {
		return countFollowersOfClass(getSoldierClass()) < getMaxFollowersForMyClass();
	}

	/** Notifica o dono que o limite de seguidores da classe foi atingido. */
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

	private Player getValidOwnerPlayer() {
		Player owner = getOwnerPlayer();

		if (owner == null || !owner.isAlive() || owner.isRemoved() || owner.isSpectator()) {
			return null;
		}

		return owner;
	}

	private boolean isValidCombatTarget(LivingEntity target) {
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
		ItemStack mainHandStack = soldierBlueprint.weaponStack().isEmpty()
				? switch (getWeaponClass()) {
			case BOW -> new ItemStack(Items.BOW);
			case SWORD -> new ItemStack(Items.IRON_SWORD);
			default -> ItemStack.EMPTY;
		}
				: soldierBlueprint.weaponStack().copy();

		if (!mainHandStack.isEmpty()) {
			mainHandStack.setCount(1);
		}

		setItemSlot(EquipmentSlot.MAINHAND, mainHandStack);

		// Peitoral interno — defesa via blueprint, não armadura visual.
		setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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

	private Component getArmorTierComponent() {
		return soldierBlueprint.chestplateStack().isEmpty()
				? Component.translatable(getArmorTier().getTranslationKey())
				: soldierBlueprint.chestplateStack().getHoverName();
	}

	private Component getWeaponClassComponent() {
		return soldierBlueprint.weaponStack().isEmpty()
				? Component.translatable(getWeaponClass().getTranslationKey())
				: soldierBlueprint.weaponStack().getHoverName();
	}

	private Component getCatalystComponent() {
		return Component.translatable(getCatalystType().getTranslationKey());
	}

	private Component getCombatPowerComponent() {
		if (canUseBowCombat()) {
			return Component.translatable(
					"message.kingdomsiege.soldier_status.power_ranged",
					formatOneDecimal(getEffectiveProjectileBaseDamage())
			);
		}

		return Component.translatable(
				"message.kingdomsiege.soldier_status.power_melee",
				formatOneDecimal(getAttributeValue(Attributes.ATTACK_DAMAGE))
		);
	}

	private List<Component> getInheritedChestplateEnchantmentsComponents() {
		List<Component> components = new ArrayList<>();

		if (!soldierBlueprint.hasInheritedChestplateEnchantments()) {
			return components;
		}

		components.add(Component.translatable("text.kingdomsiege.inheritance.chestplate_header")
				.withStyle(ChatFormatting.LIGHT_PURPLE));

		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.protection",
				soldierBlueprint.inheritedProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.projectile_protection",
				soldierBlueprint.inheritedProjectileProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.blast_protection",
				soldierBlueprint.inheritedBlastProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.fire_protection",
				soldierBlueprint.inheritedFireProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.thorns",
				soldierBlueprint.inheritedThornsLevel(), ChatFormatting.LIGHT_PURPLE);

		return components;
	}

	private List<Component> getInheritedWeaponEnchantmentsComponents() {
		List<Component> components = new ArrayList<>();

		int effectiveSharpnessLevel = getEffectiveInheritedSharpnessLevel();
		int effectiveFireAspectLevel = getEffectiveInheritedFireAspectLevel();
		int effectiveKnockbackLevel = getEffectiveInheritedKnockbackLevel();
		int effectivePowerLevel = getEffectiveInheritedPowerLevel();
		int effectivePunchLevel = getEffectiveInheritedPunchLevel();
		int effectiveFlameLevel = getEffectiveInheritedFlameLevel();

		boolean hasAnyInheritance;

		if (canUseBowCombat()) {
			hasAnyInheritance = effectivePowerLevel > 0
					|| effectivePunchLevel > 0
					|| effectiveFlameLevel > 0;
		} else {
			hasAnyInheritance = effectiveSharpnessLevel > 0
					|| effectiveFireAspectLevel > 0
					|| effectiveKnockbackLevel > 0;
		}

		if (!hasAnyInheritance) {
			return components;
		}

		components.add(Component.translatable("text.kingdomsiege.inheritance.weapon_header")
				.withStyle(ChatFormatting.GOLD));

		if (canUseBowCombat()) {
			appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.power",
					effectivePowerLevel, ChatFormatting.GOLD);
			appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.punch",
					effectivePunchLevel, ChatFormatting.GOLD);
			appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.flame",
					effectiveFlameLevel, ChatFormatting.GOLD);
			return components;
		}

		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.sharpness",
				effectiveSharpnessLevel, ChatFormatting.GOLD);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.fire_aspect",
				effectiveFireAspectLevel, ChatFormatting.GOLD);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.knockback",
				effectiveKnockbackLevel, ChatFormatting.GOLD);

		return components;
	}

	private void appendLevelInheritanceComponent(
			List<Component> components,
			String inheritanceKey,
			int level,
			ChatFormatting color
	) {
		if (level <= 0) {
			return;
		}

		components.add(
				Component.literal("• ")
						.append(Component.translatable(inheritanceKey))
						.append(Component.literal(" " + toRoman(level)))
						.withStyle(color)
		);
	}

	private Component getTerritoryStatusComponent() {
		if (hasHomePos()) {
			return Component.translatable(
					"message.kingdomsiege.soldier_status.territory",
					homePos.getX(),
					homePos.getY(),
					homePos.getZ(),
					guardRadius
			);
		}

		return Component.translatable(
				"message.kingdomsiege.soldier_status.territory_undefined",
				guardRadius
		);
	}

	private String formatOneDecimal(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	private String toRoman(int level) {
		return switch (level) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> Integer.toString(level);
		};
	}

	private void sendBasicStatusTo(Player player) {
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.header",
				getSoldierDisplayName(),
				Component.translatable(getSoldierClass().getTranslationKey()),
				Component.translatable(getSoldierMode().getTranslationKey())
		));

		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.combat",
				getWeaponClassComponent(),
				getArmorTierComponent(),
				getCatalystComponent()
		));

		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.attributes",
				Math.round(getHealth()),
				Math.round(getMaxHealth()),
				Math.round(getArmorValue()),
				Math.round(getAttributeValue(Attributes.ARMOR_TOUGHNESS))
		));

		player.sendSystemMessage(getCombatPowerComponent());

		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.rank",
				Component.translatable(getSoldierRank().getTranslationKey()),
				getMilitaryXp()
		));

		for (Component line : getInheritedChestplateEnchantmentsComponents()) {
			player.sendSystemMessage(line);
		}

		for (Component line : getInheritedWeaponEnchantmentsComponents()) {
			player.sendSystemMessage(line);
		}

		player.sendSystemMessage(getTerritoryStatusComponent());
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

		if (player.getItemInHand(hand).isEmpty()) {
			if (!level().isClientSide()) {
				sendBasicStatusTo(player);
			}

			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	// ─── Progressão militar ───────────────────────────────────────────────────

	/**
	 * Chamado pelo Minecraft toda vez que esta entidade mata outra.
	 * Concede XP militar e verifica promoção de rank.
	 */
	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity killedEntity, DamageSource damageSource) {
		boolean result = super.killedEntity(level, killedEntity, damageSource);

		int xpGain = resolveXpGainFor(killedEntity);
		if (xpGain <= 0) {
			return result;
		}

		SoldierRank rankBefore = getSoldierRank();
		setSoldierIdentity(soldierIdentity.earnXp(xpGain));
		SoldierRank rankAfter = getSoldierRank();

		if (rankAfter.ordinal() > rankBefore.ordinal()) {
			notifyOwnerOfPromotion(level, rankAfter);
		}

		return result;
	}

	private int resolveXpGainFor(LivingEntity killed) {
		if (killed == null) {
			return 0;
		}
		if (killed instanceof CastleSoldierEntity) {
			return 0;
		}
		if (killed instanceof Player) {
			return 0;
		}

		boolean isElite = killed.getMaxHealth() >= 40f || killed.getArmorValue() >= 10;
		return isElite ? 15 : 5;
	}

	private void notifyOwnerOfPromotion(ServerLevel level, SoldierRank newRank) {
		Player owner = getValidOwnerPlayer();
		if (owner == null) {
			return;
		}
		owner.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_rank_up",
				getSoldierDisplayName(),
				Component.translatable(newRank.getTranslationKey())
		));
	}

	// ─── Ataque ranged (arqueiro) ─────────────────────────────────────────────

	@Override
	public void performRangedAttack(LivingEntity target, float velocity) {
		if (!canUseBowCombat() || !isValidCombatTarget(target)) {
			return;
		}

		ItemStack arrowStack = new ItemStack(Items.ARROW);
		ItemStack weaponStack = soldierBlueprint.weaponStack().isEmpty()
				? new ItemStack(Items.BOW)
				: soldierBlueprint.weaponStack().copy();

		Arrow arrow = new Arrow(level(), this, arrowStack, weaponStack);

		double deltaX = target.getX() - getX();
		double deltaY = target.getEyeY() - arrow.getY();
		double deltaZ = target.getZ() - getZ();
		double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

		arrow.setBaseDamage(getEffectiveProjectileBaseDamage());
		arrow.shoot(deltaX, deltaY + horizontalDistance * 0.2D, deltaZ, velocity, 8.0F);
		applyInheritedProjectileEnchantments(arrow);

		playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
		level().addFreshEntity(arrow);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// Goals internos
	// ═════════════════════════════════════════════════════════════════════════

	// ─── Proteger dono quando atacado ────────────────────────────────────────

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
			Player owner = soldier.getValidOwnerPlayer();

			if (owner == null) {
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

	// ─── Assistir alvo atacado pelo dono ─────────────────────────────────────

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
			Player owner = soldier.getValidOwnerPlayer();

			if (owner == null) {
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

	// ─── Melee do espadachim ──────────────────────────────────────────────────

	private static final class SwordsmanMeleeAttackGoal extends MeleeAttackGoal {
		private final CastleSoldierEntity soldier;

		private SwordsmanMeleeAttackGoal(CastleSoldierEntity soldier, double speedModifier, boolean followingTargetEvenIfNotSeen) {
			super(soldier, speedModifier, followingTargetEvenIfNotSeen);
			this.soldier = soldier;
		}

		@Override
		public boolean canUse() {
			return soldier.canUseSwordCombat() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soldier.canUseSwordCombat() && super.canContinueToUse();
		}
	}

	// ─── Alvo hostil do espadachim ────────────────────────────────────────────

	private static final class SwordsmanNearestHostileTargetGoal extends NearestAttackableTargetGoal<Mob> {
		private final CastleSoldierEntity soldier;

		private SwordsmanNearestHostileTargetGoal(CastleSoldierEntity soldier) {
			super(soldier, Mob.class, 10, true, false, (target, level) -> soldier.isValidCombatTarget(target));
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
			super(soldier, Mob.class, 10, true, false, (target, level) -> soldier.isValidCombatTarget(target));
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

	// ─── Goal ranged do arqueiro ──────────────────────────────────────────────

	private static final class ArcherRangedAttackGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private final double speedModifier;
		private final double attackRangeSqr;
		private int attackCooldown;
		private int repositionCooldown;
		private int lateralDirection;

		private ArcherRangedAttackGoal(CastleSoldierEntity soldier, double speedModifier, double attackRange) {
			this.soldier = soldier;
			this.speedModifier = speedModifier;
			this.attackRangeSqr = attackRange * attackRange;
			this.attackCooldown = 0;
			this.repositionCooldown = 0;
			this.lateralDirection = 1;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = soldier.getTarget();
			return soldier.canUseBowCombat() && target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity target = soldier.getTarget();
			return soldier.canUseBowCombat() && target != null && target.isAlive();
		}

		@Override
		public void start() {
			attackCooldown = 0;
			repositionCooldown = 0;
			lateralDirection = soldier.getRandom().nextBoolean() ? 1 : -1;
		}

		@Override
		public void stop() {
			attackCooldown = 0;
			repositionCooldown = 0;
			soldier.getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = soldier.getTarget();

			if (!soldier.isValidCombatTarget(target)) {
				soldier.setTarget(null);
				soldier.getNavigation().stop();
				return;
			}

			double distanceToTargetSqr = soldier.distanceToSqr(target);
			double distanceToTarget = Math.sqrt(distanceToTargetSqr);
			boolean hasLineOfSight = soldier.getSensing().hasLineOfSight(target);

			soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);

			if (distanceToTarget < ARCHER_RETREAT_DISTANCE && hasLineOfSight) {
				Vec3 retreatAnchor = soldier.getCombatKiteAnchor(target, ARCHER_COMFORT_DISTANCE, lateralDirection * ARCHER_LATERAL_OFFSET);
				soldier.getNavigation().moveTo(retreatAnchor.x, retreatAnchor.y, retreatAnchor.z, speedModifier);
				repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
			} else if (distanceToTargetSqr > attackRangeSqr || !hasLineOfSight) {
				soldier.getNavigation().moveTo(target, speedModifier);
				repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
			} else if (distanceToTarget > ARCHER_COMFORT_DISTANCE + 1.0D) {
				soldier.getNavigation().moveTo(target, speedModifier * 0.85D);
				repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
			} else {
				if (--repositionCooldown <= 0) {
					repositionCooldown = ARCHER_REPOSITION_INTERVAL_TICKS;
					lateralDirection = soldier.getRandom().nextBoolean() ? 1 : -1;
					Vec3 repositionAnchor = soldier.getCombatKiteAnchor(target, ARCHER_COMFORT_DISTANCE, lateralDirection * ARCHER_LATERAL_OFFSET);
					soldier.getNavigation().moveTo(repositionAnchor.x, repositionAnchor.y, repositionAnchor.z, speedModifier * 0.9D);
				}
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

		/** Destino centrado no homePos, cobre toda a área de guarda. */
		@Override
		protected Vec3 getPosition() {
			if (!soldier.hasHomePos()) {
				return super.getPosition();
			}

			Vec3 home = soldier.getHomeAnchor();
			double radius = soldier.getGuardRadius();

			for (int attempt = 0; attempt < 10; attempt++) {
				double angle = soldier.getRandom().nextDouble() * Math.PI * 2.0D;
				// Entre 35% e 100% do raio para cobrir toda a área.
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

	// ─── Seguir dono (FOLLOW) ─────────────────────────────────────────────────

	private static final class FollowOwnerGoal extends Goal {
		private final CastleSoldierEntity soldier;
		private final double speedModifier;
		private final double startDistanceSqr;
		private final double stopDistanceSqr;
		private int timeToRecalculatePath;
		private Vec3 roamAnchor;
		private int roamRecalculateTicks;
		private int roamRestTicks;

		private FollowOwnerGoal(CastleSoldierEntity soldier, double speedModifier, double startDistance, double stopDistance) {
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

			if (distanceToOwnerSqr >= FOLLOW_REJOIN_DISTANCE_SQR) {
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
			if (roamAnchor != null && soldier.position().distanceToSqr(roamAnchor) <= FOLLOW_ROAM_ARRIVAL_THRESHOLD_SQR) {
				roamRestTicks = FOLLOW_ROAM_REST_MIN_TICKS
						+ soldier.getRandom().nextInt(FOLLOW_ROAM_REST_MAX_TICKS - FOLLOW_ROAM_REST_MIN_TICKS + 1);
				soldier.getNavigation().stop();
				return;
			}

			boolean needsNewAnchor = roamAnchor == null
					|| --roamRecalculateTicks <= 0
					|| soldier.getNavigation().isStuck();

			if (needsNewAnchor) {
				roamRecalculateTicks = FOLLOW_ROAM_RECALCULATE_TICKS;
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
}