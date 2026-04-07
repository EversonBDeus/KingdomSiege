package com.eversonbdeus.kingdomsiege.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public class CastleSoldierModel extends HumanoidModel<CastleSoldierRenderState> {

	public CastleSoldierModel(ModelPart root) {
		super(root);

		// Mantém a estrutura exigida pelo HumanoidModel,
		// mas sem exibir a camada visual de hat.
		this.hat.visible = false;
	}

	public static LayerDefinition getTexturedModelData() {
		// Usa a malha humanoide padrão do vanilla.
		// Isso garante que todas as parts obrigatórias existam,
		// incluindo "hat".
		MeshDefinition meshDefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		return LayerDefinition.create(meshDefinition, 64, 64);
	}
}