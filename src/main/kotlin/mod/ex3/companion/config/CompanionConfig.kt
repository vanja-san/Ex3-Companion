package mod.ex3.companion.config

import com.jahirtrap.configlib.TXFConfig
import com.jahirtrap.configlib.TXFConfig.Comment
import com.jahirtrap.configlib.TXFConfig.Entry
import mod.ex3.companion.companion.CombatMode

/**
 * Companion configuration, managed by ConfigLib TXF.
 *
 * Registered via [TXFConfig.init] under the mod id on both sides. Every entry is
 * a `public static` (via [JvmField]) [Entry]-annotated field; the file lives at
 * `config/ex3-companion.json5`.
 *
 * The screen has a single `server` tab — gameplay values, marked `syncServer`
 * so the operator's values are authoritative: while connected, the GUI shows
 * them read-only and the client's copy is overridden on join. Sections are
 * separated by [Comment] headers (`centered`) and spacer rows.
 *
 * Slider fields carry `isSlider` + `min`/`max` + `precision` (snap = 1/precision
 * for float steps, integer steps snap to whole numbers). Plain numeric fields use
 * the text widget with validation bounds.
 */
class CompanionConfig : TXFConfig() {
	companion object {
		// ── Section: Health ──────────────────────────────────────────────────
		@JvmField
		@Comment(category = "server", centered = true)
		var sectionHealth: String = ""

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 1.0, max = 100.0, precision = 1,
			comment = "Base max health.")
		var healthBase: Float = 20f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 20.0, precision = 2,
			comment = "Extra max health per level.")
		var healthPerLevel: Float = 1.25f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 10.0, max = 200.0, precision = 1,
			comment = "Hard cap on max health.")
		var healthCap: Float = 44f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 2.0, precision = 100,
			comment = "Health regenerated per tick.")
		var healthRegenPerTick: Float = 0.025f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 1.0, precision = 100,
			comment = "Regeneration bonus multiplier per level.")
		var healthRegenLevelScale: Float = 0.05f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 1200.0,
			comment = "Ticks after death before the companion revives (0 = immediately).")
		var healthReviveDelayTicks: Int = 0

		@JvmField
		@Entry(category = "server", syncServer = true,
			comment = "Companion takes no damage.")
		var healthInvulnerable: Boolean = false

		@JvmField
		@Comment(category = "server", spacer = true)
		var spacerHealth: String = ""

		// ── Section: Combat ──────────────────────────────────────────────────
		@JvmField
		@Comment(category = "server", centered = true)
		var sectionCombat: String = ""

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 30.0, precision = 2,
			comment = "Base damage dealt.")
		var combatDamageBase: Float = 3f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 10.0, precision = 2,
			comment = "Extra damage per level.")
		var combatDamagePerLevel: Float = 1f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 1.0, max = 50.0, precision = 1,
			comment = "Hard cap on damage dealt.")
		var combatDamageCap: Float = 20f

		@JvmField
		@Entry(category = "server", syncServer = true, min = 1.0, max = 200.0,
			comment = "Base ticks between attacks.")
		var combatIntervalBase: Int = 35

		@JvmField
		@Entry(category = "server", syncServer = true, min = 0.0, max = 20.0,
			comment = "Ticks of attack interval reduction per level.")
		var combatIntervalPerLevel: Int = 1

		@JvmField
		@Entry(category = "server", syncServer = true, min = 1.0, max = 100.0,
			comment = "Minimum attack cooldown in ticks.")
		var combatIntervalMin: Int = 16

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 4.0, max = 64.0, precision = 1,
			comment = "How far (blocks) to search for hostile targets.")
		var combatSearchRadius: Double = 16.0

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 2.0, max = 32.0, precision = 2,
			comment = "Maximum firing range (blocks).")
		var combatFireRange: Double = 10.0

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 2.0, max = 32.0, precision = 2,
			comment = "Preferred standoff distance from the target while firing (blocks).")
		var combatPreferredRange: Double = 6.0

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 5.0, max = 64.0, precision = 1,
			comment = "Distance (blocks) beyond which combat aborts.")
		var combatAbortDistance: Double = 20.0

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 5.0, max = 64.0, precision = 1,
			comment = "Distance from the owner (blocks) at which combat aborts.")
		var combatOwnerAbortDistance: Double = 30.0

		@JvmField
		@Entry(category = "server", syncServer = true, min = 1.0, max = 100.0,
			comment = "XP granted per kill.")
		var combatXpPerKill: Int = 5

		@JvmField
		@Entry(category = "server", syncServer = true, min = 20.0, max = 600.0,
			comment = "Ticks after which combat aborts if the target is not attacking.")
		var combatNoTargetTimeout: Int = 100

		@JvmField
		@Entry(category = "server", syncServer = true, min = 20.0, max = 300.0,
			comment = "Ticks after which a target is considered stale.")
		var combatStaleTargetTimeout: Int = 100

		@JvmField
		@Entry(category = "server", syncServer = true,
			comment = "Combat behaviour mode.")
		var combatCombatMode: CombatMode = CombatMode.DEFENDER

		@JvmField
		@Comment(category = "server", spacer = true)
		var spacerCombat: String = ""

		// ── Section: Healing ─────────────────────────────────────────────────
		@JvmField
		@Comment(category = "server", centered = true)
		var sectionHealing: String = ""

		@JvmField
		@Entry(category = "server", syncServer = true, min = 1.0, max = 50.0,
			comment = "Level at which the companion starts healing the owner.")
		var healingUnlockLevel: Int = 10

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 1.0, precision = 200,
			comment = "Base owner heal rate (HP/tick).")
		var healingBasePerTick: Float = 0.02f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 0.1, precision = 1000,
			comment = "Additional heal rate per level above the unlock level.")
		var healingPerLevelAbove: Float = 0.003f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 1.0, precision = 200,
			comment = "Hard cap on owner heal rate (HP/tick).")
		var healingMaxPerTick: Float = 0.05f

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 1.0, max = 16.0, precision = 2,
			comment = "Maximum distance (blocks) for healing the owner.")
		var healingRange: Double = 4.0

		@JvmField
		@Comment(category = "server", spacer = true)
		var spacerHealing: String = ""

		// ── Section: Movement ────────────────────────────────────────────────
		@JvmField
		@Comment(category = "server", centered = true)
		var sectionMovement: String = ""

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.01, max = 2.0, precision = 100,
			comment = "Normal cruising speed (blocks/tick).")
		var movementNormalSpeed: Double = 0.15

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.05, max = 3.0, precision = 20,
			comment = "Speed while the owner moves fast.")
		var movementFastSpeed: Double = 0.35

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.1, max = 5.0, precision = 10,
			comment = "Speed while catching up to a distant owner.")
		var movementCatchUpSpeed: Double = 0.7

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.01, max = 2.0, precision = 100,
			comment = "Speed while exploring points of interest.")
		var movementExploreSpeed: Double = 0.18

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 8.0, max = 256.0, precision = 1,
			comment = "Distance (blocks) at which the companion teleports to the owner.")
		var movementTeleportDistance: Double = 64.0

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.05, max = 1.0, precision = 20,
			comment = "Velocity smoothing factor (higher = snappier).")
		var movementAccel: Double = 0.2

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.05, max = 1.0, precision = 20,
			comment = "Proportional slow-down near the goal.")
		var movementFollowGain: Double = 0.2

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.05, max = 2.0, precision = 20,
			comment = "Max vertical speed (blocks/tick).")
		var movementMaxVertical: Double = 0.25

		@JvmField
		@Comment(category = "server", spacer = true)
		var spacerMovement: String = ""

		// ── Section: Exploration ─────────────────────────────────────────────
		@JvmField
		@Comment(category = "server", centered = true)
		var sectionExplore: String = ""

		@JvmField
		@Entry(category = "server", syncServer = true, min = 100.0, max = 2000.0,
			comment = "Minimum ticks between explorations.")
		var exploreCooldownMin: Int = 500

		@JvmField
		@Entry(category = "server", syncServer = true, min = 0.0, max = 2000.0,
			comment = "Extra random ticks added to the cooldown.")
		var exploreCooldownVariance: Int = 700

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 0.0, max = 1.0, precision = 20,
			comment = "Chance to start exploring when the cooldown expires.")
		var exploreChance: Float = 0.35f

		@JvmField
		@Entry(category = "server", syncServer = true, min = 20.0, max = 600.0,
			comment = "Minimum exploration duration in ticks.")
		var exploreDurationMin: Int = 60

		@JvmField
		@Entry(category = "server", syncServer = true, min = 0.0, max = 600.0,
			comment = "Extra random ticks added to exploration duration.")
		var exploreDurationVariance: Int = 120

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 8.0, max = 128.0, precision = 1,
			comment = "Max distance (blocks) from the owner to allow exploration.")
		var exploreMaxOwnerDistance: Double = 30.0

		@JvmField
		@Entry(category = "server", syncServer = true, isSlider = true, min = 10.0, max = 128.0, precision = 1,
			comment = "Distance (blocks) from the owner at which exploration aborts.")
		var exploreAbortDistance: Double = 45.0

		@JvmField
		@Entry(category = "server", syncServer = true, min = 10.0, max = 200.0,
			comment = "Minimum ticks between scan rotations at a point of interest.")
		var exploreScanIntervalMin: Int = 40

		@JvmField
		@Entry(category = "server", syncServer = true, min = 0.0, max = 200.0,
			comment = "Extra random ticks added to the scan interval.")
		var exploreScanIntervalVariance: Int = 60

		@JvmField
		@Entry(category = "server", syncServer = true, min = 4.0, max = 64.0,
			comment = "How far (blocks) to scan for interesting blocks around the owner.")
		var exploreSearchRadius: Int = 12

		@JvmField
		@Comment(category = "server", spacer = true)
		var spacerExplore: String = ""

		// ── Section: Leveling ────────────────────────────────────────────────
		@JvmField
		@Comment(category = "server", centered = true)
		var sectionXp: String = ""

		@JvmField
		@Entry(category = "server", syncServer = true, min = 1.0, max = 100.0,
			comment = "Max level the companion can reach.")
		var xpLevelCap: Int = 20

		@JvmField
		@Entry(category = "server", syncServer = true, min = 1.0, max = 100.0,
			comment = "XP formula multiplier (level^2 * multiplier).")
		var xpFormulaMultiplier: Int = 3
	}
}