package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import mod.ex3.companion.registry.ModComponents
import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * The companion entity: a floating glass cube with a glowing emerald core.
 *
 * Movement is fully custom and server-authoritative: vanilla [PathfinderMob]
 * movement is disabled by overriding [travel] as a no-op, and every tick the
 * server computes a velocity (steering + damping + safety biases) and applies it
 * via [move]. This avoids the vanilla `FlyingMoveControl` quirks.
 *
 * All logic is delegated to focused component classes:
 *  - [CompanionFlight] — flight physics, stuck detection, wall avoidance
 *  - [CompanionBrain] — state machine, follow/explore/idle decisions
 *  - [CompanionCombat] — target finding, beam firing, disengage
 *  - [CompanionHealth] — regeneration, owner healing, health sync
 *  - [CompanionSounds] — ambient sounds, beeps, particle effects
 */
class CompanionEntity(type: EntityType<CompanionEntity>, level: Level) : PathfinderMob(type, level) {

	// --- Components ---
	val flight: CompanionFlight
	val brain: CompanionBrain
	val combat: CompanionCombat
	val companionHealth: CompanionHealth
	val sounds: CompanionSounds

	/** Persistent exploration memory — loaded from core item, synced back periodically. */
	val memory: CompanionMemory = CompanionMemory()

	// --- Core state ---
	var ownerId: UUID? = null

	/** Current flight destination (follow target / explore POI). Set by brain, read by flight. */
	var flightTarget: Vec3? = null

	/** Current max horizontal speed (blocks/tick), set by the brain. */
	var speedLimit: Double = NORMAL_SPEED

	// --- Cached owner reference (avoids O(N) player list scan per call) ---
	private var cachedOwner: Player? = null
	private var cachedOwnerId: UUID? = null

	init {
		isNoGravity = true
		setComponent(ModComponents.ATTACKING, false)
		// Create components — order matters: combat before brain (brain references combat)
		flight = CompanionFlight(this)
		combat = CompanionCombat(this)
		brain = CompanionBrain(this, combat)
		companionHealth = CompanionHealth(this)
		sounds = CompanionSounds(this)
	}

	// Navigation is kept only because PathfinderMob requires it; never used.
	override fun createNavigation(level: Level): PathNavigation = FlyingPathNavigation(this, level)

	/** Vanilla movement is fully disabled: all motion is computed in [CompanionFlight]. */
	override fun travel(input: Vec3) {}

	override fun tick() {
		super.tick()
		if (!level().isClientSide) {
			fallDistance = 0.0
			brain.tick()         // decides flightTarget + speedLimit
			flight.tick()        // applies velocity
			flight.postFlightCollisions()
			companionHealth.regenerate()
			companionHealth.healOwner()
			companionHealth.syncHealthToCore()
			sounds.tickAmbientSounds()

			// Sync exploration memory to core item periodically.
			if (brain.needsMemorySync || tickCount % MEMORY_SYNC_INTERVAL == 0) {
				brain.needsMemorySync = false
				ownerPlayer()?.let { CoreSlotManager.syncMemoryToCore(it, memory) }
			}
		}
	}

	// ---------------------------------------------------------------------
	// Visibility helpers (used by multiple components)
	// ---------------------------------------------------------------------

	/** Returns true if there is a clear line of sight from this entity to [pos]. */
	fun canSeePosition(pos: Vec3): Boolean {
		val start = Vec3(x, y + EYE_HEIGHT, z)
		val hit = level().clip(
			ClipContext(start, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this),
		)
		return hit.type == HitResult.Type.MISS || hit.location.distanceToSqr(start) >= pos.distanceToSqr(start) - 0.5
	}

	/** Returns true if the block at [pos] is passable (not a solid render block). */
	fun isPositionPassable(pos: Vec3): Boolean =
		!level().getBlockState(BlockPos.containing(pos)).isSolidRender

	/** Public helper: the hover position next to [owner], used as initial spawn point. */
	fun computeSpawnPosition(owner: Player): Vec3 = brain.computeFollowTarget(owner)

	// ---------------------------------------------------------------------
	// Owner lookup
	// ---------------------------------------------------------------------

	fun ownerPlayer(): Player? {
		val id = ownerId ?: return null
		if (id == cachedOwnerId) {
			val cached = cachedOwner
			if (cached != null && cached.isAlive && !cached.isRemoved) return cached
			cachedOwner = null
			cachedOwnerId = null
		}
		// ServerLevel keeps an O(1) UUID → player map; the client fallback is a
		// linear scan, but the client only ever has a handful of players.
		val found = when (val lvl = level()) {
			is ServerLevel -> lvl.getPlayerByUUID(id)
			else -> lvl.players().firstOrNull { it.uuid == id }
		}
		cachedOwner = found
		cachedOwnerId = id
		return found
	}

	// ---------------------------------------------------------------------
	// Brain state access
	// ---------------------------------------------------------------------

	fun currentState(): BrainState =
		BrainState.entries.getOrElse(entityData[DATA_STATE].toInt()) { BrainState.FOLLOW }

	fun setState(state: BrainState) {
		entityData[DATA_STATE] = state.ordinal.toByte()
		setComponent(ModComponents.ATTACKING, state == BrainState.ATTACK)
	}

	override fun onSyncedDataUpdated(updatedItems: MutableList<SynchedEntityData.DataValue<*>>) {
		super.onSyncedDataUpdated(updatedItems)
		if (updatedItems.any { it.id == DATA_STATE.id }) {
			setComponent(ModComponents.ATTACKING, currentState() == BrainState.ATTACK)
		}
	}

