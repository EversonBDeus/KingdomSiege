package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import com.eversonbdeus.kingdomsiege.soldier.SoldierDefenseProfile;
import com.eversonbdeus.kingdomsiege.soldier.SoldierMode;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
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
	private SoldierDefenseProfile defenseProfile = SoldierDefenseProfile.UNARMORED;
	private UUID ownerUuid;
	private int defenseDurability;
	private int defenseMaxDurability;

	public CastleSoldierEntity(Level level) {
		this(ModEntities.CASTLE_SOLDIER, level);
	}

	public CastleSoldierEntity(EntityType<? extends CastleSoldierEntity> entityType, Level level) {
		super(entityType, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
				.add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
				.add(Attributes.FOLLOW_RANGE, BASE_FOLLOW_RANGE)
				.add(Attributes.ARMOR, BASE_ARMOR);
	}

	public SoldierClass getSoldierClass() {
		return soldierClass;
	}

	public void setSoldierClass(SoldierClass soldierClass) {
		this.soldierClass = soldierClass != null ? soldierClass : SoldierClass.SWORDSMAN;
		refreshDerivedAttributes();
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

	public SoldierDefenseProfile getDefenseProfile() {
		return defenseProfile;
	}

	public int getDefenseDurability() {
		return defenseDurability;
	}

	public int getDefenseMaxDurability() {
		return defenseMaxDurability;
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

		boolean hurt = super.hurtServer(serverLevel, damageSource, amount);

		if (hurt) {
			consumeDefenseDurability(amount);
		}

		return hurt;
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

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.store("SoldierClass", SoldierClass.CODEC, soldierClass);
		valueOutput.store("SoldierMode", SoldierMode.CODEC, soldierMode);
		valueOutput.store("DefenseProfile", SoldierDefenseProfile.CODEC, defenseProfile);
		valueOutput.store("DefenseDurability", Codec.INT, defenseDurability);
		valueOutput.store("DefenseMaxDurability", Codec.INT, defenseMaxDurability);
		valueOutput.storeNullable("OwnerUuid", UUIDUtil.STRING_CODEC, ownerUuid);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		setSoldierClass(valueInput.read("SoldierClass", SoldierClass.CODEC).orElse(SoldierClass.SWORDSMAN));
		setSoldierMode(valueInput.read("SoldierMode", SoldierMode.CODEC).orElse(SoldierMode.GUARD));
		defenseProfile = valueInput.read("DefenseProfile", SoldierDefenseProfile.CODEC).orElse(SoldierDefenseProfile.UNARMORED);
		defenseDurability = valueInput.read("DefenseDurability", Codec.INT).orElse(0);
		defenseMaxDurability = valueInput.read("DefenseMaxDurability", Codec.INT).orElse(0);
		setOwnerUuid(valueInput.read("OwnerUuid", UUIDUtil.STRING_CODEC).orElse(null));
		refreshDerivedAttributes();
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (!isOwnedBy(player)) {
			return super.mobInteract(player, hand);
		}

		ItemStack heldStack = player.getItemInHand(hand);

		if (isSupportedEquipmentItem(heldStack)) {
			if (!level().isClientSide()) {
				handleEquipmentInteraction(player, heldStack);
			}

			return InteractionResult.SUCCESS;
		}

		if (heldStack.isEmpty() && player.isShiftKeyDown()) {
			if (!level().isClientSide()) {
				toggleSoldierMode();
				player.sendSystemMessage(Component.translatable("message.kingdomsiege.soldier_mode_changed", Component.translatable(getSoldierMode().getTranslationKey())));
			}

			return InteractionResult.SUCCESS;
		}

		if (heldStack.isEmpty()) {
			if (!level().isClientSide()) {
				sendStatusReport(player);
			}

			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public void performRangedAttack(LivingEntity target, float velocity) {
		if (!isValidCombatTarget(target)) {
			return;
		}

		ItemStack arrowSource = getArrowSource();
		ItemStack arrowStack = arrowSource.isEmpty() ? new ItemStack(Items.ARROW) : arrowSource.copyWithCount(1);
		ItemStack weaponStack = hasEquippedBow() ? getMainHandItem() : new ItemStack(Items.BOW);

		Arrow arrow = new Arrow(level(), this, arrowStack, weaponStack);

		double deltaX = target.getX() - getX();
		double deltaY = target.getEyeY() - arrow.getY();
		double deltaZ = target.getZ() - getZ();
		double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

		arrow.setBaseDamage(ARCHER_ARROW_DAMAGE);
		arrow.shoot(deltaX, deltaY + horizontalDistance * 0.2D, deltaZ, velocity, 8.0F);

		playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
		level().addFreshEntity(arrow);

		consumeArrow();
	}

	private boolean isSupportedEquipmentItem(ItemStack stack) {
		return isSupportedWeaponItem(stack) || isSupportedAmmoItem(stack) || isSupportedDefenseItem(stack);
	}

	private boolean isSupportedWeaponItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		if (isSwordsman()) {
			return isSupportedSwordsmanWeapon(stack);
		}

		if (isArcher()) {
			return stack.is(Items.BOW);
		}

		return false;
	}

	private boolean isSupportedSwordsmanWeapon(ItemStack stack) {
		return stack.is(Items.WOODEN_SWORD)
				|| stack.is(Items.STONE_SWORD)
				|| stack.is(Items.IRON_SWORD)
				|| stack.is(Items.GOLDEN_SWORD)
				|| stack.is(Items.DIAMOND_SWORD)
				|| stack.is(Items.NETHERITE_SWORD);
	}

	private boolean isSupportedAmmoItem(ItemStack stack) {
		return isArcher() && stack != null && !stack.isEmpty() && stack.is(Items.ARROW);
	}

	private boolean isSupportedDefenseItem(ItemStack stack) {
		return SoldierDefenseProfile.fromChestplate(stack) != SoldierDefenseProfile.UNARMORED;
	}

	private void handleEquipmentInteraction(Player player, ItemStack heldStack) {
		if (isSupportedWeaponItem(heldStack)) {
			equipWeaponFromOwner(player, heldStack);
			return;
		}

		if (isSupportedAmmoItem(heldStack)) {
			equipAmmoFromOwner(player, heldStack);
			return;
		}

		if (isSupportedDefenseItem(heldStack)) {
			equipDefenseFromOwner(player, heldStack);
		}
	}

	private void equipWeaponFromOwner(Player player, ItemStack heldStack) {
		ItemStack equippedStack = heldStack.copyWithCount(1);
		ItemStack previousWeapon = getMainHandItem().copy();

		setItemSlot(EquipmentSlot.MAINHAND, equippedStack);
		refreshDerivedAttributes();

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		returnItemToOwner(player, previousWeapon);
		player.sendSystemMessage(Component.translatable("message.kingdomsiege.soldier_weapon_equipped", equippedStack.getHoverName()));
	}

	private void equipAmmoFromOwner(Player player, ItemStack heldStack) {
		ItemStack currentAmmo = getOffhandItem();
		int maxStackSize = heldStack.getMaxStackSize();
		int transferAmount;

		if (currentAmmo.isEmpty()) {
			transferAmount = Math.min(heldStack.getCount(), maxStackSize);
			setItemSlot(EquipmentSlot.OFFHAND, heldStack.copyWithCount(transferAmount));
		} else if (currentAmmo.is(Items.ARROW)) {
			int availableSpace = maxStackSize - currentAmmo.getCount();
			transferAmount = Math.min(heldStack.getCount(), availableSpace);

			if (transferAmount <= 0) {
				player.sendSystemMessage(Component.translatable("message.kingdomsiege.soldier_ammo_full"));
				return;
			}

			currentAmmo.grow(transferAmount);
		} else {
			player.sendSystemMessage(Component.translatable("message.kingdomsiege.soldier_ammo_slot_busy"));
			return;
		}

		if (!player.isCreative()) {
			heldStack.shrink(transferAmount);
		}

		player.sendSystemMessage(Component.translatable("message.kingdomsiege.soldier_ammo_equipped", getAmmoCount()));
	}

	private void equipDefenseFromOwner(Player player, ItemStack heldStack) {
		SoldierDefenseProfile newProfile = SoldierDefenseProfile.fromChestplate(heldStack);

		defenseProfile = newProfile;
		defenseMaxDurability = heldStack.getMaxDamage();
		defenseDurability = Math.max(0, defenseMaxDurability - heldStack.getDamageValue());
		refreshDerivedAttributes();

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		player.sendSystemMessage(Component.translatable("message.kingdomsiege.soldier_defense_equipped", Component.translatable(defenseProfile.getTranslationKey())));
	}

	private void returnItemToOwner(Player player, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}

		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	private void sendStatusReport(Player player) {
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.header",
				getDisplayName(),
				Component.translatable(soldierClass.getTranslationKey()),
				Component.translatable(soldierMode.getTranslationKey())
		));
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.health",
				Math.round(getHealth()),
				Math.round(getMaxHealth())
		));
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.weapon",
				getWeaponStatusComponent(),
				getDurabilityStatusComponent(getMainHandItem())
		));
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.defense",
				Component.translatable(defenseProfile.getTranslationKey()),
				Math.round(getArmorValue()),
				getDefenseDurabilityStatusComponent()
		));

		if (isArcher()) {
			player.sendSystemMessage(Component.translatable(
					"message.kingdomsiege.soldier_status.ammo",
					getAmmoStatusComponent()
			));
		}
	}

	private Component getWeaponStatusComponent() {
		ItemStack weaponStack = getMainHandItem();

		if (!weaponStack.isEmpty()) {
			return weaponStack.getHoverName();
		}

		if (isArcher()) {
			return Component.translatable("message.kingdomsiege.soldier_weapon_default_archer");
		}

		return Component.translatable("message.kingdomsiege.soldier_weapon_default_swordsman");
	}

	private Component getDurabilityStatusComponent(ItemStack stack) {
		if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
			return Component.translatable("message.kingdomsiege.soldier_status.none");
		}

		int currentDurability = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
		return Component.translatable("message.kingdomsiege.soldier_status.durability_value", currentDurability, stack.getMaxDamage());
	}

	private Component getDefenseDurabilityStatusComponent() {
		if (defenseMaxDurability <= 0) {
			return Component.translatable("message.kingdomsiege.soldier_status.none");
		}

		return Component.translatable("message.kingdomsiege.soldier_status.durability_value", defenseDurability, defenseMaxDurability);
	}

	private Component getAmmoStatusComponent() {
		ItemStack ammoStack = getArrowSource();

		if (ammoStack.isEmpty()) {
			return Component.translatable("message.kingdomsiege.soldier_status.ammo_default");
		}

		return Component.literal(String.valueOf(ammoStack.getCount()));
	}

	private ItemStack getArrowSource() {
		ItemStack offhandItem = getOffhandItem();
		return offhandItem.is(Items.ARROW) ? offhandItem : ItemStack.EMPTY;
	}

	private int getAmmoCount() {
		ItemStack ammoStack = getArrowSource();
		return ammoStack.isEmpty() ? 0 : ammoStack.getCount();
	}

	private boolean hasEquippedBow() {
		return getMainHandItem().is(Items.BOW);
	}

	private void consumeArrow() {
		ItemStack ammoStack = getArrowSource();

		if (ammoStack.isEmpty()) {
			return;
		}

		ammoStack.shrink(1);

		if (ammoStack.isEmpty()) {
			setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	}

	private void consumeDefenseDurability(float damageAmount) {
		if (defenseMaxDurability <= 0 || defenseDurability <= 0 || damageAmount <= 0.0F) {
			return;
		}

		defenseDurability = Math.max(0, defenseDurability - Math.max(1, Math.round(damageAmount)));

		if (defenseDurability == 0) {
			defenseProfile = SoldierDefenseProfile.UNARMORED;
			defenseMaxDurability = 0;
			refreshDerivedAttributes();
		}
	}

	private void refreshDerivedAttributes() {
		double previousMaxHealth = getMaxHealth();
		double newMaxHealth = BASE_MAX_HEALTH + defenseProfile.getExtraHealth();
		setBaseAttributeValue(Attributes.MAX_HEALTH, newMaxHealth);
		setBaseAttributeValue(Attributes.ARMOR, defenseProfile.getArmorBonus());
		setBaseAttributeValue(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE + getWeaponAttackBonus());

		if (getHealth() > newMaxHealth) {
			setHealth((float) newMaxHealth);
		} else if (newMaxHealth > previousMaxHealth) {
			setHealth(Math.min((float) newMaxHealth, getHealth() + (float) (newMaxHealth - previousMaxHealth)));
		}
	}

	private void setBaseAttributeValue(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);

		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	private double getWeaponAttackBonus() {
		ItemStack weaponStack = getMainHandItem();

		if (weaponStack.is(Items.WOODEN_SWORD) || weaponStack.is(Items.GOLDEN_SWORD)) {
			return 1.0D;
		}

		if (weaponStack.is(Items.STONE_SWORD)) {
			return 2.0D;
		}

		if (weaponStack.is(Items.IRON_SWORD)) {
			return 3.0D;
		}

		if (weaponStack.is(Items.DIAMOND_SWORD)) {
			return 4.0D;
		}

		if (weaponStack.is(Items.NETHERITE_SWORD)) {
			return 5.0D;
		}

		return 0.0D;
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
