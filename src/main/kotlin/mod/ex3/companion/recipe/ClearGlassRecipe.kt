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

	override fun display(): List<RecipeDisplay> = listOf()

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

		/** Writes the glass tint into every source the game reads: the mod component, the item model select and the vanilla custom_data tag. */
		fun applyGlassColor(stack: ItemStack, color: String) {
			val data = stack.getOrDefault(ModComponents.COMPANION_DATA, CompanionData.DEFAULT)
			stack.set(ModComponents.COMPANION_DATA, data.withGlassColor(color))
			stack.set(
				net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
				net.minecraft.world.item.component.CustomModelData(
					emptyList(), emptyList(), listOf(color), emptyList()
				)
			)
			val companionTag = net.minecraft.nbt.CompoundTag().also { it.putString("glassColor", color) }
			val tag = net.minecraft.nbt.CompoundTag().also { it.put("companion", companionTag) }
			stack.set(
				net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.of(tag)
			)
		}

		fun register() {
			Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Ex3Companion.id("clear_glass"), SERIALIZER)
		}
	}
}
