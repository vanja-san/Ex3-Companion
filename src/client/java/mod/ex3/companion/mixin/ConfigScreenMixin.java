package mod.ex3.companion.mixin;

import com.jahirtrap.configlib.TXFConfigClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
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
 * The tab map is reached **reflectively on purpose**. A previous version used
 * {@code @Shadow} on the library's {@code tabs} field inside a {@code required:true}
 * mixin config: any configlibtxf release that renamed or re-typed that field would
 * hard-crash the game on startup with no way to recover. Reflection degrades instead —
 * if the field is gone the mixin simply does nothing and the (harmless) tab bar stays.
 * The same reasoning makes this {@code @Pseudo}: a missing target class or a renamed
 * {@code init} becomes a warning rather than a fatal error.
 *
 * ConfigLib is bundled jar-in-jar (never obfuscated), so the mixin stays
 * {@code remap = false} and uses the library's own class/method names.
 */
@Pseudo
@Mixin(value = TXFConfigClient.ConfigScreen.class, remap = false)
public abstract class ConfigScreenMixin {

	private static final java.util.concurrent.atomic.AtomicBoolean LOOKUP_DONE =
		new java.util.concurrent.atomic.AtomicBoolean(false);

	private static volatile Field ex3$tabsField;

	@Inject(method = "init", at = @At("HEAD"))
	private void ex3$dropTabBar(CallbackInfo ci) {
		Map<?, ?> tabs = ex3$resolveTabsField(this);
		if (tabs != null) tabs.clear();
	}

	private static Map<?, ?> ex3$resolveTabsField(Object instance) {
		Field field = ex3$tabsField;
		if (field == null && LOOKUP_DONE.compareAndSet(false, true)) {
			try {
				Field found = TXFConfigClient.ConfigScreen.class.getDeclaredField("tabs");
				found.setAccessible(true);
				ex3$tabsField = found;
				field = found;
			} catch (NoSuchFieldException | RuntimeException ignored) {
				// Library changed: leave the tab bar in place rather than crashing.
				ex3$tabsField = null;
			}
		}
		if (field == null) return null;
		try {
			Object value = field.get(instance);
			return (value instanceof Map) ? (Map<?, ?>) value : null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}
}