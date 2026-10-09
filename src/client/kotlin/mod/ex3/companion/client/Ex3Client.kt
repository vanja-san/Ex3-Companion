package mod.ex3.companion.client

import mod.ex3.companion.client.render.CompanionRenderer
import mod.ex3.companion.companion.CompanionTooltip
import mod.ex3.companion.companion.CoreSlotManager
import mod.ex3.companion.network.CompanionSlotSyncPayload
import mod.ex3.companion.registry.ModEntities
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.world.item.ItemStack

object Ex3Client : ClientModInitializer {

	/** Last slot state reported to the server вЂ” avoids sending on every tick. */
	private var lastSentSlot: ItemStack? = null

	override fun onInitializeClient() {
		EntityRenderers.register(ModEntities.COMPANION, ::CompanionRenderer)

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			syncCompanionSlot(client)
			updateTooltipVisibility(client)
		}

		// Reset the change-detector and drop per-player client caches on disconnect.
		// Without this the detector leaks across worlds (first tick in a new world looks
		// like a change) and clientContainers/companionSlots grow by one entry for
		// every server UUID ever seen.
		ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
			lastSentSlot = null
			CoreSlotManager.clearClientCaches()
		}
	}

	/**
	 * Hides the core's stat block while the recipe book is on screen.
	 *
	 * `RecipeButton.getTooltipText()` builds its lines through
	 * `Screen.getTooltipFromItem`, so the data component's tooltip provider runs there too
	 * and the stats would otherwise be stacked above the vanilla "More Recipes" hint.
	 * Every screen that embeds the book - crafting, inventory, furnace - extends
	 * `AbstractRecipeBookScreen`, so one check covers them all.
	 */
	private fun updateTooltipVisibility(client: Minecraft) {
		CompanionTooltip.setHidden(client.gui.screen() is AbstractRecipeBookScreen<*>)
	}

	/**
	 * Reports companion-slot changes to the server in creative mode only.
	 *
	 * Survival clicks already reach the server through normal container packets and fire
	 * `Slot.setChanged()`, so they need no help; sending there would only add traffic.
	 * Creative is the broken case: vanilla's `CreativeModeInventoryScreen.slotClicked`
	 * applies inventory-tab clicks locally and never sends a packet.
	 */
	private fun syncCompanionSlot(client: Minecraft) {
		val player = client.player ?: return
		if (!player.hasInfiniteMaterials()) return
		if (!ClientPlayNetworking.canSend(CompanionSlotSyncPayload.TYPE)) return

		val container = CoreSlotManager.getOrCreateContainer(player)
		val stack = container.getItem(0)

		val prev = lastSentSlot
		if (prev != null && ItemStack.matches(prev, stack)) return

		lastSentSlot = stack.copy()
		ClientPlayNetworking.send(CompanionSlotSyncPayload(stack.copy()))
	}
}