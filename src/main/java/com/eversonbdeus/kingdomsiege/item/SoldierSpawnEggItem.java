package com.eversonbdeus.kingdomsiege.item;

import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import com.eversonbdeus.kingdomsiege.registry.ModDataComponents;
import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.ArmorTier;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import com.eversonbdeus.kingdomsiege.soldier.WeaponClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.function.Consumer;

public class SoldierSpawnEggItem extends SpawnEggItem {
	public SoldierSpawnEggItem(Item.Properties properties) {
		super(properties);
	}

	public SoldierBlueprintData getSoldierBlueprint(ItemStack stack) {
		SoldierBlueprintData storedBlueprint = stack.get(ModDataComponents.SOLDIER_BLUEPRINT);

		if (storedBlueprint != null) {
			return storedBlueprint;
		}

		SoldierClass legacyClass = stack.get(ModDataComponents.SOLDIER_CLASS);
		if (legacyClass != null) {
			return SoldierBlueprintData.of(legacyClass, ArmorTier.LEATHER);
		}

		return SoldierBlueprintData.defaultRecruit();
	}

	public SoldierClass getSoldierClass(ItemStack stack) {
		return getSoldierBlueprint(stack).soldierClass();
	}

	public void setSoldierBlueprint(ItemStack stack, SoldierBlueprintData blueprint) {
		SoldierBlueprintData resolvedBlueprint = blueprint != null
				? blueprint
				: SoldierBlueprintData.defaultRecruit();

		stack.set(ModDataComponents.SOLDIER_BLUEPRINT, resolvedBlueprint);
		stack.set(ModDataComponents.SOLDIER_CLASS, resolvedBlueprint.soldierClass());
	}

	public void setSoldierClass(ItemStack stack, SoldierClass soldierClass) {
		setSoldierBlueprint(stack, SoldierBlueprintData.of(soldierClass, ArmorTier.LEATHER));
	}

	@Override
	public Component getName(ItemStack stack) {
		SoldierBlueprintData blueprint = getSoldierBlueprint(stack);

		return super.getName(stack)
				.copy()
				.append(Component.literal(" ("))
				.append(Component.translatable(blueprint.soldierClass().getTranslationKey()))
				.append(Component.literal(" / "))
				.append(Component.translatable(blueprint.armorTier().getTranslationKey()))
				.append(Component.literal(")"));
	}

	@Override
	public void appendHoverText(
			ItemStack stack,
			Item.TooltipContext tooltipContext,
			TooltipDisplay tooltipDisplay,
			Consumer<Component> tooltipAdder,
			TooltipFlag tooltipFlag
	) {
		super.appendHoverText(stack, tooltipContext, tooltipDisplay, tooltipAdder, tooltipFlag);

		SoldierBlueprintData blueprint = getSoldierBlueprint(stack);

		tooltipAdder.accept(Component.translatable(
				"tooltip.kingdomsiege.soldier_egg.class",
				Component.translatable(blueprint.soldierClass().getTranslationKey())
		).withStyle(ChatFormatting.GRAY));

		tooltipAdder.accept(Component.translatable(
				"tooltip.kingdomsiege.soldier_egg.weapon",
				resolveWeaponLabel(blueprint)
		).withStyle(ChatFormatting.GRAY));

		tooltipAdder.accept(Component.translatable(
				"tooltip.kingdomsiege.soldier_egg.chestplate",
				Component.translatable(blueprint.armorTier().getTranslationKey())
		).withStyle(ChatFormatting.GRAY));

		tooltipAdder.accept(Component.translatable(
				"tooltip.kingdomsiege.soldier_egg.catalyst",
				Component.translatable(blueprint.catalystType().getTranslationKey())
		).withStyle(ChatFormatting.GRAY));

		if (blueprint.weaponClass() == WeaponClass.BOW) {
			tooltipAdder.accept(Component.translatable(
					"tooltip.kingdomsiege.soldier_egg.projectile_damage",
					formatOneDecimal(blueprint.getProjectileBaseDamage())
			).withStyle(ChatFormatting.BLUE));
		} else {
			tooltipAdder.accept(Component.translatable(
					"tooltip.kingdomsiege.soldier_egg.attack",
					formatOneDecimal(blueprint.getBaseAttackDamage())
			).withStyle(ChatFormatting.BLUE));
		}

		tooltipAdder.accept(Component.translatable(
				"tooltip.kingdomsiege.soldier_egg.health",
				formatOneDecimal(20.0D + blueprint.getBonusHealth())
		).withStyle(ChatFormatting.GREEN));

		tooltipAdder.accept(Component.translatable(
				"tooltip.kingdomsiege.soldier_egg.defense",
				formatOneDecimal(blueprint.getArmorBonus()),
				formatOneDecimal(blueprint.getToughnessBonus())
		).withStyle(ChatFormatting.GREEN));

		appendInheritedChestplateTooltip(tooltipAdder, blueprint);
		appendInheritedWeaponTooltip(tooltipAdder, blueprint);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}

