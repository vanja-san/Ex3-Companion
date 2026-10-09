package mod.ex3.companion.mixin;

import mod.ex3.companion.companion.CoreSlotBackingContainer;
import mod.ex3.companion.network.CompanionSlotSyncPayload;
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
 * and nudges the server after slot changes.
 *
 * Vanilla's {@code selectTab} rebuilds the INVENTORY tab from the player's
 * inventoryMenu, wrapping every slot in a {@code SlotWrapper} and computing positions
 * from the slot index. The companion slot (index 46) falls into the final {@code else}
 * branch, where {@code i >= 36} forces {@code y = 112} — which lands the slot on the
 * hotbar row (x=27, y=112), overlapping a real hotbar slot. It is moved out of the way
 * here.
 *
 * {@code SlotWrapper} passes {@code target.container} to its {@code Slot} super
 * constructor, so the companion slot is still identified by
 * {@code slot.container instanceof CoreSlotBackingContainer} after wrapping.
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
	 * Reports companion-slot changes to the server after a creative click.
	 *
	 * Vanilla's {@code slotClicked} applies inventory-tab clicks locally and never sends a
	 * packet, so the server's copy of the slot would otherwise stay stale. The companion
	 * slot is an inventory-menu slot, for which vanilla has no equivalent of
 * {@code ServerboundSetCreativeModeSlotPacket}.
 *
 * Only sent when the content actually differs from what we last sent, to avoid a
 * login-race overwrite.
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
					if (ClientPlayNetworking.canSend(CompanionSlotSyncPayload.TYPE)) {
						ClientPlayNetworking.send(new CompanionSlotSyncPayload(current.copy()));
					}
				}
				return;
			}
		}
	}
}
