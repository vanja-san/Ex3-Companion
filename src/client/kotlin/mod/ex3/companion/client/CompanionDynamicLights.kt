package mod.ex3.companion.client

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer
import dev.lambdaurora.lambdynlights.api.entity.EntityLightSource
import mod.ex3.companion.registry.ModEntities
import net.minecraft.core.registries.Registries

/**
 * Registers the companion as a dynamic light source with smart auto-glow.
 *
 * Uses [CompanionEntityLuminance] which automatically:
 * - Turns glow OFF during daytime when the companion is above ground.
 * - Turns glow ON during daytime when the companion enters a cave.
 * - Keeps glow ON at all times during nighttime.
 * - Smoothly fades between states (no abrupt on/off flicker).
 */
class CompanionDynamicLights : DynamicLightsInitializer {
	override fun onInitializeDynamicLights(context: DynamicLightsContext) {
		// Register the custom smart luminance type.
		val luminanceType = CompanionEntityLuminance.registerType()

		context.entityLightSourceManager().onRegisterEvent().register { registerContext ->
			val types = registerContext.registryLookup().lookup(Registries.ENTITY_TYPE).orElseThrow()
			val predicate = EntityLightSource.EntityPredicate.builder()
				.of(types, ModEntities.COMPANION)
				.build()

			// Single luminance provider: handles day/night, cave detection, and smooth transitions.
			registerContext.register(EntityLightSource(predicate, listOf(CompanionEntityLuminance.INSTANCE)))
		}
	}
}
