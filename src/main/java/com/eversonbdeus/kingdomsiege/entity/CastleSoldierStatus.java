package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * [REFATORAÇÃO] Helper de status/UI textual do soldado.
 *
 * Mantém fora da entidade base toda a montagem de Components e mensagens
 * exibidas ao jogador. A regra aqui é só organizar apresentação: não altera
 * combate, navegação, persistência nem modo militar.
 */
final class CastleSoldierStatus {
	private final CastleSoldierEntity soldier;

	CastleSoldierStatus(CastleSoldierEntity soldier) {
		this.soldier = soldier;
	}

	void sendBasicStatusTo(Player player) {
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.header",
				soldier.getSoldierDisplayName(),
				Component.translatable(soldier.getSoldierClass().getTranslationKey()),
				Component.translatable(soldier.getSoldierMode().getTranslationKey())
		));

		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.combat",
				getWeaponClassComponent(),
				getArmorTierComponent(),
				getCatalystComponent()
		));

		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.attributes",
				Math.round(soldier.getHealth()),
				Math.round(soldier.getMaxHealth()),
				Math.round(soldier.getArmorValue()),
				Math.round(soldier.getAttributeValue(Attributes.ARMOR_TOUGHNESS))
		));

		player.sendSystemMessage(getCombatPowerComponent());

		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.rank",
				Component.translatable(soldier.getSoldierRank().getTranslationKey()),
				soldier.getMilitaryXp()
		));

		// [FASE 6] Linha de histórico: kills e batalhas sobrevividas.
		player.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_status.history",
				soldier.getKillCount(),
				soldier.getBattlesCount()
		));

		for (Component line : getInheritedChestplateEnchantmentsComponents()) {
			player.sendSystemMessage(line);
		}

		for (Component line : getInheritedWeaponEnchantmentsComponents()) {
			player.sendSystemMessage(line);
		}

		player.sendSystemMessage(getTerritoryStatusComponent());
	}

	private Component getArmorTierComponent() {
		SoldierBlueprintData blueprint = soldier.getSoldierBlueprint();
		return blueprint.chestplateStack().isEmpty()
				? Component.translatable(soldier.getArmorTier().getTranslationKey())
				: blueprint.chestplateStack().getHoverName();
	}

	private Component getWeaponClassComponent() {
		SoldierBlueprintData blueprint = soldier.getSoldierBlueprint();
		return blueprint.weaponStack().isEmpty()
				? Component.translatable(soldier.getWeaponClass().getTranslationKey())
				: blueprint.weaponStack().getHoverName();
	}

	private Component getCatalystComponent() {
		return Component.translatable(soldier.getCatalystType().getTranslationKey());
	}

	private Component getCombatPowerComponent() {
		if (soldier.canUseBowCombatForStatus()) {
			return Component.translatable(
					"message.kingdomsiege.soldier_status.power_ranged",
					formatOneDecimal(soldier.getEffectiveProjectileBaseDamageForStatus())
			);
		}

		return Component.translatable(
				"message.kingdomsiege.soldier_status.power_melee",
				formatOneDecimal(soldier.getAttributeValue(Attributes.ATTACK_DAMAGE))
		);
	}

	private List<Component> getInheritedChestplateEnchantmentsComponents() {
		List<Component> components = new ArrayList<>();
		SoldierBlueprintData blueprint = soldier.getSoldierBlueprint();

		if (!blueprint.hasInheritedChestplateEnchantments()) {
			return components;
		}

		components.add(Component.translatable("text.kingdomsiege.inheritance.chestplate_header")
				.withStyle(ChatFormatting.LIGHT_PURPLE));

		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.protection",
				blueprint.inheritedProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.projectile_protection",
				blueprint.inheritedProjectileProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.blast_protection",
				blueprint.inheritedBlastProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.fire_protection",
				blueprint.inheritedFireProtectionLevel(), ChatFormatting.LIGHT_PURPLE);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.thorns",
				blueprint.inheritedThornsLevel(), ChatFormatting.LIGHT_PURPLE);

		return components;
	}

	private List<Component> getInheritedWeaponEnchantmentsComponents() {
		List<Component> components = new ArrayList<>();

		int effectiveSharpnessLevel = soldier.getEffectiveInheritedSharpnessLevelForStatus();
		int effectiveFireAspectLevel = soldier.getEffectiveInheritedFireAspectLevelForStatus();
		int effectiveKnockbackLevel = soldier.getEffectiveInheritedKnockbackLevelForStatus();
		int effectivePowerLevel = soldier.getEffectiveInheritedPowerLevelForStatus();
		int effectivePunchLevel = soldier.getEffectiveInheritedPunchLevelForStatus();
		int effectiveFlameLevel = soldier.getEffectiveInheritedFlameLevelForStatus();

		boolean hasAnyInheritance;

		if (soldier.canUseBowCombatForStatus()) {
			hasAnyInheritance = effectivePowerLevel > 0
					|| effectivePunchLevel > 0
					|| effectiveFlameLevel > 0;
		} else {
			hasAnyInheritance = effectiveSharpnessLevel > 0
					|| effectiveFireAspectLevel > 0
					|| effectiveKnockbackLevel > 0;
		}

		if (!hasAnyInheritance) {
			return components;
		}

		components.add(Component.translatable("text.kingdomsiege.inheritance.weapon_header")
				.withStyle(ChatFormatting.GOLD));

		if (soldier.canUseBowCombatForStatus()) {
			appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.power",
					effectivePowerLevel, ChatFormatting.GOLD);
			appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.punch",
					effectivePunchLevel, ChatFormatting.GOLD);
			appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.flame",
					effectiveFlameLevel, ChatFormatting.GOLD);
			return components;
		}

		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.sharpness",
				effectiveSharpnessLevel, ChatFormatting.GOLD);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.fire_aspect",
				effectiveFireAspectLevel, ChatFormatting.GOLD);
		appendLevelInheritanceComponent(components, "text.kingdomsiege.inheritance.knockback",
				effectiveKnockbackLevel, ChatFormatting.GOLD);

		return components;
	}

	private void appendLevelInheritanceComponent(
			List<Component> components,
			String inheritanceKey,
			int level,
			ChatFormatting color
	) {
		if (level <= 0) {
			return;
		}

		components.add(
				Component.literal("• ")
						.append(Component.translatable(inheritanceKey))
						.append(Component.literal(" " + toRoman(level)))
						.withStyle(color)
		);
	}

	private Component getTerritoryStatusComponent() {
		if (soldier.hasHomePos()) {
			return Component.translatable(
					"message.kingdomsiege.soldier_status.territory",
					soldier.getHomePos().getX(),
					soldier.getHomePos().getY(),
					soldier.getHomePos().getZ(),
					soldier.getGuardRadius()
			);
		}

		return Component.translatable(
				"message.kingdomsiege.soldier_status.territory_undefined",
				soldier.getGuardRadius()
		);
	}

	private String formatOneDecimal(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
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
