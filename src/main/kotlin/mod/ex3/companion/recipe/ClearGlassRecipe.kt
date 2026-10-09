package mod.ex3.companion.recipe

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mod.ex3.companion.Ex3Companion
import mod.ex3.companion.registry.ModItems
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
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
 * Crafting recipe: companion_core + sponge -> the same core with its glass tint removed.
 *
 * A separate recipe from the dye one, and grouped with it under
 * [BOOK_GROUP]. That is what turns the pair into a single book entry whose right-click
 * opens the variants popup (`RecipeBookPage` only does so when a button holds more than one
 * recipe). Grouping also means the button alternates between the two every 30 ticks, which
 * is what keeps the preview visibly changing at the vanilla rate.
 *
 * The sponge is the only way back to clear glass - "clear" is not a dye, so no dye can
 * produce it.
 *
 * Preserves every companion component (health, XP, level, memory).
 */
class ClearGlassRecipe(
	commonInfo: Recipe.CommonInfo,
	bookInfo: CraftingRecipe.CraftingBookInfo,
) : NormalCraftingRecipe(commonInfo, bookInfo) {

	override fun matches(input: CraftingInput, level: Level): Boolean {
		var core = false
		var sponge = false
		for (i in 0 until input.size()) {
			val stack = input.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) core = true
			else if (stack.`is`(Items.SPONGE)) sponge = true
			else if (!stack.isEmpty) return false
		}
		return core && sponge
	}

	override fun assemble(input: CraftingInput): ItemStack {
		for (i in 0 until input.size()) {
			val stack = input.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) {
				val result = stack.copy()
				CoreGlassTint.apply(result, CoreGlassTint.CLEAR)
				return result
			}
		}
		return ItemStack.EMPTY
	}

	override fun createPlacementInfo(): PlacementInfo =
		PlacementInfo.create(listOf(
			Ingredient.of(ModItems.COMPANION_CORE),
			Ingredient.of(Items.SPONGE),
		))

	/** Feeds the vanilla recipe book; an empty list would hide the recipe entirely. */
	override fun display(): List<RecipeDisplay> {
		val core = Ingredient.of(ModItems.COMPANION_CORE).display()
		return listOf(
			ShapelessCraftingRecipeDisplay(
				listOf(core, Ingredient.of(Items.SPONGE).display()),
				SlotDisplay.ItemStackSlotDisplay(CoreGlassTint.previewTemplate(CoreGlassTint.CLEAR)),
				SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE),
			),
		)
	}

	override fun getSerializer(): RecipeSerializer<out ClearGlassRecipe> = SERIALIZER

	companion object {
		/** Shared with [DyeGlassRecipe] so the book groups them into one entry. */
		const val BOOK_GROUP = "companion_core_tint"

		val CODEC: MapCodec<ClearGlassRecipe> = RecordCodecBuilder.mapCodec { i ->
			i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter { it.commonInfo },
				CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter { it.bookInfo },
			).apply(i, ::ClearGlassRecipe)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ClearGlassRecipe> =
			StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, { it.commonInfo },
				CraftingRecipe.CraftingBookInfo.STREAM_CODEC, { it.bookInfo },
				::ClearGlassRecipe
			)

		val SERIALIZER: RecipeSerializer<ClearGlassRecipe> = RecipeSerializer(CODEC, STREAM_CODEC)

		fun register() {
			Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Ex3Companion.id("clear_glass"), SERIALIZER)
		}
	}
}