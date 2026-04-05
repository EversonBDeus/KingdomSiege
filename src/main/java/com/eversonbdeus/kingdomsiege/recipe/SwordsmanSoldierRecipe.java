package com.eversonbdeus.kingdomsiege.recipe;

import com.eversonbdeus.kingdomsiege.registry.ModItems;
import com.eversonbdeus.kingdomsiege.registry.ModRecipes;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class SwordsmanSoldierRecipe extends CustomRecipe {
	public SwordsmanSoldierRecipe() {
		super();
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return getValidatedInput(input) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ValidatedInput validatedInput = getValidatedInput(input);

		if (validatedInput == null) {
			return ItemStack.EMPTY;
		}

		SoldierBlueprintData blueprint = SoldierBlueprintData.swordsmanFromCraft(validatedInput.swordStack(), validatedInput.chestplateStack());
		return ModItems.createConfiguredSoldierEgg(blueprint);
	}

	@Override
	public RecipeSerializer<SwordsmanSoldierRecipe> getSerializer() {
		return ModRecipes.SWORDSMAN_SOLDIER;
	}

	private ValidatedInput getValidatedInput(CraftingInput input) {
		if (input == null || input.width() != 3 || input.height() != 3) {
			return null;
		}

		ItemStack swordStack = input.getItem(2, 0);
		ItemStack chestplateStack = input.getItem(1, 1);
		ItemStack soldierCoreStack = input.getItem(0, 2);

		if (!isEmpty(input.getItem(0, 0))
				|| !isEmpty(input.getItem(1, 0))
				|| !isEmpty(input.getItem(0, 1))
				|| !isEmpty(input.getItem(2, 1))
				|| !isEmpty(input.getItem(1, 2))
				|| !isEmpty(input.getItem(2, 2))) {
			return null;
		}

		if (!soldierCoreStack.is(ModItems.SOLDIER_CORE)) {
			return null;
		}

		EquipmentPattern swordPattern = EquipmentPattern.fromSword(swordStack);
		EquipmentPattern chestplatePattern = EquipmentPattern.fromChestplate(chestplateStack);

		if (swordPattern == null || chestplatePattern == null) {
			return null;
		}

		if (swordPattern != chestplatePattern) {
			return null;
		}

		// O peitoral do craft precisa estar novo.
		// Como ele define a defesa interna da unidade, não aceitamos armadura usada.
		if (isUsedChestplate(chestplateStack)) {
			return null;
		}

		return new ValidatedInput(swordStack, chestplateStack);
	}

	private static boolean isUsedChestplate(ItemStack stack) {
		return stack != null
				&& !stack.isEmpty()
				&& stack.isDamageableItem()
				&& stack.getDamageValue() > 0;
	}

	private static boolean isEmpty(ItemStack stack) {
		return stack == null || stack.isEmpty();
	}

	private enum EquipmentPattern {
		IRON,
		GOLD,
		DIAMOND,
		NETHERITE;

		private static EquipmentPattern fromSword(ItemStack stack) {
			if (stack == null || stack.isEmpty()) {
				return null;
			}

			if (stack.is(Items.IRON_SWORD)) {
				return IRON;
			}

			if (stack.is(Items.GOLDEN_SWORD)) {
				return GOLD;
			}

			if (stack.is(Items.DIAMOND_SWORD)) {
				return DIAMOND;
			}

			if (stack.is(Items.NETHERITE_SWORD)) {
				return NETHERITE;
			}

			return null;
		}

		private static EquipmentPattern fromChestplate(ItemStack stack) {
			if (stack == null || stack.isEmpty()) {
				return null;
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

			return null;
		}
	}

	private record ValidatedInput(ItemStack swordStack, ItemStack chestplateStack) {
	}
}