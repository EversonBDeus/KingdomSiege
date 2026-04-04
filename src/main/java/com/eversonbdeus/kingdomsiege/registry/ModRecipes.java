package com.eversonbdeus.kingdomsiege.registry;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.recipe.ArcherSoldierRecipe;
import com.eversonbdeus.kingdomsiege.recipe.ArcherSoldierRecipeSerializer;
import com.eversonbdeus.kingdomsiege.recipe.SwordsmanSoldierRecipe;
import com.eversonbdeus.kingdomsiege.recipe.SwordsmanSoldierRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
	public static final RecipeSerializer<SwordsmanSoldierRecipe> SWORDSMAN_SOLDIER = Registry.register(
			BuiltInRegistries.RECIPE_SERIALIZER,
			Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "swordsman_soldier"),
			SwordsmanSoldierRecipeSerializer.create()
	);

	public static final RecipeSerializer<ArcherSoldierRecipe> ARCHER_SOLDIER = Registry.register(
			BuiltInRegistries.RECIPE_SERIALIZER,
			Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, "archer_soldier"),
			ArcherSoldierRecipeSerializer.create()
	);

	private ModRecipes() {
	}

	public static void register() {
		KingdomSiege.LOGGER.info("Registrando receitas de {}.", KingdomSiege.MOD_NAME);
	}
}
