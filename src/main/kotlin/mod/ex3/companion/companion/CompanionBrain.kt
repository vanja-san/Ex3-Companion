package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Decides WHAT the companion should do each tick: follow, explore, attack, or idle.
 * Sets [CompanionEntity.flightTarget] and [CompanionEntity.speedLimit] for [CompanionFlight] to act on.
 *
 * Delegates combat execution to [CompanionCombat] and sound/feedback to [CompanionSounds].
 */
class CompanionBrain(private val e: CompanionEntity, private val combat: CompanionCombat) {

	// --- Brain timers (ticks) ---
	var exploreCooldown: Int = randomExploreCooldown()
		private set
	private var exploreTimer: Int = 0
	private var scanTimer: Int = 0
	private var stationaryTicks: Int = 0

	/** Mood pulse: spikes when something interesting happens; used by sounds. */
	var moodPulse: Float = 0f
		private set

	/** Set when memory changes and needs to be synced back to the core item. */
	var needsMemorySync: Boolean = false

	/**
	 * Main brain tick: decides the flight target, speed, and look direction.
	 * Called once per server tick from [CompanionEntity.tick].
	 */
	fun tick() {
		val owner = e.ownerPlayer()

		if (owner == null || !owner.isAlive) {
			e.setState(BrainState.IDLE)
			idleBrain(null, Double.MAX_VALUE)
		} else {
			val distanceToOwner = e.distanceToSqr(owner)

			// Safety net: snap-teleport when too far (e.g. owner used elytra, portal, etc.)
			val teleportDistSq = CompanionConfig.get().movement.teleportDistance.let { it * it }
			if (distanceToOwner > teleportDistSq) {
				val spawnPos = e.computeSpawnPosition(owner)
				e.setPos(spawnPos.x, spawnPos.y, spawnPos.z)
				e.flightTarget = null
				e.flight.resetStuck()
				return
			}

			when (e.currentState()) {
				BrainState.FOLLOW -> followBrain(owner, distanceToOwner)
				BrainState.EXPLORE -> exploreBrain(owner)
				BrainState.ATTACK -> attackBrain(owner)
				BrainState.IDLE -> idleBrain(owner, distanceToOwner)
			}
		}

		moodPulse *= MOOD_DECAY
	}

	// ---------------------------------------------------------------------
	// FOLLOW
	// ---------------------------------------------------------------------

	private fun followBrain(owner: Player, distanceSq: Double) {
		// Combat has priority over everything else while following.
		if (combat.tryStartCombat(owner)) return

		val target = orbitAroundOwner(computeFollowTarget(owner), owner)
		e.flightTarget = target

		val cfg = CompanionConfig.get().movement
		e.speedLimit = when {
			distanceSq > OWNER_FAR_DISTANCE_SQ -> cfg.catchUpSpeed
			e.position().distanceToSqr(target) > CATCH_UP_DISTANCE_SQ -> cfg.fastSpeed
			else -> cfg.normalSpeed
		}
		e.lookControl.setLookAt(owner.eyePosition)

		// Detect stationary owner → transition to IDLE after a grace period.
		val ownerMoved = owner.position().distanceToSqr(owner.xo, owner.yo, owner.zo) > 0.01
		if (ownerMoved) {
			stationaryTicks = 0
		} else {
			stationaryTicks++
			if (stationaryTicks >= IDLE_STATIONARY_THRESHOLD) {
				stationaryTicks = 0
				e.setState(BrainState.IDLE)
				return
			}
		}

		// Occasionally start exploring when things are calm.
		val exploreCfg = CompanionConfig.get().explore
		exploreCooldown--
		if (exploreCooldown <= 0 && distanceSq < exploreCfg.maxOwnerDistance * exploreCfg.maxOwnerDistance && Random.nextFloat() < exploreCfg.chance) {
			startExploration(owner)
		}
	}

	fun computeFollowTarget(owner: Player): Vec3 {
		val yawRad = Math.toRadians(owner.yRot.toDouble())
		val leftX = cos(yawRad)
		val leftZ = sin(yawRad)
		val backX = sin(yawRad)
		val backZ = -cos(yawRad)

		val bob = sin(e.bobPhase.toDouble()) * FOLLOW_BOB_AMPLITUDE
		val level = e.level()

		for (mult in FOLLOW_OFFSET_MULTIPLIERS) {
			val target = Vec3(
				owner.x + (leftX * FOLLOW_SIDE_OFFSET * mult) + (backX * FOLLOW_BACK_OFFSET * mult),
				owner.y + (FOLLOW_HEIGHT_OFFSET * mult) + bob,
				owner.z + (leftZ * FOLLOW_SIDE_OFFSET * mult) + (backZ * FOLLOW_BACK_OFFSET * mult),
			)
			if (isPositionSafe(target, level)) {
				return target
			}
		}
		return Vec3(owner.x, owner.y + 1.0, owner.z)
	}

