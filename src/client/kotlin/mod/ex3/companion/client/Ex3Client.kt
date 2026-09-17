package mod.ex3.companion.client

import mod.ex3.companion.client.render.CompanionRenderer
import mod.ex3.companion.companion.CoreSlotBackingContainer
import mod.ex3.companion.network.CompanionSlotPayload
import mod.ex3.companion.registry.ModEntities
import mod.ex3.companion.registry.ModItems
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.EntityRenderers

object Ex3Client : ClientModInitializer {

	/** Whether the companion slot contained a core on the previous tick. */
	private var lastHasCore: Boolean? = null

	override fun onInitializeClient() {
		EntityRenderers.register(ModEntities.COMPANION, ::CompanionRenderer)

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			syncCompanionSlot(client)
		}
	}

	/**
	 * Checks every tick whether the companion slot content changed on the client.
	 * If it did, sends a [CompanionSlotPayload] to the server so that creative-mode
	 * inventory clicks (which don't trigger server-side setChanged) are handled.
	 */
	private fun syncCompanionSlot(client: Minecraft) {
		val player = client.player ?: return

		val container = mod.ex3.companion.companion.CoreSlotManager.getOrCreateContainer(player)
		val stack = container.getItem(0)
		val hasCore = stack.`is`(ModItems.COMPANION_CORE)

		val prev = lastHasCore
		if (prev == null) {
			lastHasCore = hasCore
			return
		}

		if (prev != hasCore) {
			lastHasCore = hasCore
			ClientPlayNetworking.send(CompanionSlotPayload(stack.copy()))
		}
	}
}
