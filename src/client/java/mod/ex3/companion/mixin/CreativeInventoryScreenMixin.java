package mod.ex3.companion.mixin;

import mod.ex3.companion.companion.CoreSlotBackingContainer;
import mod.ex3.companion.network.CompanionSlotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Positions the companion core slot correctly in the creative inventory
 * and syncs slot changes to the server via a custom packet.
 * Vanilla's {@code selectTab} wraps every slot from the player's inventoryMenu
 * into a SlotWrapper with auto-calculated positions. The companion slot (index 46)
 * falls into the default branch and ends up off-screen.
 * Creative-mode inventory clicks don't reliably trigger server-side
 * {@code Container.setChanged()} for custom slots, so we send an
 * explicit sync packet after each click — but only when the content
 * actually changed, to avoid overwriting the server state on login.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {

	@Shadow
	private static CreativeModeTab selectedTab;

	@Unique
	private static final int EX3_SLOT_X = 127;
	@Unique
	private static final int EX3_SLOT_Y = 20;

	/** Last item we sent to the server — prevents login-race overwrite. */
	@Unique
	private ItemStack ex3$lastSentCore = ItemStack.EMPTY;

	@Inject(method = "selectTab", at = @At("TAIL"))
	private void ex3$positionCompanionSlot(CreativeModeTab tab, CallbackInfo ci) {
		if (tab.getType() != CreativeModeTab.Type.INVENTORY) return;

		CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
		var menu = self.getMenu();
		for (int i = menu.slots.size() - 1; i >= 0; i--) {
			Slot slot = menu.slots.get(i);
			if (slot.container instanceof CoreSlotBackingContainer) {
				((SlotAccessor) slot).ex3$setX(EX3_SLOT_X);
				((SlotAccessor) slot).ex3$setY(EX3_SLOT_Y);
				ex3$lastSentCore = slot.getItem().copy();
				return;
			}
		}
	}

	/**
	 * After vanilla processes a click on any slot, check if the companion slot
	 * changed and sync the new state to the server — but only if it actually
	 * differs from what we last sent, to avoid a login-race overwrite.
	 */
	@Inject(method = "slotClicked", at = @At("RETURN"))
	private void ex3$syncCompanionSlot(
		Slot slot, int slotId, int buttonNum, ContainerInput clickType, CallbackInfo ci
	) {
		if (selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;

		CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
		for (Slot s : self.getMenu().slots) {
			if (s.container instanceof CoreSlotBackingContainer) {
				ItemStack current = s.getItem();
				if (!ItemStack.matches(current, ex3$lastSentCore)) {
					ex3$lastSentCore = current.copy();
					if (ClientPlayNetworking.canSend(CompanionSlotPayload.TYPE)) {
						ClientPlayNetworking.send(new CompanionSlotPayload(current.copy()));
					}
				}
				return;
			}
		}
	}
}
