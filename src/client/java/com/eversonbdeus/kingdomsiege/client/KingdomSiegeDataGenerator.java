package com.eversonbdeus.kingdomsiege.client;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class KingdomSiegeDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		fabricDataGenerator.createPack();
		KingdomSiege.LOGGER.info("Configurando gerador de dados de {}.", KingdomSiege.MOD_NAME);
	}
}