	/**
	 * Checks if a position is safe to fly to: not inside a solid block,
	 * has at least [minVerticalClearance] blocks of space above and below.
	 */
	private fun isPositionSafe(pos: Vec3, level: net.minecraft.world.level.Level): Boolean {
		val block = net.minecraft.core.BlockPos.containing(pos)
		if (level.getBlockState(block).isSolidRender) return false

		// Check vertical clearance: at least 2 blocks above and 1 below.
		for (i in 1..FOLLOW_VERTICAL_CLEARANCE_ABOVE) {
			if (level.getBlockState(block.above(i)).isSolidRender) return false
		}
		for (i in 1..FOLLOW_VERTICAL_CLEARANCE_BELOW) {
			if (level.getBlockState(block.below(i)).isSolidRender) return false
		}
		return true
	}

	/**
	 * When the companion is close to the owner and its destination lies on the
	 * far side, it swings AROUND the owner instead of cutting through the body.
	 */
	private fun orbitAroundOwner(desired: Vec3, owner: Player): Vec3 {
		val dx = e.x - owner.x
		val dz = e.z - owner.z
		val distSq = (dx * dx) + (dz * dz)
		if (distSq > ORBIT_ENGAGE_DISTANCE_SQ) return desired

		val dist = sqrt(distSq)
		if (dist < 1e-3) return desired

		val nx = dx / dist
		val nz = dz / dist
		val radius = maxOf(dist, ORBIT_MIN_RADIUS)
		val orbitPos = Vec3(
			owner.x - (nz * radius),
			maxOf(desired.y, e.y),
			owner.z + (nx * radius),
		)
		if (!e.isPositionPassable(orbitPos) || !e.canSeePosition(orbitPos)) return desired
		return orbitPos
	}

	// ---------------------------------------------------------------------
	// EXPLORE
	// ---------------------------------------------------------------------

	private fun startExploration(owner: Player) {
		val target = findInterestingTarget(owner) ?: run {
			exploreCooldown = randomExploreCooldown()
			return
		}
		e.flightTarget = target
		e.speedLimit = CompanionConfig.get().movement.exploreSpeed
		val cfg = CompanionConfig.get().explore
		exploreTimer = cfg.durationMin + rng.nextInt(cfg.durationVariance)
		scanTimer = 0
		e.setState(BrainState.EXPLORE)
		moodPulse = 1f
		e.sounds.playCuriousBeep()
	}

	private fun findInterestingTarget(owner: Player): Vec3? {
		val level = owner.level()
		val center = owner.blockPosition()
		val r = CompanionConfig.get().explore.searchRadius
		val min = center.offset(-r, -r, -r)
		val max = center.offset(r, r, r)
		val memory = e.memory

		// Collect interesting blocks, scoring unexplored chunks higher.
		val scored = mutableListOf<Pair<Vec3, Double>>()
		for (pos in BlockPos.betweenClosed(min, max)) {
			val block = level.getBlockState(pos).block
			if (block !in INTERESTING_BLOCKS) continue

			val hoverPoint = Vec3(pos.x + 0.5, pos.y + 1.2, pos.z + 0.5)
			if (!e.canSeePosition(hoverPoint)) continue

			val explored = memory.isChunkExplored(pos)
			val distSq = pos.distSqr(center)
			// Unexplored chunks get a bonus (lower score = better), explored get a penalty.
			val score = distSq * if (explored) EXPLORED_CHUNK_PENALTY else 1.0
			scored.add(hoverPoint to score)
		}

		if (scored.isEmpty()) return null
		val best = scored.minByOrNull { it.second } ?: return null
		return best.first
	}

	private fun exploreBrain(owner: Player) {
		if (combat.tryStartCombat(owner)) return

		exploreTimer--
		val cfg = CompanionConfig.get().explore
		val abortDistSq = cfg.abortDistance * cfg.abortDistance

		val currentTarget = e.flightTarget
		if (currentTarget != null && !e.canSeePosition(currentTarget)) {
			exploreCooldown = randomExploreCooldown()
			e.setState(BrainState.FOLLOW)
			e.sounds.playCuriousBeep()
			return
		}

		if (exploreTimer <= 0 || e.distanceToSqr(owner) > abortDistSq) {
			exploreCooldown = randomExploreCooldown()
			e.setState(BrainState.FOLLOW)
			e.sounds.playCuriousBeep()
			return
		}

		val target = e.flightTarget
		if (target != null && e.position().distanceToSqr(target) < ARRIVAL_DISTANCE_SQ) {
			// Mark the chunk as explored and record ore Y-level if applicable.
			val blockPos = net.minecraft.core.BlockPos.containing(target)
			e.memory.markExplored(blockPos)
			val block = e.level().getBlockState(blockPos).block
			if (block in INTERESTING_BLOCKS) {
				e.memory.recordOreY(block.descriptionId, blockPos.y)
				needsMemorySync = true
			}

			// Emit a subtle particle at the POI so the player can see what caught the companion's eye.
			if (e.level() is ServerLevel && Random.nextFloat() < 0.02f) {
				val state = e.level().getBlockState(net.minecraft.core.BlockPos.containing(target))
				(e.level() as ServerLevel).sendParticles(
					BlockParticleOption(ParticleTypes.BLOCK, state),
					target.x, target.y, target.z, 3, 0.15, 0.15, 0.15, 0.02,
				)
			}
			scanTimer--
			if (scanTimer <= 0) {
				scanTimer = cfg.scanIntervalMin + rng.nextInt(cfg.scanIntervalVariance)
				val angle = rng.nextDouble() * Math.PI * 2
				val scanOffsetX = cos(angle) * 4.0
				val scanOffsetZ = sin(angle) * 4.0
				e.lookControl.setLookAt(e.position().add(scanOffsetX, 0.5, scanOffsetZ))
				e.sounds.playBeep(pitch = 1.1f + (rng.nextFloat() * 0.25f))
			}
		} else if (target != null) {
			e.speedLimit = CompanionConfig.get().movement.exploreSpeed
			e.lookControl.setLookAt(target)
		}
	}

