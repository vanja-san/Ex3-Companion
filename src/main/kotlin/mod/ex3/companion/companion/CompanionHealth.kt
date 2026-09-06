package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel

/**
 * Handles companion health regeneration, owner healing, and health syncing to the core item.
 */
class CompanionHealth(private val e: CompanionEntity) {

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

		val healPerTick = cfg.basePerTick + ((lvl - cfg.unlockLevel) * cfg.perLevelAbove)
		val newHealth = (owner.health + healPerTick).coerceAtMost(owner.maxHealth)
		if (newHealth > owner.health) {
			owner.health = newHealth
			if (e.level() is ServerLevel) {
				(e.level() as ServerLevel).sendParticles(
					ParticleTypes.HAPPY_VILLAGER,
					owner.x, owner.eyePosition.y - 0.2, owner.z,
					2, 0.3, 0.3, 0.3, 0.0,
				)
			}
		}
	}

	/** Pushes the live health to the core item every tick for smooth bar updates. */
	fun syncHealthToCore() {
		val owner = e.ownerPlayer() ?: return
		CoreSlotManager.syncHealthToCore(owner, e.health)
	}
}
