package com.eversonbdeus.kingdomsiege.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ArcherSoldierRecipeSerializer {
	private static final MapCodec<ArcherSoldierRecipe> CODEC = MapCodec.unit(ArcherSoldierRecipe::new);
	private static final StreamCodec<RegistryFriendlyByteBuf, ArcherSoldierRecipe> STREAM_CODEC = StreamCodec.unit(new ArcherSoldierRecipe());

	private ArcherSoldierRecipeSerializer() {
	}

	public static RecipeSerializer<ArcherSoldierRecipe> create() {
		return new RecipeSerializer<>(CODEC, STREAM_CODEC);
	}
}
