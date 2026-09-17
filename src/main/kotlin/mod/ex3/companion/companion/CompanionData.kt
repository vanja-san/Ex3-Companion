package mod.ex3.companion.companion

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mod.ex3.companion.config.CompanionConfig
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipProvider
import java.util.function.Consumer
import java.util.UUID

/**
 * Persistent data of a companion stored on the core item (data component).
 * Kept intentionally small; leveling fields are prepared for future use.
 */
data class CompanionData(
	val companionId: UUID?, // UUID of the spawned entity, null when not summoned
	val health: Float,
	val xp: Int,
	val level: Int,
	val reviveAt: Long = 0L, // server game time (ticks) before the companion may respawn
	val memory: CompanionMemory = CompanionMemory.EMPTY,
	val glassColor: String = "clear", // "clear" = transparent glass, or dye color name
) : TooltipProvider {

	override fun addToTooltip(
		context: Item.TooltipContext,
		tooltip: Consumer<Component>,
		flag: TooltipFlag,
		components: DataComponentGetter,
	) {
		tooltip.accept(Component.translatable("ex3companion.tooltip.health", health.toInt()).withStyle(ChatFormatting.GREEN))
		val xpRemaining = (xpToNextLevel(level) - xp).coerceAtLeast(0)
		// Level in blue, XP in gray — separate components so each keeps its own color.
		val levelComp = Component.translatable("ex3companion.tooltip.level", level).withStyle(ChatFormatting.BLUE)
		val xpComp = Component.translatable("ex3companion.tooltip.xp", xpRemaining).withStyle(ChatFormatting.GRAY)
		tooltip.accept(levelComp.append(xpComp))

		val dmg = damage(level)
		val intervalTicks = attackIntervalTicks(level)
		val intervalSec = String.format("%.1f", intervalTicks / 20.0)
		// Damage in red, attack speed in gray — separate components so each keeps its own color.
		val dmgComp = Component.translatable("ex3companion.tooltip.combat", "%.1f".format(dmg)).withStyle(ChatFormatting.RED)
		val speedComp = Component.translatable("ex3companion.tooltip.attack_speed", intervalSec).withStyle(ChatFormatting.GRAY)
		tooltip.accept(dmgComp.append(speedComp))

		val healCfg = CompanionConfig
		if (level >= healCfg.healingUnlockLevel) {
			val healPerSec = "%.1f".format((healCfg.healingBasePerTick + (level - healCfg.healingUnlockLevel) * healCfg.healingPerLevelAbove).coerceAtMost(healCfg.healingMaxPerTick) * 20)
			tooltip.accept(Component.translatable("ex3companion.tooltip.healing_active", healPerSec).withStyle(ChatFormatting.LIGHT_PURPLE))
		} else {
			val lvlLeft = healCfg.healingUnlockLevel - level
			tooltip.accept(Component.translatable("ex3companion.tooltip.healing_locked", healCfg.healingUnlockLevel, lvlLeft).withStyle(ChatFormatting.DARK_GRAY))
		}

		if (health < maxHealth(level)) {
			tooltip.accept(Component.translatable("ex3companion.tooltip.recovering").withStyle(ChatFormatting.GOLD))
		}
	}

	/** String form of [companionId] for codecs (empty when absent). */
	val idString: String get() = companionId?.toString() ?: ""

	fun withHealth(newHealth: Float): CompanionData =
		copy(health = newHealth.coerceIn(MIN_HEALTH, maxHealth(level)))

	fun withId(newId: UUID?): CompanionData = copy(companionId = newId)

	/** Marks the companion as "recovering" until the given game time (ticks). */
	fun withReviveAt(newReviveAt: Long): CompanionData = copy(reviveAt = newReviveAt)

	fun withGlassColor(color: String): CompanionData = copy(glassColor = color)

	fun withMemory(newMemory: CompanionMemory): CompanionData = copy(memory = newMemory)

	companion object {
		const val MIN_HEALTH: Float = 0f

		val DEFAULT: CompanionData get() = CompanionData(null, CompanionConfig.healthBase, 0, 1, 0L)

		/** XP required to advance from [level] to [level] + 1. Grows quadratically so late levels are harder. */
		fun xpToNextLevel(level: Int): Int = level * level * CompanionConfig.xpFormulaMultiplier

		/** Maximum health a companion of the given level can have. */
		fun maxHealth(level: Int): Float {
			val base = CompanionConfig.healthBase + (level - 1) * CompanionConfig.healthPerLevel
			return base.coerceAtMost(CompanionConfig.healthCap)
		}

		/** Attack interval in ticks for the given level. */
		fun attackIntervalTicks(level: Int): Int =
			(CompanionConfig.combatIntervalBase - ((level - 1) * CompanionConfig.combatIntervalPerLevel)).coerceAtLeast(CompanionConfig.combatIntervalMin)

		/** Attack damage for the given level. */
		fun damage(level: Int): Float =
			(CompanionConfig.combatDamageBase + ((level - 1) * CompanionConfig.combatDamagePerLevel)).coerceAtMost(CompanionConfig.combatDamageCap)

		private fun parseId(raw: String): UUID? =
			if (raw.isEmpty()) null else runCatching { UUID.fromString(raw) }.getOrNull()

	/**
	 * NOTE: The default values for optional fields reference CompanionConfig's
	 * static entries at codec-build time (class load). If the config is reloaded later,
	 * these defaults won't update. This is fine because existing save data always has
	 * explicit values, and the codec defaults are only used for brand-new items.
	 */
	val CODEC: Codec<CompanionData> = RecordCodecBuilder.create { instance ->
			instance.group(
				Codec.STRING.optionalFieldOf("companion_id", "").forGetter(CompanionData::idString),
				Codec.FLOAT.optionalFieldOf("health", CompanionConfig.healthBase).forGetter(CompanionData::health),
				Codec.INT.optionalFieldOf("xp", 0).forGetter(CompanionData::xp),
				Codec.INT.optionalFieldOf("level", 1).forGetter(CompanionData::level),
				Codec.LONG.optionalFieldOf("revive_at", 0L).forGetter(CompanionData::reviveAt),
				CompanionMemory.CODEC.optionalFieldOf("memory", CompanionMemory.EMPTY).forGetter(CompanionData::memory),
				Codec.STRING.optionalFieldOf("glass_color", "clear").forGetter(CompanionData::glassColor),
			).apply(instance) { id: String, health: Float, xp: Int, level: Int, reviveAt: Long, memory: CompanionMemory, glassColor: String ->
				CompanionData(parseId(id), health, xp, level, reviveAt, memory, glassColor)
			}
		}

		val STREAM_CODEC: StreamCodec<io.netty.buffer.ByteBuf, CompanionData> = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			CompanionData::idString,
			ByteBufCodecs.FLOAT,
			CompanionData::health,
			ByteBufCodecs.VAR_INT,
			CompanionData::xp,
			ByteBufCodecs.VAR_INT,
			CompanionData::level,
			ByteBufCodecs.VAR_LONG,
			CompanionData::reviveAt,
			ByteBufCodecs.STRING_UTF8,
			CompanionData::glassColor,
		) { id: String, health: Float, xp: Int, level: Int, reviveAt: Long, glassColor: String ->
			CompanionData(parseId(id), health, xp, level, reviveAt, CompanionMemory.EMPTY, glassColor)
		}
	}
}
