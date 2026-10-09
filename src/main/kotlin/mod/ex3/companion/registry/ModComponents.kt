package mod.ex3.companion.registry

import mod.ex3.companion.Ex3Companion
import mod.ex3.companion.companion.CompanionData
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries

object ModComponents {
	val COMPANION_DATA: DataComponentType<CompanionData> = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Ex3Companion.id("companion_data"),
		DataComponentType.builder<CompanionData>()
			.persistent(CompanionData.CODEC)
			.networkSynchronized(CompanionData.STREAM_CODEC)
			.build(),
	)

	/**
	 * Mirrors the companion's ATTACK brain state as an entity data component so the
	 * dynamic-light predicate can switch the emitted color (redstone-red while fighting).
	 * In-memory only; the client reads it from the synced brain state, never network-synced.
	 */
	val ATTACKING: DataComponentType<Boolean> = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Ex3Companion.id("attacking"),
		DataComponentType.builder<Boolean>()
			.persistent(com.mojang.serialization.Codec.BOOL)
			.build(),
	)

	fun initialize() {
		// Static initialization happens on field access.
		//
		// The core deliberately registers no ItemComponentTooltipProvider: the recipe book
		// renders this item as the result of 17 grouped recipes, and a stats block there
		// would sit next to the vanilla "More Recipes" hint instead of the item name.
		// Companion stats are surfaced by the health bar and the /companion command.
	}
}
