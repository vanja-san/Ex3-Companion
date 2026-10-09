package mod.ex3.companion.registry

import mod.ex3.companion.Ex3Companion
import mod.ex3.companion.item.CompanionCoreItem
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

object ModItems {
	val COMPANION_CORE_KEY: ResourceKey<Item> =
		ResourceKey.create(Registries.ITEM, Ex3Companion.id("companion_core"))

	val COMPANION_CORE: Item = register(
		COMPANION_CORE_KEY,
		::CompanionCoreItem,
		// No MAX_DAMAGE/DAMAGE: the core is not a durability item. Giving it one made
		// `isDamageableItem()` true, which opened vanilla's anvil repair/combine branches
		// and let the core be consumed as a repair material — an unintended item surface
		// this mod never wanted.
		Item.Properties().stacksTo(1).fireResistant(),
	)

	private val CREATIVE_TAB_KEY: ResourceKey<CreativeModeTab> =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, Ex3Companion.id("creative_tab"))

	@Suppress("unused")
	val CREATIVE_TAB: CreativeModeTab = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		CREATIVE_TAB_KEY,
		FabricCreativeModeTab.builder()
			.icon { ItemStack(COMPANION_CORE) }
			.title(Component.translatable("creativeTab.ex3companion"))
			.displayItems { _, output ->
				output.accept(COMPANION_CORE)
			}
			.build(),
	)

	private fun register(key: ResourceKey<Item>, factory: (Item.Properties) -> Item, properties: Item.Properties): Item {
		val item = factory(properties.setId(key))
		return Registry.register(BuiltInRegistries.ITEM, key, item)
	}

	fun initialize() {
	}
}
