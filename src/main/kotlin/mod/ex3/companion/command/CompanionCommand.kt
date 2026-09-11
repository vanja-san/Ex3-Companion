package mod.ex3.companion.command

import com.mojang.brigadier.CommandDispatcher
import mod.ex3.companion.companion.CompanionData
import mod.ex3.companion.registry.ModComponents
import mod.ex3.companion.registry.ModItems
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions

/**
 * Debug command: /companioncore give
 * Hands the sender a companion core so the slot can be tested manually.
 * Requires moderator permissions (op level 2+) on a server.
 *
 * Only registered when the JVM flag `-Dex3companion.debug=true` is set, so it
 * never ships in a normal release build.
 */
object CompanionCommand {

	private val DEBUG_ENABLED: Boolean = System.getProperty("ex3companion.debug") == "true"

	fun register() {
		if (!DEBUG_ENABLED) return
		CommandRegistrationCallback.EVENT.register(
			CommandRegistrationCallback { dispatcher: CommandDispatcher<CommandSourceStack>, _, _ ->
				dispatcher.register(
					Commands.literal("companioncore")
						.requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
						.then(
							Commands.literal("give").executes { context ->
								val player: ServerPlayer = context.source.playerOrException
								val stack = net.minecraft.world.item.ItemStack(ModItems.COMPANION_CORE)
								stack[ModComponents.COMPANION_DATA] = CompanionData.DEFAULT
								if (!player.inventory.add(stack)) {
									player.drop(stack, false)
								}
								context.source.sendSuccess(
									{ Component.translatable("ex3companion.command.given") },
									false,
								)
								com.mojang.brigadier.Command.SINGLE_SUCCESS
							},
						),
				)
			},
		)
	}
}
