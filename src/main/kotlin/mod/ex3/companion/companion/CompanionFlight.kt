package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Handles all flight physics: velocity smoothing, wall avoidance, ground clearance,
 * stuck detection, multi-directional escape probes, and door-finding for indoor escape.
 *
 * Does NOT decide WHERE to fly — that is [CompanionBrain]'s job (sets [CompanionEntity.flightTarget]).
 * This class only applies the physics each tick via [tick].
 */
class CompanionFlight(private val e: CompanionEntity) {

	// --- Stuck detection state ---
	private var prevDistToTarget: Double = Double.MAX_VALUE
	private var recoveryWaypoint: Vec3? = null
	private var failedDirections = mutableSetOf<Int>()
	private var doorScanCooldown: Int = 0
	private var doorOpenCooldown: Int = 0

	/** Ticks the companion has been fully stuck with no escape. */
	var hardStuckTicks: Int = 0
		/** Ticks of no progress toward the current flight target. */
		var stuckTicks: Int = 0
			private set

	/** Resets all stuck state (called when a new target is set or after teleport). */
	fun resetStuck() {
		stuckTicks = 0
		hardStuckTicks = 0
		recoveryWaypoint = null
		failedDirections.clear()
	}

	/**
	 * Main tick: computes and applies velocity toward [CompanionEntity.flightTarget].
	 * Call once per server tick from [CompanionEntity.tick].
	 */
	fun tick() {
		val goal = e.flightTarget
		if (goal == null) {
			hoverInPlace()
			return
		}
		openNearbyDoors()

		val pos = e.position()
		val movCfg = CompanionConfig.get().movement

		// --- Stuck detection ---
		val distToTarget = pos.distanceTo(goal)
		if (distToTarget > ARRIVAL) {
			if (distToTarget >= (prevDistToTarget - 0.01)) {
				stuckTicks++
				hardStuckTicks++
			} else {
				stuckTicks = 0
				hardStuckTicks = 0
				failedDirections.clear()
				recoveryWaypoint = null
			}
		}
		prevDistToTarget = distToTarget

		// --- Hard-stuck fallback: teleport to owner if completely trapped ---
		if (hardStuckTicks >= HARD_STUCK_THRESHOLD) {
			val owner = e.ownerPlayer()
			if (owner != null) {
				val spawnPos = e.computeSpawnPosition(owner)
				e.setPos(spawnPos.x, spawnPos.y, spawnPos.z)
			}
			hardStuckTicks = 0
			stuckTicks = 0
			recoveryWaypoint = null
			failedDirections.clear()
			e.flightTarget = null
			return
		}

		// --- Recovery: probe all directions, pick best escape ---
		val effectiveGoal = if (stuckTicks >= STUCK_THRESHOLD) {
			val wp = recoveryWaypoint
			if (wp != null) {
				if (pos.distanceTo(wp) < WP_ARRIVAL) {
					if (distToTarget < RECOVERY_DONE_DIST) {
						recoveryWaypoint = null
						stuckTicks = 0
						hardStuckTicks = 0
						failedDirections.clear()
						goal
					} else {
						recoveryWaypoint = null
						stuckTicks = STUCK_THRESHOLD - REPROBE_DELAY
						goal
					}
				} else {
					wp
				}
			} else {
				val best = probeEscapeDirection(goal)
				if (best != null) {
					recoveryWaypoint = best
					best
				} else {
					failedDirections.clear()
					stuckTicks = STUCK_THRESHOLD - REPROBE_DELAY
					goal
				}
			}
		} else {
			goal
		}
		val to = effectiveGoal.subtract(pos)
		val dist = to.length()

		val desired = if (dist < ARRIVAL) {
			Vec3.ZERO
		} else {
			val mag = min(e.speedLimit, dist * movCfg.followGain)
			to.scale(mag / dist)
		}

		var dm = e.deltaMovement.add(desired.subtract(e.deltaMovement).scale(movCfg.accel))

		val recovering = recoveryWaypoint != null

		if (!recovering) {
			val floorY = floorBelow()
			if (floorY != null) {
				val clearance = e.y - floorY
				if (clearance < GROUND_CLEARANCE) {
					val lift = GROUND_LIFT_ACCEL * (1.0 - (clearance / GROUND_CLEARANCE))
					dm = Vec3(dm.x, max(dm.y, 0.0) + lift, dm.z)
				}
			}

			// Ceiling avoidance: push down when too close to overhead blocks.
			val ceilY = ceilingAbove()
			if (ceilY != null) {
				val gap = ceilY - (e.y + e.bbHeight)
				if (gap < CEILING_CLEARANCE) {
					val pushDown = CEILING_PUSH_ACCEL * (1.0 - (gap / CEILING_CLEARANCE))
					dm = Vec3(dm.x, min(dm.y, 0.0) - pushDown, dm.z)
				}
			}

			dm = applyOwnerAvoidance(dm)
			dm = applyWallAvoidance(dm)
			dm = applyProjectileDodge(dm)
			dm = applyMobAvoidance(dm)
		}

		val h = sqrt((dm.x * dm.x) + (dm.z * dm.z))
		if (h > e.speedLimit) dm = Vec3((dm.x / h) * e.speedLimit, dm.y, (dm.z / h) * e.speedLimit)
		dm = Vec3(dm.x, dm.y.coerceIn(-movCfg.maxVertical, movCfg.maxVertical), dm.z)

		e.deltaMovement = dm
		e.move(net.minecraft.world.entity.MoverType.SELF, dm)
	}

