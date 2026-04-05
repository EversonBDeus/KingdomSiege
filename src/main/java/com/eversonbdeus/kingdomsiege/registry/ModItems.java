package com.eversonbdeus.kingdomsiege.registry;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.item.SoldierSpawnEggItem;
import com.eversonbdeus.kingdomsiege.soldier.ArmorTier;
import com.eversonbdeus.kingdomsiege.soldier.SoldierBlueprintData;
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
	public static final Item SOLDIER_CORE = register(
			"soldier_core",
			Item::new,
			new Item.Properties().stacksTo(64)
	);

	public static final Item CASTLE_SOLDIER_SPAWN_EGG = register(
			"castle_soldier_spawn_egg",
			SoldierSpawnEggItem::new,
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

	public static ItemStack createConfiguredSoldierEgg(SoldierBlueprintData blueprint) {
		ItemStack stack = new ItemStack(CASTLE_SOLDIER_SPAWN_EGG);
		stack.set(ModDataComponents.SOLDIER_BLUEPRINT, blueprint);
		stack.set(ModDataComponents.SOLDIER_CLASS, blueprint.soldierClass());
		return stack;
	}

	public static ItemStack createConfiguredSoldierEgg(SoldierClass soldierClass) {
		return createConfiguredSoldierEgg(SoldierBlueprintData.of(soldierClass, ArmorTier.LEATHER));
	}

	public static ItemStack createSwordsmanSoldierEgg() {
		return createConfiguredSoldierEgg(SoldierBlueprintData.of(SoldierClass.SWORDSMAN, ArmorTier.LEATHER));
	}

	public static ItemStack createArcherSoldierEgg() {
		return createConfiguredSoldierEgg(SoldierBlueprintData.of(SoldierClass.ARCHER, ArmorTier.LEATHER));
	}

	public static void register() {
		KingdomSiege.LOGGER.info("Registrando itens de {}.", KingdomSiege.MOD_NAME);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(entries -> entries.accept(SOLDIER_CORE));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(entries -> entries.accept(CASTLE_SOLDIER_SPAWN_EGG));
	}
}
