package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel

/**
 * Handles companion health regeneration, owner healing, and health syncing to the core item.
 */
class CompanionHealth(private val e: CompanionEntity) {

	private var healParticleTimer: Int = 0
	/**
	 * Tick at which each spawned healing particle is expected to have faded out.
	 * Used to cap the number of simultaneously-visible particles at [HEAL_PARTICLE_MAX].
	 */
	private val healParticleExpiry = ArrayDeque<Int>()

	/** Applies the maximum health for the given level and clamps current health. */
	fun syncMaxHealth(level: Int) {
		val attribute = e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) ?: return
		attribute.baseValue = CompanionData.maxHealth(level).toDouble()
		e.setCompanionHealth(e.health.coerceIn(CompanionData.MIN_HEALTH, e.maxHealth))
	}

	/** Regenerates health each tick if below max. */
	fun regenerate() {
		if (e.health < e.maxHealth) {
			val cfg = CompanionConfig.get().health
			val scale = 1f + (e.readLevel() - 1) * cfg.regenLevelScale
			e.setCompanionHealth((e.health + cfg.regenPerTick * scale).coerceAtMost(e.maxHealth))
		}
	}

	/**
	 * Heals the owner when the companion is nearby and not in combat.
	 * Requires level >= healUnlockLevel. Heal rate scales with level.
	 */
	fun healOwner() {
		val owner = e.ownerPlayer() ?: return
		val lvl = e.readLevel()
		val cfg = CompanionConfig.get().healing
		if (lvl < cfg.unlockLevel) return
		if (owner.health >= owner.maxHealth) return
		if (e.currentState() == BrainState.ATTACK) return

		val distSq = e.distanceToSqr(owner)
		val rangeSq = cfg.range * cfg.range
		if (distSq > rangeSq) return

		val healPerTick = (cfg.basePerTick + ((lvl - cfg.unlockLevel) * cfg.perLevelAbove)).coerceAtMost(cfg.maxPerTick)
		val newHealth = (owner.health + healPerTick).coerceAtMost(owner.maxHealth)
		if (newHealth > owner.health) {
			owner.health = newHealth

			// Drop particles that have already faded out.
			val now = e.tickCount
			while (healParticleExpiry.isNotEmpty() && healParticleExpiry.first() <= now) {
				healParticleExpiry.removeFirst()
			}

			// Spawn a new particle at a steady interval, but never more than
			// HEAL_PARTICLE_MAX visible at once — new ones just add to existing ones.
			healParticleTimer--
			if (healParticleTimer <= 0 && healParticleExpiry.size < HEAL_PARTICLE_MAX) {
				healParticleTimer = HEAL_PARTICLE_INTERVAL
				if (e.level() is ServerLevel) {
					(e.level() as ServerLevel).sendParticles(
						ParticleTypes.HAPPY_VILLAGER,
						owner.x, owner.y + 0.4, owner.z,
						1, 0.3, 0.2, 0.3, 0.0,
					)
				}
				healParticleExpiry.addLast(now + HEAL_PARTICLE_LIFETIME)
			}
		}
	}

	/** Pushes the live health to the core item every tick for smooth bar updates. */
	fun syncHealthToCore() {
		val owner = e.ownerPlayer() ?: return
		CoreSlotManager.syncHealthToCore(owner, e.health)
	}

	companion object {
		/** Maximum healing particles visible at the owner at once. */
		private const val HEAL_PARTICLE_MAX = 5
		/** Ticks between healing particles (0.4 s at 20 TPS). */
		private const val HEAL_PARTICLE_INTERVAL = 8
		/** Approximate lifetime of a HAPPY_VILLAGER particle in ticks (~1.5 s). */
		private const val HEAL_PARTICLE_LIFETIME = 30
	}
}
