package mod.ex3.companion.recipe

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mod.ex3.companion.Ex3Companion
import mod.ex3.companion.registry.ModItems
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.NormalCraftingRecipe
import net.minecraft.world.item.crafting.PlacementInfo
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.world.level.Level

/**
 * Crafting recipe: companion_core + any dye -> the same core tinted in that dye's colour.
 *
 * One recipe covers all 16 colours, mirroring vanilla's [net.minecraft.world.item.crafting.DyeRecipe]:
 * the colour is derived from the dye actually placed in the grid rather than baked into the
 * recipe. This mod previously shipped one recipe per colour, which made every entry in the
 * recipe book identical (names come from the result item) and needed an exact-colour match
 * in [matches] purely to stop the wrong dye from matching every recipe.
 *
 * Preserves every companion component (health, XP, level, memory); a vanilla shapeless
 * recipe would create a fresh item via result.create() and silently drop all of them.
 */
class DyeGlassRecipe(
	commonInfo: Recipe.CommonInfo,
	bookInfo: CraftingRecipe.CraftingBookInfo,
) : NormalCraftingRecipe(commonInfo, bookInfo) {

	override fun matches(input: CraftingInput, level: Level): Boolean =
		coreAndDye(input) != null

	override fun assemble(input: CraftingInput): ItemStack {
		val color = coreAndDye(input) ?: return ItemStack.EMPTY
		for (i in 0 until input.size()) {
			val stack = input.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) {
				val result = stack.copy()
				ClearGlassRecipe.applyGlassColor(result, color)
				return result
			}
		}
		return ItemStack.EMPTY
	}

	/** The colour to apply, or null unless the grid holds exactly one core and one dye. */
	private fun coreAndDye(input: CraftingInput): String? {
		var core = false
		var color: String? = null
		for (i in 0 until input.size()) {
			val stack = input.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) {
				core = true
			} else if (!stack.isEmpty) {
				// Anything that is not exactly one dye (a second dye, a sponge, junk) fails.
				val c = DYE_COLOR_BY_ITEM[stack.item]
				if (c == null || color != null) return null
				color = c
			}
		}
		return if (core && color != null) color else null
	}

	override fun createPlacementInfo(): PlacementInfo =
		PlacementInfo.create(listOf(
			Ingredient.of(ModItems.COMPANION_CORE),
			DYE_INGREDIENT,
		))

	/**
	 * Feeds the vanilla recipe book. Empty is not enough: `RecipeManager` builds every
	 * client-side book entry from `display()`, so an empty list hides the recipe entirely
	 * even though `category` and `createPlacementInfo()` are set.
	 *
	 * The result slot uses [DyedCoreDemoDisplay], our port of vanilla's `DyedSlotDemo`.
	 * Vanilla's own demo cannot work here: it tints by writing the vanilla dye colour
	 * component, while our model selects its texture on `custom_model_data`, and
	 * `dye_color` is not a model select property at all.
	 */
	override fun display(): List<RecipeDisplay> {
		val dye = DYE_INGREDIENT.display()
		val core = Ingredient.of(ModItems.COMPANION_CORE).display()
		return listOf(
			ShapelessCraftingRecipeDisplay(
				listOf(core, dye),
				DyedCoreDemoDisplay(dye, core),
				SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE),
			),
		)
	}

	override fun getSerializer(): RecipeSerializer<out DyeGlassRecipe> = SERIALIZER

	companion object {
		private val DYE_INGREDIENT: Ingredient = Ingredient.of(
			BuiltInRegistries.ITEM.filter { it is DyeItem }.stream()
		)

		/** Maps each dye item to its color name (e.g. gray_dye -> "gray"). */
		private val DYE_COLOR_BY_ITEM: Map<Item, String> = DyeColor.entries.associate { dyeColor ->
			Items.DYE.pick(dyeColor) to dyeColor.serializedName
		}

		/** Colour name of the dye in [stack], or null if it holds no dye. */
		fun colorOf(stack: ItemStack): String? = DYE_COLOR_BY_ITEM[stack.item]

		val CODEC: MapCodec<DyeGlassRecipe> = RecordCodecBuilder.mapCodec { i ->
			i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter { it.commonInfo },
				CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter { it.bookInfo },
			).apply(i, ::DyeGlassRecipe)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DyeGlassRecipe> =
			StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, { it.commonInfo },
				CraftingRecipe.CraftingBookInfo.STREAM_CODEC, { it.bookInfo },
				::DyeGlassRecipe
			)

		val SERIALIZER: RecipeSerializer<DyeGlassRecipe> = RecipeSerializer(CODEC, STREAM_CODEC)

		fun register() {
			Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Ex3Companion.id("dye_glass"), SERIALIZER)
		}
	}
}