	// ---------------------------------------------------------------------
	// ATTACK
	// ---------------------------------------------------------------------

	private fun attackBrain(owner: Player) {
		val result = combat.tick(owner, moodPulse)
		moodPulse = result.moodPulse

		if (result.disengaged) {
			exploreCooldown = randomExploreCooldown()
			e.setState(BrainState.FOLLOW)
			e.sounds.playCuriousBeep()
			return
		}

		// Set flight target and speed from combat result
		e.flightTarget = result.flightTarget
		e.speedLimit = result.speedLimit
		if (result.lookTarget != null) {
			e.lookControl.setLookAt(result.lookTarget)
		}
	}

	// ---------------------------------------------------------------------
	// IDLE
	// ---------------------------------------------------------------------

	private fun idleBrain(owner: Player?, distanceSq: Double) {
		if (owner != null && distanceSq < FOLLOW_RESUME_DISTANCE_SQ) {
			combat.stopCombat()
			stationaryTicks = 0
			e.setState(BrainState.FOLLOW)
			moodPulse = 1f
			e.sounds.playCuriousBeep()
			return
		}

		val t = tickCount().toDouble() * DRIFT_SPEED
		e.flightTarget = Vec3(e.xo + (sin(t) * 0.4), e.yo + 1.2 + (cos(t * 0.7) * 0.2), e.zo)
		e.speedLimit = DRIFT_SPEED_LIMIT
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	private fun tickCount(): Int = e.tickCount

	private fun randomExploreCooldown(): Int {
		val cfg = CompanionConfig.get().explore
		return cfg.cooldownMin + rng.nextInt(cfg.cooldownVariance)
	}

	companion object {
		private val rng = Random.Default

		// Follow tuning
		private const val FOLLOW_SIDE_OFFSET = 1.7
		private const val FOLLOW_BACK_OFFSET = 1.1
		private const val FOLLOW_HEIGHT_OFFSET = 1.25
		private const val FOLLOW_BOB_AMPLITUDE = 0.12
		private const val FOLLOW_RESUME_DISTANCE_SQ = 20.0 * 20.0
		private const val FOLLOW_VERTICAL_CLEARANCE_ABOVE = 2
		private const val FOLLOW_VERTICAL_CLEARANCE_BELOW = 1
		private const val IDLE_STATIONARY_THRESHOLD = 200
		private val FOLLOW_OFFSET_MULTIPLIERS = doubleArrayOf(1.0, 0.5, 0.0)

		// Speed-selection thresholds
		private const val CATCH_UP_DISTANCE_SQ = 9.0
		private const val OWNER_FAR_DISTANCE_SQ = 64.0
		private const val ARRIVAL_DISTANCE_SQ = 0.36

		// Orbit
		private const val ORBIT_ENGAGE_DISTANCE_SQ = 2.25
		private const val ORBIT_MIN_RADIUS = 1.5

		// Idle drift
		private const val DRIFT_SPEED = 0.03
		private const val DRIFT_SPEED_LIMIT = 0.06

		// Mood
		private const val MOOD_DECAY = 0.985f

		// Memory: explored chunks get a distance penalty to prefer unexplored areas
		private const val EXPLORED_CHUNK_PENALTY = 4.0

		// Interesting blocks for exploration
		val INTERESTING_BLOCKS = setOf(
			Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
			Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
			Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
			Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
			Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.RAW_COPPER_BLOCK, Blocks.RAW_GOLD_BLOCK,
			Blocks.SEA_LANTERN,
			Blocks.CHEST, Blocks.ENDER_CHEST,
			Blocks.AMETHYST_CLUSTER,
			Blocks.BEACON,
			Blocks.NETHERITE_BLOCK,
			Blocks.CRYING_OBSIDIAN,
			Blocks.TNT,
			Blocks.JUKEBOX, Blocks.NOTE_BLOCK,
			Blocks.SHROOMLIGHT, Blocks.GLOWSTONE,
			Blocks.ANCIENT_DEBRIS,
		)
	}
}
