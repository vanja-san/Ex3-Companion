package mod.ex3.companion.config

import net.minecraft.server.permissions.Permissions
import net.minecraft.world.entity.player.Player

/**
 * Operator detection based on MC 26.3's layered permission system.
 *
 * A player counts as an "operator" when their permission set grants the
 * command-level permission GAMEMASTERS (level 2) — the same threshold the
 * classic `Player.hasPermissions(2)` check used. Works on both the server
 * ([ServerPlayer]) and the client ([LocalPlayer], whose permission set is
 * synced from the server on join), so the same helper can gate the GUI.
 */
object CompanionPermissions {

	fun isOperator(player: Player?): Boolean =
		player?.permissions()?.hasPermission(Permissions.COMMANDS_GAMEMASTER) == true
}