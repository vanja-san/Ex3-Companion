package mod.ex3.companion.companion

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import kotlin.random.Random

/**
 * Handles ambient sounds, beep feedback, and particle effects.
 */
class CompanionSounds(private val e: CompanionEntity) {

	private var ambientSoundTimer: Int = randomAmbientDelay()

	/** Ticks ambient sounds and plays beeps at random intervals. */
	fun tickAmbientSounds() {
		ambientSoundTimer--
		if (ambientSoundTimer <= 0) {
			ambientSoundTimer = randomAmbientDelay()
			val chance = AMBIENT_SOUND_CHANCE + (e.brain.moodPulse * 0.02f)
			if (Random.nextFloat() < chance) {
				playBeep(pitch = 0.9f + (Random.nextFloat() * 0.35f))
			}
		}

		// State-dependent ambient particles.
		tickStateParticles()
	}

	private fun tickStateParticles() {
		val level = (e.level() as? ServerLevel) ?: return
		val state = e.currentState()

		when (state) {
			BrainState.FOLLOW -> {
				// Occasional small happy sparkle when close to owner.
				val owner = e.ownerPlayer()
				if (owner != null && e.distanceTo(owner) < 4.0 && Random.nextFloat() < 0.015f) {
					level.sendParticles(
						ParticleTypes.HAPPY_VILLAGER,
						e.x + (Random.nextDouble() - 0.5) * 0.6,
						e.y + 0.5 + Random.nextDouble() * 0.3,
						e.z + (Random.nextDouble() - 0.5) * 0.6,
						1, 0.0, 0.0, 0.0, 0.0,
					)
				}
			}
			BrainState.EXPLORE -> {
				// Subtle note particles when actively scanning at a POI.
				if (Random.nextFloat() < 0.02f) {
					level.sendParticles(
						ParticleTypes.NOTE,
						e.x + (Random.nextDouble() - 0.5) * 0.8,
						e.y + 0.6,
						e.z + (Random.nextDouble() - 0.5) * 0.8,
						1, 0.0, 0.0, 0.0, 0.0,
					)
				}
			}
			BrainState.ATTACK -> {
				// Angry smoke particles while in combat.
				if (Random.nextFloat() < 0.08f) {
					level.sendParticles(
						ParticleTypes.SMOKE,
						e.x + (Random.nextDouble() - 0.5) * 0.5,
						e.y + 0.3 + Random.nextDouble() * 0.4,
						e.z + (Random.nextDouble() - 0.5) * 0.5,
						2, 0.05, 0.05, 0.05, 0.01,
					)
				}
			}
			BrainState.IDLE -> {
				// Slow ambient drift particles (soul fire / end rod feel).
				if (Random.nextFloat() < 0.008f) {
					level.sendParticles(
						ParticleTypes.END_ROD,
						e.x + (Random.nextDouble() - 0.5) * 0.4,
						e.y + 0.4,
						e.z + (Random.nextDouble() - 0.5) * 0.4,
						1, 0.0, 0.02, 0.0, 0.005,
					)
				}
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

	/** Level-up feedback: happy beeps. */
	fun playLevelUpFx() {
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
