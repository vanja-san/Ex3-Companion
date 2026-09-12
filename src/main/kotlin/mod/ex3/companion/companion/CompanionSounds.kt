package mod.ex3.companion.companion

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import kotlin.random.Random

/**
 * Handles ambient sounds, beep feedback, and event-driven emotion effects.
 *
 * Emotions are DISCRETE events: a burst of particles + a sound, played once when
 * something actually happens (healing, combat start, finding a POI, level-up).
 * There are no constant per-state particles — the companion is silent while flying.
 */
class CompanionSounds(private val e: CompanionEntity) {

	private var ambientSoundTimer: Int = randomAmbientDelay()

	/** Ticks rare ambient beeps (not particles). */
	fun tickAmbientSounds() {
		ambientSoundTimer--
		if (ambientSoundTimer <= 0) {
			ambientSoundTimer = randomAmbientDelay()
			val chance = AMBIENT_SOUND_CHANCE + (e.brain.moodPulse * 0.02f)
			if (Random.nextFloat() < chance) {
				playBeep(pitch = 0.9f + (Random.nextFloat() * 0.35f))
			}
		}
	}

	/**
	 * Plays a discrete emotion: a burst of particles around the companion plus a
	 * matching sound. Called once per event, never continuously.
	 */
	fun playEmotion(emotion: Emotion) {
		val level = (e.level() as? ServerLevel) ?: return
		when (emotion) {
			Emotion.HAPPY -> {
				level.sendParticles(
					ParticleTypes.HAPPY_VILLAGER,
					e.x, e.y + 0.5, e.z,
					8, 0.4, 0.3, 0.4, 0.02,
				)
				playBeep(pitch = 1.4f)
			}
			Emotion.CURIOUS -> {
				level.sendParticles(
					ParticleTypes.NOTE,
					e.x, e.y + 0.6, e.z,
					6, 0.4, 0.3, 0.4, 0.0,
				)
				playCuriousBeep()
			}
			Emotion.ANGRY -> {
				level.sendParticles(
					ParticleTypes.ANGRY_VILLAGER,
					e.x, e.y + 0.5, e.z,
					4, 0.3, 0.3, 0.3, 0.0,
				)
				level.sendParticles(
					ParticleTypes.SMOKE,
					e.x, e.y + 0.3, e.z,
					6, 0.3, 0.3, 0.3, 0.01,
				)
				playBeep(pitch = 0.6f)
			}
			Emotion.SAD -> {
				level.sendParticles(
					ParticleTypes.SMOKE,
					e.x, e.y + 0.4, e.z,
					6, 0.3, 0.3, 0.3, 0.01,
				)
				playBeep(pitch = 0.5f)
			}
		}
	}

	fun playBeep(pitch: Float = 1f) {
		val level = (e.level() as? ServerLevel) ?: return
		level.playSound(null, e, ModSounds.beep(), SoundSource.NEUTRAL, 0.6f, pitch)
	}

	fun playCuriousBeep() {
		playBeep(pitch = 1.3f + (Random.nextFloat() * 0.2f))
	}

	fun playDeathPoof() {
		val level = (e.level() as? ServerLevel) ?: return
		level.sendParticles(ParticleTypes.POOF, e.x, e.y + 0.45, e.z, 12, 0.25, 0.25, 0.25, 0.02)
		level.playSound(null, e, ModSounds.SAD_BLIP, SoundSource.NEUTRAL, 0.6f, 0.55f)
	}

	/** Level-up feedback: happy emotion + celebratory beeps. */
	fun playLevelUpFx() {
		playEmotion(Emotion.HAPPY)
		playBeep(pitch = 1.6f)
		playBeep(pitch = 2.0f)
	}

	private fun randomAmbientDelay(): Int =
		AMBIENT_DELAY_MIN + Random.nextInt(AMBIENT_DELAY_VARIANCE)

	companion object {
		private const val AMBIENT_SOUND_CHANCE = 0.008f
		private const val AMBIENT_DELAY_MIN = 20 * 6
		private const val AMBIENT_DELAY_VARIANCE = 20 * 14
	}
}

/** Discrete emotional feedback: a burst of particles + a sound, played once per event. */
enum class Emotion {
	/** Healing the owner, level-up. */
	HAPPY,
	/** Found an interesting place while exploring. */
	CURIOUS,
	/** Attack mode activated / combat started. */
	ANGRY,
	/** Death or disengage. */
	SAD,
}
