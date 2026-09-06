package mod.ex3.companion.companion

import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents

/**
 * The companion's "voice": copper golem sounds (beep-boops).
 * Referenced directly via [SoundEvents] constants so missing keys are
 * impossible at runtime (a bad string key crashed the server once already).
 *
 * NOTE: the copper golem has NO ambient sound in 26.2 — item get / no-get
 * chirps are used as its voice instead.
 */
object ModSounds {
	/** Bright chirp — curiosity / happy reactions. */
	val CHIRP: SoundEvent = SoundEvents.COPPER_GOLEM_ITEM_GET

	/** Low descending blip — disappointment / death poof. */
	val SAD_BLIP: SoundEvent = SoundEvents.COPPER_GOLEM_ITEM_NO_GET

	/** Generic ambient beep-boop. */
	fun beep(): SoundEvent = CHIRP
}
