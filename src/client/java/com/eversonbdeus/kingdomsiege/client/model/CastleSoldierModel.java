package com.eversonbdeus.kingdomsiege.client.model;

import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class CastleSoldierModel extends HumanoidModel<CastleSoldierRenderState> {

	public CastleSoldierModel(ModelPart root) {
		super(root);
		this.hat.visible = false;
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition meshDefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	@Override
	public void setupAnim(CastleSoldierRenderState state) {
		super.setupAnim(state);

		this.leftArm.visible = true;
		this.rightArm.visible = true;

		if (state.soldierClass == SoldierClass.ARCHER) {
			applyManualArcherPose(state);
			return;
		}

		if (state.soldierClass == SoldierClass.SWORDSMAN) {
			HumanoidArm mainArm = state.mainArm != null ? state.mainArm : HumanoidArm.RIGHT;
			applyPlayerLikeSwordSwing(mainArm, state.attackTime);
		}
	}

	private void applyManualArcherPose(CastleSoldierRenderState state) {
		if (!state.archerCombatReadyActive) {
			return;
		}

		float headYaw = this.head.yRot;
		float headPitch = this.head.xRot;

		// Assim que identifica hostil, os dois braços sobem.
		this.rightArm.xRot = -1.30F + headPitch * 0.25F;
		this.rightArm.yRot = headYaw - 0.35F;
		this.rightArm.zRot = 0.0F;

		this.leftArm.xRot = -1.30F + headPitch * 0.25F;
		this.leftArm.yRot = headYaw + 0.35F;
		this.leftArm.zRot = 0.0F;

		// Quando o draw realmente começa, aprofunda a pose.
		if (state.archerBowPoseActive) {
			float drawProgress = Mth.clamp(state.ticksUsingItem / 12.0F, 0.0F, 1.0F);

			// Braço direito sustenta o arco.
			this.rightArm.xRot = -1.55F + headPitch * 0.35F;
			this.rightArm.yRot = headYaw - (0.50F - drawProgress * 0.05F);
			this.rightArm.zRot = 0.0F;

			// Braço esquerdo puxa a corda.
			this.leftArm.xRot = -1.05F + headPitch - drawProgress * 0.60F;
			this.leftArm.yRot = headYaw + (0.38F - drawProgress * 0.18F);
			this.leftArm.zRot = 0.0F;
		}
	}

	private void applyPlayerLikeSwordSwing(HumanoidArm mainArm, float attackTime) {
		ModelPart attackArm = mainArm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;

		if (attackTime <= 0.0F) {
			this.body.yRot = 0.0F;
			attackArm.xRot = Math.min(attackArm.xRot, -0.20F);
			attackArm.zRot = 0.0F;
			return;
		}

		float swingRoot = Mth.sqrt(attackTime);
		float bodyYaw = Mth.sin(swingRoot * (float) (Math.PI * 2.0D)) * 0.2F;

		if (mainArm == HumanoidArm.LEFT) {
			bodyYaw *= -1.0F;
		}

		this.body.yRot = bodyYaw;
		this.rightArm.yRot += bodyYaw;
		this.leftArm.yRot += bodyYaw;
		this.leftArm.xRot += bodyYaw;

		float easedAttack = 1.0F - attackTime;
		easedAttack *= easedAttack;
		easedAttack *= easedAttack;
		easedAttack = 1.0F - easedAttack;

		float swingArc = Mth.sin(easedAttack * (float) Math.PI);
		float headAssist = Mth.sin(attackTime * (float) Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;

		attackArm.xRot -= swingArc * 1.2F + headAssist;
		attackArm.yRot += this.body.yRot * 2.0F;
		attackArm.zRot += Mth.sin(attackTime * (float) Math.PI) * -0.4F;
	}
}