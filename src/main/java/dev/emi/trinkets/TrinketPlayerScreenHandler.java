package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import net.minecraft.util.Pair;

/**
 * Interface for putting methods onto the player's screen handler
 */
public interface TrinketPlayerScreenHandler {
	
	/**
	 * Called to inform the player's slot handler that it needs to remove and read its trinket slots to reflect new changes
	 */
	public void updateTrinketSlots();

	public Pair<Integer, Integer> getGroupPos(SlotGroup group);
}
