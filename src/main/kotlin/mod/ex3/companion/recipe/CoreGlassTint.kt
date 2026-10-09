package mod.ex3.companion.recipe

import mod.ex3.companion.companion.CompanionData
import mod.ex3.companion.registry.ModComponents
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.CustomModelData

/**
 * Reads and writes the companion core's glass tint.
 *
 * The tint is stored in three places on purpose: the mod's own component (what the code
 * reads), the item-model select (what the renderer reads), and vanilla `custom_data` (what
 * anything looking at raw NBT sees). They are written together so they cannot disagree.
 */
object CoreGlassTint {

	/** The untinted colour name. */
	const val CLEAR: String = "clear"

	/**
	 * Writes [color] into every source the game reads.
	 *
	 * The existing `custom_data` NBT is merged into rather than replaced, so an unrelated
	 * key written by another tool (or a future field of ours) survives re-tinting instead
	 * of being silently dropped.
	 */
	fun apply(stack: ItemStack, color: String) {
		val data = stack.getOrDefault(ModComponents.COMPANION_DATA, CompanionData.DEFAULT)
		stack.set(ModComponents.COMPANION_DATA, data.withGlassColor(color))
		stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(emptyList(), emptyList(), listOf(color), emptyList()))
		val existing = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
		existing.put("companion", CompoundTag().also { it.putString("glassColor", color) })
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(existing))
	}

	/** A fresh, untinted core, used as the base for recipe-book previews. */
	fun freshCore(): ItemStack {
		val stack = ItemStack(mod.ex3.companion.registry.ModItems.COMPANION_CORE)
		apply(stack, CLEAR)
		return stack
	}
}