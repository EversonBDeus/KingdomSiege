package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SoldierBlueprintData(
		SoldierClass soldierClass,
		ArmorTier armorTier,
		WeaponClass weaponClass,
		CatalystType catalystType
) {
	public static final Codec<SoldierBlueprintData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SoldierClass.CODEC.fieldOf("soldier_class").forGetter(SoldierBlueprintData::soldierClass),
			ArmorTier.CODEC.fieldOf("armor_tier").forGetter(SoldierBlueprintData::armorTier),
			WeaponClass.CODEC.fieldOf("weapon_class").forGetter(SoldierBlueprintData::weaponClass),
			CatalystType.CODEC.optionalFieldOf("catalyst_type", CatalystType.NONE).forGetter(SoldierBlueprintData::catalystType)
	).apply(instance, SoldierBlueprintData::new));

	public SoldierBlueprintData {
		soldierClass = soldierClass != null ? soldierClass : SoldierClass.SWORDSMAN;
		armorTier = armorTier != null ? armorTier : ArmorTier.LEATHER;
		weaponClass = weaponClass != null && weaponClass != WeaponClass.NONE
				? weaponClass
				: WeaponClass.fromSoldierClass(soldierClass);
		catalystType = catalystType != null ? catalystType : CatalystType.NONE;
	}

	public static SoldierBlueprintData defaultRecruit() {
		return of(SoldierClass.SWORDSMAN, ArmorTier.LEATHER);
	}

	public static SoldierBlueprintData of(SoldierClass soldierClass, ArmorTier armorTier) {
		return new SoldierBlueprintData(soldierClass, armorTier, WeaponClass.fromSoldierClass(soldierClass), CatalystType.NONE);
	}

	public double getBonusHealth() {
		return armorTier.getBonusHealth();
	}

	public double getArmorBonus() {
		return armorTier.getArmorBonus();
	}

	public double getToughnessBonus() {
		return armorTier.getToughnessBonus();
	}

	public double getKnockbackResistanceBonus() {
		return armorTier.getKnockbackResistanceBonus();
	}
}
