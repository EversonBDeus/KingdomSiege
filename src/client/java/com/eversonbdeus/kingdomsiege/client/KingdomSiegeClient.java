package com.eversonbdeus.kingdomsiege.client;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import net.fabricmc.api.ClientModInitializer;

public class KingdomSiegeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KingdomSiege.LOGGER.info("Inicializando cliente de {}.", KingdomSiege.MOD_NAME);
	}
}