	val isExploring: Boolean get() = currentState() == BrainState.EXPLORE
	val isAttacking: Boolean get() = currentState() == BrainState.ATTACK
	val moodLevel: Float get() = brain.moodPulse

	/** Glass color name: "clear" = transparent, or dye color name like "white", "cyan". Synched from server to client. */
	var glassColorName: String
		get() {
			val idx = entityData[DATA_GLASS_COLOR].toInt()
			return if (idx < 0) "clear" else DYE_COLOR_NAMES.getOrElse(idx) { "clear" }
		}
		set(value) {
			entityData[DATA_GLASS_COLOR] = glassColorIndex(value).toByte()
		}

	fun readLevel(): Int =
		ownerPlayer()?.let { CoreSlotManager.readCompanionLevel(it) } ?: 1

	// ---------------------------------------------------------------------
	// Delegation helpers (used by CoreSlotManager and components)
	// ---------------------------------------------------------------------

	/** Phase driving the gentle hover bobbing (read by the renderer too). */
	val bobPhase: Float get() = tickCount * BOB_SPEED

	fun setCompanionHealth(value: Float) {
		health = value
	}

	fun syncMaxHealth(level: Int) = companionHealth.syncMaxHealth(level)
	fun playDeathPoof() = sounds.playDeathPoof()
	fun playLevelUpFx() = sounds.playLevelUpFx()

	// ---------------------------------------------------------------------
	// Entity boilerplate
	// ---------------------------------------------------------------------

	override fun defineSynchedData(builder: SynchedEntityData.Builder) {
		super.defineSynchedData(builder)
		builder.define(DATA_STATE, BrainState.FOLLOW.ordinal.toByte())
		builder.define(DATA_GLASS_COLOR, (-1).toByte()) // -1 = clear glass (default)
	}

	override fun shouldBeSaved(): Boolean = false

	override fun isPickable(): Boolean {
		val owner = ownerPlayer() ?: return false
		return owner.isSecondaryUseActive
	}

	override fun canBeCollidedWith(other: net.minecraft.world.entity.Entity?): Boolean = false

	override fun isPushable(): Boolean = false

	override fun pushEntities() {}

	override fun push(entity: net.minecraft.world.entity.Entity) {}

	override fun causeFallDamage(fallDistance: Double, multiplier: Float, source: DamageSource): Boolean = false

	override fun playStepSound(pos: BlockPos, state: net.minecraft.world.level.block.state.BlockState) {}

	override fun dampensVibrations(): Boolean = true

	// Companion only takes damage from mob attacks, projectiles, explosions, and lava.
	override fun hurtServer(level: ServerLevel, source: DamageSource, damage: Float): Boolean {
		if (CompanionConfig.healthInvulnerable) return false
		if (!isAllowedDamageSource(source)) return false

		val hpAfter = (health - damage).coerceAtLeast(0f)
		val wouldKill = hpAfter <= 0f

		if (wouldKill) {
			val owner = ownerPlayer()
			if (owner != null) {
				CoreSlotManager.killCompanion(owner, this)
			}
			super.hurtServer(level, source, damage)
			return true
		}

		if (!super.hurtServer(level, source, damage)) return false
		return true
	}

	private fun isAllowedDamageSource(source: DamageSource): Boolean =
		source.`is`(DamageTypes.MOB_ATTACK) ||
			source.`is`(DamageTypes.MOB_ATTACK_NO_AGGRO) ||
			source.`is`(DamageTypes.MOB_PROJECTILE) ||
			source.`is`(DamageTypes.ARROW) ||
			source.`is`(DamageTypes.TRIDENT) ||
			source.`is`(DamageTypes.STING) ||
			source.`is`(DamageTypes.SPIT) ||
			source.`is`(DamageTypes.WIND_CHARGE) ||
			source.`is`(DamageTypes.FIREBALL) ||
			source.`is`(DamageTypes.WITHER_SKULL) ||
			source.`is`(DamageTypes.EXPLOSION) ||
			source.`is`(DamageTypes.PLAYER_EXPLOSION) ||
			source.`is`(DamageTypes.LAVA)

	override fun handlePortal() {}

	companion object {
		val DATA_STATE: EntityDataAccessor<Byte> =
			SynchedEntityData.defineId(CompanionEntity::class.java, EntityDataSerializers.BYTE)

		val DATA_GLASS_COLOR: EntityDataAccessor<Byte> =
			SynchedEntityData.defineId(CompanionEntity::class.java, EntityDataSerializers.BYTE)

		/** Ordered list of dye color names matching DyeColor ordinal (0–15). */
		val DYE_COLOR_NAMES: List<String> = DyeColor.entries.map { it.serializedName }

		/** Reverse lookup: color name → DyeColor ordinal, for O(1) glass color writes. */
		private val GLASS_COLOR_INDEX: Map<String, Int> =
			DYE_COLOR_NAMES.withIndex().associate { (i, name) -> name to i }

		/** Returns the DyeColor ordinal for a glass color name, or -1 if unknown/clear. */
		fun glassColorIndex(name: String): Int = GLASS_COLOR_INDEX[name] ?: -1

		private const val NORMAL_SPEED = 0.15
		const val EYE_HEIGHT = 0.45
		private const val BOB_SPEED = 0.09f
		private const val MEMORY_SYNC_INTERVAL = 100

		fun createAttributes(): AttributeSupplier.Builder =
			createMobAttributes()
				.add(Attributes.MAX_HEALTH, CompanionConfig.healthBase.toDouble())
				.add(Attributes.FLYING_SPEED, 0.6)
	}
}
