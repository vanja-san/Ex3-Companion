package mod.ex3.companion.network

import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.item.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Wire-format guards for the creative-mode slot sync.
 *
 * SCOPE LIMIT — read before adding cases here. Vanilla binds every item's data components
 * only during a full datapack reload (`ReloadableServerResources` ->
 * `DataComponentInitializers.PendingComponents::apply`), which a plain JUnit run cannot
 * perform. Constructing a *non-empty* `ItemStack` therefore fails with
 * "Components not bound yet", so only `ItemStack.EMPTY` can be exercised here.
 *
 * Round-tripping a real core — and, more importantly, the trust decision in
 * `CoreSlotManager.applyClientSlotSync` (creative-only gate, count/type normalisation) —
 * needs a live server and belongs in a game test via fabric-game-test-api.
 */
class CompanionSlotSyncPayloadTest {

	companion object {
		@JvmStatic
		@BeforeAll
		fun bootstrapGame() {
			net.minecraft.SharedConstants.tryDetectVersion()
			net.minecraft.server.Bootstrap.bootStrap()
		}
	}

	@Test
	@DisplayName("an empty stack round-trips as empty")
	fun emptyStackRoundTrips() {
		val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
		CompanionSlotSyncPayload.CODEC.encode(buf, CompanionSlotSyncPayload(ItemStack.EMPTY))

		val decoded = CompanionSlotSyncPayload.CODEC.decode(buf)
		assertTrue(decoded.stack.isEmpty)
	}

	@Test
	@DisplayName("decode consumes exactly the bytes encode produced")
	fun codecIsBalanced() {
		val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
		CompanionSlotSyncPayload.CODEC.encode(buf, CompanionSlotSyncPayload(ItemStack.EMPTY))
		assertTrue(buf.readableBytes() > 0, "an empty stack still encodes a presence flag")

		CompanionSlotSyncPayload.CODEC.decode(buf)
		assertEquals(0, buf.readableBytes(), "decoder left bytes unread")
	}

	@Test
	@DisplayName("payload type id is stable — clients and servers must agree")
	fun payloadTypeIsStable() {
		assertEquals(
			"ex3-companion:companion_slot",
			CompanionSlotSyncPayload.TYPE.id().toString(),
		)
		assertSame(CompanionSlotSyncPayload.TYPE, CompanionSlotSyncPayload(ItemStack.EMPTY).type())
	}
}