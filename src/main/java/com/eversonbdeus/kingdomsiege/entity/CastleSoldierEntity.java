package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.ArmorTier;
import com.eversonbdeus.kingdomsiege.soldier.CatalystType;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import com.eversonbdeus.kingdomsiege.soldier.SoldierMode;
import com.eversonbdeus.kingdomsiege.soldier.WeaponClass;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
	private static final double BASE_MAX_HEALTH = 20.0D;
	private static final double BASE_ARMOR = 0.0D;
	private static final double BASE_ARMOR_TOUGHNESS = 0.0D;
	private static final double BASE_KNOCKBACK_RESISTANCE = 0.0D;
	private static final double INHERITED_PROTECTION_REDUCTION_PER_LEVEL = 0.04D;
	private static final double INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL = 0.08D;
	private static final float THORNS_CHANCE_PER_LEVEL = 0.15F;

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
	private static final int DEFAULT_GUARD_RADIUS = 8;

	private SoldierBlueprintData soldierBlueprint = SoldierBlueprintData.defaultRecruit();
	private SoldierMode soldierMode = SoldierMode.GUARD;
	private UUID ownerUuid;
	private BlockPos homePos;
	private int guardRadius = DEFAULT_GUARD_RADIUS;

	public CastleSoldierEntity(Level level) {
		this(ModEntities.CASTLE_SOLDIER, level);
	}

	public CastleSoldierEntity(EntityType<? extends CastleSoldierEntity> entityType, Level level) {
		super(entityType, level);
		xpReward = 0;
		refreshDerivedAttributes();
	}

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

	public int getGuardRadius() {
		return guardRadius;
	}

	public void setGuardRadius(int guardRadius) {
		this.guardRadius = Math.max(1, guardRadius);
	}


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
				currentBlueprint.inheritedThornsLevel()
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

		this.soldierMode = resolvedMode;
		clearCurrentTarget();
		getNavigation().stop();
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

		Player owner = getValidOwnerPlayer();
		return owner != null && distanceToSqr(owner) <= OWNER_PROTECT_RANGE_SQR;
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
	public void tick() {
		super.tick();

		if (!level().isClientSide()) {
			validateOwnerState();
			validateCurrentTarget();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float amount) {
		if (isFriendlyDamageSource(damageSource)) {
			return false;
		}

		float adjustedAmount = applyInheritedDefensiveEnchantments(damageSource, amount);

		if (adjustedAmount <= 0.0F) {
			return false;
		}

		boolean damaged = super.hurtServer(serverLevel, damageSource, adjustedAmount);

		if (damaged) {
			applyInheritedThorns(serverLevel, damageSource);
		}

		return damaged;
	}

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

		adjustedAmount = applyDamageReduction(adjustedAmount, soldierBlueprint.inheritedProtectionLevel(), INHERITED_PROTECTION_REDUCTION_PER_LEVEL);

		if (damageSource != null && damageSource.is(DamageTypeTags.IS_PROJECTILE)) {
			adjustedAmount = applyDamageReduction(adjustedAmount, soldierBlueprint.inheritedProjectileProtectionLevel(), INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL);
		}

		if (damageSource != null && damageSource.is(DamageTypeTags.IS_EXPLOSION)) {
			adjustedAmount = applyDamageReduction(adjustedAmount, soldierBlueprint.inheritedBlastProtectionLevel(), INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL);
		}

		if (damageSource != null && damageSource.is(DamageTypeTags.IS_FIRE)) {
			adjustedAmount = applyDamageReduction(adjustedAmount, soldierBlueprint.inheritedFireProtectionLevel(), INHERITED_SPECIAL_PROTECTION_REDUCTION_PER_LEVEL);
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

	private void clearCurrentTarget() {
		setTarget(null);
		getNavigation().stop();
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

		return target instanceof Monster;
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

	private void refreshDerivedAttributes() {
		double previousMaxHealth = getAttributeValue(Attributes.MAX_HEALTH);
		float previousHealth = getHealth();

		setBaseAttribute(Attributes.MAX_HEALTH, BASE_MAX_HEALTH + soldierBlueprint.getBonusHealth());
		setBaseAttribute(Attributes.ATTACK_DAMAGE, soldierBlueprint.getBaseAttackDamage());
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

		if (healthRatio < 0.0D) {
			healthRatio = 0.0D;
		}

		if (healthRatio > 1.0D) {
			healthRatio = 1.0D;
		}

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

		// Nesta etapa o peitoral deixa de ser equipamento visual padrão.
		// O peitoral continua salvo no blueprint e passa a servir como defesa interna.
		setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		setDropChance(EquipmentSlot.CHEST, 0.0F);
	}

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
					formatOneDecimal(soldierBlueprint.getProjectileBaseDamage())
			);
		}

		return Component.translatable(
				"message.kingdomsiege.soldier_status.power_melee",
				formatOneDecimal(getAttributeValue(Attributes.ATTACK_DAMAGE))
		);
	}

	private Component getInheritedEnchantmentsComponent() {
		if (!soldierBlueprint.hasInheritedChestplateEnchantments()) {
			return null;
		}

		return Component.literal("Heranças do peitoral: " + soldierBlueprint.getInheritedChestplateEnchantmentsSummary());
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

	private void sendBasicStatusTo(Player player) {
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.header",
				getDisplayName(),
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

		Component inheritedEnchantmentsComponent = getInheritedEnchantmentsComponent();
		if (inheritedEnchantmentsComponent != null) {
			player.sendSystemMessage(inheritedEnchantmentsComponent);
		}

		player.sendSystemMessage(getTerritoryStatusComponent());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.store("SoldierBlueprint", SoldierBlueprintData.CODEC, soldierBlueprint);
		valueOutput.store("SoldierMode", SoldierMode.CODEC, soldierMode);
		valueOutput.storeNullable("OwnerUuid", UUIDUtil.STRING_CODEC, ownerUuid);
		valueOutput.store("GuardRadius", Codec.INT, guardRadius);
		valueOutput.storeNullable("HomePosX", Codec.INT, homePos != null ? homePos.getX() : null);
		valueOutput.storeNullable("HomePosY", Codec.INT, homePos != null ? homePos.getY() : null);
		valueOutput.storeNullable("HomePosZ", Codec.INT, homePos != null ? homePos.getZ() : null);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		applyBlueprint(valueInput.read("SoldierBlueprint", SoldierBlueprintData.CODEC).orElse(SoldierBlueprintData.defaultRecruit()));
		setSoldierMode(valueInput.read("SoldierMode", SoldierMode.CODEC).orElse(SoldierMode.GUARD));
		setOwnerUuid(valueInput.read("OwnerUuid", UUIDUtil.STRING_CODEC).orElse(null));
		setGuardRadius(valueInput.read("GuardRadius", Codec.INT).orElse(DEFAULT_GUARD_RADIUS));

		Integer homePosX = valueInput.read("HomePosX", Codec.INT).orElse(null);
		Integer homePosY = valueInput.read("HomePosY", Codec.INT).orElse(null);
		Integer homePosZ = valueInput.read("HomePosZ", Codec.INT).orElse(null);

		if (homePosX != null && homePosY != null && homePosZ != null) {
			setHomePos(new BlockPos(homePosX, homePosY, homePosZ));
		} else {
			clearHomePos();
		}
	}

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

		arrow.setBaseDamage(soldierBlueprint.getProjectileBaseDamage());
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

	private static final class SwordsmanNearestHostileTargetGoal extends NearestAttackableTargetGoal<Monster> {
		private final CastleSoldierEntity soldier;

		private SwordsmanNearestHostileTargetGoal(CastleSoldierEntity soldier) {
			super(soldier, Monster.class, true);
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

	private static final class ArcherNearestHostileTargetGoal extends NearestAttackableTargetGoal<Monster> {
		private final CastleSoldierEntity soldier;

		private ArcherNearestHostileTargetGoal(CastleSoldierEntity soldier) {
			super(soldier, Monster.class, true);
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
		}

		@Override
		public void stop() {
			attackCooldown = 0;
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
			Player owner = soldier.getValidOwnerPlayer();

			if (!soldier.isFollowMode() || owner == null) {
				return false;
			}

			if (soldier.getTarget() != null) {
				return false;
			}

			return soldier.distanceToSqr(owner) > startDistanceSqr;
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
