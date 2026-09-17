package mod.ex3.companion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.permissions.Permissions;

/**
 * Shared gateway deciding whether the config screen (Mod Menu button and
 * quick-configure icon) is shown for this mod.
 *
 * Lives in an ordinary package (not the {@code modmenu} mixin package) so it
 * can be referenced from both the Java mixins and the Kotlin ModMenuIntegration.
 *
 * The screen only contains server settings now, so it is only useful while
 * in a world and only for players who can actually manage them: every
 * singleplayer / LAN host player is an operator by default, on a dedicated
 * server the vanilla operator check decides. Outside of a world
 * ({@code player == null}) there is nothing to configure, so the button is
 * hidden. In every hidden context the ModMenuIntegration factory also refuses
 * to open the screen, so the button and any residual click paths stay inert.
 */
public final class Ex3ConfigGate {

	private Ex3ConfigGate() {
	}

	public static boolean canShowConfigScreen() {
		var player = Minecraft.getInstance().player;
		// GAMEMASTER is the new-permission-system equivalent of the old op level 2:
		// in singleplayer / LAN the host owns an all-levels permission set, on a
		// dedicated server only operators (by default) satisfy it.
		return player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}
}