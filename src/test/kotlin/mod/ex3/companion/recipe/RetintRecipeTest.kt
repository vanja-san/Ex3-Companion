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
 * Pins the shape of the retint recipe in the recipe book.
 *
 * The book represents one entry per recipe group, and that shape has real cost: grouping
 * several recipes into one button is what enables the right-click variants popup
 * (`RecipeButton.isOnlyOption()` is false only for groups), but it also halves the preview
 * cycle, because `RecipeButton.getDisplayStack()` advances a given recipe only once every
 * `30 ticks * selectedEntries.size()` while `RecipeBookComponent` steps the clock every 30
 * ticks. Vanilla's own dye recipes keep `selectedEntries.size() == 1` and cycle at the full
 * rate.
 *
 * So this mod ships exactly one retint recipe - any dye tints, a sponge clears - which
 * keeps the preview at the vanilla rate and leaves no variants popup to open. These tests
 * fail if the 16 per-colour recipes, or a separate sponge recipe, come back.
 *
 * SCOPE LIMIT: `display()` cannot be exercised here. It resolves `Ingredient.of(item)`,
 * which needs intrusive registry holders, and building an ItemStack at all needs a full
 * datapack reload. Book rendering stays manually verified.
 */
class RetintRecipeTest {

	companion object {
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
	}

	@Test
	@DisplayName("a single retint recipe covers every dye and the sponge")
	fun oneRetintRecipeCoversEverything() {
		val files = RECIPE_DIR.listFiles()!!.map { it.name }.filter { it.endsWith(".json") }.sorted()
		// companion_core (vanilla crafting_shaped) + the one retint recipe. The sponge
		// recipe is gone and there is no per-colour file.
		assertEquals(listOf("companion_core.json", "dye_companion_core.json"), files)
	}

	@Test
	@DisplayName("the retint recipe declares no group, so it stays a single-entry button")
	fun retintRecipeIsUngrouped() {
		// A group would re-introduce the halved preview cycle and a variants popup for a
		// single recipe, which has nothing to switch between.
		val recipe = load("dye_companion_core.json", DyeGlassRecipe.SERIALIZER.codec())
		assertEquals("", recipe.group())
	}

	@Test
	@DisplayName("the retint recipe parses and is the mod's own type")
	fun retintRecipeParses() {
		val recipe = load("dye_companion_core.json", DyeGlassRecipe.SERIALIZER.codec())
		assertTrue(recipe is DyeGlassRecipe, "expected DyeGlassRecipe, got ${recipe.javaClass.name}")
	}
}