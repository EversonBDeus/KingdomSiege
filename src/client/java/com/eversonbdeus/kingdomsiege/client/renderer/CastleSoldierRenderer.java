package com.eversonbdeus.kingdomsiege.client.renderer;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierModel;
import com.eversonbdeus.kingdomsiege.client.model.CastleSoldierRenderState;
import com.eversonbdeus.kingdomsiege.client.model.ModEntityModelLayers;
import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class CastleSoldierRenderer extends MobRenderer<CastleSoldierEntity, CastleSoldierRenderState, CastleSoldierModel> {

	// Texturas por classe — devem existir em:
	// src/main/resources/assets/kingdomsiege/textures/entity/
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

	// Copia dados relevantes da entidade para o RenderState (thread seguro).
	// Este método é chamado pelo pipeline de renderização antes de getTextureLocation.
	@Override
	public void extractRenderState(CastleSoldierEntity entity, CastleSoldierRenderState renderState, float partialTick) {
		super.extractRenderState(entity, renderState, partialTick);
		// Lê a classe diretamente do método público da entidade — sem tocar na IA.
		renderState.soldierClass = entity.getSoldierClass();
	}

	@Override
	public Identifier getTextureLocation(CastleSoldierRenderState state) {
		// Cada classe recebe sua textura. Arqueiro tem visual diferente do espadachim.
		if (state.soldierClass == SoldierClass.ARCHER) {
			return TEXTURE_ARCHER;
		}
		// Fallback: espadachim e qualquer classe futura sem textura própria.
		return TEXTURE_SWORDSMAN;
	}
}