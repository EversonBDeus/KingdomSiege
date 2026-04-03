package com.eversonbdeus.kingdomsiege.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

public class CastleSoldierModel extends EntityModel<CastleSoldierRenderState> {
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public CastleSoldierModel(ModelPart root) {
		super(root);
		head = root.getChild(PartNames.HEAD);
		leftArm = root.getChild(PartNames.LEFT_ARM);
		rightArm = root.getChild(PartNames.RIGHT_ARM);
		leftLeg = root.getChild(PartNames.LEFT_LEG);
		rightLeg = root.getChild(PartNames.RIGHT_LEG);
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition modelData = new MeshDefinition();
		PartDefinition root = modelData.getRoot();

		root.addOrReplaceChild(
				PartNames.HEAD,
				CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
				PartPose.offset(0, 6, 0)
		);

		root.addOrReplaceChild(
				PartNames.BODY,
				CubeListBuilder.create().texOffs(16, 16).addBox(-4, -6, -2, 8, 12, 4),
				PartPose.offset(0, 12, 0)
		);

		root.addOrReplaceChild(
				PartNames.LEFT_ARM,
				CubeListBuilder.create().texOffs(40, 16).addBox(-1, -2, -2, 4, 12, 4),
				PartPose.offset(5, 8, 0)
		);

		root.addOrReplaceChild(
				PartNames.RIGHT_ARM,
				CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-3, -2, -2, 4, 12, 4),
				PartPose.offset(-5, 8, 0)
		);

		root.addOrReplaceChild(
				PartNames.LEFT_LEG,
				CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
				PartPose.offset(1.9F, 18, 0)
		);

		root.addOrReplaceChild(
				PartNames.RIGHT_LEG,
				CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4),
				PartPose.offset(-1.9F, 18, 0)
		);

		return LayerDefinition.create(modelData, 64, 64);
	}

	@Override
	public void setupAnim(CastleSoldierRenderState state) {
		super.setupAnim(state);

		head.xRot = state.xRot * ((float) Math.PI / 180F);
		head.yRot = state.yRot * ((float) Math.PI / 180F);

		float swingAmount = state.walkAnimationSpeed;
		float swing = state.walkAnimationPos * 0.6662F;

		leftLeg.xRot = Mth.cos(swing) * 1.2F * swingAmount;
		rightLeg.xRot = Mth.cos(swing + Mth.PI) * 1.2F * swingAmount;
		leftArm.xRot = Mth.cos(swing + Mth.PI) * 0.8F * swingAmount;
		rightArm.xRot = Mth.cos(swing) * 0.8F * swingAmount;
	}
}
