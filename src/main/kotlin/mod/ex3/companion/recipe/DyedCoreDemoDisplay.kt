package mod.ex3.companion.recipe

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mod.ex3.companion.Ex3Companion
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.display.DisplayContentsFactory
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.util.context.ContextMap
import java.util.stream.Stream

/**
 * Recipe-book result preview that tints the companion core with a dye, re-rolling over time.
 *
 * This is a faithful port of vanilla's `SlotDisplay.DyedSlotDemo`, which cannot be used as-is:
 * that demo tints through `DyedItemColor.applyDyes`, i.e. it writes the vanilla dye colour
 * component. Our item model selects its texture on `minecraft:custom_model_data`
 * (`assets/ex3-companion/items/companion_core.json`), and `dye_color` is not a model select
 * property at all, so writing it has no visual effect and the preview stayed colourless.
 *
 * So this applies the same demonstration behaviour while writing the component our model
 * actually reads. Like the vanilla version, the result stream is lazy and randomised: the
 * book re-resolves it as the entry cycles, so the preview visibly changes colour between
 * dyes instead of showing one fixed result.
 *
 * Vanilla does the same thing with `Stream.generate(...).limit(256).filter(...).limit(16)`
 * inside `SlotDisplay.applyDemoTransformation`, which is private - hence the reimplementation.
 * Note this does NOT expand the entry into one slot per dye: the limit keeps a handful of
 * candidates and the book picks among them, which is what keeps a single recipe entry.
 */
class DyedCoreDemoDisplay(
	private val dye: SlotDisplay,
	private val core: SlotDisplay,
) : SlotDisplay {

	override fun type(): SlotDisplay.Type<*> = TYPE

	override fun <T : Any> resolve(context: ContextMap, factory: DisplayContentsFactory<T>): Stream<T> {
		if (factory !is DisplayContentsFactory.ForStacks<T>) return Stream.empty()

		val cores = core.resolveForStacks(context)
		if (cores.isEmpty()) return Stream.empty()
		val dyes = dye.resolveForStacks(context)
		if (dyes.isEmpty()) return Stream.empty()

		// Lazy and randomised, exactly like vanilla's applyDemoTransformation: the book
		// re-resolves as the entry cycles, so the preview walks through the dyes.
		return Stream
			.generate<ItemStack> {
				val target = cores[RandomSource.create().nextInt(cores.size)]
				val dyeStack = dyes[RandomSource.create().nextInt(dyes.size)]
				val color = DyeGlassRecipe.colorOf(dyeStack)
				if (color == null) ItemStack.EMPTY else tinted(target, color)
			}
			.filter { !it.isEmpty() }
			.limit(MAX_DEMO_SLOTS.toLong())
			.map(factory::forStack)
	}

	private fun tinted(target: ItemStack, color: String): ItemStack {
		val copy = target.copy()
		ClearGlassRecipe.applyGlassColor(copy, color)
		return copy
	}

	companion object {
		/** Upper bound on preview candidates, matching vanilla's demo limit. */
		private const val MAX_DEMO_SLOTS = 16

		private val MAP_CODEC: MapCodec<DyedCoreDemoDisplay> = RecordCodecBuilder.mapCodec { i ->
			i.group(
				SlotDisplay.CODEC.fieldOf("dye").forGetter { it.dye },
				SlotDisplay.CODEC.fieldOf("core").forGetter { it.core },
			).apply(i, ::DyedCoreDemoDisplay)
		}

		private val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DyedCoreDemoDisplay> =
			StreamCodec.composite(
				SlotDisplay.STREAM_CODEC, { it.dye },
				SlotDisplay.STREAM_CODEC, { it.core },
				::DyedCoreDemoDisplay,
			)

		val TYPE: SlotDisplay.Type<DyedCoreDemoDisplay> = SlotDisplay.Type(MAP_CODEC, STREAM_CODEC)

		/**
		 * Registers the display type. Must run on both sides: the recipe's `display()` is
		 * evaluated on the server when the recipe book state is built, and the resulting
		 * entry is synced to clients, which need the type to decode it.
		 */
		fun register() {
			Registry.register(BuiltInRegistries.SLOT_DISPLAY, Ex3Companion.id("dyed_core_demo"), TYPE)
		}
	}
}