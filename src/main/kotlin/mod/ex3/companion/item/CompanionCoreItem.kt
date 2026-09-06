package mod.ex3.companion.item

import mod.ex3.companion.companion.CompanionData
import mod.ex3.companion.registry.ModComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * The companion core: crafted item that is inserted into the companion slot.
 * All persistent companion state (id, health, xp) lives on this stack as a data component.
 *
 * The health bar is rendered by the vanilla item-bar machinery (data-component driven):
 * damage = missing health. It is hidden entirely when health is full.
 */
class CompanionCoreItem(properties: Properties) : Item(properties) {

	override fun isFoil(stack: ItemStack): Boolean = true

	override fun isBarVisible(stack: ItemStack): Boolean {
		val data = stack.getOrDefault(ModComponents.COMPANION_DATA, CompanionData.DEFAULT)
		val max = CompanionData.maxHealth(data.level).coerceAtLeast(1f)
		return data.health < max
	}

	override fun getBarWidth(stack: ItemStack): Int {
		val data = stack.getOrDefault(ModComponents.COMPANION_DATA, CompanionData.DEFAULT)
		val max = CompanionData.maxHealth(data.level).coerceAtLeast(1f)
		return (data.health / max * 13).toInt().coerceIn(0, 13)
	}

	override fun getBarColor(stack: ItemStack): Int {
		val data = stack.getOrDefault(ModComponents.COMPANION_DATA, CompanionData.DEFAULT)
		val max = CompanionData.maxHealth(data.level).coerceAtLeast(1f)
		val fraction = (data.health / max).coerceIn(0f, 1f)
		return when {
			fraction > 0.75f -> 0xFF55FF55.toInt() // healthy (green)
			fraction > 0.25f -> 0xFFFFAA00.toInt() // moderate (orange)
			else -> 0xFFFF5555.toInt()             // low (red)
		}
	}
}
