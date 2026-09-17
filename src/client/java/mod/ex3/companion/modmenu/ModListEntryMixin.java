package mod.ex3.companion.modmenu;

import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import mod.ex3.companion.client.Ex3ConfigGate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Optional Mod Menu integration (see {@code ex3.modmenu.mixins.json}):
 * removes the quick-configure entry of the Ex³ Companion mod whenever its
 * config screen is not available ([Ex3ConfigGate]) — the hover icon is not
 * drawn and clicks on the row do not open the screen. Mirrors the hidden
 * "Configure" button from {@link ModsScreenMixin}. Other mods are unaffected.
 *
 * Klaxon: this mixin targets an optional mod's classes and is `required:
 * false`, so when the Mod Menu API shape changes the config is skipped with a
 * warning instead of crashing the game.
 */
@Pseudo
@Mixin(value = ModListEntry.class, remap = false)
public abstract class ModListEntryMixin {

	@Shadow
	public Mod mod;

	@Shadow
	protected ModListWidget list;

	@Shadow
	public abstract void openConfig();

	@Redirect(
		method = "mouseClicked",
		at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/gui/widget/entries/ModListEntry;openConfig()V")
	)
	private void ex3$guardQuickConfigureClick(ModListEntry self, MouseButtonEvent click, boolean doubleClick) {
		if (!isHidden(self.mod.getId())) {
			self.openConfig();
		}
	}

	@Redirect(
		method = "extractContent",
		at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/gui/ModsScreen;getModHasConfigScreen(Ljava/lang/String;)Z")
	)
	private boolean ex3$hideQuickConfigureIcon(
		ModsScreen screen, String modId, GuiGraphicsExtractor drawContext, int mouseX, int mouseY, boolean mouseOver, float tickDelta
	) {
		if (isHidden(modId)) {
			return false;
		}
		return screen.getModHasConfigScreen(modId);
	}

	private boolean isHidden(String modId) {
		// Only this mod's entry is gated; other mods keep their own buttons.
		// "ex3-companion" comes from Ex3Companion.MOD_ID; kept literal to avoid
		// a dependency from the mixin package on the main mod class.
		return "ex3-companion".equals(modId) && !Ex3ConfigGate.canShowConfigScreen();
	}
}