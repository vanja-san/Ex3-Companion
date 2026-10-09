package mod.ex3.companion.recipe

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mod.ex3.companion.Ex3Companion
import mod.ex3.companion.companion.CompanionData
import mod.ex3.companion.registry.ModComponents
import mod.ex3.companion.registry.ModItems
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
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
 * Crafting recipe: companion_core + sponge → same companion_core with glass color reset to "clear".
 * Preserves all companion data (health, XP, level) — only clears the stained-glass tint.
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
				applyGlassColor(result, "clear")
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

	/**
	 * Feeds the vanilla recipe book. Empty is not enough: `RecipeManager` builds every
	 * client-side book entry from `display()`, so an empty list hides the recipe entirely
	 * even though `category` and `createPlacementInfo()` are set. `createPlacementInfo()`
	 * covers crafting-grid placement, `display()` covers the book.
	 */
	override fun display(): List<RecipeDisplay> = listOf(
		ShapelessCraftingRecipeDisplay(
			INGREDIENT_DISPLAYS,
			SlotDisplay.ItemStackSlotDisplay(displayResult()),
			SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE),
		),
	)

	override fun getSerializer(): RecipeSerializer<out ClearGlassRecipe> = SERIALIZER

	companion object {
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

		/** Slot displays for the recipe book, shared with [createPlacementInfo]. */
		private val INGREDIENT_DISPLAYS: List<SlotDisplay> = listOf(
			Ingredient.of(ModItems.COMPANION_CORE).display(),
			Ingredient.of(Items.SPONGE).display(),
		)

		/**
		 * Result shown in the book: a fresh core with the tint already reset, so the entry
		 * matches what crafting actually produces.
		 */
		private fun displayResult(): ItemStackTemplate {
			val stack = ItemStack(ModItems.COMPANION_CORE)
			applyGlassColor(stack, "clear")
			return ItemStackTemplate.fromNonEmptyStack(stack)
		}

		/**
		 * Writes the glass tint into every source the game reads: the mod component, the
		 * item model select and the vanilla custom_data tag.
		 *
		 * The existing `custom_data` NBT is merged into rather than replaced, so a
		 * `companion` key written by another tool (or a future field of ours) survives
		 * re-tinting instead of being silently dropped.
		 */
		fun applyGlassColor(stack: ItemStack, color: String) {
			val data = stack.getOrDefault(ModComponents.COMPANION_DATA, CompanionData.DEFAULT)
			stack.set(ModComponents.COMPANION_DATA, data.withGlassColor(color))
			stack.set(
				net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
				net.minecraft.world.item.component.CustomModelData(
					emptyList(), emptyList(), listOf(color), emptyList()
				)
			)
			val existing = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)?.copyTag()
				?: net.minecraft.nbt.CompoundTag()
			existing.put("companion", net.minecraft.nbt.CompoundTag().also { it.putString("glassColor", color) })
			stack.set(
				net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.of(existing)
			)
		}

		fun register() {
			Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Ex3Companion.id("clear_glass"), SERIALIZER)
		}
	}
}
