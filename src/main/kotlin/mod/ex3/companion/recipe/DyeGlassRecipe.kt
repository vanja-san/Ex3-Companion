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
import java.util.stream.Stream

/**
 * Crafting recipe: companion_core + any dye -> the core tinted in that dye's colour, and
 * companion_core + sponge -> the core untinted.
 *
 * One recipe covers every retint, which is what the recipe book can actually represent.
 * The colour comes from the dye in the grid, mirroring vanilla's
 * [net.minecraft.world.item.crafting.DyeRecipe], rather than being baked into the recipe.
 *
 * This mod previously shipped 16 recipes (one per colour) plus a separate sponge recipe.
 * Beyond making every book entry look identical - names come from the result item - that
 * layout fought the book: grouping several recipes into one button is what enables the
 * right-click variants popup, but it also halves the preview cycle, because
 * `RecipeButton.getDisplayStack()` advances a given recipe only every
 * `30 ticks * selectedEntries.size()`. A single entry cycles at the vanilla rate.
 *
 * Preserves every companion component (health, XP, level, memory); a vanilla shapeless
 * recipe would create a fresh item via result.create() and silently drop all of them.
 */
class DyeGlassRecipe(
	commonInfo: Recipe.CommonInfo,
	bookInfo: CraftingRecipe.CraftingBookInfo,
) : NormalCraftingRecipe(commonInfo, bookInfo) {

	override fun matches(input: CraftingInput, level: Level): Boolean =
		tintFor(input) != null

	override fun assemble(input: CraftingInput): ItemStack {
		val color = tintFor(input) ?: return ItemStack.EMPTY
		for (i in 0 until input.size()) {
			val stack = input.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) {
				val result = stack.copy()
				CoreGlassTint.apply(result, color)
				return result
			}
		}
		return ItemStack.EMPTY
	}

	/**
	 * The colour this grid produces, or null unless it holds exactly one core plus exactly
	 * one tinting item (a dye of any colour, or a sponge to go back to clear glass).
	 */
	private fun tintFor(input: CraftingInput): String? {
		var core = false
		var color: String? = null
		for (i in 0 until input.size()) {
			val stack = input.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) {
				core = true
			} else if (!stack.isEmpty) {
				// Two tinting items, or something that is neither a dye nor a sponge: reject.
				if (color != null) return null
				color = when {
					stack.`is`(Items.SPONGE) -> CoreGlassTint.CLEAR
					else -> COLOR_BY_ITEM[stack.item] ?: return null
				}
			}
		}
		return if (core && color != null) color else null
	}

	override fun createPlacementInfo(): PlacementInfo =
		PlacementInfo.create(listOf(
			Ingredient.of(ModItems.COMPANION_CORE),
			// Any dye, or a sponge to go back to clear glass.
			Ingredient.of(Stream.concat(TINT_ITEMS.stream(), Stream.of(Items.SPONGE))),
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

		/** Every dye item, used to build the "any dye or sponge" placement ingredient. */
		private val TINT_ITEMS: List<Item> = BuiltInRegistries.ITEM
			.filter { it is DyeItem }
			.toList()

		/** Maps each dye item to its color name (e.g. gray_dye -> "gray"). */
		private val COLOR_BY_ITEM: Map<Item, String> = DyeColor.entries.associate { dyeColor ->
			Items.DYE.pick(dyeColor) to dyeColor.serializedName
		}

		/** Colour name of the dye in [stack], or null if it holds no dye. */
		fun colorOf(stack: ItemStack): String? = COLOR_BY_ITEM[stack.item]

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