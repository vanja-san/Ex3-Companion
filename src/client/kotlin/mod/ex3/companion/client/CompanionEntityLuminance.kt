package mod.ex3.companion.client

import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
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
 */
class CompanionEntityLuminance private constructor() : EntityLuminance {

	private var smoothLuminance: Double = 0.0
	private var displayedLuminance: Int = 0

	override fun type(): EntityLuminance.Type = COMPANION_LUMINANCE_TYPE

	override fun getLuminance(itemLightSourceManager: ItemLightSourceManager, entity: Entity): Int {
		val level = entity.level()
		val blockPos = entity.blockPosition()

		// Effective brightness = max(skyLight, blockLight), reduced by sky-darkening.
		// At noon above ground this is 15; inside a dark cave this is 0.
		val effectiveBrightness = level.getMaxLocalRawBrightness(blockPos, level.skyDarken)

		val target = if (effectiveBrightness < CAVE_THRESHOLD) MAX_LUMINANCE else 0

		// Float lerp — runs every render frame, giving ~50 updates/sec.
		val alpha = FADE_SPEED
		smoothLuminance += (target - smoothLuminance) * alpha

		// Snap when close enough to avoid endless micro-dithering.
		val snapped = if (abs(smoothLuminance - target) < 0.5) {
			smoothLuminance = target.toDouble()
			target
		} else {
			smoothLuminance.toInt()
		}

		if (snapped != displayedLuminance) {
			displayedLuminance = snapped
		}
		return displayedLuminance
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
