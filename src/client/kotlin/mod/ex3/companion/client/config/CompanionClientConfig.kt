package mod.ex3.companion.client.config

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Client-side-only settings stored in the client's own config directory
 * (config/ex3-companion-client.json). Unlike the gameplay config these never
 * leave the client — they only affect how the mod renders/behaves locally.
 */
class CompanionClientConfig {

	// NOTE: These are `var` (not `val`) because Gson deserializes them reflectively.
	// Kotlin `val` compiles to a `final` field; JDK 17+ prints a warning on final-field mutation.
	/** Companion emits light in the dark (requires LambDynamicLights installed). */
	@Expose var dynamicLight: Boolean = true

	companion object {
		private val LOGGER = LoggerFactory.getLogger("Ex3-Companion")
		private val GSON = GsonBuilder().setPrettyPrinting().create()
		private val FILE = File(FabricLoader.getInstance().configDir.toFile(), "ex3-companion-client.json")

		@Volatile
		private var instance: CompanionClientConfig? = null

		fun load() {
			try {
				if (FILE.exists()) {
					FileReader(FILE).use { reader ->
						instance = GSON.fromJson(reader, CompanionClientConfig::class.java)
					}
				}
			} catch (e: Exception) {
				LOGGER.warn("Failed to load ex3-companion client config, using defaults", e)
			}
			if (instance == null) {
				instance = CompanionClientConfig()
			}
			save()
		}

		fun save() {
			try {
				FileWriter(FILE).use { writer ->
					GSON.toJson(instance, writer)
				}
			} catch (e: Exception) {
				LOGGER.warn("Failed to save ex3-companion client config", e)
			}
		}

		fun get(): CompanionClientConfig {
			val current = instance
			if (current != null) return current
			synchronized(this) {
				val recheck = instance
				if (recheck != null) return recheck
				return CompanionClientConfig().also { instance = it }
			}
		}
	}
}