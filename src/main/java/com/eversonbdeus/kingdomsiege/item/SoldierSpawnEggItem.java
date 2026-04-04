package com.eversonbdeus.kingdomsiege.item;

import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import com.eversonbdeus.kingdomsiege.registry.ModComponents;
import com.eversonbdeus.kingdomsiege.registry.ModEntities;
import com.eversonbdeus.kingdomsiege.soldier.ArmorTier;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class SoldierSpawnEggItem extends SpawnEggItem {
	private final SoldierBlueprintData defaultBlueprint;

	public SoldierSpawnEggItem(SoldierBlueprintData defaultBlueprint, Item.Properties properties) {
		super(properties.component(ModComponents.SOLDIER_BLUEPRINT, defaultBlueprint));
		this.defaultBlueprint = defaultBlueprint;
	}

	public SoldierBlueprintData getSoldierBlueprint(ItemStack stack) {
		SoldierBlueprintData storedBlueprint = stack.get(ModComponents.SOLDIER_BLUEPRINT);

		if (storedBlueprint != null) {
			return storedBlueprint;
		}

		SoldierClass legacyClass = stack.get(ModComponents.SOLDIER_CLASS);
		if (legacyClass != null) {
			return SoldierBlueprintData.of(legacyClass, ArmorTier.LEATHER);
		}

		return defaultBlueprint;
	}

	public SoldierClass getSoldierClass(ItemStack stack) {
		return getSoldierBlueprint(stack).soldierClass();
	}

	public void setSoldierBlueprint(ItemStack stack, SoldierBlueprintData blueprint) {
		SoldierBlueprintData resolvedBlueprint = blueprint != null ? blueprint : defaultBlueprint;
		stack.set(ModComponents.SOLDIER_BLUEPRINT, resolvedBlueprint);
		stack.set(ModComponents.SOLDIER_CLASS, resolvedBlueprint.soldierClass());
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

		soldier.applyBlueprint(getSoldierBlueprint(stack));

		if (player != null) {
			soldier.setOwnerUuid(player.getUUID());
		}

		return InteractionResult.SUCCESS;
	}
}
