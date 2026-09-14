package mod.ex3.companion.mixin

import mod.ex3.companion.registry.ModItems
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

/**
 * Allows renaming the companion core in an anvil exactly like any vanilla item.
 *
 * Vanilla only produces an anvil result for items that can store enchantments
 * ([net.minecraft.world.item.enchantment.EnchantmentHelper.canStoreEnchantments]);
 * the companion core is not enchantable, so it would normally get no result slot
 * at all. We lift that gate for the core alone and let the ENTIRE vanilla rename
 * flow run unchanged: cost (1 level), rename-only logic, result-slot preview,
 * broadcast, and the take handler (level deduction, text filter, anvil damage).
 *
 * The name is stored in the item's vanilla [net.minecraft.core.component.DataComponents.CUSTOM_NAME],
 * so it survives save/load and any respawn of the companion automatically.
 */
@Mixin(AnvilMenu::class)
abstract class AnvilMenuMixin {

	@Redirect(
		method = ["createResult"],
		at = [
			At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;canStoreEnchantments(Lnet/minecraft/world/item/ItemStack;)Z",
			),
		],
	)
	private fun `ex3$canStoreEnchantments`(input: ItemStack): Boolean =
		if (input.`is`(ModItems.COMPANION_CORE)) true else EnchantmentHelper.canStoreEnchantments(input)
}