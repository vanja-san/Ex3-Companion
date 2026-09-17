package mod.ex3.companion.mixin;

import com.jahirtrap.configlib.TXFConfigClient;
import net.minecraft.client.gui.components.tabs.Tab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * configlibtxf always renders a MenuTabBar, even when the mod registers a single
 * category — a lone "Server" tab that adds nothing but a row of chrome. This
 * mixin clears the collected tabs before {@link TXFConfigClient.ConfigScreen#init}
 * adds the navigation widget, so no tab bar is drawn or handled.
 *
 * Only the tab-strip chrome is removed: the per-entry {@code EntryInfo.tab}
 * references (and the tab manager's current tab) are untouched, so every entry
 * of the single category still fills the list. The screen then shows the menu
 * header band above the scroll list instead of a tab row.
 *
 * ConfigLib is bundled jar-in-jar (never obfuscated), so the mixin stays
 * `remap = false` and uses the library's own class/method names.
 */
@Mixin(value = TXFConfigClient.ConfigScreen.class, remap = false)
public abstract class ConfigScreenMixin {

	@Shadow
	public Map<String, Tab> tabs;

	@Inject(method = "init", at = @At("HEAD"))
	private void ex3$dropTabBar(CallbackInfo ci) {
		this.tabs.clear();
	}
}