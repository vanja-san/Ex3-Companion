package mod.ex3.companion.companion

import mod.ex3.companion.config.CompanionConfig
import mod.ex3.companion.recipe.ClearGlassRecipe
import mod.ex3.companion.registry.ModComponents
import mod.ex3.companion.registry.ModEntities
import mod.ex3.companion.registry.ModItems
import net.minecraft.core.component.DataComponents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the per-player companion core slot: the backing container, persistence,
 * and spawning / despawning of the companion entity when the core is inserted or removed.
 */
object CoreSlotManager {
	private val LOGGER = LoggerFactory.getLogger("Ex3-Companion")
	private const val SAVE_KEY = "ex3_companion_core"

	/**
	 * Backing containers are kept SEPARATELY per side: in singleplayer the client
	 * and the integrated server share one JVM, and sharing one container caused
	 * client-side click prediction to run server spawn logic (duplicate stacks,
	 * endless companion spawning). Server map is authoritative, client map is visual.
	 */
	private val serverContainers = ConcurrentHashMap<UUID, CoreSlotBackingContainer>()
	private val clientContainers = ConcurrentHashMap<UUID, CoreSlotBackingContainer>()

	/** Players whose container updates should not trigger lifecycle logic (seeding). */
	private val suppressed = ConcurrentHashMap.newKeySet<UUID>()

	/** The companion-core slot once it has been injected into the inventory menu. */
	private val companionSlots = ConcurrentHashMap<UUID, CompanionCoreSlot>()

	/** Cached companion entity per player UUID — avoids expensive spatial queries every tick. */
	private val companionCache = ConcurrentHashMap<UUID, CompanionEntity>()

	/** Removes a player's companion from the cache (e.g. when companion detects dimension mismatch). */
	@JvmStatic
	fun removeFromCompanionCache(playerUuid: UUID) {
		companionCache.remove(playerUuid)
	}

	/** Last synced health per player — avoids unnecessary NBT dirty-marking every tick. */
	private val lastSyncedHealth = ConcurrentHashMap<UUID, Float>()

	/** Last synced memory hash per player — avoids unnecessary writes every tick. */
	private val lastMemoryHash = ConcurrentHashMap<UUID, Int>()

	// ------------------------------------------------------------------
	// Container access
	// ------------------------------------------------------------------

	@JvmStatic
	fun getOrCreateContainer(player: Player): CoreSlotBackingContainer {
		val map = if (player.level().isClientSide) clientContainers else serverContainers
		return map.computeIfAbsent(player.uuid) {
			CoreSlotBackingContainer(player)
		}
	}

	/**
	 * Returns the player's backing container if one already exists, without creating it.
	 * Used by per-tick scans so players without a companion core never allocate a container.
	 */
	private fun getContainerIfPresent(player: Player): CoreSlotBackingContainer? {
		val map = if (player.level().isClientSide) clientContainers else serverContainers
		return map[player.uuid]
	}

	/** Records the companion-core slot after it is injected into the inventory menu. */
	@JvmStatic
	fun setCompanionSlot(player: Player, slot: CompanionCoreSlot) {
		companionSlots[player.uuid] = slot
	}

	/** The companion-core slot injected into the inventory menu, or null if absent. */
	@Suppress("unused")
	@JvmStatic
	fun getCompanionSlot(player: Player): CompanionCoreSlot? =
		companionSlots[player.uuid]

	/**
	 * Called whenever the slot content changes. Server side drives entity
	 * lifecycle + persistence; client side is a no-op.
	 */
	fun onSlotChanged(player: Player) {
		if (player.level().isClientSide) return
		if (suppressed.contains(player.uuid)) return

		val container = getOrCreateContainer(player)
		val stack = container.getItem(0)

		if (stack.`is`(ModItems.COMPANION_CORE)) {
			ensureCompanionSpawned(player, stack)
		} else {
			despawnCompanion(player, withPoof = false)
		}
	}

	// ------------------------------------------------------------------
	// Entity lifecycle
	// ------------------------------------------------------------------