	/** Safety spring: bounce away from floor or ceiling after collision. */
	fun postFlightCollisions() {
		val dm = e.deltaMovement
		if (e.verticalCollision && dm.y <= 0.0) {
			// Hit floor: bounce up.
			e.deltaMovement = Vec3(dm.x, BOUNCE_UP_SPEED, dm.z)
		} else if (e.verticalCollision && dm.y >= 0.0) {
			// Hit ceiling: push down.
			e.deltaMovement = Vec3(dm.x, -BOUNCE_UP_SPEED, dm.z)
		}
	}

	// ---------------------------------------------------------------------
	// Private helpers
	// ---------------------------------------------------------------------

	private fun hoverInPlace() {
		var dm = e.deltaMovement.scale(0.8)
		val floorY = floorBelow()
		if (floorY != null && ((e.y - floorY) < GROUND_CLEARANCE)) {
			dm = Vec3(dm.x, max(dm.y, 0.0) + GROUND_LIFT_ACCEL, dm.z)
		}
		val ceilY = ceilingAbove()
		if (ceilY != null) {
			val gap = ceilY - (e.y + e.bbHeight)
			if (gap < CEILING_CLEARANCE) {
				dm = Vec3(dm.x, min(dm.y, 0.0) - CEILING_PUSH_ACCEL, dm.z)
			}
		}
		e.deltaMovement = dm
		e.move(net.minecraft.world.entity.MoverType.SELF, dm)
	}

