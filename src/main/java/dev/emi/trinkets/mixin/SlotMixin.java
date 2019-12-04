package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.container.Slot;

/**
 * Accessor mixin for changing slot x and y positions
 */
@Mixin(Slot.class)
public interface SlotMixin {

	@Accessor("xPosition")
	public abstract int getXPosition();

	@Accessor("xPosition")
	public abstract void setXPosition(int xPosition);

	@Accessor("yPosition")
	public abstract int getYPosition();

	@Accessor("yPosition")
	public abstract void setYPosition(int yPosition);
}