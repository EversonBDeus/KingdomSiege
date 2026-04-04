package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

public enum ArmorTier {
	LEATHER("leather", 4.0D, 2.0D, 0.0D, 0.0D),
	CHAIN("chain", 6.0D, 3.0D, 0.0D, 0.0D),
	IRON("iron", 10.0D, 5.0D, 0.0D, 0.0D),
	GOLD("gold", 8.0D, 4.0D, 0.0D, 0.0D),
	DIAMOND("diamond", 16.0D, 7.0D, 2.0D, 0.0D),
	NETHERITE("netherite", 20.0D, 8.0D, 3.0D, 0.15D);

	public static final Codec<ArmorTier> CODEC = Codec.STRING.xmap(ArmorTier::fromName, ArmorTier::getSerializedName);

	private final String serializedName;
	private final double bonusHealth;
	private final double armorBonus;
	private final double toughnessBonus;
	private final double knockbackResistanceBonus;

	ArmorTier(String serializedName, double bonusHealth, double armorBonus, double toughnessBonus, double knockbackResistanceBonus) {
		this.serializedName = serializedName;
		this.bonusHealth = bonusHealth;
		this.armorBonus = armorBonus;
		this.toughnessBonus = toughnessBonus;
		this.knockbackResistanceBonus = knockbackResistanceBonus;
	}

	public String getSerializedName() {
		return serializedName;
	}

	public double getBonusHealth() {
		return bonusHealth;
	}

	public double getArmorBonus() {
		return armorBonus;
	}

	public double getToughnessBonus() {
		return toughnessBonus;
	}

	public double getKnockbackResistanceBonus() {
		return knockbackResistanceBonus;
	}

	public String getTranslationKey() {
		return "armor_tier." + serializedName;
	}

	public static ArmorTier fromName(String name) {
		if (name == null || name.isBlank()) {
			return LEATHER;
		}

		String normalized = name.toLowerCase(Locale.ROOT);

		for (ArmorTier value : values()) {
			if (value.serializedName.equals(normalized)) {
				return value;
			}
		}

		return LEATHER;
	}

	public static ArmorTier fromChestplate(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return LEATHER;
		}

		if (stack.is(Items.LEATHER_CHESTPLATE)) {
			return LEATHER;
		}

		if (stack.is(Items.CHAINMAIL_CHESTPLATE)) {
			return CHAIN;
		}

		if (stack.is(Items.IRON_CHESTPLATE)) {
			return IRON;
		}

		if (stack.is(Items.GOLDEN_CHESTPLATE)) {
			return GOLD;
		}

		if (stack.is(Items.DIAMOND_CHESTPLATE)) {
			return DIAMOND;
		}

		if (stack.is(Items.NETHERITE_CHESTPLATE)) {
			return NETHERITE;
		}

		return LEATHER;
	}
}
