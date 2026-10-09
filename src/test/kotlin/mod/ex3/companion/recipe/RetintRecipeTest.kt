package mod.ex3.companion.recipe

import com.google.gson.JsonParser
import net.minecraft.world.item.crafting.CraftingRecipe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins the shape of the retint recipes in the recipe book.
 *
 * The book renders one button per recipe group, and grouping is what enables the
 * right-click variants popup: `RecipeBookPage.mouseClicked()` opens the overlay only when
 * `!button.isOnlyOption()`, and `isOnlyOption()` is `selectedEntries.size() == 1`. All
 * seventeen retint recipes therefore have to share one group.
 *
 * This mirrors vanilla dyed items exactly. Stained glass ships sixteen separate
 * `crafting_shaped` recipes - one per dye, each pinning its own dye ingredient - that all
 * declare `"group": "stained_glass"`. The book shows a single entry for them, and the
 * overlay lists sixteen separate buttons, each with its own statically correct result icon.
 *
 * That is why these are plain vanilla `crafting_shapeless` recipes with a fully specified
 * result instead of custom recipe types. A custom `SlotDisplay` was tried first, to render
 * one button cycling through the colours; that is the wrong shape here, because the book
 * has to hold one recipe per variant. With a static result per recipe,
 * `RecipeButton.getDisplayStack()` never cycles within a recipe, so the 30-tick book
 * stepping cannot make a preview look stuck.
 *
 * SCOPE LIMIT: the recipes cannot be decoded here. `ShapelessRecipe.CODEC` resolves the
 * result item and the `ex3-companion:companion_data` component through registries that
 * `Bootstrap.bootStrap()` does not populate with mod content, so both fail with an unknown
 * registry key. Only the on-disk shape is pinned here; `assemble()` and book rendering stay
 * manually verified in game.
 */
class RetintRecipeTest {

	companion object {
		private const val GROUP = "companion_core_tint"
		private const val ITEM = "ex3-companion:companion_core"
		private val RECIPE_DIR = File("src/main/resources/data/ex3-companion/recipe")

		/** Vanilla's sixteen dye colours, in the order the game registers them. */
		private val DYE_COLORS = listOf(
			"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
			"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
		)

		private fun recipeNames(): List<String> =
			DYE_COLORS.map { "dye_companion_core_$it.json" } + "clear_companion_core.json"

		@JvmStatic
		@BeforeAll
		fun bootstrapGame() {
			net.minecraft.SharedConstants.tryDetectVersion()
			net.minecraft.server.Bootstrap.bootStrap()
		}

		private fun readJson(name: String) = JsonParser.parseString(File(RECIPE_DIR, name).readText()).asJsonObject

		private fun assertFileExists(name: String) {
			val file = File(RECIPE_DIR, name)
			assertTrue(file.isFile, "missing recipe file: ${file.path}")
		}

		/** Reads just the recipe-book section (category + group) of a recipe file. */
		private fun groupOf(name: String): String {
			assertFileExists(name)
			return CraftingRecipe.CraftingBookInfo.MAP_CODEC.codec()
				.parse(com.mojang.serialization.JsonOps.INSTANCE, readJson(name))
				.result()
				.orElseThrow { AssertionError("failed to read book info of $name") }
				.group()
		}

		/** The ingredient ids, as written in the JSON. Vanilla writes a bare id per ingredient. */
		private fun ingredientIdsOf(name: String): List<String> =
			readJson(name).getAsJsonArray("ingredients").map { it.asString }

		/** The result's `custom_model_data` string, which drives the item-model select. */
		private fun resultModelDataOf(name: String): String =
			readJson(name).getAsJsonObject("result")
				.getAsJsonObject("components")
				.getAsJsonObject("minecraft:custom_model_data")
				.getAsJsonArray("strings")[0].asString
	}

	@Test
	@DisplayName("retinting is one recipe per colour plus sponge, like vanilla stained glass")
	fun oneRecipePerColourPlusSponge() {
		val files = RECIPE_DIR.listFiles()!!.map { it.name }.filter { it.endsWith(".json") }.sorted()
		assertEquals((recipeNames() + "companion_core.json").sorted(), files)
	}

	@Test
	@DisplayName("every retint recipe declares the same group, which is what opens the variants popup")
	fun everyRetintRecipeSharesAGroup() {
		for (name in recipeNames()) {
			assertEquals(GROUP, groupOf(name), "wrong group in $name")
		}
	}

	@Test
	@DisplayName("every retint recipe takes the core plus exactly one tint item")
	fun everyRetintRecipeIsCorePlusTint() {
		for (color in DYE_COLORS) {
			assertEquals(
				listOf(ITEM, "minecraft:${color}_dye"),
				ingredientIdsOf("dye_companion_core_$color.json"),
			)
		}

		assertEquals(listOf(ITEM, "minecraft:sponge"), ingredientIdsOf("clear_companion_core.json"))
	}

	@Test
	@DisplayName("each recipe's result carries its own colour, so every overlay button looks different")
	fun eachResultCarriesItsOwnColour() {
		for (color in DYE_COLORS) {
			assertEquals(color, resultModelDataOf("dye_companion_core_$color.json"))
		}

		assertEquals(CoreGlassTint.CLEAR, resultModelDataOf("clear_companion_core.json"))
	}
}