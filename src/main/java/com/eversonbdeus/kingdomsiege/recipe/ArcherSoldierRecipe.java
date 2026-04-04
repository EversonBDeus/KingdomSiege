package com.eversonbdeus.kingdomsiege.recipe;

import com.eversonbdeus.kingdomsiege.registry.ModItems;
import com.eversonbdeus.kingdomsiege.registry.ModRecipes;
import com.eversonbdeus.kingdomsiege.soldier.ArmorTier;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ArcherSoldierRecipe extends CustomRecipe {
	public ArcherSoldierRecipe() {
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

		SoldierBlueprintData blueprint = SoldierBlueprintData.archerFromCraft(validatedInput.bowStack(), validatedInput.chestplateStack());
		return ModItems.createConfiguredSoldierEgg(blueprint);
	}

	@Override
	public RecipeSerializer<ArcherSoldierRecipe> getSerializer() {
		return ModRecipes.ARCHER_SOLDIER;
	}

	private ValidatedInput getValidatedInput(CraftingInput input) {
		if (input == null || input.width() != 3 || input.height() != 3) {
			return null;
		}

		ItemStack bowStack = input.getItem(2, 0);
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

		if (!bowStack.is(Items.BOW)) {
			return null;
		}

		if (!isSupportedChestplate(chestplateStack)) {
			return null;
		}

		return new ValidatedInput(bowStack, chestplateStack);
	}

	private static boolean isSupportedChestplate(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		return switch (ArmorTier.fromChestplate(stack)) {
			case LEATHER, CHAIN, IRON, GOLD, DIAMOND, NETHERITE -> true;
		};
	}

	private static boolean isEmpty(ItemStack stack) {
		return stack == null || stack.isEmpty();
	}

	private record ValidatedInput(ItemStack bowStack, ItemStack chestplateStack) {
	}
}
