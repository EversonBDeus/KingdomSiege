package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CastleSoldierEntity extends PathfinderMob {
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
				.add(Attributes.MOVEMENT_SPEED, 0.25D)
				.add(Attributes.ATTACK_DAMAGE, 3.0D)
				.add(Attributes.FOLLOW_RANGE, 16.0D);
	}

	public SoldierClass getSoldierClass() {
		return soldierClass;
	}

	public void setSoldierClass(SoldierClass soldierClass) {
		this.soldierClass = soldierClass != null ? soldierClass : SoldierClass.SWORDSMAN;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0D));
		goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(3, new RandomLookAroundGoal(this));
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
}