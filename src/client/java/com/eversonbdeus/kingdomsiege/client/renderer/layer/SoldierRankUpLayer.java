package com.eversonbdeus.kingdomsiege.client.renderer.layer;

import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierModel;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Camada visual de rank-up para espadachins e arqueiros.
 *
 * Quando o soldado sobe de rank, exibe um efeito elétrico idêntico ao do
 * Creeper carregado (energy swirl sobre o modelo), com brilho máximo por
 * toda a duração da invulnerabilidade (10 segundos / 200 ticks).
 *
 * Detectado via efeito DAMAGE_RESISTANCE com amplificador especial
 * {@link #RANK_UP_EFFECT_AMPLIFIER}, que serve de sinal servidor → cliente
 * sem necessidade de alterar CastleSoldierEntity.
 */
public class SoldierRankUpLayer extends RenderLayer<CastleSoldierRenderState, CastleSoldierModel> {

    /**
     * Amplificador reservado exclusivamente para o rank-up.
     * Nenhuma fonte vanilla atinge esse valor, portanto não há falso-positivo.
     * Amplificador 39 = nível 40 → 100 % de redução de dano (efeito limitado a 100 %).
     */
    public static final int RANK_UP_EFFECT_AMPLIFIER = 39;

    /** Textura de redemoinho energético do Creeper carregado, reutilizada aqui. */
    private static final Identifier ENERGY_SWIRL_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper_armor.png");

    public SoldierRankUpLayer(RenderLayerParent<CastleSoldierRenderState, CastleSoldierModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       CastleSoldierRenderState state,
                       float yRot,
                       float xRot) {

        if (!state.rankUpAnimationActive) {
            return;
        }

        // Offset animado a partir do tick do soldado; valor negativo inverte o sentido
        // vertical, igual ao comportamento vanilla do Creeper carregado.
        float t = state.rankUpAnimTimeTick * 0.02F;

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.energySwirl(ENERGY_SWIRL_TEXTURE, -t * 0.02F, t * 0.01F)
        );

        poseStack.pushPose();
        // Leve escala para o efeito ficar por cima da skin sem z-fighting.
        poseStack.scale(1.02F, 1.02F, 1.02F);

        this.getParentModel().renderToBuffer(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,  // sempre iluminado ao máximo (igual ao Creeper carregado)
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }
}
