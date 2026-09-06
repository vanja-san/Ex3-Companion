package mod.ex3.companion.companion

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos

/**
 * Persistent exploration memory for the companion.
 * Tracks explored chunks and ore Y-level preferences so the companion
 * avoids scanning the same areas and learns where ores are found.
 *
 * Stored as a field inside [CompanionData] (core item data component).
 * Bounded to [MAX_EXPLORED_CHUNKS] and [MAX_ORE_PREFERENCES] to keep the component small.
 */
data class CompanionMemory(
	val exploredChunks: MutableSet<Long> = mutableSetOf(),
	val oreYPreferences: MutableMap<String, Int> = mutableMapOf(),
) {

	/** Returns true if the chunk containing [pos] was already explored. */
	fun isChunkExplored(pos: BlockPos): Boolean =
		exploredChunks.contains(packChunk(pos))

	/** Records the chunk containing [pos] as explored. Evicts oldest if at capacity. */
	fun markExplored(pos: BlockPos) {
		val packed = packChunk(pos)
		if (exploredChunks.add(packed) && exploredChunks.size > MAX_EXPLORED_CHUNKS) {
			exploredChunks.iterator().let { iter ->
				iter.next()
				iter.remove()
			}
		}
	}

	/** Records that an ore of [oreName] was found at [y] level. */
	fun recordOreY(oreName: String, y: Int) {
		oreYPreferences[oreName] = y
		if (oreYPreferences.size > MAX_ORE_PREFERENCES) {
			oreYPreferences.keys.iterator().let { iter ->
				iter.next()
				iter.remove()
			}
		}
	}

	/** Returns the preferred Y level for [oreName], or null if never seen. */
	fun preferredYForOre(oreName: String): Int? = oreYPreferences[oreName]

	/** Clears exploration data (e.g. when companion respawns in a new dimension). */
	fun clear() {
		exploredChunks.clear()
		oreYPreferences.clear()
	}

	companion object {
		private const val MAX_EXPLORED_CHUNKS = 1024
		private const val MAX_ORE_PREFERENCES = 50

		val EMPTY = CompanionMemory()

		/** Packs a block position into a chunk key (section coords). */
		private fun packChunk(pos: BlockPos): Long =
			(pos.x shr 4).toLong() and 0x3FFFFFFL or ((pos.z shr 4).toLong() and 0x3FFFFFFL shl 26)

		val CODEC: Codec<CompanionMemory> = RecordCodecBuilder.create { instance ->
			instance.group(
				Codec.LONG.listOf().optionalFieldOf("explored_chunks", emptyList())
					.forGetter { it.exploredChunks.toList() },
				Codec.unboundedMap(Codec.STRING, Codec.INT)
					.optionalFieldOf("ore_y_preferences", emptyMap())
					.forGetter { it.oreYPreferences },
			).apply(instance) { chunks: List<Long>, prefs: Map<String, Int> ->
				CompanionMemory(
					exploredChunks = LinkedHashSet(chunks.takeLast(MAX_EXPLORED_CHUNKS)),
					oreYPreferences = LinkedHashMap(prefs.entries.toList().takeLast(MAX_ORE_PREFERENCES).associate { (k, v) -> k to v }),
				)
			}
		}
	}
}
