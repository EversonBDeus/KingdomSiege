package com.eversonbdeus.kingdomsiege.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class SwordsmanSoldierRecipeSerializer {
	private static final MapCodec<SwordsmanSoldierRecipe> CODEC = MapCodec.unit(SwordsmanSoldierRecipe::new);
	private static final StreamCodec<RegistryFriendlyByteBuf, SwordsmanSoldierRecipe> STREAM_CODEC = StreamCodec.unit(new SwordsmanSoldierRecipe());

	private SwordsmanSoldierRecipeSerializer() {
	}

	public static RecipeSerializer<SwordsmanSoldierRecipe> create() {
		return new RecipeSerializer<>(CODEC, STREAM_CODEC);
	}
}
