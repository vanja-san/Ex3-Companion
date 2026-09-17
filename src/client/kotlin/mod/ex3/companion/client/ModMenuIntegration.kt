package mod.ex3.companion.client

import com.jahirtrap.configlib.TXFConfigClient
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import mod.ex3.companion.Ex3Companion

/**
 * Own Mod Menu integration. A mod's own [getModConfigScreenFactory] takes
 * precedence over the config screen factory that configlibtxf provides via
 * [getProvidedConfigScreenFactories] (Mod Menu uses `put` over `putIfAbsent`),
 * so this is the single place that decides what opens from Mod Menu.
 *
 * The screen only hosts server settings, so it is only opened for players who
 * may manage them ([Ex3ConfigGate]): in a singleplayer/LAN world and for
 * operators on a dedicated server. In every other context the button and
 * quick-configure icon are hidden by the optional ModsScreenMixin /
 * ModListEntryMixin and opening the screen here is a no-op (returns the
 * parent screen).
 */
class ModMenuIntegration : ModMenuApi {

	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { parent ->
		if (Ex3ConfigGate.canShowConfigScreen()) {
			TXFConfigClient.getScreen(parent, Ex3Companion.MOD_ID)
		} else {
			parent
		}
	}
}