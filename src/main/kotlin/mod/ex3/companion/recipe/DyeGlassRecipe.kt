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
 * Crafting recipe: companion_core + any dye -> the same core tinted in [color].
 * Preserves every companion component (health, XP, level, memory); a vanilla shapeless
 * recipe would create a fresh item via result.create() and silently drop all of them.
 */
class DyeGlassRecipe(
	commonInfo: Recipe.CommonInfo,
	bookInfo: CraftingRecipe.CraftingBookInfo,
	val color: String,
) : NormalCraftingRecipe(commonInfo, bookInfo) {

	override fun matches(input: CraftingInput, level: Level): Boolean {
		var core = false
		var dyeColor: String? = null
		for (i in 0 until input.size()) {
			val stack = input.getItem(i)
			if (stack.`is`(ModItems.COMPANION_CORE)) {
				core = true
			} else if (!stack.isEmpty) {
				// Only the dye matching THIS recipe's color may craft it. Without this
				// check every dye matched all 16 recipes and the first one (alphabetical)
				// won, so e.g. gray dye always produced a black companion.
				val c = DYE_COLOR_BY_ITEM[stack.item]
				if (c != null && dyeColor == null) dyeColor = c else return false
			}
		}
		return core && dyeColor == color
	}

	override fun assemble(input: CraftingInput): ItemStack {
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

	override fun createPlacementInfo(): PlacementInfo =
		PlacementInfo.create(listOf(
			Ingredient.of(ModItems.COMPANION_CORE),
			DYE_INGREDIENT,
		))

	override fun display(): List<RecipeDisplay> = listOf()

	override fun getSerializer(): RecipeSerializer<out DyeGlassRecipe> = SERIALIZER

	companion object {
		private val DYE_INGREDIENT: Ingredient = Ingredient.of(
			BuiltInRegistries.ITEM.filter { it is DyeItem }.stream()
		)

		/** Maps each dye item to its color name (e.g. gray_dye -> "gray"). */
		private val DYE_COLOR_BY_ITEM: Map<Item, String> = DyeColor.entries.associate { dyeColor ->
			Items.DYE.pick(dyeColor) to dyeColor.serializedName
		}

		val CODEC: MapCodec<DyeGlassRecipe> = RecordCodecBuilder.mapCodec { i ->
			i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter { it.commonInfo },
				CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter { it.bookInfo },
				Codec.STRING.fieldOf("color").forGetter { it.color },
			).apply(i, ::DyeGlassRecipe)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DyeGlassRecipe> =
			StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, { it.commonInfo },
				CraftingRecipe.CraftingBookInfo.STREAM_CODEC, { it.bookInfo },
				ByteBufCodecs.STRING_UTF8, { it.color },
				::DyeGlassRecipe
			)

		val SERIALIZER: RecipeSerializer<DyeGlassRecipe> = RecipeSerializer(CODEC, STREAM_CODEC)

		fun register() {
			Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Ex3Companion.id("dye_glass"), SERIALIZER)
		}
	}
}