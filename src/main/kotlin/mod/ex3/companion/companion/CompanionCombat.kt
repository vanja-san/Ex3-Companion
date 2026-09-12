package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

/**
 * Handles combat: target finding, beam firing, target tracking, and disengage logic.
 * Called by [CompanionBrain] during the ATTACK state.
 */
class CompanionCombat(private val e: CompanionEntity) {

	var combatTarget: LivingEntity? = null
		private set
	private var attackCooldown: Int = 0
	private var noTargetTicks: Int = 0

	/** Ticks since the target last took damage from us (for stale-target detection). */
	private var staleTargetTicks: Int = 0
	/** HP of the target when we last hit it; used to detect healing. */
	private var lastTargetHealth: Float = 0f
	/** Countdown to next re-target evaluation. */
	private var retargetTimer: Int = 0

	/** Enters ATTACK when a hostile mob is near the owner. Returns true if combat started. */
	fun tryStartCombat(owner: Player): Boolean {
		val target = findTarget(owner) ?: return false
		combatTarget = target
		// Keep the existing attackCooldown instead of resetting it to 0. Slimes split
		// into smaller slimes on death, and each new slime is a fresh target — resetting
		// the cooldown here made the companion fire instantly at every new slime, so the
		// smaller (faster-dying) the slime, the faster the attack rate appeared.
		noTargetTicks = 0
		staleTargetTicks = 0
		lastTargetHealth = target.health
		retargetTimer = RETARGET_INTERVAL
		e.setState(BrainState.ATTACK)
		e.sounds.playEmotion(Emotion.ANGRY)
		return true
	}

	/**
	 * Ticks the combat state: validates target, fires beam, checks disengage conditions.
	 * Returns a [CompanionBrain.CombatTickResult] with flight target, speed, and look direction.
	 */
	fun tick(owner: Player, currentMoodPulse: Float): CombatTickResult {
		val target = combatTarget
		val cfg = CompanionConfig.get().combat
		val movCfg = CompanionConfig.get().movement
		val exploreCfg = CompanionConfig.get().explore
		val abortDistSq = exploreCfg.abortDistance * exploreCfg.abortDistance

		val deadOrGone = target == null || !target.isAlive || target.isRemoved
		val tooFar = target != null && e.distanceToSqr(target) > cfg.abortDistance * cfg.abortDistance
		val ownerTooFar = e.distanceToSqr(owner) > cfg.ownerAbortDistance * cfg.ownerAbortDistance

		val notAttacking = target != null && !isAttackingOwner(target, owner) && !isTargetingCompanion(target)
		if (notAttacking) {
			noTargetTicks++
			if (noTargetTicks >= cfg.noTargetTimeout) {
				stopCombat()
				return CombatTickResult(
					disengaged = true,
					flightTarget = e.flightTarget,
					speedLimit = e.speedLimit,
					lookTarget = null,
					moodPulse = 1f,
				)
			}
		} else {
			noTargetTicks = 0
		}

		// --- Stale target detection ---
		// If the target is alive but we haven't dealt damage in a while, or it healed
		// above the last recorded HP, consider it stale (e.g. witch self-healing).
		if (target != null && target.isAlive) {
			if (target.health >= lastTargetHealth) {
				staleTargetTicks++
			} else {
				staleTargetTicks = 0
			}
			lastTargetHealth = target.health

			if (staleTargetTicks >= cfg.staleTargetTimeout) {
				stopCombat()
				return CombatTickResult(
					disengaged = true,
					flightTarget = e.flightTarget,
					speedLimit = e.speedLimit,
					lookTarget = null,
					moodPulse = 1f,
				)
			}
		}

		// --- Periodic re-targeting ---
		retargetTimer--
		if (retargetTimer <= 0) {
			retargetTimer = RETARGET_INTERVAL
			val better = findBetterTarget(owner, target)
			if (better != null && better !== target) {
				combatTarget = better
				staleTargetTicks = 0
				lastTargetHealth = better.health
				// Don't reset attackCooldown — keep firing on the normal rhythm.ackCooldown — keep firing on the normal rhythm.
			}
		}

		if (deadOrGone || tooFar || ownerTooFar) {
			stopCombat()
			return CombatTickResult(
				disengaged = true,
				flightTarget = e.flightTarget,
				speedLimit = e.speedLimit,
				lookTarget = null,
				moodPulse = 1f,
			)
		}

		// Hover at a standoff distance from the target while shooting (ranged attacker).
		val hoverY = minOf(
			target!!.y + target.bbHeight + 0.8,
			owner.eyePosition.y + ATTACK_HOVER_MAX_ABOVE_OWNER,
		)
		val attackTarget = standoffPosition(target, hoverY, cfg.preferredRange)

		val finalTarget = if (e.isPositionPassable(attackTarget) && e.canSeePosition(attackTarget)) {
			attackTarget
		} else {
			val fallback = Vec3(
				e.x + (attackTarget.x - e.x) * 0.5,
				hoverY,
				e.z + (attackTarget.z - e.z) * 0.5,
			)
			if (e.isPositionPassable(fallback)) fallback else attackTarget
		}

		val speed = if (e.position().distanceToSqr(finalTarget) > CATCH_UP_DISTANCE_SQ) movCfg.catchUpSpeed else movCfg.normalSpeed

		attackCooldown--
		val fireRangeSq = cfg.fireRange * cfg.fireRange
		var mood = currentMoodPulse
		if (attackCooldown <= 0 && e.distanceToSqr(target) < fireRangeSq && e.hasLineOfSight(target)) {
			fireBeam(target)
			attackCooldown = CompanionData.attackIntervalTicks(e.readLevel())
			mood = 1f
		}

		return CombatTickResult(
			disengaged = false,
			flightTarget = finalTarget,
			speedLimit = speed,
			lookTarget = target.eyePosition,
			moodPulse = mood,
		)
	}

