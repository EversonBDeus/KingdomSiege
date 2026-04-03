package com.eversonbdeus.kingdomsiege.registry;

import com.eversonbdeus.kingdomsiege.KingdomSiege;
import com.eversonbdeus.kingdomsiege.entity.CastleSoldierEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<CastleSoldierEntity> CASTLE_SOLDIER = register(
			"castle_soldier",
			EntityType.Builder.<CastleSoldierEntity>of(CastleSoldierEntity::new, MobCategory.CREATURE)
					.sized(0.6F, 1.95F)
	);

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(
				BuiltInRegistries.ENTITY_TYPE.key(),
				net.minecraft.resources.Identifier.fromNamespaceAndPath(KingdomSiege.MOD_ID, name)
		);

		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		KingdomSiege.LOGGER.info("Registrando entidades de {}.", KingdomSiege.MOD_NAME);
		FabricDefaultAttributeRegistry.register(CASTLE_SOLDIER, CastleSoldierEntity.createAttributes());
	}
}