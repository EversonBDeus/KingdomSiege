package com.eversonbdeus.kingdomsiege;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KingdomSiege implements ModInitializer {
	public static final String MOD_ID = "kingdomsiege";
	public static final String MOD_NAME = "Kingdom Siege";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Inicializando {}.", MOD_NAME);
	}
}