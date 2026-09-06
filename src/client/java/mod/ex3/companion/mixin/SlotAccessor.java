package mod.ex3.companion.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes mutable {@code x} and {@code y} on {@link Slot} so the creative
 * inventory mixin can reposition the companion slot without subclassing.
 */
@Mixin(Slot.class)
public interface SlotAccessor {

	@Accessor("x")
	@Mutable
	void ex3$setX(int x);

	@Accessor("y")
	@Mutable
	void ex3$setY(int y);
}
