package mod.ex3.companion.mixin

import mod.ex3.companion.companion.CoreSlotManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Persists the companion core slot content in the player's save data.
 * Runs on world save/load. Death-clone transfer is handled separately
 * via ServerPlayerEvents.COPY_FROM (the clone does not re-read NBT).
 */
@Mixin(ServerPlayer::class)
abstract class ServerPlayerMixin {

    @Inject(method = ["addAdditionalSaveData"], at = [At("TAIL")])
    private fun `ex3$saveCore`(output: ValueOutput, ci: CallbackInfo) {
        val self = this as ServerPlayer
        CoreSlotManager.writeSaveData(self, output)
    }

    @Inject(method = ["readAdditionalSaveData"], at = [At("TAIL")])
    private fun `ex3$loadCore`(input: ValueInput, ci: CallbackInfo) {
        val self = this as ServerPlayer
        CoreSlotManager.readSaveData(self, input)
    }
}
