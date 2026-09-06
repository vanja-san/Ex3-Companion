package mod.ex3.companion.registry

import mod.ex3.companion.Ex3Companion
import mod.ex3.companion.companion.CompanionEntity
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

object ModEntities {
	private val KEY: ResourceKey<EntityType<*>> =
		ResourceKey.create(Registries.ENTITY_TYPE, Ex3Companion.id("companion"))

	val COMPANION: EntityType<CompanionEntity> = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		KEY,
		EntityType.Builder.of(::CompanionEntity, MobCategory.MISC)
			.sized(0.4f, 0.4f)
			.clientTrackingRange(8)
			// Frequent position packets + vanilla interpolation keep motion smooth
			// now that movement is fully server-authoritative.
			.updateInterval(2)
			.build(KEY),
	)

	fun initialize() {
		FabricDefaultAttributeRegistry.register(COMPANION, CompanionEntity.createAttributes())
	}
}
