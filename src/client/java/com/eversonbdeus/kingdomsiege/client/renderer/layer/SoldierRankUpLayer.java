package com.eversonbdeus.kingdomsiege.client.renderer.layer;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
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
 * Usa uma textura própria compatível com o UV do modelo humanoide do soldado.
 * Isso evita o problema de usar a textura do creeper charged vanilla em uma
 * malha que não compartilha o mesmo mapeamento.
 */
public class SoldierRankUpLayer extends EnergySwirlLayer<CastleSoldierRenderState, CastleSoldierModel> {

    /**
     * Amplificador reservado exclusivamente para o rank-up.
     */
    public static final int RANK_UP_EFFECT_AMPLIFIER = 39;

    /**
     * Textura própria da aura de rank-up, compatível com o modelo humanoide.
     */
    private static final Identifier ENERGY_SWIRL_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    KingdomSiege.MOD_ID,
                    "textures/entity/effect/soldier_rank_up_aura.png"
            );

    /**
     * Velocidade do redemoinho visual.
     */
    private static final float ENERGY_SWIRL_SPEED = 0.02F;

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
        return tickCount * ENERGY_SWIRL_SPEED;
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