package mod.ex3.companion.recipe

import com.google.gson.JsonParser
import com.mojang.serialization.MapCodec
import net.minecraft.world.item.crafting.Recipe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins the shape of the retint recipes in the recipe book.
 *
 * The book represents one entry per recipe group, and grouping is what enables the
 * right-click variants popup: `RecipePage` opens the overlay only when
 * `!button.isOnlyOption()`, and `isOnlyOption()` is `selectedEntries.size() == 1`. The
 * dye and sponge recipes therefore have to share a group.
 *
 * That grouping also has a cost, and the reason it used to look broken is worth recording.
 * `RecipeBookComponent` steps the book clock every 30 ticks and
 * `RecipeButton.getDisplayStack()` advances a given recipe only once every
 * `30 ticks * selectedEntries.size()`, so with two entries each recipe's own preview moves
 * every 60 ticks. Before `DyedCoreDemoDisplay` existed, both entries also rendered as the
 * same colourless core, so the alternation was invisible and the preview looked stuck.
 * With a working preview the button visibly alternates every 30 ticks - a dyed core, then a
 * clear one - which matches vanilla's rate of change.
 *
 * So both recipes are required, in one group. These tests fail if the 16 per-colour recipes
 * or an ungrouped sponge recipe come back.
 *
 * SCOPE LIMIT: `display()` cannot be exercised here. It resolves `Ingredient.of(item)`,
 * which needs intrusive registry holders, and building an ItemStack at all needs a full
 * datapack reload. Book rendering stays manually verified.
 */
class RetintRecipeTest {

	companion object {
		private val GROUP = ClearGlassRecipe.BOOK_GROUP
		private val RECIPE_DIR = File("src/main/resources/data/ex3-companion/recipe")

		@JvmStatic
		@BeforeAll
		fun bootstrapGame() {
			net.minecraft.SharedConstants.tryDetectVersion()
			net.minecraft.server.Bootstrap.bootStrap()
		}

		/**
		 * Recipes are parsed through the serializer's own codec rather than
		 * `Recipe.DIRECT_CODEC`, which dispatches through the recipe-serializer registry -
		 * frozen by the time a unit test runs.
		 */
		private fun load(name: String, codec: MapCodec<*>): Recipe<*> {
			val file = File(RECIPE_DIR, name)
			assertTrue(file.isFile, "missing recipe file: ${file.path}")
			val json = JsonParser.parseString(file.readText())
			return codec.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json)
				.result()
				.orElseThrow { AssertionError("failed to parse $name: $json") } as Recipe<*>
		}

		/** Reads just the recipe-book section (category + group) of a recipe file. */
		private fun groupOf(name: String): String {
			val file = File(RECIPE_DIR, name)
			assertTrue(file.isFile, "missing recipe file: ${file.path}")
			val json = JsonParser.parseString(file.readText()).asJsonObject
			return net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo.MAP_CODEC.codec()
				.parse(com.mojang.serialization.JsonOps.INSTANCE, json)
				.result()
				.orElseThrow { AssertionError("failed to read book info of $name") }
				.group()
		}
	}

	@Test
	@DisplayName("retinting is two recipes - any dye, and sponge - not one per colour")
	fun twoRetintRecipesCoverEverything() {
		val files = RECIPE_DIR.listFiles()!!.map { it.name }.filter { it.endsWith(".json") }.sorted()
		// The core crafting recipe plus exactly two retint recipes. The old layout had
		// sixteen dye files as well.
		assertEquals(
			listOf("clear_companion_core.json", "companion_core.json", "dye_companion_core.json"),
			files,
		)
	}

	@Test
	@DisplayName("both retint recipes declare the same group, which is what opens the variants popup")
	fun bothRetintRecipesShareAGroup() {
		assertEquals(GROUP, groupOf("dye_companion_core.json"))
		assertEquals(GROUP, groupOf("clear_companion_core.json"))
	}

	@Test
	@DisplayName("the dye recipe parses and is the mod's own type")
	fun dyeRecipeParses() {
		assertTrue(load("dye_companion_core.json", DyeGlassRecipe.SERIALIZER.codec()) is DyeGlassRecipe)
	}

	@Test
	@DisplayName("the sponge recipe parses and is the mod's own type")
	fun clearRecipeParses() {
		assertTrue(load("clear_companion_core.json", ClearGlassRecipe.SERIALIZER.codec()) is ClearGlassRecipe)
	}
}