package mod.ex3.companion.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import mod.ex3.companion.companion.CoreSlotBackingContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces the slot background sprite to render even when the companion core item
 * is present, so the slot border is always visible behind the item.
 * Checks the slot's container type rather than the slot class directly,
 * because in the creative inventory the slot is wrapped in a SlotWrapper
 * that delegates to the real CompanionCoreSlot.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {

	@Unique
	private static final Identifier EX3_SLOT_SPRITE =
		Identifier.fromNamespaceAndPath("minecraft", "container/slot");

	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void ex3$renderCompanionSlotBackground(
		GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci
	) {
		if (slot.container instanceof CoreSlotBackingContainer && !slot.getItem().isEmpty()) {
			graphics.blitSprite(
				RenderPipelines.GUI_TEXTURED, EX3_SLOT_SPRITE,
				slot.x, slot.y, 16, 16
			);
		}
	}
}
