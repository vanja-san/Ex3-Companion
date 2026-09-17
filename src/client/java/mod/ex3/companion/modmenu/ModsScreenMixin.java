package mod.ex3.companion.modmenu;

import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import mod.ex3.companion.client.Ex3ConfigGate;
import net.minecraft.client.gui.components.SpriteIconButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mod Menu is an optional dependency, so this mixin lives in its own
 * `required: false` config (`ex3.modmenu.mixins.json`) and is marked
 * {@link Pseudo}: when Mod Menu is absent the whole mixin is skipped.
 *
 * Hides the Mod Menu "Configure" button while the Ex³ Companion mod is
 * selected and the player cannot manage its settings ([Ex3ConfigGate]),
 * i.e. outside of any world and on a dedicated server for non-operators.
 */
@Pseudo
@Mixin(value = ModsScreen.class, remap = false)
public abstract class ModsScreenMixin {

	@Shadow
	private ModListEntry selected;

	@Shadow
	private SpriteIconButton configureButton;

	@Inject(method = "updateSelectedEntry", at = @At("RETURN"))
	private void ex3$hideConfigOutsidePermittedContext(CallbackInfo ci) {
		if (this.configureButton != null && this.selected != null
				&& this.selected.getMod().getId().equals("ex3-companion")) {
			if (Ex3ConfigGate.canShowConfigScreen()) {
				this.configureButton.visible = true;
				this.configureButton.active = true;
			} else {
				this.configureButton.visible = false;
			}
		}
	}
}