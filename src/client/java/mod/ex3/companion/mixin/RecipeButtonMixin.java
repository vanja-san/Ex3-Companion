package mod.ex3.companion.mixin;

import mod.ex3.companion.companion.CompanionTooltip;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Suppresses the core's stat block only while the recipe book builds its tooltip.
 *
 * {@code RecipeBook.getTooltipText} is the single place in the game where a recipe book
 * entry's tooltip is assembled, so scoping the suppression around that call is exact: the
 * companion slot in the inventory keeps its stats even while the book is open next to it.
 *
 * Testing the screen type cannot do this. Crafting, inventory and furnace screens all
 * extend AbstractRecipeBookScreen permanently, which means "has a recipe book", not "the
 * book is showing", and the companion slot lives in the inventory one. The tooltip pipeline
 * offers no other signal either - Screen.getTooltipFromItem passes the same TooltipContext,
 * player and flag on both paths, and TooltipDisplay.hide_tooltip applies to the stack
 * everywhere rather than in the book alone.
 *
 * The flag is restored on the way out, so it is off for every other tooltip.
 */
@Mixin(RecipeButton.class)
public abstract class RecipeButtonMixin {

	@Inject(method = "getTooltipText", at = @At("HEAD"))
	private void ex3$hideCoreStats(ItemStack displayStack, CallbackInfoReturnable<List<Component>> cir) {
		CompanionTooltip.setHidden(true);
	}

	@Inject(method = "getTooltipText", at = @At("RETURN"))
	private void ex3$restoreTooltip(ItemStack displayStack, CallbackInfoReturnable<List<Component>> cir) {
		CompanionTooltip.setHidden(false);
	}
}
