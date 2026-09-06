package mod.ex3.companion.network

import mod.ex3.companion.Ex3Companion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * Client->Server payload that syncs the companion core slot content.
 * Needed because creative-mode inventory clicks don't reliably trigger
 * server-side Container.setChanged() for custom slots.
 */
data class CompanionSlotPayload(val stack: ItemStack) : CustomPacketPayload {

	override fun type(): CustomPacketPayload.Type<*> = TYPE

	companion object {
		@JvmField
		val TYPE: CustomPacketPayload.Type<CompanionSlotPayload> =
			CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Ex3Companion.MOD_ID, "companion_slot"))

		@JvmField
		val CODEC: StreamCodec<RegistryFriendlyByteBuf, CompanionSlotPayload> =
			StreamCodec.of(
				{ buf, payload -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.stack) },
				{ buf -> CompanionSlotPayload(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)) },
			)
	}
}
