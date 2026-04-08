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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

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
	public void extractRenderState(CastleSoldierEntity entity, CastleSoldierRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);

		SoldierClass visualClass = entity.getVisualSoldierClass();
		state.soldierClass = visualClass;

		if (visualClass == SoldierClass.SWORDSMAN) {
			state.attackTime = Math.max(state.attackTime, entity.getVisualMeleeSwingProgress(partialTick));
			return;
		}

		if (visualClass == SoldierClass.ARCHER) {
			state.mainArm = entity.getMainArm();
			state.attackArm = entity.getMainArm();

			state.archerCombatReadyActive =
					entity.getTarget() != null
							|| entity.isAggressive()
							|| entity.isVisualArcherBowPoseActive();

			state.archerBowPoseActive =
					entity.isUsingItem()
							&& entity.getUsedItemHand() == InteractionHand.MAIN_HAND
							&& entity.getUseItem().is(Items.BOW);

			state.isUsingItem = entity.isUsingItem();
			state.useItemHand = entity.isUsingItem() ? entity.getUsedItemHand() : InteractionHand.MAIN_HAND;
			state.ticksUsingItem = entity.isUsingItem() ? entity.getTicksUsingItem() : 0.0F;

			// Não deixar a pose vanilla disputar com a pose manual do model.
			state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
			state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
		}
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(CastleSoldierEntity mob, HumanoidArm arm) {
		if (mob.getVisualSoldierClass() == SoldierClass.ARCHER) {
			return HumanoidModel.ArmPose.EMPTY;
		}

		return super.getArmPose(mob, arm);
	}

	@Override
	public Identifier getTextureLocation(CastleSoldierRenderState state) {
		return state.soldierClass == SoldierClass.ARCHER ? TEXTURE_ARCHER : TEXTURE_SWORDSMAN;
	}
}