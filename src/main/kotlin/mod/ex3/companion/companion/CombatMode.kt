package mod.ex3.companion.companion

/**
 * Combat mode determining how the companion selects and engages targets.
 *
 * - [DEFENDER]: only fights mobs actively attacking the owner or companion (default).
 * - [AGGRESSIVE]: proactively seeks nearby hostiles within an expanded search radius.
 * - [STRATEGIC]: considers mob type — keeps distance from creepers, approaches skeletons.
 */
enum class CombatMode {
	DEFENDER,
	AGGRESSIVE,
	STRATEGIC,
}
