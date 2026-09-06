package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * Handles combat: target finding, beam firing, target tracking, and disengage logic.
 * Called by [CompanionBrain] during the ATTACK state.
 */
class CompanionCombat(private val e: CompanionEntity) {

	var combatTarget: LivingEntity? = null
		private set
	private var attackCooldown: Int = 0
	private var noTargetTicks: Int = 0

	/** Enters ATTACK when a hostile mob is near the owner. Returns true if combat started. */
	fun tryStartCombat(owner: Player): Boolean {
		val target = findTarget(owner) ?: return false
		combatTarget = target
		attackCooldown = 0
		noTargetTicks = 0
		e.setState(BrainState.ATTACK)
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
		val ownerTooFar = e.distanceToSqr(owner) > abortDistSq

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

		// Hover above the target while shooting
		val hoverY = minOf(
			target!!.y + target.bbHeight + 0.8,
			owner.eyePosition.y + ATTACK_HOVER_MAX_ABOVE_OWNER,
		)
		val attackTarget = Vec3(target.x, hoverY, target.z)

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

		val lvl = e.readLevel()
		val damage = CompanionData.damage(lvl)
		val owner = e.ownerPlayer()
		val source = if (owner != null) {
			level.damageSources().playerAttack(owner)
		} else {
			level.damageSources().mobAttack(e)
		}
		target.hurtServer(level, source, damage)

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

	/** Defender mode: only fight mobs actively attacking the owner or companion. */
	private fun findDefenderTarget(owner: Player, radius: Double): LivingEntity? =
		e.level().getEntitiesOfClass(
			Monster::class.java,
			owner.boundingBox.inflate(radius),
		) {
			it.isAlive && (isAttackingOwner(it, owner) || isTargetingCompanion(it)) &&
				e.canSeePosition(it.eyePosition)
		}.minByOrNull { it.distanceToSqr(owner) }

	/** Aggressive mode: seek any nearby hostile, even if not targeting owner. */
	private fun findAggressiveTarget(owner: Player, radius: Double): LivingEntity? =
		e.level().getEntitiesOfClass(
			Monster::class.java,
			owner.boundingBox.inflate(radius),
		) {
			it.isAlive && e.canSeePosition(it.eyePosition)
		}.minByOrNull { it.distanceToSqr(e.position()) }

	/** Strategic mode: prioritize threats by type — avoid creepers, approach ranged mobs. */
	private fun findStrategicTarget(owner: Player, radius: Double): LivingEntity? {
		val candidates = e.level().getEntitiesOfClass(
			Monster::class.java,
			owner.boundingBox.inflate(radius),
		) {
			it.isAlive && (isAttackingOwner(it, owner) || isTargetingCompanion(it) || isNearOwner(it, owner, 8.0)) &&
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
		entity.type.descriptionId.contains("creeper")

	private fun isRangedMob(entity: LivingEntity): Boolean {
		val id = entity.type.descriptionId
		return id.contains("skeleton") || id.contains("blaze") || id.contains("ghast") ||
			id.contains("pillager") || id.contains("evoker")
	}

	private fun isAttackingOwner(entity: LivingEntity, owner: Player): Boolean =
		(entity is Mob) && (entity.target == owner || entity == owner.lastHurtByMob)

	private fun isTargetingCompanion(entity: LivingEntity): Boolean =
		(entity is Mob) && entity.target == e

	fun stopCombat() {
		combatTarget = null
	}

	companion object {
		private const val ATTACK_HOVER_MAX_ABOVE_OWNER = 1.0
		private const val BEAM_PARTICLES_PER_BLOCK = 4.0
		private const val BEAM_MIN_STEPS = 6
		private const val BEAM_MAX_STEPS = 40
		private const val CATCH_UP_DISTANCE_SQ = 9.0
		// Strategic mode scoring bonuses
		private const val STRATEGIC_ATTACKING_BONUS = 50.0
		private const val STRATEGIC_CREEPER_PENALTY = 80.0
		private const val STRATEGIC_RANGED_BONUS = 30.0
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
