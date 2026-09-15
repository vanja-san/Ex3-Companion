package mod.ex3.companion

import mod.ex3.companion.command.CompanionCommand
import mod.ex3.companion.companion.CoreSlotManager
import mod.ex3.companion.config.CompanionConfig
import mod.ex3.companion.config.CompanionPermissions
import mod.ex3.companion.network.CompanionSlotPayload
import mod.ex3.companion.network.ConfigSyncPayload
import mod.ex3.companion.network.ConfigUpdatePayload
import mod.ex3.companion.registry.ModComponents
import mod.ex3.companion.registry.ModEntities
import mod.ex3.companion.registry.ModItems
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

object Ex3Companion : ModInitializer {
	const val MOD_ID: String = "ex3-companion"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// Load config first — other systems reference it.
		CompanionConfig.load()

		// Registries (static init happens on field access).
		ModComponents.initialize()
		ModItems.initialize()
		ModEntities.initialize()
		mod.ex3.companion.recipe.ClearGlassRecipe.register()
		mod.ex3.companion.recipe.DyeGlassRecipe.register()

		registerNetworking()
		registerEvents()
		CompanionCommand.register()

		LOGGER.info("Ex3 Companion initialized")
	}

	private fun registerNetworking() {
		PayloadTypeRegistry.serverboundPlay().register(CompanionSlotPayload.TYPE, CompanionSlotPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(ConfigUpdatePayload.TYPE, ConfigUpdatePayload.CODEC)

		ServerPlayNetworking.registerGlobalReceiver(CompanionSlotPayload.TYPE) { payload, context ->
			val player = context.player()
			val stack = payload.stack
			context.server().execute {
				val container = CoreSlotManager.getOrCreateContainer(player)
				val current = container.getItem(0)

				if (stack.`is`(ModItems.COMPANION_CORE)) {
					CoreSlotManager.setAndNotify(player, stack)
				} else if (current.`is`(ModItems.COMPANION_CORE)) {
					CoreSlotManager.setAndNotify(player, ItemStack.EMPTY)
				}
			}
		}

		// Operator-only gameplay config updates. The request payload is applied,
		// written to disk, then the new values are rebroadcast to all players.
		ServerPlayNetworking.registerGlobalReceiver(ConfigUpdatePayload.TYPE) { payload, context ->
			if (!CompanionPermissions.isOperator(context.player())) return@registerGlobalReceiver
			val parsed = CompanionConfig.parse(payload.json) ?: return@registerGlobalReceiver
			context.server().execute {
				val oldHealth = CompanionConfig.get().health
				CompanionConfig.applyParsed(parsed)
				CompanionConfig.save()
				// Live-tune every summoned companion so existing entities pick up the new
				// max health; when the health tuning itself changed, top them up to full so
				// the bar doesn't show "missing" HP and regen resumes toward the new value.
				val healthTuningChanged = oldHealth.base != parsed.health.base ||
					oldHealth.perLevel != parsed.health.perLevel ||
					oldHealth.cap != parsed.health.cap
				CoreSlotManager.retuneAllCompanions(context.server(), topUpToFull = healthTuningChanged)
				val serialized = CompanionConfig.serialize(parsed)
				context.server().playerList.players.forEach { ServerPlayNetworking.send(it, ConfigSyncPayload(serialized)) }
			}
		}
	}

	private fun registerEvents() {
		// Login: restore slot content, summon if needed, and sync the gameplay
		// config so the client GUI shows server-authoritative values.
		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			CoreSlotManager.onPlayerJoin(handler.player)
			ServerPlayNetworking.send(handler.player, ConfigSyncPayload(CompanionConfig.serialize(CompanionConfig.get())))
		}

		// Logout: quietly hide the companion; the core stays in the slot.
		ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
			CoreSlotManager.onPlayerLeave(handler.player)
		}

		// Death: companion dies together with the player, core stays in the slot.
		ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
			if (entity is net.minecraft.server.level.ServerPlayer) {
				CoreSlotManager.onPlayerDeath(entity)
			}
		}

		// Death clone: transfer the core item to the new player instance.
		ServerPlayerEvents.COPY_FROM.register { oldPlayer: net.minecraft.world.entity.player.Player,
			newPlayer: net.minecraft.world.entity.player.Player, _ ->
			CoreSlotManager.onPlayerCopy(oldPlayer, newPlayer)
		}

		// Respawn: summon the companion again with stored health.
		ServerPlayerEvents.AFTER_RESPAWN.register { _, newPlayer: net.minecraft.world.entity.player.Player, _ ->
			CoreSlotManager.onPlayerRespawn(newPlayer)
		}

		// Every server tick: bring companions back once their recovery period ends.
		ServerTickEvents.END_SERVER_TICK.register { server ->
			CoreSlotManager.tickRevives(server)
		}

		// Dimension change via portal: despawn companion in old dimension, re-summon in new.
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register { player, origin, destination ->
			CoreSlotManager.onPlayerChangedDimension(player, origin, destination)
		}
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
