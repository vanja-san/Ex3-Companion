package mod.ex3.companion.network

import mod.ex3.companion.Ex3Companion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/**
 * Server -> Client payload that synchronizes the server's gameplay config.
 * Sent to every player on join and rebroadcast whenever an operator updates it,
 * so the client always renders server-authoritative values (read-only for
 * non-operators).
 */
data class ConfigSyncPayload(val json: String) : CustomPacketPayload {

	override fun type(): CustomPacketPayload.Type<*> = TYPE

	companion object {
		@JvmField
		val TYPE: CustomPacketPayload.Type<ConfigSyncPayload> =
			CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Ex3Companion.MOD_ID, "config_sync"))

		@JvmField
		val CODEC: StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> =
			StreamCodec.of(
				{ buf, payload -> buf.writeUtf(payload.json) },
				{ buf -> ConfigSyncPayload(buf.readUtf()) },
			)
	}
}