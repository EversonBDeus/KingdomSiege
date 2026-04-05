package com.eversonbdeus.kingdomsiege.soldier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record SoldierBlueprintData(
		SoldierClass soldierClass,
		ArmorTier armorTier,
		WeaponClass weaponClass,
		CatalystType catalystType,
		ItemStack weaponStack,
		ItemStack chestplateStack,
		int inheritedProtectionLevel,
		int inheritedProjectileProtectionLevel,
		int inheritedBlastProtectionLevel,
		int inheritedFireProtectionLevel,
		int inheritedThornsLevel,
		int inheritedSharpnessLevel,
		int inheritedFireAspectLevel,
		int inheritedKnockbackLevel,
		int inheritedPowerLevel,
		int inheritedPunchLevel,
		int inheritedFlameLevel
) {
	private static final double DEFAULT_ARCHER_PROJECTILE_DAMAGE = 2.5D;
	private static final double DEFAULT_ARCHER_MELEE_DAMAGE = 1.0D;

	public static final Codec<SoldierBlueprintData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SoldierClass.CODEC.fieldOf("soldier_class").forGetter(SoldierBlueprintData::soldierClass),
			ArmorTier.CODEC.fieldOf("armor_tier").forGetter(SoldierBlueprintData::armorTier),
			WeaponClass.CODEC.fieldOf("weapon_class").forGetter(SoldierBlueprintData::weaponClass),
			CatalystType.CODEC.optionalFieldOf("catalyst_type", CatalystType.NONE).forGetter(SoldierBlueprintData::catalystType),
			ItemStack.CODEC.optionalFieldOf("weapon_stack", ItemStack.EMPTY).forGetter(SoldierBlueprintData::weaponStack),
			ItemStack.CODEC.optionalFieldOf("chestplate_stack", ItemStack.EMPTY).forGetter(SoldierBlueprintData::chestplateStack),
			InheritedEnchantments.CODEC.optionalFieldOf("inherited_enchantments", InheritedEnchantments.EMPTY)
					.forGetter(InheritedEnchantments::fromBlueprint)
	).apply(instance, (soldierClass, armorTier, weaponClass, catalystType, weaponStack, chestplateStack, inheritedEnchantments) ->
			new SoldierBlueprintData(
					soldierClass,
					armorTier,
					weaponClass,
					catalystType,
					weaponStack,
					chestplateStack,
					inheritedEnchantments.protectionLevel(),
					inheritedEnchantments.projectileProtectionLevel(),
					inheritedEnchantments.blastProtectionLevel(),
					inheritedEnchantments.fireProtectionLevel(),
					inheritedEnchantments.thornsLevel(),
					inheritedEnchantments.sharpnessLevel(),
					inheritedEnchantments.fireAspectLevel(),
					inheritedEnchantments.knockbackLevel(),
					inheritedEnchantments.powerLevel(),
					inheritedEnchantments.punchLevel(),
					inheritedEnchantments.flameLevel()
			)
	));

	public SoldierBlueprintData {
		soldierClass = soldierClass != null ? soldierClass : SoldierClass.SWORDSMAN;
		armorTier = armorTier != null ? armorTier : ArmorTier.LEATHER;
		weaponClass = weaponClass != null && weaponClass != WeaponClass.NONE
				? weaponClass
				: WeaponClass.fromSoldierClass(soldierClass);
		catalystType = catalystType != null ? catalystType : CatalystType.NONE;

		weaponStack = sanitizeStoredStack(weaponStack);
		chestplateStack = sanitizeStoredStack(chestplateStack);
		inheritedProtectionLevel = sanitizeEnchantmentLevel(inheritedProtectionLevel);
		inheritedProjectileProtectionLevel = sanitizeEnchantmentLevel(inheritedProjectileProtectionLevel);
		inheritedBlastProtectionLevel = sanitizeEnchantmentLevel(inheritedBlastProtectionLevel);
		inheritedFireProtectionLevel = sanitizeEnchantmentLevel(inheritedFireProtectionLevel);
		inheritedThornsLevel = sanitizeEnchantmentLevel(inheritedThornsLevel);
		inheritedSharpnessLevel = sanitizeEnchantmentLevel(inheritedSharpnessLevel);
		inheritedFireAspectLevel = sanitizeEnchantmentLevel(inheritedFireAspectLevel);
		inheritedKnockbackLevel = sanitizeEnchantmentLevel(inheritedKnockbackLevel);
		inheritedPowerLevel = sanitizeEnchantmentLevel(inheritedPowerLevel);
		inheritedPunchLevel = sanitizeEnchantmentLevel(inheritedPunchLevel);
		inheritedFlameLevel = sanitizeEnchantmentLevel(inheritedFlameLevel);
	}

	public int inheritedProtectionLevel() {
		return getEffectiveChestplateEnchantmentLevel(inheritedProtectionLevel, Enchantments.PROTECTION);
	}

	public int inheritedProjectileProtectionLevel() {
		return getEffectiveChestplateEnchantmentLevel(inheritedProjectileProtectionLevel, Enchantments.PROJECTILE_PROTECTION);
	}

	public int inheritedBlastProtectionLevel() {
		return getEffectiveChestplateEnchantmentLevel(inheritedBlastProtectionLevel, Enchantments.BLAST_PROTECTION);
	}

	public int inheritedFireProtectionLevel() {
		return getEffectiveChestplateEnchantmentLevel(inheritedFireProtectionLevel, Enchantments.FIRE_PROTECTION);
	}

	public int inheritedThornsLevel() {
		return getEffectiveChestplateEnchantmentLevel(inheritedThornsLevel, Enchantments.THORNS);
	}

	public int inheritedSharpnessLevel() {
		return getEffectiveWeaponEnchantmentLevel(inheritedSharpnessLevel, Enchantments.SHARPNESS);
	}

	public int inheritedFireAspectLevel() {
		return getEffectiveWeaponEnchantmentLevel(inheritedFireAspectLevel, Enchantments.FIRE_ASPECT);
	}

	public int inheritedKnockbackLevel() {
		return getEffectiveWeaponEnchantmentLevel(inheritedKnockbackLevel, Enchantments.KNOCKBACK);
	}

	public int inheritedPowerLevel() {
		return getEffectiveWeaponEnchantmentLevel(inheritedPowerLevel, Enchantments.POWER);
	}

	public int inheritedPunchLevel() {
		return getEffectiveWeaponEnchantmentLevel(inheritedPunchLevel, Enchantments.PUNCH);
	}

	public int inheritedFlameLevel() {
		return getEffectiveWeaponEnchantmentLevel(inheritedFlameLevel, Enchantments.FLAME);
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
				defaultChestplateStack(armorTier),
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		);
	}

	// ─── ETAPA 8 — factory methods com CatalystType ───────────────────────────

	/**
	 * Cria blueprint de espadachim sem catalisador.
	 * Delega para o overload de 3 parâmetros com CatalystType.NONE.
	 */
	public static SoldierBlueprintData swordsmanFromCraft(ItemStack swordStack, ItemStack chestplateStack) {
		return swordsmanFromCraft(swordStack, chestplateStack, CatalystType.NONE);
	}

	/**
	 * Cria blueprint de espadachim com catalisador já resolvido.
	 * Chamado por SoldierBlueprintFactory.createSwordsmanBlueprint(sword, chest, catalystStack).
	 *
	 * @param catalystType catalisador já resolvido via CatalystType.fromItem()
	 */
	public static SoldierBlueprintData swordsmanFromCraft(
			ItemStack swordStack,
			ItemStack chestplateStack,
			CatalystType catalystType
	) {
		ArmorTier armorTier = ArmorTier.fromChestplate(chestplateStack);
		InheritedEnchantments inheritedEnchantments = InheritedEnchantments.fromSwordsmanCraft(swordStack, chestplateStack);

		return new SoldierBlueprintData(
				SoldierClass.SWORDSMAN,
				armorTier,
				WeaponClass.SWORD,
				catalystType != null ? catalystType : CatalystType.NONE,
				sanitizeCraftSwordStack(swordStack),
				sanitizeCraftChestplateStack(chestplateStack),
				inheritedEnchantments.protectionLevel(),
				inheritedEnchantments.projectileProtectionLevel(),
				inheritedEnchantments.blastProtectionLevel(),
				inheritedEnchantments.fireProtectionLevel(),
				inheritedEnchantments.thornsLevel(),
				inheritedEnchantments.sharpnessLevel(),
				inheritedEnchantments.fireAspectLevel(),
				inheritedEnchantments.knockbackLevel(),
				inheritedEnchantments.powerLevel(),
				inheritedEnchantments.punchLevel(),
				inheritedEnchantments.flameLevel()
		);
	}

	/**
	 * Cria blueprint de arqueiro sem catalisador.
	 * Delega para o overload de 3 parâmetros com CatalystType.NONE.
	 */
	public static SoldierBlueprintData archerFromCraft(ItemStack bowStack, ItemStack chestplateStack) {
		return archerFromCraft(bowStack, chestplateStack, CatalystType.NONE);
	}

	/**
	 * Cria blueprint de arqueiro com catalisador já resolvido.
	 * Chamado por SoldierBlueprintFactory.createArcherBlueprint(bow, chest, catalystStack).
	 *
	 * @param catalystType catalisador já resolvido via CatalystType.fromItem()
	 */
	public static SoldierBlueprintData archerFromCraft(
			ItemStack bowStack,
			ItemStack chestplateStack,
			CatalystType catalystType
	) {
		ArmorTier armorTier = ArmorTier.fromChestplate(chestplateStack);
		InheritedEnchantments inheritedEnchantments = InheritedEnchantments.fromArcherCraft(bowStack, chestplateStack);

		return new SoldierBlueprintData(
				SoldierClass.ARCHER,
				armorTier,
				WeaponClass.BOW,
				catalystType != null ? catalystType : CatalystType.NONE,
				sanitizeCraftBowStack(bowStack),
				sanitizeCraftChestplateStack(chestplateStack),
				inheritedEnchantments.protectionLevel(),
				inheritedEnchantments.projectileProtectionLevel(),
				inheritedEnchantments.blastProtectionLevel(),
				inheritedEnchantments.fireProtectionLevel(),
				inheritedEnchantments.thornsLevel(),
				inheritedEnchantments.sharpnessLevel(),
				inheritedEnchantments.fireAspectLevel(),
				inheritedEnchantments.knockbackLevel(),
				inheritedEnchantments.powerLevel(),
				inheritedEnchantments.punchLevel(),
				inheritedEnchantments.flameLevel()
		);
	}

	// ─── Cálculos derivados ───────────────────────────────────────────────────

	public double getBaseAttackDamage() {
		if (weaponClass == WeaponClass.BOW) {
			return DEFAULT_ARCHER_MELEE_DAMAGE;
		}
		return resolveSwordAttackDamage(weaponStack);
	}

	public double getProjectileBaseDamage() {
		if (weaponClass != WeaponClass.BOW) {
			return 0.0D;
		}
		return DEFAULT_ARCHER_PROJECTILE_DAMAGE;
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

	public boolean hasInheritedChestplateEnchantments() {
		return inheritedProtectionLevel() > 0
				|| inheritedProjectileProtectionLevel() > 0
				|| inheritedBlastProtectionLevel() > 0
				|| inheritedFireProtectionLevel() > 0
				|| inheritedThornsLevel() > 0;
	}

	public boolean hasInheritedWeaponEnchantments() {
		return inheritedSharpnessLevel() > 0
				|| inheritedFireAspectLevel() > 0
				|| inheritedKnockbackLevel() > 0
				|| inheritedPowerLevel() > 0
				|| inheritedPunchLevel() > 0
				|| inheritedFlameLevel() > 0;
	}

	// ─── Privados ─────────────────────────────────────────────────────────────

	private int getEffectiveChestplateEnchantmentLevel(int storedLevel, ResourceKey<Enchantment> enchantmentKey) {
		return getEffectiveEnchantmentLevel(storedLevel, chestplateStack, enchantmentKey);
	}

	private int getEffectiveWeaponEnchantmentLevel(int storedLevel, ResourceKey<Enchantment> enchantmentKey) {
		return getEffectiveEnchantmentLevel(storedLevel, weaponStack, enchantmentKey);
	}

	private int getEffectiveEnchantmentLevel(int storedLevel, ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
		return Math.max(
				sanitizeEnchantmentLevel(storedLevel),
				resolveEnchantmentLevel(stack, enchantmentKey)
		);
	}

	private static int resolveEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		ItemEnchantments enchantments = stack.getEnchantments();
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
			if (entry.getKey().is(enchantmentKey)) {
				return sanitizeEnchantmentLevel(entry.getIntValue());
			}
		}
		return 0;
	}

	private static int sanitizeEnchantmentLevel(int level) {
		return Math.max(0, level);
	}

	private static double resolveSwordAttackDamage(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return 6.0D;
		if (stack.is(Items.WOODEN_SWORD))    return 4.0D;
		if (stack.is(Items.STONE_SWORD))     return 5.0D;
		if (stack.is(Items.IRON_SWORD))      return 6.0D;
		if (stack.is(Items.GOLDEN_SWORD))    return 4.0D;
		if (stack.is(Items.DIAMOND_SWORD))   return 7.0D;
		if (stack.is(Items.NETHERITE_SWORD)) return 8.0D;
		return 6.0D;
	}

	private static ItemStack sanitizeStoredStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		return sanitized;
	}

	private static ItemStack sanitizeCraftSwordStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return defaultWeaponStack(SoldierClass.SWORDSMAN);
		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		return sanitized;
	}

	private static ItemStack sanitizeCraftBowStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return defaultWeaponStack(SoldierClass.ARCHER);
		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		return sanitized;
	}

	private static ItemStack sanitizeCraftChestplateStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return defaultChestplateStack(ArmorTier.LEATHER);
		ItemStack sanitized = stack.copy();
		sanitized.setCount(1);
		if (sanitized.isDamageableItem()) {
			sanitized.setDamageValue(0);
		}
		return sanitized;
	}

	private static ItemStack defaultWeaponStack(SoldierClass soldierClass) {
		return switch (soldierClass) {
			case ARCHER   -> new ItemStack(Items.BOW);
			case SWORDSMAN -> new ItemStack(Items.IRON_SWORD);
		};
	}

	private static ItemStack defaultChestplateStack(ArmorTier armorTier) {
		return switch (armorTier) {
			case CHAIN     -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
			case IRON      -> new ItemStack(Items.IRON_CHESTPLATE);
			case GOLD      -> new ItemStack(Items.GOLDEN_CHESTPLATE);
			case DIAMOND   -> new ItemStack(Items.DIAMOND_CHESTPLATE);
			case NETHERITE -> new ItemStack(Items.NETHERITE_CHESTPLATE);
			case LEATHER   -> new ItemStack(Items.LEATHER_CHESTPLATE);
		};
	}

	// ─── InheritedEnchantments (inner record) ─────────────────────────────────

	private record InheritedEnchantments(
			int protectionLevel,
			int projectileProtectionLevel,
			int blastProtectionLevel,
			int fireProtectionLevel,
			int thornsLevel,
			int sharpnessLevel,
			int fireAspectLevel,
			int knockbackLevel,
			int powerLevel,
			int punchLevel,
			int flameLevel
	) {
		private static final InheritedEnchantments EMPTY = new InheritedEnchantments(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		private static final Codec<InheritedEnchantments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("protection_level", 0).forGetter(InheritedEnchantments::protectionLevel),
				Codec.INT.optionalFieldOf("projectile_protection_level", 0).forGetter(InheritedEnchantments::projectileProtectionLevel),
				Codec.INT.optionalFieldOf("blast_protection_level", 0).forGetter(InheritedEnchantments::blastProtectionLevel),
				Codec.INT.optionalFieldOf("fire_protection_level", 0).forGetter(InheritedEnchantments::fireProtectionLevel),
				Codec.INT.optionalFieldOf("thorns_level", 0).forGetter(InheritedEnchantments::thornsLevel),
				Codec.INT.optionalFieldOf("sharpness_level", 0).forGetter(InheritedEnchantments::sharpnessLevel),
				Codec.INT.optionalFieldOf("fire_aspect_level", 0).forGetter(InheritedEnchantments::fireAspectLevel),
				Codec.INT.optionalFieldOf("knockback_level", 0).forGetter(InheritedEnchantments::knockbackLevel),
				Codec.INT.optionalFieldOf("power_level", 0).forGetter(InheritedEnchantments::powerLevel),
				Codec.INT.optionalFieldOf("punch_level", 0).forGetter(InheritedEnchantments::punchLevel),
				Codec.INT.optionalFieldOf("flame_level", 0).forGetter(InheritedEnchantments::flameLevel)
		).apply(instance, InheritedEnchantments::new));

		private static InheritedEnchantments fromBlueprint(SoldierBlueprintData blueprint) {
			return new InheritedEnchantments(
					blueprint.inheritedProtectionLevel(),
					blueprint.inheritedProjectileProtectionLevel(),
					blueprint.inheritedBlastProtectionLevel(),
					blueprint.inheritedFireProtectionLevel(),
					blueprint.inheritedThornsLevel(),
					blueprint.inheritedSharpnessLevel(),
					blueprint.inheritedFireAspectLevel(),
					blueprint.inheritedKnockbackLevel(),
					blueprint.inheritedPowerLevel(),
					blueprint.inheritedPunchLevel(),
					blueprint.inheritedFlameLevel()
			);
		}

		private static InheritedEnchantments fromSwordsmanCraft(ItemStack swordStack, ItemStack chestplateStack) {
			return new InheritedEnchantments(
					resolveEnchantmentLevel(chestplateStack, Enchantments.PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.PROJECTILE_PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.BLAST_PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.FIRE_PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.THORNS),
					resolveEnchantmentLevel(swordStack, Enchantments.SHARPNESS),
					resolveEnchantmentLevel(swordStack, Enchantments.FIRE_ASPECT),
					resolveEnchantmentLevel(swordStack, Enchantments.KNOCKBACK),
					0,
					0,
					0
			);
		}

		private static int resolveArcherFlameLevel(ItemStack bowStack) {
			return Math.max(
					resolveEnchantmentLevel(bowStack, Enchantments.FLAME),
					resolveEnchantmentLevel(bowStack, Enchantments.FIRE_ASPECT)
			);
		}

		private static InheritedEnchantments fromArcherCraft(ItemStack bowStack, ItemStack chestplateStack) {
			return new InheritedEnchantments(
					resolveEnchantmentLevel(chestplateStack, Enchantments.PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.PROJECTILE_PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.BLAST_PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.FIRE_PROTECTION),
					resolveEnchantmentLevel(chestplateStack, Enchantments.THORNS),
					0,
					0,
					0,
					resolveEnchantmentLevel(bowStack, Enchantments.POWER),
					resolveEnchantmentLevel(bowStack, Enchantments.PUNCH),
					resolveArcherFlameLevel(bowStack)
			);
		}
	}
}