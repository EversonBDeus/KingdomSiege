package com.eversonbdeus.kingdomsiege.entity;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum SoldierClass {
    SWORDSMAN("swordsman"),
    ARCHER("archer");

    public static final Codec<SoldierClass> CODEC = Codec.STRING.xmap(SoldierClass::fromName, SoldierClass::getSerializedName);

    private final String serializedName;

    SoldierClass(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public String getTranslationKey() {
        return "soldier_class." + serializedName;
    }

    public static SoldierClass fromName(String name) {
        if (name == null || name.isBlank()) {
            return SWORDSMAN;
        }

        String normalized = name.toLowerCase(Locale.ROOT);

        for (SoldierClass soldierClass : values()) {
            if (soldierClass.serializedName.equals(normalized)) {
                return soldierClass;
            }
        }

        return SWORDSMAN;
    }
}