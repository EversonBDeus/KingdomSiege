package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

public enum WeaponClass {
	SWORD("sword"),
	BOW("bow"),
	CROSSBOW("crossbow"),
	AXE("axe"),
	STAFF("staff"),
	NONE("none");

	public static final Codec<WeaponClass> CODEC = Codec.STRING.xmap(WeaponClass::fromName, WeaponClass::getSerializedName);

	private final String serializedName;

	WeaponClass(String serializedName) {
		this.serializedName = serializedName;
	}

	public String getSerializedName() {
		return serializedName;
	}

	public String getTranslationKey() {
		return "weapon_class." + serializedName;
	}

	public static WeaponClass fromName(String name) {
		if (name == null || name.isBlank()) {
			return NONE;
		}

		String normalized = name.toLowerCase(Locale.ROOT);

		for (WeaponClass value : values()) {
			if (value.serializedName.equals(normalized)) {
				return value;
			}
		}

		return NONE;
	}

	public static WeaponClass fromSoldierClass(SoldierClass soldierClass) {
		if (soldierClass == SoldierClass.ARCHER) {
			return BOW;
		}

		return SWORD;
	}

	public static WeaponClass fromItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return NONE;
		}

		if (stack.is(Items.BOW)) {
			return BOW;
		}

		if (stack.is(Items.CROSSBOW)) {
			return CROSSBOW;
		}

		if (stack.is(Items.WOODEN_SWORD)
				|| stack.is(Items.STONE_SWORD)
				|| stack.is(Items.IRON_SWORD)
				|| stack.is(Items.GOLDEN_SWORD)
				|| stack.is(Items.DIAMOND_SWORD)
				|| stack.is(Items.NETHERITE_SWORD)) {
			return SWORD;
		}

		if (stack.is(Items.WOODEN_AXE)
				|| stack.is(Items.STONE_AXE)
				|| stack.is(Items.IRON_AXE)
				|| stack.is(Items.GOLDEN_AXE)
				|| stack.is(Items.DIAMOND_AXE)
				|| stack.is(Items.NETHERITE_AXE)) {
			return AXE;
		}

		return NONE;
	}
}
