package com.eversonbdeus.kingdomsiege.client;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.client.model.ModEntityModelLayers;
import com.eversonbdeus.kingdomsiege.client.renderer.CastleSoldierRenderer;
import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class KingdomSiegeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KingdomSiege.LOGGER.info("Inicializando cliente de {}.", KingdomSiege.MOD_NAME);
		ModEntityModelLayers.registerModelLayers();
		EntityRenderers.register(ModEntities.CASTLE_SOLDIER, CastleSoldierRenderer::new);
	}
}
