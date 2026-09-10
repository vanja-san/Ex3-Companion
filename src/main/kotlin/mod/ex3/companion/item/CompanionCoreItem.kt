package mod.ex3.companion.item

import mod.ex3.companion.companion.CompanionData
import mod.ex3.companion.companion.CoreSlotManager
import mod.ex3.companion.registry.ModComponents
import mod.ex3.companion.registry.ModItems
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

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

	/**
	 * Right-click in air: equips the core into the companion slot.
	 * If a core is already equipped, swaps it with the held one.
	 */
	override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
		if (level.isClientSide) {
			return InteractionResult.FAIL
		}

		val heldStack = player.getItemInHand(hand)
		val container = CoreSlotManager.getOrCreateContainer(player)
		val equippedStack = container.getItem(0)

		if (equippedStack.`is`(ModItems.COMPANION_CORE)) {
			// Something already equipped — swap it out.
			player.setItemInHand(hand, equippedStack.copy())
			container.setItem(0, heldStack.copy())
			CoreSlotManager.onSlotChanged(player)
			return InteractionResult.SUCCESS
		}

		// Empty slot — equip from hand.
		player.setItemInHand(hand, ItemStack.EMPTY)
		container.setItem(0, heldStack.copy())
		CoreSlotManager.onSlotChanged(player)
		return InteractionResult.SUCCESS
	}
}
