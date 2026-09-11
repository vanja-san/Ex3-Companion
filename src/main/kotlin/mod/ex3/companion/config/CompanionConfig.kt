package mod.ex3.companion.config

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import mod.ex3.companion.companion.CombatMode
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Mod configuration. Loaded once at server start from config/ex3-companion.json.
 * All values have sensible defaults matching the current hardcoded values.
 */
class CompanionConfig {

	// NOTE: These are `var` (not `val`) because Gson deserializes them reflectively.
	// Kotlin `val` compiles to a `final` field; JDK 17+ prints a warning on final-field mutation.
	@Expose var health = HealthConfig()
	@Expose var combat = CombatConfig()
	@Expose var healing = HealingConfig()
	@Expose var movement = MovementConfig()
	@Expose var explore = ExploreConfig()
	@Expose var xp = XpConfig()

	class HealthConfig {
		@Expose var base: Float = 20f
		@Expose var perLevel: Float = 2f
		@Expose var cap: Float = 60f
		@Expose var regenPerTick: Float = 0.025f
		@Expose var regenLevelScale: Float = 0.05f
		@Expose var reviveDelayTicks: Long = 0L
		@Expose var invulnerable: Boolean = false
	}

	class CombatConfig {
		@Expose var damageBase: Float = 3.0f
		@Expose var damagePerLevel: Float = 1.0f
		@Expose var damageCap: Float = 20.0f
		@Expose var intervalBase: Int = 40
		@Expose var intervalPerLevel: Int = 2
		@Expose var intervalMin: Int = 6
		@Expose var searchRadius: Double = 16.0
		@Expose var fireRange: Double = 10.0
		/** Preferred horizontal standoff distance from the target while firing (ranged attacker). */
		@Expose var preferredRange: Double = 6.0
		@Expose var abortDistance: Double = 20.0
		/** Distance from the owner at which combat aborts (owner ran away from the fight). */
		@Expose var ownerAbortDistance: Double = 30.0
		@Expose var xpPerKill: Int = 5
		/** Ticks after which combat aborts if the target is not actively attacking owner or companion. */
		@Expose var noTargetTimeout: Int = 100
		/** Ticks after which a target is considered stale (e.g. witch self-healing faster than damage). */
		@Expose var staleTargetTimeout: Int = 100
		/** Combat mode: DEFENDER, AGGRESSIVE, or STRATEGIC. */
		@Expose var combatMode: CombatMode = CombatMode.DEFENDER
	}

	class HealingConfig {
		@Expose var unlockLevel: Int = 10
		@Expose var basePerTick: Float = 0.02f
		@Expose var perLevelAbove: Float = 0.003f
		@Expose var range: Double = 4.0
	}

	class MovementConfig {
		@Expose var normalSpeed: Double = 0.15
		@Expose var fastSpeed: Double = 0.35
		@Expose var catchUpSpeed: Double = 0.7
		@Expose var exploreSpeed: Double = 0.18
		@Expose var teleportDistance: Double = 64.0
		/** Velocity smoothing factor (0–1). Higher = snappier response. */
		@Expose var accel: Double = 0.2
		/** Proportional slow-down near the goal. */
		@Expose var followGain: Double = 0.2
		/** Max vertical speed (blocks/tick). */
		@Expose var maxVertical: Double = 0.25
	}

	class ExploreConfig {
		/** Minimum ticks between explorations (~25 s at 20 TPS). */
		@Expose var cooldownMin: Int = 500
		/** Extra random ticks added to cooldown (~35 s at 20 TPS). */
		@Expose var cooldownVariance: Int = 700
		/** Chance to start exploring when cooldown expires and owner is nearby. */
		@Expose var chance: Float = 0.35f
		/** Minimum exploration duration in ticks. */
		@Expose var durationMin: Int = 60
		/** Extra random ticks added to duration. */
		@Expose var durationVariance: Int = 120
		/** Max squared distance from owner to allow exploration. */
		@Expose var maxOwnerDistance: Double = 30.0
		/** Max squared distance from owner before exploration is aborted. */
		@Expose var abortDistance: Double = 45.0
		/** Minimum ticks between scan rotations while hovering at POI. */
		@Expose var scanIntervalMin: Int = 40
		/** Extra random ticks added to scan interval. */
		@Expose var scanIntervalVariance: Int = 60
		/** How far (blocks) to scan for interesting blocks around the owner. */
		@Expose var searchRadius: Int = 12
	}

	class XpConfig {
		@Expose var levelCap: Int = 50
		@Expose var formulaMultiplier: Int = 15
	}

	companion object {
		private val LOGGER = LoggerFactory.getLogger("Ex3-Companion")
		private val GSON = GsonBuilder().setPrettyPrinting().create()
		private val FILE = File(FabricLoader.getInstance().configDir.toFile(), "ex3-companion.json")

		@Volatile
		private var instance: CompanionConfig? = null

		fun load() {
			try {
				if (FILE.exists()) {
					FileReader(FILE).use { reader ->
						instance = GSON.fromJson(reader, CompanionConfig::class.java)
					}
				}
			} catch (e: Exception) {
				LOGGER.warn("Failed to load ex3-companion config, using defaults", e)
			}
			if (instance == null) {
				instance = CompanionConfig()
			}
			save()
		}

		fun save() {
			try {
				FileWriter(FILE).use { writer ->
					GSON.toJson(instance, writer)
				}
			} catch (e: Exception) {
				LOGGER.warn("Failed to save ex3-companion config", e)
			}
		}

		fun get(): CompanionConfig {
			// Double-checked locking: safe if get() is called before load() from
			// multiple threads (e.g. entity attribute setup during world load).
			val current = instance
			if (current != null) return current
			synchronized(this) {
				val recheck = instance
				if (recheck != null) return recheck
				return CompanionConfig().also { instance = it }
			}
		}
	}
}
