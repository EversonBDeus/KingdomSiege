package com.eversonbdeus.kingdomsiege.client.renderer;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierModel;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierRenderState;
import com.eversonbdeus.kingdomsiege.client.model.ModEntityModelLayers;
import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class CastleSoldierRenderer extends MobRenderer<CastleSoldierEntity, CastleSoldierRenderState, CastleSoldierModel> {
	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "textures/entity/castle_soldier.png");

	public CastleSoldierRenderer(EntityRendererProvider.Context context) {
		super(context, new CastleSoldierModel(context.bakeLayer(ModEntityModelLayers.CASTLE_SOLDIER)), 0.5F);
	}

	@Override
	public CastleSoldierRenderState createRenderState() {
		return new CastleSoldierRenderState();
	}

	@Override
	public Identifier getTextureLocation(CastleSoldierRenderState state) {
		return TEXTURE;
	}
}