	/**
	 * Computes a hover position [range] blocks horizontally from the target, on the
	 * far side from the companion. If the companion is closer than [range], this
	 * pushes it back out to the standoff distance — ranged-attacker behavior.
	 */
	private fun standoffPosition(target: LivingEntity, hoverY: Double, range: Double): Vec3 {
		val dx = e.x - target.x
		val dz = e.z - target.z
		val horizontalDist = sqrt(dx * dx + dz * dz)
		val dirX = if (horizontalDist > 0.001) dx / horizontalDist else 1.0
		val dirZ = if (horizontalDist > 0.001) dz / horizontalDist else 0.0
		return Vec3(target.x + dirX * range, hoverY, target.z + dirZ * range)
	}

	private fun fireBeam(target: LivingEntity) {
		val level = (e.level() as? ServerLevel) ?: return

		val from = e.position().add(0.0, CompanionEntity.EYE_HEIGHT, 0.0)
		val to = target.position().add(0.0, target.bbHeight * 0.6, 0.0)
		val steps = (from.distanceTo(to) * BEAM_PARTICLES_PER_BLOCK).toInt().coerceIn(BEAM_MIN_STEPS, BEAM_MAX_STEPS)

		for (i in 0..steps) {
			val p = from.lerp(to, i.toDouble() / steps)
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0)
		}
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW, to.x, to.y, to.z, 4, 0.15, 0.15, 0.15, 0.02)
		e.sounds.playBeep(pitch = 1.5f)

		// The target may have died between the line-of-sight check and this call.
		if (!target.isAlive) return

		val lvl = e.readLevel()
		val damage = CompanionData.damage(lvl)
		val owner = e.ownerPlayer()
		val source = if (owner != null) {
			level.damageSources().playerAttack(owner)
		} else {
			level.damageSources().mobAttack(e)
		}
		target.hurtServer(level, source, damage)

		// Track health so stale-target detection can see if the target healed back.
		if (target.isAlive) {
			lastTargetHealth = target.health
			staleTargetTicks = 0
		}

		if (target is Mob) target.target = e

		if (!target.isAlive) awardKillXp()
	}

	private fun awardKillXp() {
		val owner = e.ownerPlayer() ?: return
		CoreSlotManager.addCompanionXp(owner, CompanionConfig.get().combat.xpPerKill)
	}

	private fun findTarget(owner: Player): LivingEntity? {
		val cfg = CompanionConfig.get().combat
		val mode = cfg.combatMode

		return when (mode) {
			CombatMode.DEFENDER -> findDefenderTarget(owner, cfg.searchRadius)
			CombatMode.AGGRESSIVE -> findAggressiveTarget(owner, cfg.searchRadius * 2.0)
			CombatMode.STRATEGIC -> findStrategicTarget(owner, cfg.searchRadius * 1.5)
		}
	}

	/**
	 * Returns hostile mobs near [center] within [radius]. Uses the `Enemy` marker
	 * interface so slimes, magma cubes, phantoms, etc. are included — not just
	 * `Monster` subclasses (slimes extend `Mob` and implement `Enemy`).
	 */
	private fun hostileMobs(center: Entity, radius: Double, predicate: (Mob) -> Boolean): List<Mob> =
		e.level().getEntitiesOfClass(
			Mob::class.java,
			center.boundingBox.inflate(radius),
		) { it.isAlive && it is Enemy && predicate(it) }

	/** Defender mode: only fight mobs actively attacking the owner or companion. */
	private fun findDefenderTarget(owner: Player, radius: Double): LivingEntity? =
		hostileMobs(owner, radius) {
			(isAttackingOwner(it, owner) || isTargetingCompanion(it)) && e.canSeePosition(it.eyePosition)
		}.minByOrNull { it.distanceToSqr(owner) }

	/** Aggressive mode: seek any nearby hostile, even if not targeting owner. */
	private fun findAggressiveTarget(owner: Player, radius: Double): LivingEntity? =
		hostileMobs(owner, radius) { e.canSeePosition(it.eyePosition) }
			.minByOrNull { it.distanceToSqr(e.position()) }

	/** Strategic mode: prioritize threats by type — avoid creepers, approach ranged mobs. */
	private fun findStrategicTarget(owner: Player, radius: Double): LivingEntity? {
		val candidates = hostileMobs(owner, radius) {
			(isAttackingOwner(it, owner) || isTargetingCompanion(it) || isNearOwner(it, owner, 8.0)) &&
				e.canSeePosition(it.eyePosition)
		}
		if (candidates.isEmpty()) return null

		return candidates.minBy { mob ->
			var score = mob.distanceToSqr(e.position())

			// Prefer targeting mobs that are close to the owner.
			if (isAttackingOwner(mob, owner)) score -= STRATEGIC_ATTACKING_BONUS

			// Creeper penalty: keep distance, don't prioritize.
			if (isCreeperType(mob)) score += STRATEGIC_CREEPER_PENALTY

			// Skeleton/ranged mob bonus: approach faster.
			if (isRangedMob(mob)) score -= STRATEGIC_RANGED_BONUS

			score
		}
	}

	private fun isNearOwner(entity: LivingEntity, owner: Player, distance: Double): Boolean =
		entity.distanceToSqr(owner) < distance * distance

	private fun isCreeperType(entity: LivingEntity): Boolean =
		EntityType.getKey(entity.type) == CREEPER_ID

	private fun isRangedMob(entity: LivingEntity): Boolean =
		EntityType.getKey(entity.type) in RANGED_MOB_IDS

	private fun isAttackingOwner(entity: LivingEntity, owner: Player): Boolean =
		(entity is Mob) && (entity.target == owner || entity == owner.lastHurtByMob)

	private fun isTargetingCompanion(entity: LivingEntity): Boolean =
		(entity is Mob) && entity.target == e

	/**
	 * During periodic re-targeting, find a higher-priority target than [current].
	 * Priority: mobs attacking owner > mobs attacking companion > nearest hostile.
	 * Only switches if the new target is strictly better (higher priority tier or
	 * same tier but significantly closer).
	 */
	private fun findBetterTarget(owner: Player, current: LivingEntity?): LivingEntity? {
		val cfg = CompanionConfig.get().combat
		val radius = cfg.searchRadius
		val candidates = hostileMobs(owner, radius) { e.canSeePosition(it.eyePosition) }
		if (candidates.isEmpty()) return null

		val currentPriority = if (current != null) targetPriority(current, owner) else -1
		var best = current
		var bestPriority = currentPriority
		var bestDistSq = current?.let { e.distanceToSqr(it) } ?: Double.MAX_VALUE

		for (mob in candidates) {
			val pri = targetPriority(mob, owner)
			val distSq = e.distanceToSqr(mob)

			// Strictly higher priority tier → always switch.
			// Same tier but ≥3× closer → switch.
			val dominated = when {
				pri > bestPriority -> true
				pri == bestPriority && best != null && distSq < bestDistSq * 0.11 -> true
				else -> false
			}
			if (dominated) {
				best = mob
				bestPriority = pri
				bestDistSq = distSq
			}
		}
		return best
	}

	/** Returns a priority tier for a target: 2 = attacking owner, 1 = attacking companion, 0 = other hostile. */
	private fun targetPriority(entity: LivingEntity, owner: Player): Int =
		when {
			isAttackingOwner(entity, owner) -> 2
			// A mob attacking the companion at melee range is an immediate threat —
			// treat it as high priority so the companion re-targets and backs off.
			isTargetingCompanion(entity) && entity.distanceToSqr(e) < MELEE_THREAT_DIST_SQ -> 2
			isTargetingCompanion(entity) -> 1
			else -> 0
		}

	fun stopCombat() {
		combatTarget = null
	}

	companion object {
		private const val ATTACK_HOVER_MAX_ABOVE_OWNER = 1.0
		private const val BEAM_PARTICLES_PER_BLOCK = 2.0
		private const val BEAM_MIN_STEPS = 6
		private const val BEAM_MAX_STEPS = 24
		private const val CATCH_UP_DISTANCE_SQ = 9.0
		/** Ticks between periodic re-target evaluations. */
		private const val RETARGET_INTERVAL = 40
		// Strategic mode scoring bonuses
		private const val STRATEGIC_ATTACKING_BONUS = 50.0
		private const val STRATEGIC_CREEPER_PENALTY = 80.0
		private const val STRATEGIC_RANGED_BONUS = 30.0
		/** Squared distance at which a mob attacking the companion counts as an immediate melee threat. */
		private const val MELEE_THREAT_DIST_SQ = 3.0 * 3.0

		/** Registry key of the creeper — used to avoid prioritizing it in strategic mode. */
		private val CREEPER_ID: Identifier = Identifier.fromNamespaceAndPath("minecraft", "creeper")

		/** Registry keys of mobs treated as ranged threats in strategic mode (approached faster). */
		private val RANGED_MOB_IDS = setOf(
			Identifier.fromNamespaceAndPath("minecraft", "skeleton"),
			Identifier.fromNamespaceAndPath("minecraft", "blaze"),
			Identifier.fromNamespaceAndPath("minecraft", "ghast"),
			Identifier.fromNamespaceAndPath("minecraft", "pillager"),
			Identifier.fromNamespaceAndPath("minecraft", "evoker"),
		)
	}
}

/** Result of a combat tick, returned to the brain for flight/look updates. */
data class CombatTickResult(
	val disengaged: Boolean,
	val flightTarget: Vec3?,
	val speedLimit: Double,
	val lookTarget: Vec3?,
	val moodPulse: Float,
)
