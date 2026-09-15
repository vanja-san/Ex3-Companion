package mod.ex3.companion.network

import mod.ex3.companion.Ex3Companion
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/**
 * Client -> Server payload that requests applying a new gameplay config.
 * The server only honors it from operators; it then saves the config to disk
 * and rebroadcasts the updated values to every connected player.
 */
data class ConfigUpdatePayload(val json: String) : CustomPacketPayload {

	override fun type(): CustomPacketPayload.Type<*> = TYPE

	companion object {
		@JvmField
		val TYPE: CustomPacketPayload.Type<ConfigUpdatePayload> =
			CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Ex3Companion.MOD_ID, "config_update"))

		@JvmField
		val CODEC: StreamCodec<RegistryFriendlyByteBuf, ConfigUpdatePayload> =
			StreamCodec.of(
				{ buf, payload -> buf.writeUtf(payload.json) },
				{ buf -> ConfigUpdatePayload(buf.readUtf()) },
			)
	}
}