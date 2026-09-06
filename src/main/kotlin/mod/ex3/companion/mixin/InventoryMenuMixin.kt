package mod.ex3.companion.mixin

import mod.ex3.companion.companion.CompanionCoreSlot
import mod.ex3.companion.companion.CoreSlotManager
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.InventoryMenu
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Adds the companion core slot to the player inventory menu,
 * in the free space of the armor slot area (right column, above the offhand slot).
 */
@Mixin(InventoryMenu::class)
abstract class InventoryMenuMixin {

    @Inject(
        method = ["<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V"],
        at = [At("TAIL")],
    )
    private fun `ex3$addCompanionSlot`(inventory: Inventory, active: Boolean, owner: Player, ci: CallbackInfo) {
        val self = this as InventoryMenu

        // Guard against double registration.
        if (self.slots.any { it is CompanionCoreSlot }) return

        // addSlot is declared in AbstractContainerMenu, so it is exposed via an invoker accessor.
        // The returned slot is the one actually registered; keep it for fast client lookup.
        val added = (self as AbstractContainerMenuAccessor).`ex3$invokeAddSlot`(
            CompanionCoreSlot(CoreSlotManager.getOrCreateContainer(owner), 77, 8),
        )
        if (added is CompanionCoreSlot) {
            CoreSlotManager.setCompanionSlot(owner, added)
        }
    }
}