	private fun floorBelow(): Double? {
		val start = Vec3(e.x, e.y, e.z)
		val end = Vec3(e.x, e.y - GROUND_PROBE_DISTANCE, e.z)
		val hit = e.level().clip(
			ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e),
		)
		if (hit.type != HitResult.Type.BLOCK) return null
		return hit.location.y
	}

	/** Returns the Y of the nearest solid block above the companion, or null if none within probe range. */
	private fun ceilingAbove(): Double? {
		val start = Vec3(e.x, e.y + e.bbHeight, e.z)
		val end = Vec3(e.x, e.y + e.bbHeight + CEILING_PROBE_DISTANCE, e.z)
		val hit = e.level().clip(
			ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e),
		)
		if (hit.type != HitResult.Type.BLOCK) return null
		return hit.location.y
	}

	/**
	 * Returns the height of the open space above [pos] (blocks until solid), up to [maxCheck].
	 * Used to score escape directions and door exits by vertical clearance.
	 */
	private fun verticalClearance(pos: Vec3, maxCheck: Int = 4): Int {
		val level = e.level()
		val base = BlockPos.containing(pos)
		for (i in 1..maxCheck) {
			if (level.getBlockState(base.above(i)).isSolidRender) return i - 1
		}
		return maxCheck
	}

	// ---------------------------------------------------------------------
	// Multi-directional escape probe
	// ---------------------------------------------------------------------

	private fun probeEscapeDirection(goal: Vec3): Vec3? {
		val pos = e.position()
		val level = e.level()

		// Door exit has priority during stuck recovery.
		if (doorScanCooldown <= 0) {
			val doorExit = findDoorExit()
			if (doorExit != null) {
				doorScanCooldown = DOOR_SCAN_COOLDOWN
				return doorExit
			}
		}
		doorScanCooldown--

		// Pre-check: is there a ceiling nearby? Penalize upward probes if so.
		val ceilY = ceilingAbove()
		val nearCeiling = ceilY != null && (ceilY - (e.y + e.bbHeight)) < CEILING_CLEARANCE * 2

		var best: Vec3? = null
		var bestScore = Double.NEGATIVE_INFINITY

		for ((idx, dir) in ESCAPE_DIRS.withIndex()) {
			if (idx in failedDirections) continue

			val probe = pos.add(dir.scale(PROBE_REACH))
			val probeBlock = BlockPos.containing(probe)

			if (level.getBlockState(probeBlock).isSolidRender) {
				val beyond = pos.add(dir.scale(PROBE_REACH * 2))
				val beyondBlock = BlockPos.containing(beyond)
				if (level.getBlockState(beyondBlock).isSolidRender) {
					failedDirections.add(idx)
					continue
				}
			}

			val openness = measureOpenness(probe)
			val vSpace = verticalClearance(probe, maxCheck = 3)
			val dirToGoal = goal.subtract(pos).normalize()
			val alignment = dir.dot(dirToGoal)

			// Penalize upward probes when near ceiling; penalize downward probes when near floor.
			var verticalPenalty = 0.0
			if (nearCeiling && dir.y > 0) verticalPenalty = NEAR_CEILING_PENALTY
			val floorY = floorBelow()
			val nearFloor = floorY != null && (e.y - floorY) < GROUND_CLEARANCE * 2
			if (nearFloor && dir.y < 0) verticalPenalty = NEAR_FLOOR_PENALTY

			val score = (openness * OPENNESS_WEIGHT) +
				(vSpace * VERTICAL_SPACE_WEIGHT) +
				(alignment * ALIGNMENT_WEIGHT) -
				(probe.distanceTo(pos) * DISTANCE_PENALTY) -
				verticalPenalty

			if (score > bestScore) {
				bestScore = score
				best = probe
			}
		}

		if (best != null && bestScore < MIN_ESCAPE_SCORE) return null
		return best
	}

	private fun measureOpenness(pos: Vec3): Int {
		val center = BlockPos.containing(pos)
		var count = 0
		for (dx in -1..1) {
			for (dy in -1..1) {
				for (dz in -1..1) {
					if (dx == 0 && dy == 0 && dz == 0) continue
					if (!e.level().getBlockState(center.offset(dx, dy, dz)).isSolidRender) count++
				}
			}
		}
		return count
	}

	private fun findDoorExit(): Vec3? {
		val level = e.level()
		val center = e.blockPosition()
		val r = DOOR_SCAN_RADIUS
		val min = center.offset(-r, -r, -r)
		val max = center.offset(r, r, r)

		for (pos in BlockPos.betweenClosed(min, max)) {
			val block = level.getBlockState(pos).block
			if (block !in DOOR_BLOCKS) continue
			for (d in HORIZONTAL_DIRECTIONS) {
				val adj = pos.relative(d)
				if (level.getBlockState(adj).isAir) {
					val exitPos = Vec3(adj.x + 0.5, adj.y + 0.5, adj.z + 0.5)
					// Check that there's enough vertical space above the exit (at least 2 blocks).
					val clearance = verticalClearance(exitPos, maxCheck = 3)
					if (clearance >= DOOR_MIN_CLEARANCE) {
						return exitPos
					}
				}
			}
		}
		return null
	}

	/**
	 * Opens a nearby closed wooden door, but only when it actually blocks the
	 * companion's path to its flight target AND the companion is stuck (not
	 * making progress toward the goal). This prevents opening doors the
	 * companion is merely hovering near. Throttled to avoid spamming.
	 */
	private fun openNearbyDoors() {
		if (doorOpenCooldown > 0) {
			doorOpenCooldown--
			return
		}
		doorOpenCooldown = DOOR_OPEN_INTERVAL

		val level = e.level()
		val goal = e.flightTarget ?: return
		val center = e.blockPosition()
		val r = DOOR_OPEN_RADIUS

		// Only open a door when we're actually blocked from reaching the goal.
		val blocked = stuckTicks >= DOOR_STUCK_THRESHOLD

		for (pos in BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (!DoorBlock.isWoodenDoor(level, pos)) continue
			val state = level.getBlockState(pos)
			if (state.getValue(DoorBlock.OPEN)) continue

			// The door must be between us and the goal (roughly in the direction of travel).
			val doorCenter = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
			val toDoor = doorCenter.subtract(e.position())
			val distToDoor = toDoor.length()
			if (distToDoor < 1e-3) continue
			val toGoal = goal.subtract(e.position())
			val distToGoal = toGoal.length()
			if (distToGoal < 1e-3) continue
			val inPath = distToDoor < distToGoal &&
				toGoal.normalize().dot(toDoor.normalize()) > DOOR_PATH_DOT

			if (blocked && inPath) {
				(state.block as DoorBlock).setOpen(e, level, state, pos, true)
				return
			}
		}
	}

	private fun applyOwnerAvoidance(dm: Vec3): Vec3 {
		val owner = e.ownerPlayer() ?: return dm
		val dx = e.x - owner.x
		val dz = e.z - owner.z
		val distSq = (dx * dx) + (dz * dz)

		var result = dm

		// Horizontal avoidance when close to the owner.
		if (distSq in (1e-4..<OWNER_AVOID_RADIUS_SQ)) {
			val dist = sqrt(distSq)
			val nx = dx / dist
			val nz = dz / dist
			result = result.add(Vec3(nx * AVOID_FORCE, 0.0, nz * AVOID_FORCE))
		}

		// While following, descend back to the follow height if we climbed too high
		// (e.g. to let the owner pass in a narrow corridor).
		if (!e.isAttacking && !e.isExploring) {
			val followHeight = owner.y + FOLLOW_HEIGHT_REFERENCE
			if (e.y > followHeight + DESCEND_MARGIN) {
				val excess = e.y - (followHeight + DESCEND_MARGIN)
				result = Vec3(result.x, result.y - (excess * DESCEND_FORCE), result.z)

				// If a floor is close below (e.g. a roof), we can't descend here —
				// move horizontally toward the owner to get clear of the obstruction.
				val floorY = floorBelow()
				if (floorY != null && (e.y - floorY) < DESCEND_BLOCKED_DIST) {
					val dist = sqrt(distSq)
					if (dist > 1e-3) {
						val nx = dx / dist
						val nz = dz / dist
						// Toward the owner (opposite of the avoidance direction).
						result = Vec3(result.x - (nx * ROOF_CLEAR_FORCE), result.y, result.z - (nz * ROOF_CLEAR_FORCE))
					}
				}
			}
		}

		return result
	}

	private fun applyWallAvoidance(dm: Vec3): Vec3 {
		val hlen = sqrt((dm.x * dm.x) + (dm.z * dm.z))
		val hasHorizontal = hlen > 1e-4
		val hasVertical = abs(dm.y) > 1e-4
		if (!hasHorizontal && !hasVertical) return dm

		var result = dm

		// Horizontal wall lookahead.
		if (hasHorizontal) {
			val look = Vec3((dm.x / hlen) * WALL_LOOKAHEAD, 0.0, (dm.z / hlen) * WALL_LOOKAHEAD)
			val start = Vec3(e.x, e.y, e.z)
			val end = start.add(look)
			val hit = e.level().clip(
				ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e),
			)
			if (hit.type == HitResult.Type.BLOCK) {
				val n = hit.direction.step()
				val into = (result.x * n.x) + (result.z * n.z)
				if (into < 0) {
					result = Vec3(result.x - (n.x * into), result.y, result.z - (n.z * into))
					// Add upward bias when hitting a wall (helps climb over obstacles),
					// unless the owner is close — avoids climbing up in narrow corridors.
					val owner = e.ownerPlayer()
					val ownerClose = owner != null && e.distanceToSqr(owner) < OWNER_AVOID_RADIUS_SQ
					val ceilY = ceilingAbove()
					val nearCeiling = ceilY != null && (ceilY - (e.y + e.bbHeight)) < CLIMB_CEILING_LIMIT
					if (!ownerClose && !nearCeiling) {
						result = Vec3(result.x, result.y + WALL_CLIMB_BIAS, result.z)
					}
				}
			}
		}

		// Vertical lookahead: detect ceiling/floor when moving upward/downward.
		if (hasVertical) {
			val probeDir = if (dm.y > 0) Vec3(0.0, WALL_LOOKAHEAD, 0.0) else Vec3(0.0, -WALL_LOOKAHEAD, 0.0)
			val start = Vec3(e.x, e.y + e.bbHeight * 0.5, e.z)
			val end = start.add(probeDir)
			val hit = e.level().clip(
				ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e),
			)
			if (hit.type == HitResult.Type.BLOCK) {
				// Kill the vertical component heading into the solid.
				if (dm.y > 0 && hit.direction == Direction.DOWN) {
					result = Vec3(result.x, min(result.y, 0.0), result.z)
				} else if (dm.y < 0 && hit.direction == Direction.UP) {
					result = Vec3(result.x, max(result.y, 0.0), result.z)
				}
			}
		}

		return result
	}

	/**
	 * Detects hostile projectiles heading toward the companion and applies
	 * a perpendicular dodge force to avoid them.
	 */
	private fun applyProjectileDodge(dm: Vec3): Vec3 {
		val level = e.level()
		val pos = e.position()
		var dodge = Vec3.ZERO

		val nearbyEntities = level.getEntitiesOfClass(
			Entity::class.java,
			e.boundingBox.inflate(DODGE_SCAN_RADIUS),
		) { it is Projectile && it.owner !== e }

		for (proj in nearbyEntities) {
			val projVel = proj.deltaMovement
			if (projVel.lengthSqr() < 0.01) continue

			// Vector from projectile to companion.
			val toCompanion = pos.subtract(proj.position())
			val dist = toCompanion.length()
			if (dist > DODGE_SCAN_RADIUS || dist < 0.5) continue

			// Check if projectile is heading toward us (dot > 0 = approaching).
			val approachDot = projVel.normalize().dot(toCompanion.normalize())
			if (approachDot < DODGE_APPROACH_THRESHOLD) continue

			// Time to impact estimate (simplified).
			val closingSpeed = projVel.length() * approachDot
			val tti = dist / closingSpeed
			if (tti > DODGE_TTI_MAX) continue

			// Perpendicular dodge: cross product of velocity and up gives a sideways vector.
			val perp = projVel.cross(toCompanion).normalize()
			// Alternate dodge direction based on tick count for unpredictability.
			val sign = if (e.tickCount % 2 == 0) 1.0 else -1.0
			val strength = DODGE_FORCE * (1.0 - (tti / DODGE_TTI_MAX)).coerceAtLeast(0.3)
			dodge = dodge.add(perp.scale(sign * strength))
		}

		return if (dodge.lengthSqr() > 1e-6) dm.add(dodge) else dm
	}

	/**
	 * Pushes the companion away from nearby hostile mobs (melee attackers that
	 * swarm it during combat). Applies a dodge force to the velocity so the
	 * companion keeps distance from every threat, not just its current target.
	 */
	private fun applyMobAvoidance(dm: Vec3): Vec3 {
		val level = e.level()
		val pos = e.position()
		var avoidance = Vec3.ZERO

		// `Enemy` covers Monster, Slime, MagmaCube, Phantom, etc. — not just Monster.
		val nearby = level.getEntitiesOfClass(
			Mob::class.java,
			e.boundingBox.inflate(MOB_AVOID_RADIUS),
		) { it.isAlive && it is Enemy && it.distanceToSqr(e) < MOB_AVOID_RADIUS_SQ }

		for (mob in nearby) {
			val toCompanion = pos.subtract(mob.position())
			val dist = toCompanion.length()
			if (dist < 1e-3) continue
			val strength = (MOB_AVOID_RADIUS - dist) / MOB_AVOID_RADIUS
			avoidance = avoidance.add(toCompanion.normalize().scale(strength))
		}
		if (avoidance.lengthSqr() > 1e-6) {
			val len = avoidance.length()
			avoidance = avoidance.scale(1.0 / len)  // normalize
		}

		return if (avoidance.lengthSqr() > 1e-6) dm.add(avoidance.scale(MOB_AVOID_FORCE)) else dm
	}

	companion object {
		// Flight physics
		const val ARRIVAL = 0.05
		private const val GROUND_PROBE_DISTANCE = 3.0
		private const val GROUND_CLEARANCE = 1.1
		private const val GROUND_LIFT_ACCEL = 0.04
		private const val BOUNCE_UP_SPEED = 0.09

		// Ceiling avoidance
		private const val CEILING_PROBE_DISTANCE = 2.5
		private const val CEILING_CLEARANCE = 0.8
		private const val CEILING_PUSH_ACCEL = 0.04

		// Owner avoidance
		private const val OWNER_AVOID_RADIUS_SQ = 5.76
		private const val AVOID_FORCE = 0.06

		// Descend back to follow height (narrow-corridor / roof-stuck fix)
		private const val FOLLOW_HEIGHT_REFERENCE = 1.5
		private const val DESCEND_MARGIN = 0.3
		private const val DESCEND_FORCE = 0.15
		private const val DESCEND_BLOCKED_DIST = 2.0
		private const val ROOF_CLEAR_FORCE = 0.15

		// Wall avoidance
		private const val WALL_LOOKAHEAD = 1.4
		private const val WALL_CLIMB_BIAS = 0.03
		/** Max gap (blocks) between companion top and ceiling below which climb bias is disabled. */
		private const val CLIMB_CEILING_LIMIT = 2.0

		// Projectile dodge
		private const val DODGE_SCAN_RADIUS = 8.0
		private const val DODGE_APPROACH_THRESHOLD = 0.5
		private const val DODGE_TTI_MAX = 40.0
		private const val DODGE_FORCE = 0.15

		// Hostile-mob avoidance (melee attackers)
		private const val MOB_AVOID_RADIUS = 3.0
		private const val MOB_AVOID_RADIUS_SQ = MOB_AVOID_RADIUS * MOB_AVOID_RADIUS
		private const val MOB_AVOID_FORCE = 0.12

		// Stuck recovery
		private const val STUCK_THRESHOLD = 30
		private const val HARD_STUCK_THRESHOLD = 120
		private const val REPROBE_DELAY = 8
		private const val RECOVERY_DONE_DIST = 2.0
		private const val WP_ARRIVAL = 1.2
		private const val PROBE_REACH = 3.0
		private const val DOOR_SCAN_RADIUS = 4
		private const val DOOR_SCAN_COOLDOWN = 40
		private const val DOOR_MIN_CLEARANCE = 2
		private const val DOOR_OPEN_RADIUS = 2
		private const val DOOR_OPEN_INTERVAL = 20
		/** Ticks of being stuck before the companion opens a blocking door. */
		private const val DOOR_STUCK_THRESHOLD = 10
		/** Min dot product for a door to count as "in the path" to the goal. */
		private const val DOOR_PATH_DOT = 0.5
		private const val OPENNESS_WEIGHT = 2.0
		private const val VERTICAL_SPACE_WEIGHT = 1.0
		private const val ALIGNMENT_WEIGHT = 1.5
		private const val DISTANCE_PENALTY = 0.05
		private const val MIN_ESCAPE_SCORE = -2.0
		private const val NEAR_CEILING_PENALTY = 3.0
		private const val NEAR_FLOOR_PENALTY = 2.0

		private val ESCAPE_DIRS = arrayOf(
			Vec3( 1.0,  0.0,  0.0),
			Vec3(-1.0,  0.0,  0.0),
			Vec3( 0.0,  0.0,  1.0),
			Vec3( 0.0,  0.0, -1.0),
			Vec3( 0.0,  1.0,  0.0),
			Vec3( 0.0, -1.0,  0.0),
		)
		private val HORIZONTAL_DIRECTIONS = arrayOf(
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
		)

		val DOOR_BLOCKS = setOf(
			Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
			Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.MANGROVE_DOOR, Blocks.CHERRY_DOOR,
			Blocks.BAMBOO_DOOR, Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR,
			Blocks.IRON_DOOR,
		)
	}
}
