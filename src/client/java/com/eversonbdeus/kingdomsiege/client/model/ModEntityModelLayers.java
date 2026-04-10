package com.eversonbdeus.kingdomsiege.client.model;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class ModEntityModelLayers {
	public static final ModelLayerLocation CASTLE_SOLDIER = createMain("castle_soldier");
	public static final ModelLayerLocation CASTLE_SOLDIER_RANK_UP_SWIRL = createMain("castle_soldier_rank_up_swirl");

	private ModEntityModelLayers() {
	}

	private static ModelLayerLocation createMain(String name) {
		return new ModelLayerLocation(Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, name), "main");
	}

	public static void registerModelLayers() {
		ModelLayerRegistry.registerModelLayer(CASTLE_SOLDIER, CastleSoldierModel::getTexturedModelData);
		ModelLayerRegistry.registerModelLayer(
				CASTLE_SOLDIER_RANK_UP_SWIRL,
				CastleSoldierModel::getRankUpSwirlTexturedModelData
		);
	}
}