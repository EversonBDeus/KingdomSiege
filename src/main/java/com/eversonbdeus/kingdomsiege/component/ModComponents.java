package com.eversonbdeus.kingdomsiege.component;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.entity.SoldierClass;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModComponents {
    public static final DataComponentType<SoldierClass> SOLDIER_CLASS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "soldier_class"),
            DataComponentType.<SoldierClass>builder()
                    .persistent(SoldierClass.CODEC)
                    .build()
    );

    private ModComponents() {
    }

    public static void register() {
        KingdomSiege.LOGGER.info("Registrando componentes de {}.", KingdomSiege.MOD_NAME);
    }
}