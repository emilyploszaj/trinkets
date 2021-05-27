package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import net.minecraft.util.Pair;

import java.util.List;

/**
 * Interface for putting methods onto the player's screen handler
 */
public interface TrinketPlayerScreenHandler {
	
	/**
	 * Called to inform the player's slot handler that it needs to remove and re-add its trinket slots to reflect new changes
	 */
	void updateTrinketSlots();

	Pair<Integer, Integer> getGroupPos(SlotGroup group);

	List<Pair<Integer, Integer>> getSlotHeights(SlotGroup group);

	int getSlotWidth(SlotGroup group);
}
