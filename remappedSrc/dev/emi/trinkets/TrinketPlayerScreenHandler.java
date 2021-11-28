package dev.emi.trinkets;

import java.util.List;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import net.minecraft.util.Pair;

/**
 * Interface for putting methods onto the player's screen handler
 */
public interface TrinketPlayerScreenHandler {

	/**
	 * Called to inform the player's slot handler that it needs to remove and re-add its trinket slots to reflect new changes
	 */
	void updateTrinketSlots(boolean slotsChanged);

	Pair<Integer, Integer> getGroupPos(SlotGroup group);

	List<Pair<Integer, Integer>> getSlotHeights(SlotGroup group);

	List<SlotType> getSlotTypes(SlotGroup group);

	int getSlotWidth(SlotGroup group);

	int getGroupCount();
}
