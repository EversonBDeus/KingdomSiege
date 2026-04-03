package com.eversonbdeus.kingdomsiege.item;

import com.eversonbdeus.kingdomsiege.registry.ModComponents;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

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
}