package mod.ex3.companion.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import mod.ex3.companion.client.config.CompanionConfigFactory

class ModMenuIntegration : ModMenuApi {

	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
		ConfigScreenFactory { parent -> CompanionConfigFactory.createScreen(parent) }
}
