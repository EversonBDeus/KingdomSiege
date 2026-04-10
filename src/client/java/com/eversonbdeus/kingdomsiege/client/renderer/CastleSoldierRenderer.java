package com.eversonbdeus.kingdomsiege.client.renderer;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierModel;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierRenderState;
import com.eversonbdeus.kingdomsiege.client.model.ModEntityModelLayers;
import com.eversonbdeus.kingdomsiege.client.renderer.layer.SoldierRankUpLayer;
import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public class CastleSoldierRenderer extends HumanoidMobRenderer<CastleSoldierEntity, CastleSoldierRenderState, CastleSoldierModel> {

	private static final Identifier TEXTURE_SWORDSMAN =
			Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "textures/entity/castle_soldier.png");

	private static final Identifier TEXTURE_ARCHER =
			Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "textures/entity/castle_soldier_archer.png");

	public CastleSoldierRenderer(EntityRendererProvider.Context context) {
		super(context, new CastleSoldierModel(context.bakeLayer(ModEntityModelLayers.CASTLE_SOLDIER)), 0.5F);

		// Camada de efeito visual do rank-up.
		this.addLayer(new SoldierRankUpLayer(this, context));
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

		// Copia o estado visual sincronizado do arqueiro para o render state.
		// Isso evita pose residual entre frames, mas mantém a informação real
		// que veio da entidade via SynchedEntityData.
		state.archerCombatReadyActive = entity.isVisualArcherCombatReadyActive();
		state.archerBowPoseActive = entity.isVisualArcherBowPoseActive();

		// ─── Rank-up: usa estado visual sincronizado como caminho principal ────
		state.rankUpAnimationActive = entity.isVisualRankUpAnimationActive();

		// Fallback leve:
		// mantém a leitura do efeito só como apoio de compatibilidade/debug.
		if (!state.rankUpAnimationActive) {
			MobEffectInstance resistanceEffect = entity.getEffect(MobEffects.RESISTANCE);

			if (resistanceEffect != null && resistanceEffect.getDuration() > 0) {
				boolean hasReservedAmplifier =
						resistanceEffect.getAmplifier() == SoldierRankUpLayer.RANK_UP_EFFECT_AMPLIFIER;

				boolean isLikelyRankUpWindow =
						resistanceEffect.getDuration() <= 200;

				state.rankUpAnimationActive = hasReservedAmplifier || isLikelyRankUpWindow;
			}
		}

		state.rankUpAnimTimeTick = state.rankUpAnimationActive
				? entity.tickCount + partialTick
				: 0.0F;
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