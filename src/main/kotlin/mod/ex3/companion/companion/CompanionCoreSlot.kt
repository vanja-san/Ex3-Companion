package mod.ex3.companion.companion

import mod.ex3.companion.registry.ModItems
import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * The companion slot shown in the player inventory, in the free space of the armor area.
 * Only accepts the companion core item.
 */
class CompanionCoreSlot(
	container: CoreSlotBackingContainer,
	x: Int,
	y: Int,
) : Slot(container, 0, x, y) {

	/**
	 * Vanilla only draws a slot background for non-empty slots or slots that provide a
	 * no-item icon. Returning the standard slot sprite here makes an empty companion slot
	 * visible as a normal vanilla slot (without a placeholder icon, like the user asked).
	 */
	override fun getNoItemIcon(): Identifier =
		Identifier.fromNamespaceAndPath("minecraft", "container/slot")

	override fun mayPlace(stack: ItemStack): Boolean =
		stack.`is`(ModItems.COMPANION_CORE)

	override fun getMaxStackSize(): Int = 1

	override fun setChanged() {
		super.setChanged()
		val owner = (container as? CoreSlotBackingContainer)?.owner() ?: return
		CoreSlotManager.onSlotChanged(owner)
	}
}
