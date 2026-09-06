package mod.ex3.companion.companion

import net.minecraft.world.entity.player.Player
import net.minecraft.world.SimpleContainer
import java.lang.ref.WeakReference

/**
 * Single-slot backing container for the companion core slot.
 * Delegates change notifications to [CoreSlotManager] with the owning player.
 */
class CoreSlotBackingContainer(player: Player) : SimpleContainer(1) {

	private val playerRef = WeakReference(player)

	override fun setChanged() {
		super.setChanged()
		playerRef.get()?.let { CoreSlotManager.onSlotChanged(it) }
	}

	fun owner(): Player? = playerRef.get()
}
