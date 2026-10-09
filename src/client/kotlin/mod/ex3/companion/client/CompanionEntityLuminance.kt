package mod.ex3.companion.client

import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID
import kotlin.math.abs

/**
 * Smart auto-glow luminance provider for the companion.
 *
 * Uses actual sky-light levels to detect caves — checks
 * `getMaxLocalRawBrightness` at the entity position, which naturally
 * returns 0 inside caves regardless of ceiling height.
 *
 * Smooth transition: accumulates a float and lerps toward the target
 * every render frame (~50 Hz), producing a gradual fade with no visible steps.
 *
 * The fade state is **per entity**, not per provider. LambDynamicLights registers one
 * provider instance and calls [getLuminance] once per rendered entity, so a single
 * shared accumulator made every visible companion lerp against the others' values —
 * two companions next to each other made the brightness ping-pong and flicker. State is
 * held in a bounded LRU so players who log out cannot leak entries.
 */
class CompanionEntityLuminance private constructor() : EntityLuminance {

	/** Mutable per-entity fade state. */
	private class Smooth {
		var luminance: Double = 0.0
		var displayed: Int = 0
	}

	private val states: MutableMap<UUID, Smooth> =
		Collections.synchronizedMap(object : LinkedHashMap<UUID, Smooth>(16, 0.75f, true) {
			override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, Smooth>): Boolean =
				size > MAX_TRACKED
		})

	override fun type(): EntityLuminance.Type = COMPANION_LUMINANCE_TYPE

	override fun getLuminance(itemLightSourceManager: ItemLightSourceManager, entity: Entity): Int {
		// Emission is governed by LambDynamicLights' own global toggle; here we
		// only decide how bright the companion is (which we modulate by light).
		val level = entity.level()
		val blockPos = entity.blockPosition()

		// Effective brightness = max(skyLight, blockLight), reduced by sky-darkening.
		// At noon above ground this is 15; inside a dark cave this is 0.
		val effectiveBrightness = level.getMaxLocalRawBrightness(blockPos, level.skyDarken)

		val target = if (effectiveBrightness < CAVE_THRESHOLD) MAX_LUMINANCE else 0

		val state = states.getOrPut(entity.uuid) { Smooth() }

		// Float lerp — runs every render frame, giving ~50 updates/sec.
		state.luminance += (target - state.luminance) * FADE_SPEED

		// Snap when close enough to avoid endless micro-dithering.
		state.displayed = if (abs(state.luminance - target) < 0.5) {
			state.luminance = target.toDouble()
			target
		} else {
			state.luminance.toInt()
		}

		return state.displayed
	}

	companion object {
		val INSTANCE = CompanionEntityLuminance()

		private const val MAX_LUMINANCE = 15
		/**
		 * Effective brightness below this value is considered "dark enough to glow".
		 * 7 means: sky light ≤ 7 at current time-of-day (e.g. dusk with skyDarken=8
		 * reduces sky light 15→7, so surface at dusk barely glows; caves always glow).
		 */
		private const val CAVE_THRESHOLD = 7
		/**
		 * Lerp factor per render frame (~50 Hz).
		 * 0.15 → reaches 90% of target in ~12 frames (~0.24 s).
		 * Produces a visible but smooth fade — not instant, not sluggish.
		 */
		private const val FADE_SPEED = 0.15

		/** Upper bound on tracked companions (LRU eviction) — far above any realistic count. */
		private const val MAX_TRACKED = 16

		/** Registered type, set during initialization. */
		lateinit var COMPANION_LUMINANCE_TYPE: EntityLuminance.Type
			private set

		fun registerType(): EntityLuminance.Type {
			COMPANION_LUMINANCE_TYPE = EntityLuminance.Type.registerSimple(
				Identifier.fromNamespaceAndPath("ex3-companion", "companion_smart"),
				INSTANCE,
			)
			return COMPANION_LUMINANCE_TYPE
		}
	}
}