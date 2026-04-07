package com.eversonbdeus.kingdomsiege.client.renderer;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierModel;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierRenderState;
import com.eversonbdeus.kingdomsiege.client.model.ModEntityModelLayers;
import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class CastleSoldierRenderer extends HumanoidMobRenderer<CastleSoldierEntity, CastleSoldierRenderState, CastleSoldierModel> {

	private static final Identifier TEXTURE_SWORDSMAN =
			Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "textures/entity/castle_soldier.png");

	private static final Identifier TEXTURE_ARCHER =
			Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "textures/entity/castle_soldier_archer.png");

	public CastleSoldierRenderer(EntityRendererProvider.Context context) {
		super(context, new CastleSoldierModel(context.bakeLayer(ModEntityModelLayers.CASTLE_SOLDIER)), 0.5F);
	}

	@Override
	public CastleSoldierRenderState createRenderState() {
		return new CastleSoldierRenderState();
	}

	@Override
	public void extractRenderState(CastleSoldierEntity entity, CastleSoldierRenderState renderState, float partialTick) {
		super.extractRenderState(entity, renderState, partialTick);

		renderState.soldierClass = entity.getSoldierClass();
		applyWeaponPose(entity, renderState);
	}

	private void applyWeaponPose(CastleSoldierEntity entity, CastleSoldierRenderState renderState) {
		renderState.rightArmPose = HumanoidModel.ArmPose.EMPTY;
		renderState.leftArmPose = HumanoidModel.ArmPose.EMPTY;

		if (entity.getMainHandItem().isEmpty()) {
			return;
		}

		if (entity.getSoldierClass() == SoldierClass.ARCHER) {
			renderState.rightArmPose = entity.isUsingItem()
					? HumanoidModel.ArmPose.BOW_AND_ARROW
					: HumanoidModel.ArmPose.ITEM;
			return;
		}

		renderState.rightArmPose = HumanoidModel.ArmPose.ITEM;
	}

	@Override
	public Identifier getTextureLocation(CastleSoldierRenderState state) {
		if (state.soldierClass == SoldierClass.ARCHER) {
			return TEXTURE_ARCHER;
		}

		return TEXTURE_SWORDSMAN;
	}
}