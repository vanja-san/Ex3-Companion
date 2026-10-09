package mod.ex3.companion.companion

/**
 * Whether the companion core's stat block is currently hidden from tooltips.
 *
 * The recipe book renders the core as the result of seventeen grouped recipes, and there
 * the stat block would sit on top of the vanilla "More Recipes" hint instead of showing
 * the item name, the way dyed glass shows nothing extra. It is only unwanted in that one
 * place though, so the client flips this flag while a recipe book screen is open and the
 * tooltip provider checks it. Reading the screen directly from [CompanionData] would drag
 * client classes into common code, which is why the state is passed the other way.
 */
object CompanionTooltip {

	@Volatile
	private var hidden: Boolean = false

	/** True while a screen that embeds the recipe book is the open one. */
	fun isHidden(): Boolean = hidden

	fun setHidden(value: Boolean) {
		hidden = value
	}
}