	private fun ensureCompanionSpawned(player: Player, stack: ItemStack) {
		// Idempotent: exactly one companion per owning player, searched by owner UUID
		// (never by the component-stored entity id, which may lag behind after syncs).
		// While recovering from the owner's death, do not summon yet.
		val data = readData(stack)
		if (data.reviveAt > player.level().gameTime) return

		// Don't spawn during revive regeneration — wait until health is fully restored.
		if (data.reviveAt > 0) {
			val max = CompanionData.maxHealth(data.level)
			if (data.health < max) return
		}

		val existing = findCompanion(player)
		if (existing != null) {
			existing.syncMaxHealth(data.level)
			existing.health = data.health
			existing.glassColorName = data.glassColor
			// Keep the entity name in sync with the (possibly renamed) core item.
			existing.setCustomName(stack.get(DataComponents.CUSTOM_NAME))
			// Keep the entity id fresh on the core item (also persists colour sources).
			writeData(player, stack, data.withId(existing.uuid))
			return
		}

		val level = (player.level() as? ServerLevel) ?: return
		val entity = CompanionEntity(ModEntities.COMPANION, level)
		entity.ownerId = player.uuid
		// Spawn BESIDE the player (the follow hover point), never inside their body.
		val spawnPos = entity.computeSpawnPosition(player)
		entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z)
		entity.syncMaxHealth(data.level)
		entity.health = data.health
		entity.glassColorName = data.glassColor
		// The anvil-renamed (vanilla CUSTOM_NAME) core names the companion entity.
		entity.setCustomName(stack.get(DataComponents.CUSTOM_NAME))
		// Load exploration memory from the core item.
		entity.memory.exploredChunks.addAll(data.memory.exploredChunks)
		entity.memory.oreYPreferences.putAll(data.memory.oreYPreferences)
		level.addFreshEntity(entity)

