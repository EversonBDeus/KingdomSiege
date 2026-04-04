package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum CatalystType {
	NONE("none"),
	SHIELD("shield"),
	BANNER("banner"),
	GOLDEN_APPLE("golden_apple"),
	EMERALD("emerald"),
	REDSTONE("redstone"),
	QUARTZ("quartz"),
	OBSIDIAN("obsidian"),
	SADDLE("saddle"),
	BOOK("book");

	public static final Codec<CatalystType> CODEC = Codec.STRING.xmap(CatalystType::fromName, CatalystType::getSerializedName);

	private final String serializedName;

	CatalystType(String serializedName) {
		this.serializedName = serializedName;
	}

	public String getSerializedName() {
		return serializedName;
	}

	public String getTranslationKey() {
		return "catalyst_type." + serializedName;
	}

	public static CatalystType fromName(String name) {
		if (name == null || name.isBlank()) {
			return NONE;
		}

		String normalized = name.toLowerCase(Locale.ROOT);

		for (CatalystType value : values()) {
			if (value.serializedName.equals(normalized)) {
				return value;
			}
		}

		return NONE;
	}
}
