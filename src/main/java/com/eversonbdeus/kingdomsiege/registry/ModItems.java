package com.eversonbdeus.kingdomsiege.registry;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.item.SoldierSpawnEggItem;
import com.eversonbdeus.kingdomsiege.soldier.SoldierClass;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public final class ModItems {
	public static final Item CASTLE_SOLDIER_SPAWN_EGG = register(
			"castle_soldier_spawn_egg",
			properties -> new SoldierSpawnEggItem(SoldierClass.SWORDSMAN, properties),
			new Item.Properties().stacksTo(64).spawnEgg(ModEntities.CASTLE_SOLDIER)
	);

	private ModItems() {
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
		ResourceKey<Item> itemKey = ResourceKey.create(
				BuiltInRegistries.ITEM.key(),
				net.minecraft.resources.Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, name)
		);

		T item = itemFactory.apply(settings.setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return item;
	}

	public static ItemStack createConfiguredSoldierEgg(SoldierClass soldierClass) {
		ItemStack stack = new ItemStack(CASTLE_SOLDIER_SPAWN_EGG);
		stack.set(ModComponents.SOLDIER_CLASS, soldierClass);
		return stack;
	}

	public static ItemStack createSwordsmanSoldierEgg() {
		return createConfiguredSoldierEgg(SoldierClass.SWORDSMAN);
	}

	public static ItemStack createArcherSoldierEgg() {
		return createConfiguredSoldierEgg(SoldierClass.ARCHER);
	}

	public static void register() {
		KingdomSiege.LOGGER.info("Registrando itens de {}.", KingdomSiege.MOD_NAME);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(entries -> entries.accept(CASTLE_SOLDIER_SPAWN_EGG));
	}
}