		// Remember the entity id on the core item (informational / future use).
		writeData(player, stack, data.withId(entity.uuid))
	}

	fun despawnCompanion(player: Player, withPoof: Boolean) {
		val companion = findCompanion(player) ?: return
		if (withPoof) companion.playDeathPoof()
		companion.discard()
		companionCache.remove(player.uuid)
	}

	/**
	 * Called after the player has been moved to a different level (e.g. portal).
	 * The player entity is the same instance, now in [newLevel].
	 * We search the [oldLevel] for the companion and discard it, then trigger
	 * re-summoning in the new level via [onSlotChanged].
	 */
	fun onPlayerChangedDimension(player: Player, oldLevel: ServerLevel, newLevel: ServerLevel) {
		if (player.level().isClientSide) return

		// 1. Find and discard the companion in the old dimension.
		val stale = oldLevel.getEntitiesOfClass(
			CompanionEntity::class.java,
			net.minecraft.world.phys.AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0),
		) { it.ownerId == player.uuid }
		for (c in stale) {
			c.discard()
		}
		companionCache.remove(player.uuid)

		// 2. Re-summon in the new dimension (player is already there).
		val stack = getOrCreateContainer(player).getItem(0)
		if (stack.`is`(ModItems.COMPANION_CORE)) {
			val data = readData(stack)
			if (data.reviveAt <= player.level().gameTime) {
				ensureCompanionSpawned(player, stack)
			}
		}
	}

	private fun findCompanion(player: Player): CompanionEntity? {
		// Check cache first — avoids expensive spatial query every tick.
		val cached = companionCache[player.uuid]
		if (cached != null && cached.isAlive && !cached.isRemoved && (cached.ownerId == player.uuid)) {
			return cached
		}
		// Cache miss or stale: do spatial query and update cache.
		val found = player.level().getEntitiesOfClass(
			CompanionEntity::class.java,
			player.boundingBox.inflate(64.0),
		) { it.ownerId == player.uuid }
		if (found.size > 1) {
			LOGGER.warn("Multiple companions found for player {} — discarding extras", player.scoreboardName)
			for (extra in found.drop(1)) extra.discard()
		}
		val companion = found.firstOrNull()
		if (companion != null) {
			companionCache[player.uuid] = companion
		} else {
			companionCache.remove(player.uuid)
		}
		return companion
	}

	/**
	 * Re-applies the gameplay config to every summoned companion after the server
	 * config changes (e.g. operator config edit via the GUI). Without this, a live
	 * health-config change leaves existing entities on their old MAX_HEALTH attribute:
	 * the client bar shows the new (larger) max while health stays at the old value,
	 * and regeneration never fires because health already equals the stale max.
	 *
	 * Always refreshes the MAX_HEALTH attribute; when [topUpToFull] it also tops the
	 * companion up to the new full health and pushes that to the core item.
	 */
	fun retuneAllCompanions(server: MinecraftServer, topUpToFull: Boolean) {
		for (player in server.playerList.players) {
			if (player.level().isClientSide) continue
			val companion = findCompanion(player) ?: continue
			val container = getOrCreateContainer(player)
			val stack = container.getItem(0)
			if (!stack.`is`(ModItems.COMPANION_CORE)) continue
			val level = readData(stack).level
			companion.syncMaxHealth(level)
			if (topUpToFull) {
				companion.health = companion.maxHealth
				syncHealthToCore(player, companion.maxHealth)
			}
		}
	}

	// ------------------------------------------------------------------
	// Player lifecycle events
	// ------------------------------------------------------------------

	/** On login: the container was seeded by the save-data mixin; summon if needed. */
	fun onPlayerJoin(player: Player) {
		if (player.level().isClientSide) return
		// Only run lifecycle if the player actually has a container (i.e. had a core).
		// Avoids allocating a container for every player who joins.
		if (getContainerIfPresent(player) != null) {
			onSlotChanged(player)
		}
	}

	/** On logout: quietly remove the entity; the core stays in the slot.
	 *  NOTE: serverContainers is NOT cleared here because writeSaveData runs
	 *  AFTER this callback and needs the container to persist the core.
	 *  The map entry is small (~200 bytes) and will be reclaimed when the
	 *  player's UUID is garbage-collected or the server shuts down. */
	fun onPlayerLeave(player: Player) {
		if (player.level().isClientSide) return
		despawnCompanion(player, withPoof = false)
		// Visual-only client container can be cleared immediately.
		clientContainers.remove(player.uuid)
		// Slot reference is tied to the old InventoryMenu; will be re-created on join.
		companionSlots.remove(player.uuid)
		companionCache.remove(player.uuid)
		lastSyncedHealth.remove(player.uuid)
		lastMemoryHash.remove(player.uuid)
	}

	/**
	 * Called when the companion takes fatal damage from mobs.
	 * Sets health to zero on the core item, plays death effects, despawns the entity,
	 * and starts the revive timer so it reappears after regeneration.
	 */
	fun killCompanion(player: Player, companion: CompanionEntity) {
		if (player.level().isClientSide) return
		val container = getOrCreateContainer(player)
		val stack = container.getItem(0)
		if (stack.`is`(ModItems.COMPANION_CORE)) {
			val data = readData(stack)
			val respawnTime = player.level().gameTime + CompanionConfig.healthReviveDelayTicks
			writeData(player, stack, data.withHealth(0f).withReviveAt(respawnTime))
			lastSyncedHealth.remove(player.uuid)
		}
		companion.playDeathPoof()
		companion.discard()
		companionCache.remove(player.uuid)
	}

	/** On death: the companion dies together with its owner; health drops to
	 *  zero and it stays dead until fully regenerated after a delay. */
	fun onPlayerDeath(player: Player) {
		if (player.level().isClientSide) return
		val container = getOrCreateContainer(player)
		val stack = container.getItem(0)
		if (stack.`is`(ModItems.COMPANION_CORE)) {
			val data = readData(stack)
			val respawnTime = player.level().gameTime + CompanionConfig.healthReviveDelayTicks
			writeData(player, stack, data.withHealth(0f).withReviveAt(respawnTime))
		}
		despawnCompanion(player, withPoof = true)
		companionCache.remove(player.uuid)
	}

	/** Death clone / dimension change: transfer the core item to the new player instance.
	 *  If the old player is alive, this is a dimension change — despawn the companion
	 *  so it gets re-summoned in the new dimension by [tickRevives] or [onSlotChanged]. */
	fun onPlayerCopy(oldPlayer: Player, newPlayer: Player) {
		if (newPlayer.level().isClientSide) return
		val stack = getOrCreateContainer(oldPlayer).getItem(0)
		if (!stack.isEmpty) {
			seedSilently(newPlayer, stack)
		}
		// Dimension change (old player alive): despawn companion in old dimension.
		// For death, onPlayerDeath already handled this — only act for dimension changes.
		if (oldPlayer.isAlive) {
			despawnCompanion(oldPlayer, withPoof = false)
			companionCache.remove(oldPlayer.uuid)
			companionSlots.remove(oldPlayer.uuid)
		}
	}

	/** After respawn: re-summon the companion next to the player with stored health. */
	fun onPlayerRespawn(player: Player) {
		if (player.level().isClientSide) return
		// Only run lifecycle if the player actually has a container (i.e. had a core).
		if (getContainerIfPresent(player) != null) {
			onSlotChanged(player)
		}
	}

	/**
	 * Called every server tick. Finds the companion core wherever it is in the
	 * player's inventory, regenerates health after the revive delay expires,
	 * and only spawns the companion when the core is in the companion slot AND
	 * health is full.
	 */
	fun tickRevives(server: MinecraftServer) {
		for (player in server.playerList.players) {
			if (player.level().isClientSide) continue

			val coreStack = findCoreAnywhere(player) ?: continue
			val data = readData(coreStack)
			val max = CompanionData.maxHealth(data.level)

			// Companion is alive — nothing to do here.
			if (findCompanion(player) != null) continue

			// Still in revive cooldown — do not regenerate health yet.
			if (data.reviveAt > player.level().gameTime) continue

			// Revive delay expired: regenerate health on the core item (works even in inventory).
			if (data.health < max) {
				val scale = 1f + (data.level - 1) * CompanionConfig.healthRegenLevelScale
				val newHealth = (data.health + CompanionConfig.healthRegenPerTick * scale).coerceAtMost(max)
				writeData(player, coreStack, data.withHealth(newHealth))
				continue
			}

			// Health is full — only spawn if the core is in the companion slot.
			val inSlot = isInCompanionSlot(player, coreStack)
			if (inSlot) {
				writeData(player, coreStack, data.withReviveAt(0L))
				ensureCompanionSpawned(player, coreStack)
			}
		}
	}

	/**
	 * Searches the companion slot first, then the player's entire inventory
	 * for a companion core item. Returns the stack or null.
	 */
	private fun findCoreAnywhere(player: Player): ItemStack? {
		// Check companion slot first (without creating a container for players without cores).
		val slotStack = getContainerIfPresent(player)?.getItem(0)
		if (slotStack != null && slotStack.`is`(ModItems.COMPANION_CORE)) return slotStack

		// Search player inventory: main (0-35), armor (36-39), offhand (40).
		val inv = player.inventory
		for (i in 0 until inv.containerSize) {
			val stack = inv.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) return stack
		}
		return null
	}

	/** True if [stack] is in the companion slot (container slot 0). */
	private fun isInCompanionSlot(player: Player, stack: ItemStack): Boolean {
		val slotStack = getOrCreateContainer(player).getItem(0)
		return slotStack === stack
	}

	// ------------------------------------------------------------------
	// Persistence (player save data via mixins)
	// ------------------------------------------------------------------

	@JvmStatic
	fun writeSaveData(player: ServerPlayer, output: ValueOutput) {
		val stack = getOrCreateContainer(player).getItem(0)
		if (!stack.isEmpty) {
			output.store(SAVE_KEY, ItemStack.CODEC, stack.copy())
		}
	}

	@JvmStatic
	fun readSaveData(player: ServerPlayer, input: ValueInput) {
		val result = input.read(SAVE_KEY, ItemStack.CODEC)
        val stack = result.orElse(null) ?: return
        if (stack.`is`(ModItems.COMPANION_CORE)) {
			seedSilently(player, stack)
		}
	}

	/** Puts a stack into the container without triggering lifecycle logic. */
	private fun seedSilently(player: Player, stack: ItemStack) {
		suppressed.add(player.uuid)
		try {
			getOrCreateContainer(player).setItem(0, stack.copy())
		} finally {
			suppressed.remove(player.uuid)
		}
	}

	/**
	 * Sets the companion slot content and runs lifecycle logic.
	 * Used by the creative-mode sync packet handler.
	 */
	@JvmStatic
	fun setAndNotify(player: Player, stack: ItemStack) {
		getOrCreateContainer(player).setItem(0, stack.copy())
		onSlotChanged(player)
	}

	/**
	 * Pushes the companion's current health back onto the core item so the
	 * tooltip can display the live value. Only writes when health actually
	 * changed to avoid unnecessary NBT dirty-marking every tick.
	 */
	fun syncHealthToCore(player: Player, health: Float) {
		if (player.level().isClientSide) return
		// Skip write if health hasn't changed since last sync.
		val prev = lastSyncedHealth[player.uuid]
		if (prev != null && (prev == health)) return
		lastSyncedHealth[player.uuid] = health

		val container = getOrCreateContainer(player)
		val stack = container.getItem(0)
		if (!stack.`is`(ModItems.COMPANION_CORE)) return
		writeData(player, stack, readData(stack).withHealth(health))
	}

	/**
	 * Pushes the companion's exploration memory back onto the core item for persistence.
	 * Only writes when the memory content actually changed (checked via hash).
	 */
	fun syncMemoryToCore(player: Player, memory: CompanionMemory) {
		if (player.level().isClientSide) return
		val hash = memory.hashCode()
		val prev = lastMemoryHash[player.uuid]
		if (prev != null && prev == hash) return
		lastMemoryHash[player.uuid] = hash

		val container = getOrCreateContainer(player)
		val stack = container.getItem(0)
		if (!stack.`is`(ModItems.COMPANION_CORE)) return
		writeData(player, stack, readData(stack).withMemory(memory))
	}

	// ------------------------------------------------------------------
	// Leveling
	// ------------------------------------------------------------------

	/**
	 * Adds kill-assist XP to the player's core. Handles level-ups and notifies
	 * the companion entity so it can play feedback effects.
	 */
	fun addCompanionXp(player: Player, amount: Int) {
		if (player.level().isClientSide) return
		val container = getOrCreateContainer(player)
		val stack = container.getItem(0)
		if (!stack.`is`(ModItems.COMPANION_CORE)) return

		val data = readData(stack)
		// At level cap, XP is still accumulated (for display) but no more level-ups.
		val levelCap = CompanionConfig.xpLevelCap
		var xp = data.xp + amount
		var level = data.level
		while (level < levelCap && xp >= CompanionData.xpToNextLevel(level)) {
			xp -= CompanionData.xpToNextLevel(level)
			level++
		}
		writeData(player, stack, data.copy(xp = xp, level = level))

		if (level != data.level) {
			val lvl = level
			findCompanion(player)?.let { companion ->
				companion.syncMaxHealth(lvl)
				companion.playLevelUpFx()
			}
			// Send level-up notification to the player.
			player.sendSystemMessage(
				net.minecraft.network.chat.Component.translatable("ex3companion.level_up", lvl).withStyle(net.minecraft.ChatFormatting.GREEN),
			)
		}
	}

	/** Current level stored on the player's core (1 when no core). */
	fun readCompanionLevel(player: Player): Int {
		val stack = getOrCreateContainer(player).getItem(0)
		return if (stack.`is`(ModItems.COMPANION_CORE)) readData(stack).level else 1
	}

	// ------------------------------------------------------------------
	// Data component helpers
	// ------------------------------------------------------------------

	private fun readData(stack: ItemStack): CompanionData {
		val data = stack.getOrDefault(ModComponents.COMPANION_DATA, CompanionData.DEFAULT)
		// Glass tint authority: the item model select (custom_model_data[0]) is always fresh,
		// so it self-heals stale colors stuck in the mod component. Fall back to the recipe's
		// custom_data tag, then to the mod component.
		val modelColor = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)?.strings()?.firstOrNull()
		if (modelColor != null) return data.withGlassColor(modelColor)
		val customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
		val nbt = customData?.copyTag()
		if (nbt != null && nbt.contains("companion")) {
			val companion = nbt.getCompoundOrEmpty("companion")
			val color = companion.getStringOr("glassColor", "clear")
			return data.withGlassColor(color)
		}
		return data
	}

	private fun writeData(player: Player, stack: ItemStack, data: CompanionData) {
		stack[ModComponents.COMPANION_DATA] = data
		ClearGlassRecipe.applyGlassColor(stack, data.glassColor)
		// Mark the backing container dirty so vanilla broadcastChanges() detects the
		// component mutation and sends a slot update to the client. Without this the
		// health bar stays stale because SimpleContainer only tracks setItem() calls.
		val container = getOrCreateContainer(player)
		suppressed.add(player.uuid)
		try {
			container.setChanged()
		} finally {
			suppressed.remove(player.uuid)
		}
	}
}
