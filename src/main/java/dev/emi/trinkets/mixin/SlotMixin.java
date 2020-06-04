package dev.emi.trinkets.mixin;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for changing slot x and y positions
 */
@Mixin(Slot.class)
public interface SlotMixin {

	@Accessor("x")
	public abstract int getXPosition();

	@Accessor("x")
	public abstract void setXPosition(int xPosition);

	@Accessor("y")
	public abstract int getYPosition();

	@Accessor("y")
	public abstract void setYPosition(int yPosition);
}