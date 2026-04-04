package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record SoldierBlueprintData(
		SoldierClass soldierClass,
		ArmorTier armorTier,
		WeaponClass weaponClass,
		CatalystType catalystType,
		ItemStack weaponStack,
		ItemStack chestplateStack
) {
	public static final Codec<SoldierBlueprintData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SoldierClass.CODEC.fieldOf("soldier_class").forGetter(SoldierBlueprintData::soldierClass),
			ArmorTier.CODEC.fieldOf("armor_tier").forGetter(SoldierBlueprintData::armorTier),
			WeaponClass.CODEC.fieldOf("weapon_class").forGetter(SoldierBlueprintData::weaponClass),
			CatalystType.CODEC.optionalFieldOf("catalyst_type", CatalystType.NONE).forGetter(SoldierBlueprintData::catalystType),
			ItemStack.CODEC.optionalFieldOf("weapon_stack", ItemStack.EMPTY).forGetter(SoldierBlueprintData::weaponStack),
			ItemStack.CODEC.optionalFieldOf("chestplate_stack", ItemStack.EMPTY).forGetter(SoldierBlueprintData::chestplateStack)
	).apply(instance, SoldierBlueprintData::new));

	public SoldierBlueprintData {
		soldierClass = soldierClass != null ? soldierClass : SoldierClass.SWORDSMAN;
		armorTier = armorTier != null ? armorTier : ArmorTier.LEATHER;
		weaponClass = weaponClass != null && weaponClass != WeaponClass.NONE
				? weaponClass
				: WeaponClass.fromSoldierClass(soldierClass);
		catalystType = catalystType != null ? catalystType : CatalystType.NONE;

		// Importante:
		// Não gerar ItemStack padrão aqui durante decode de codec/datapack.
		// Quando a recipe vier sem weapon_stack/chestplate_stack, mantemos EMPTY
		// para evitar crash no carregamento de mundo.
		weaponStack = sanitizeStoredStack(weaponStack);
		chestplateStack = sanitizeStoredStack(chestplateStack);
	}

	public static SoldierBlueprintData defaultRecruit() {
		return of(SoldierClass.SWORDSMAN, ArmorTier.LEATHER);
	}

	public static SoldierBlueprintData of(SoldierClass soldierClass, ArmorTier armorTier) {
		return new SoldierBlueprintData(
				 soldierClass,
				 armorTier,
				 WeaponClass.fromSoldierClass(soldierClass),
				 CatalystType.NONE,
				 defaultWeaponStack(soldierClass),
				 defaultChestplateStack(armorTier)
		);
	}

	public static SoldierBlueprintData swordsmanFromCraft(ItemStack swordStack, ItemStack chestplateStack) {
		ArmorTier armorTier = ArmorTier.fromChestplate(chestplateStack);

		return new SoldierBlueprintData(
				SoldierClass.SWORDSMAN,
				armorTier,
				WeaponClass.SWORD,
				CatalystType.GOLDEN_APPLE,
				sanitizeCraftSwordStack(swordStack),
				sanitizeCraftChestplateStack(chestplateStack)
		);
	}

	public static SoldierBlueprintData archerFromCraft(ItemStack bowStack, ItemStack chestplateStack) {
		ArmorTier armorTier = ArmorTier.fromChestplate(chestplateStack);

		return new SoldierBlueprintData(
				SoldierClass.ARCHER,
				armorTier,
				WeaponClass.BOW,
				CatalystType.GOLDEN_APPLE,
				sanitizeCraftBowStack(bowStack),
				sanitizeCraftChestplateStack(chestplateStack)
		);
	}

	private static ItemStack sanitizeStoredStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		return sanitized;
	}

	private static ItemStack sanitizeCraftSwordStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return defaultWeaponStack(SoldierClass.SWORDSMAN);
		}

		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		return sanitized;
	}

	private static ItemStack sanitizeCraftBowStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return defaultWeaponStack(SoldierClass.ARCHER);
		}

		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		return sanitized;
	}

	private static ItemStack sanitizeCraftChestplateStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return defaultChestplateStack(ArmorTier.LEATHER);
		}

		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		return sanitized;
	}

	private static ItemStack defaultWeaponStack(SoldierClass soldierClass) {
		return switch (soldierClass) {
			case ARCHER -> new ItemStack(Items.BOW);
			case SWORDSMAN -> new ItemStack(Items.IRON_SWORD);
		};
	}

	private static ItemStack defaultChestplateStack(ArmorTier armorTier) {
		return switch (armorTier) {
			case CHAIN -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
			case IRON -> new ItemStack(Items.IRON_CHESTPLATE);
			case GOLD -> new ItemStack(Items.GOLDEN_CHESTPLATE);
			case DIAMOND -> new ItemStack(Items.DIAMOND_CHESTPLATE);
			case NETHERITE -> new ItemStack(Items.NETHERITE_CHESTPLATE);
			case LEATHER -> new ItemStack(Items.LEATHER_CHESTPLATE);
		};
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