		ItemStack stack = context.getItemInHand();
		BlockPos clickedPos = context.getClickedPos();
		Direction clickedFace = context.getClickedFace();
		BlockPos spawnPos = clickedPos.relative(clickedFace);
		Player player = context.getPlayer();

		CastleSoldierEntity soldier = ModEntities.CASTLE_SOLDIER.spawn(
				serverLevel,
				stack,
				player,
				spawnPos,
				EntitySpawnReason.SPAWN_ITEM_USE,
				true,
				clickedFace == Direction.UP
		);

		if (soldier == null) {
			return InteractionResult.PASS;
		}

		soldier.initializeFromBlueprint(
				getSoldierBlueprint(stack),
				player != null ? player.getUUID() : null,
				soldier.blockPosition()
		);

		return InteractionResult.SUCCESS;
	}

	private void appendInheritedChestplateTooltip(Consumer<Component> tooltipAdder, SoldierBlueprintData blueprint) {
		if (!blueprint.hasInheritedChestplateEnchantments()) {
			return;
		}

		tooltipAdder.accept(Component.translatable("text.kingdomsiege.inheritance.chestplate_header")
				.withStyle(ChatFormatting.LIGHT_PURPLE));

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.protection",
				blueprint.inheritedProtectionLevel(),
				ChatFormatting.LIGHT_PURPLE
		);

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.projectile_protection",
				blueprint.inheritedProjectileProtectionLevel(),
				ChatFormatting.LIGHT_PURPLE
		);

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.blast_protection",
				blueprint.inheritedBlastProtectionLevel(),
				ChatFormatting.LIGHT_PURPLE
		);

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.fire_protection",
				blueprint.inheritedFireProtectionLevel(),
				ChatFormatting.LIGHT_PURPLE
		);

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.thorns",
				blueprint.inheritedThornsLevel(),
				ChatFormatting.LIGHT_PURPLE
		);
	}

	private void appendInheritedWeaponTooltip(Consumer<Component> tooltipAdder, SoldierBlueprintData blueprint) {
		if (!blueprint.hasInheritedWeaponEnchantments()) {
			return;
		}

		tooltipAdder.accept(Component.translatable("text.kingdomsiege.inheritance.weapon_header")
				.withStyle(ChatFormatting.GOLD));

		if (blueprint.weaponClass() == WeaponClass.BOW) {
			appendLevelInheritanceLine(
					tooltipAdder,
					"text.kingdomsiege.inheritance.power",
					blueprint.inheritedPowerLevel(),
					ChatFormatting.GOLD
			);

			appendLevelInheritanceLine(
					tooltipAdder,
					"text.kingdomsiege.inheritance.punch",
					blueprint.inheritedPunchLevel(),
					ChatFormatting.GOLD
			);

			appendLevelInheritanceLine(
					tooltipAdder,
					"text.kingdomsiege.inheritance.flame",
					blueprint.inheritedFlameLevel(),
					ChatFormatting.GOLD
			);

			return;
		}

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.sharpness",
				blueprint.inheritedSharpnessLevel(),
				ChatFormatting.GOLD
		);

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.fire_aspect",
				blueprint.inheritedFireAspectLevel(),
				ChatFormatting.GOLD
		);

		appendLevelInheritanceLine(
				tooltipAdder,
				"text.kingdomsiege.inheritance.knockback",
				blueprint.inheritedKnockbackLevel(),
				ChatFormatting.GOLD
		);
	}

	private void appendLevelInheritanceLine(
			Consumer<Component> tooltipAdder,
			String inheritanceKey,
			int level,
			ChatFormatting color
	) {
		if (level <= 0) {
			return;
		}

		tooltipAdder.accept(
				Component.translatable(
						"text.kingdomsiege.inheritance.level_line",
						Component.translatable(inheritanceKey),
						toRoman(level)
				).withStyle(color)
		);
	}

	private void appendFlagInheritanceLine(
			Consumer<Component> tooltipAdder,
			String inheritanceKey,
			boolean enabled,
			ChatFormatting color
	) {
		if (!enabled) {
			return;
		}

		tooltipAdder.accept(
				Component.translatable(
						"text.kingdomsiege.inheritance.single_line",
						Component.translatable(inheritanceKey)
				).withStyle(color)
		);
	}

	private Component resolveWeaponLabel(SoldierBlueprintData blueprint) {
		if (!blueprint.weaponStack().isEmpty()) {
			return blueprint.weaponStack().getHoverName();
		}

		return Component.translatable(blueprint.weaponClass().getTranslationKey());
	}

	private String formatOneDecimal(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private String toRoman(int level) {
		return switch (level) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> Integer.toString(level);
		};
	}
}
