package mod.ex3.companion.recipe

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import com.mojang.serialization.MapCodec
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Recipe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Locks the recipe-book grouping that the tint recipes depend on.
 *
 * The variants UI is gated on `RecipeButton.isOnlyOption()` -> `selectedEntries.size() == 1`
 * (RecipeButton.java:123), so the right-click variants popup only appears while a group
 * collects more than one recipe. That chain starts with `Recipe.BookInfo`'s optional
 * `group` string being parsed out of the recipe JSON, which is what these tests check.
 *
 * Recipes are parsed through each serializer's own codec rather than
 * `Recipe.DIRECT_CODEC`: the latter dispatches through the recipe-serializer registry,
 * which is already frozen by the time a unit test runs.
 *
 * SCOPE LIMIT: `display()` cannot be exercised here. It resolves `Ingredient.of(item)`,
 * which needs intrusive registry holders, and building an ItemStack at all needs a full
 * datapack reload. Both require a live game, so the book rendering stays manual-tested.
 */
class RecipeGroupTest {

	companion object {
		private const val GROUP = "companion_core_tint"
		private val RECIPE_DIR = File("src/main/resources/data/ex3-companion/recipe")

		@JvmStatic
		@BeforeAll
		fun bootstrapGame() {
			net.minecraft.SharedConstants.tryDetectVersion()
			net.minecraft.server.Bootstrap.bootStrap()
		}

		private fun load(name: String, codec: MapCodec<*>): Recipe<*> {
			val file = File(RECIPE_DIR, name)
			assertTrue(file.isFile, "missing recipe file: ${file.path}")
			val json = JsonParser.parseString(file.readText())
			return codec.codec().parse(JsonOps.INSTANCE, json)
				.result()
				.orElseThrow { AssertionError("failed to parse $name: $json") } as Recipe<*>
		}

		/** Reads just the recipe-book section (category + group) of a recipe file. */
		private fun bookInfoOf(name: String): CraftingRecipe.CraftingBookInfo {
			val file = File(RECIPE_DIR, name)
			assertTrue(file.isFile, "missing recipe file: ${file.path}")
			val json = JsonParser.parseString(file.readText()).asJsonObject
			return CraftingRecipe.CraftingBookInfo.MAP_CODEC.codec()
				.parse(JsonOps.INSTANCE, json)
				.result()
				.orElseThrow { AssertionError("failed to read book info of $name") }
		}
	}

	@Test
	@DisplayName("the dye recipe parses from a single JSON with no per-colour colour field")
	fun dyeRecipeParsesWithoutColourField() {
		val recipe = load("dye_companion_core.json", DyeGlassRecipe.SERIALIZER.codec())
		assertTrue(recipe is DyeGlassRecipe, "expected DyeGlassRecipe, got ${recipe.javaClass.name}")
	}

	/**
	 * The decisive check for the variants UI: the JSON's group string, read through the
	 * exact codec our recipes embed via `CraftingRecipe.CraftingBookInfo.MAP_CODEC`.
	 *
	 * Parsed standalone rather than through the recipe, because building a recipe runs
	 * `createPlacementInfo()` in its constructor, which resolves ModItems and therefore
	 * cannot run outside a live registry (see the scope limit above).
	 */
	@Test
	@DisplayName("both tint recipes declare the same group through CraftingBookInfo")
	fun bothTintRecipesDeclareTheGroupInJson() {
		assertEquals(GROUP, bookInfoOf("dye_companion_core.json").group())
		assertEquals(GROUP, bookInfoOf("clear_companion_core.json").group())
	}

	@Test
	@DisplayName("the tint recipes are a single entry each, not one per colour")
	fun tintRecipesAreSingleFiles() {
		// A regression guard for the old layout: 16 dye recipe files, one per colour.
		val dyeFiles = RECIPE_DIR.listFiles()!!.filter { it.name.startsWith("dye_companion_core") }.map { it.name }.sorted()
		assertEquals(listOf("dye_companion_core.json"), dyeFiles)
	}
}