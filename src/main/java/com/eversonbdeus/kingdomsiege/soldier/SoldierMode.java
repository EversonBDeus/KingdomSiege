package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;

public enum SoldierMode {
    GUARD("entity.kingdomsiege.soldier_mode.guard"),
    FOLLOW("entity.kingdomsiege.soldier_mode.follow");

    public static final Codec<SoldierMode> CODEC = Codec.STRING.xmap(SoldierMode::fromName, SoldierMode::getSerializedName);

    private final String translationKey;

    SoldierMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getSerializedName() {
        return name().toLowerCase();
    }

    public SoldierMode next() {
        return this == GUARD ? FOLLOW : GUARD;
    }

    private static SoldierMode fromName(String name) {
        for (SoldierMode value : values()) {
            if (value.getSerializedName().equalsIgnoreCase(name) || value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }

        return GUARD;
    }
}