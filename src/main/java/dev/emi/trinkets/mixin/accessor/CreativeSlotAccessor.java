package dev.emi.trinkets.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.screen.slot.Slot;

/**
 * You'll access widen this into being accessible but won't make its field accessible? Yes.
 */
@Mixin(CreativeSlot.class)
public interface CreativeSlotAccessor {
	
	@Accessor("slot")
	public Slot getSlot();
}
