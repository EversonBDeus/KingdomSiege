package com.eversonbdeus.kingdomsiege.item;

import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import com.eversonbdeus.kingdomsiege.registry.ModComponents;
import com.eversonbdeus.kingdomsiege.registry.ModEntities;
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
	private final SoldierClass defaultSoldierClass;

	public SoldierSpawnEggItem(SoldierClass defaultSoldierClass, Item.Properties properties) {
		super(properties.component(ModComponents.SOLDIER_CLASS, defaultSoldierClass));
		this.defaultSoldierClass = defaultSoldierClass;
	}

	public SoldierClass getSoldierClass(ItemStack stack) {
		SoldierClass storedClass = stack.get(ModComponents.SOLDIER_CLASS);
		return storedClass != null ? storedClass : defaultSoldierClass;
	}

	public void setSoldierClass(ItemStack stack, SoldierClass soldierClass) {
		stack.set(ModComponents.SOLDIER_CLASS, soldierClass);
	}

	@Override
	public Component getName(ItemStack stack) {
		SoldierClass soldierClass = getSoldierClass(stack);

		return super.getName(stack)
				.copy()
				.append(Component.literal(" ("))
				.append(Component.translatable(soldierClass.getTranslationKey()))
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

		soldier.setSoldierClass(getSoldierClass(stack));

		if (player != null) {
			soldier.setOwnerUuid(player.getUUID());
		}

		return InteractionResult.SUCCESS;
	}
}