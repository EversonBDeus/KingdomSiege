package com.eversonbdeus.kingdomsiege.client.renderer.layer;

import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierModel;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierRenderState;
import com.eversonbdeus.kingdomsiege.client.model.ModEntityModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.Identifier;

/**
 * Camada visual de rank-up para espadachins e arqueiros.
 *
 * Usa uma model layer levemente inflada para a aura. Isso evita que o
 * efeito fique colado na malha base do soldado e desapareça visualmente.
 */
public class SoldierRankUpLayer extends EnergySwirlLayer<CastleSoldierRenderState, CastleSoldierModel> {

    /**
     * Amplificador reservado exclusivamente para o rank-up.
     */
    public static final int RANK_UP_EFFECT_AMPLIFIER = 39;

    /**
     * Textura vanilla do Creeper charged.
     */
    private static final Identifier ENERGY_SWIRL_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper_armor.png");

    /**
     * Modelo separado para a aura visual.
     */
    private final CastleSoldierModel swirlModel;

    public SoldierRankUpLayer(
            RenderLayerParent<CastleSoldierRenderState, CastleSoldierModel> parent,
            EntityRendererProvider.Context context
    ) {
        super(parent);
        this.swirlModel = new CastleSoldierModel(
                context.bakeLayer(ModEntityModelLayers.CASTLE_SOLDIER_RANK_UP_SWIRL)
        );
    }

    @Override
    protected boolean isPowered(CastleSoldierRenderState renderState) {
        return renderState.rankUpAnimationActive;
    }

    @Override
    protected float xOffset(float tickCount) {
        return tickCount * 0.01F;
    }

    @Override
    protected Identifier getTextureLocation() {
        return ENERGY_SWIRL_TEXTURE;
    }

    @Override
    protected CastleSoldierModel model() {
        return this.swirlModel;
    }
}