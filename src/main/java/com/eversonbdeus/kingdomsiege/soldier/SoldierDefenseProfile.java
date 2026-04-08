package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

public enum SoldierDefenseProfile {
	UNARMORED("unarmored", 0.0D, 0.0D),
	LIGHT("light", 4.0D, 2.0D),
	MEDIUM("medium", 8.0D, 4.0D),
	HEAVY("heavy", 12.0D, 6.0D);

	public static final Codec<SoldierDefenseProfile> CODEC =
			Codec.STRING.xmap(SoldierDefenseProfile::fromName, SoldierDefenseProfile::getSerializedName);

	private final String serializedName;
	private final double extraHealth;
	private final double armorBonus;

	SoldierDefenseProfile(String serializedName, double extraHealth, double armorBonus) {
		this.serializedName = serializedName;
		this.extraHealth = extraHealth;
		this.armorBonus = armorBonus;
	}

	public String getSerializedName() {
		return serializedName;
	}

	public double getExtraHealth() {
		return extraHealth;
	}

	public double getArmorBonus() {
		return armorBonus;
	}

	public String getTranslationKey() {
		return "soldier_defense_profile." + serializedName;
	}

	public static SoldierDefenseProfile fromName(String name) {
		if (name == null || name.isBlank()) {
			return UNARMORED;
		}

		String normalized = name.toLowerCase(Locale.ROOT);

		for (SoldierDefenseProfile value : values()) {
			if (value.serializedName.equals(normalized)) {
				return value;
			}
		}

		return UNARMORED;
	}

	public static SoldierDefenseProfile fromChestplate(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return UNARMORED;
		}

		if (stack.is(Items.LEATHER_CHESTPLATE) || stack.is(Items.GOLDEN_CHESTPLATE) || stack.is(Items.CHAINMAIL_CHESTPLATE)) {
			return LIGHT;
		}

		if (stack.is(Items.IRON_CHESTPLATE)) {
			return MEDIUM;
		}

		if (stack.is(Items.DIAMOND_CHESTPLATE) || stack.is(Items.NETHERITE_CHESTPLATE)) {
			return HEAVY;
		}

		return UNARMORED;
	}
}
