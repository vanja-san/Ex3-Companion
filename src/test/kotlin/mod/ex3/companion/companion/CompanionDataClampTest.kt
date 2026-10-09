package mod.ex3.companion.companion

import com.mojang.serialization.JsonOps
import mod.ex3.companion.config.CompanionConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards the clamp applied to `level`, which arrives from item NBT and is therefore
 * fully attacker-controlled (a crafted core item, or any client-forged component).
 *
 * Regression origin: an unclamped `level` made `maxHealth()` negative, which crashed the
 * server tick with `IllegalArgumentException` from `coerceIn(MIN_HEALTH, maxHealth)`,
 * and made `level * level` overflow Int past 46340.
 */
class CompanionDataClampTest {

	@Test
	@DisplayName("sanitizeLevel clamps hostile levels into the configured band")
	fun sanitizeLevelClamps() {
		assertEquals(1, CompanionData.sanitizeLevel(Int.MIN_VALUE))
		assertEquals(1, CompanionData.sanitizeLevel(-100))
		assertEquals(1, CompanionData.sanitizeLevel(0))
		assertEquals(7, CompanionData.sanitizeLevel(7))
		assertEquals(CompanionConfig.xpLevelCap, CompanionData.sanitizeLevel(Int.MAX_VALUE))
	}

	@Test
	@DisplayName("maxHealth stays in (0, cap] for every representable level")
	fun maxHealthIsAlwaysUsable() {
		// -100 and -1000 matter specifically: they are the levels that produced a
		// negative maxHealth. Int.MIN_VALUE alone is NOT enough — `level - 1` overflows
		// back to a large positive number there, which hides the bug.
		val hostile = intArrayOf(Int.MIN_VALUE, -100_000, -1000, -100, -1, 0, 1, 2, 50, CompanionConfig.xpLevelCap, Int.MAX_VALUE)
		for (level in hostile) {
			val max = CompanionData.maxHealth(level)
			assertTrue(max >= 1f, "maxHealth($level) = $max must be >= 1 so coerceIn has a valid range")
			assertTrue(max <= CompanionConfig.healthCap, "maxHealth($level) = $max exceeds cap")
		}
	}

	@Test
	@DisplayName("coerceIn(MIN_HEALTH, maxHealth) never throws — the original crash")
	fun healthClampRangeIsNeverEmpty() {
		val hostile = intArrayOf(Int.MIN_VALUE, -100, 0, Int.MAX_VALUE)
		for (level in hostile) {
			// The exact expression that used to throw IllegalArgumentException.
			val clamped = 7f.coerceIn(CompanionData.MIN_HEALTH, CompanionData.maxHealth(level))
			assertTrue(clamped >= CompanionData.MIN_HEALTH)
		}
	}

	@Test
	@DisplayName("withHealth tolerates a forged level instead of throwing")
	fun withHealthDoesNotThrowOnForgedLevel() {
		val hostile = listOf(Int.MIN_VALUE, -100, 0, Int.MAX_VALUE)
		for (level in hostile) {
			val data = CompanionData(null, 5f, 0, level)
			val updated = data.withHealth(3f)
			assertTrue(updated.health in CompanionData.MIN_HEALTH..CompanionData.maxHealth(level))
		}
	}

	@Test
	@DisplayName("xpToNextLevel stays positive and cannot overflow Int")
	fun xpToNextLevelDoesNotOverflow() {
		for (level in intArrayOf(Int.MIN_VALUE, -1, 0, 1, 1000, CompanionConfig.xpLevelCap, Int.MAX_VALUE)) {
			val needed = CompanionData.xpToNextLevel(level)
			assertTrue(needed > 0, "xpToNextLevel($level) = $needed must be positive")
		}
	}

	@Test
	@DisplayName("damage and attack interval stay within their configured bounds")
	fun derivedStatsAreBounded() {
		for (level in intArrayOf(Int.MIN_VALUE, -100, 0, 1, 25, Int.MAX_VALUE)) {
			val dmg = CompanionData.damage(level)
			assertTrue(dmg >= 0f && dmg <= CompanionConfig.combatDamageCap, "damage($level) = $dmg out of range")

			val interval = CompanionData.attackIntervalTicks(level)
			assertTrue(
				interval in CompanionConfig.combatIntervalMin..CompanionConfig.combatIntervalBase,
				"attackIntervalTicks($level) = $interval out of range",
			)
		}
	}

	@Test
	@DisplayName("level is clamped at decode time, not only when read")
	fun codecClampsLevelOnDecode() {
		for (level in intArrayOf(Int.MIN_VALUE, -100, 0, Int.MAX_VALUE)) {
			val json = CompanionData.CODEC
				.encodeStart(JsonOps.INSTANCE, CompanionData.DEFAULT)
				.result()
				.orElseThrow()
				.asJsonObject
				.deepCopy()
				.apply { addProperty("level", level) }

			val decoded = CompanionData.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow()

			assertEquals(CompanionData.sanitizeLevel(level), decoded.level)
			assertTrue(decoded.level >= 1, "decoded level $level must be clamped to >= 1")
		}
	}

	@Test
	@DisplayName("a codec round-trip preserves a legitimate companion")
	fun codecRoundTripPreservesLegitimateData() {
		val original = CompanionData(null, 17f, 42, 7, 123L, CompanionMemory.empty(), "cyan")

		val json = CompanionData.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow()
		val decoded = CompanionData.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow()

		assertEquals(original.health, decoded.health)
		assertEquals(original.xp, decoded.xp)
		assertEquals(original.level, decoded.level)
		assertEquals(original.reviveAt, decoded.reviveAt)
		assertEquals(original.glassColor, decoded.glassColor)
	}
}