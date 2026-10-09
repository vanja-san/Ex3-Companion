package mod.ex3.companion.network

import mod.ex3.companion.Ex3Companion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * Client -> Server sync of the companion core slot, used by creative mode only.
 *
 * WHY THIS EXISTS: vanilla's `CreativeModeInventoryScreen.slotClicked` does **not** send a
 * click packet for slots on the INVENTORY tab. It calls
 * `minecraft.player.inventoryMenu.clicked(...)` locally and then `broadcastChanges()`, so
 * the server never learns about those clicks and its own `CoreSlotBackingContainer`
 * silently diverges from what the player sees. Vanilla solves the same problem for the
 * creative item list with `ServerboundSetCreativeModeSlotPacket`, which the screen sends
 * through `gameMode.handleCreativeModeItemAdd(...)`; there is no equivalent for an
 * inventory-menu slot, hence this packet. (Survival clicks *are* sent to the server and do
 * fire `Slot.setChanged()`, so they need no help.)
 *
 * SECURITY: this is the one place where a client hands the server an ItemStack, so it is
 * restricted exactly the way vanilla restricts `ServerboundSetCreativeModeSlotPacket`
 * (`ServerGamePacketListenerImpl.handleSetCreativeModeSlot`):
 *  - accepted only from players with infinite materials (creative) — see
 *    `CoreSlotManager.applyClientSlotSync`, which also normalises count and type.
 * In survival the payload is ignored outright, so it cannot create or duplicate items.
 * Item creation in creative is meaningless anyway: creative mode is infinite items by
 * design. Progression values arriving this way are clamped in `CompanionData`.
 */
data class CompanionSlotSyncPayload(val stack: ItemStack) : CustomPacketPayload {

	override fun type(): CustomPacketPayload.Type<*> = TYPE

	companion object {
		@JvmField
		val TYPE: CustomPacketPayload.Type<CompanionSlotSyncPayload> =
			CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Ex3Companion.MOD_ID, "companion_slot"))

		@JvmField
		val CODEC: StreamCodec<RegistryFriendlyByteBuf, CompanionSlotSyncPayload> =
			StreamCodec.of(
				{ buf: RegistryFriendlyByteBuf, payload: CompanionSlotSyncPayload ->
					ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.stack)
				},
				{ buf: RegistryFriendlyByteBuf ->
					CompanionSlotSyncPayload(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf))
				},
			)
	}
}