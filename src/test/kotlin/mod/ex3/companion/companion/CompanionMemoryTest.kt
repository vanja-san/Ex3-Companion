package mod.ex3.companion.companion

import com.mojang.serialization.JsonOps
import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Bounds and round-tripping for the exploration memory stored on the core item.
 */
class CompanionMemoryTest {

	@Test
	@DisplayName("markExplored keeps the chunk set within its cap")
	fun exploredChunksAreBounded() {
		val memory = CompanionMemory.empty()
		// Far more entries than the cap, spread over distinct chunks.
		for (x in 0 until 2000) {
			memory.markExplored(BlockPos(x * 16, 64, 0))
		}
		assertEquals(1024, memory.exploredChunks.size)
	}

	@Test
	@DisplayName("recordOreY keeps the preference map within its cap")
	fun orePreferencesAreBounded() {
		val memory = CompanionMemory.empty()
		for (i in 0 until 500) {
			memory.recordOreY("ore$i", i)
		}
		assertEquals(50, memory.oreYPreferences.size)
	}

	@Test
	@DisplayName("empty() hands out independent instances, not a shared singleton")
	fun emptyIsNotShared() {
		// Regression: `EMPTY` used to be one global mutable CompanionMemory used as a
		// codec/field default — a single write through it would have corrupted every
		// player's memory on the server.
		val a = CompanionMemory.empty()
		val b = CompanionMemory.empty()
		assertNotSame(a, b)

		a.markExplored(BlockPos(0, 64, 0))
		a.recordOreY("minecraft:diamond_ore", -59)
		assertTrue(b.exploredChunks.isEmpty())
		assertTrue(b.oreYPreferences.isEmpty())
	}

	@Test
	@DisplayName("codec round-trip preserves explored chunks and ore Y levels")
	fun codecRoundTripPreservesData() {
		val memory = CompanionMemory.empty()
		memory.markExplored(BlockPos(16, 64, 16))
		memory.markExplored(BlockPos(-32, 64, 48))
		memory.recordOreY("minecraft:diamond_ore", -59)
		memory.recordOreY("minecraft:copper_ore", 16)

		val encoded = CompanionMemory.CODEC
			.encodeStart(JsonOps.INSTANCE, memory)
			.result()
			.orElseThrow()

		val decoded = CompanionMemory.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow()

		assertEquals(memory.exploredChunks, decoded.exploredChunks)
		assertEquals(memory.oreYPreferences, decoded.oreYPreferences)
		assertTrue(decoded.isChunkExplored(BlockPos(16, 64, 16)))
		assertEquals(-59, decoded.preferredYForOre("minecraft:diamond_ore"))
	}

	@Test
	@DisplayName("decode trims oversized input down to the cap")
	fun decodeTrimsOversizedInput() {
		val oversized = CompanionMemory(
			exploredChunks = LinkedHashSet((0 until 5000).map { it.toLong() }),
			oreYPreferences = LinkedHashMap((0 until 500).associate { "ore$it" to it }),
		)

		val encoded = CompanionMemory.CODEC
			.encodeStart(JsonOps.INSTANCE, oversized)
			.result()
			.orElseThrow()

		val decoded = CompanionMemory.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow()

		assertEquals(1024, decoded.exploredChunks.size)
		assertEquals(50, decoded.oreYPreferences.size)
	}
}