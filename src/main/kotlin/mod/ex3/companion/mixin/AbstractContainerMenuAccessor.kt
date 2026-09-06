package mod.ex3.companion.mixin

import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

/**
 * Exposes the protected `addSlot` of [AbstractContainerMenu].
 * Needed because @Shadow can only reference members declared directly in the
 * target class, while InventoryMenu inherits addSlot from its parent.
 */
@Mixin(AbstractContainerMenu::class)
abstract class AbstractContainerMenuAccessor {

	@Invoker("addSlot")
	abstract fun `ex3$invokeAddSlot`(slot: Slot): Slot
}
