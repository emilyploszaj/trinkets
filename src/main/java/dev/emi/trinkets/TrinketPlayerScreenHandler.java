package dev.emi.trinkets;

import java.util.List;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;

/**
 * Interface for putting methods onto the player's screen handler
 */
public interface TrinketPlayerScreenHandler {

	/**
	 * Called to inform the player's slot handler that it needs to remove and re-add its trinket slots to reflect new changes
	 */
	void trinkets$updateTrinketSlots(boolean slotsChanged);

	int trinkets$getGroupNum(SlotGroup group);

	Point trinkets$getGroupPos(SlotGroup group);

	List<Point> trinkets$getSlotHeights(SlotGroup group);

	List<SlotType> trinkets$getSlotTypes(SlotGroup group);

	int trinkets$getSlotWidth(SlotGroup group);

	int trinkets$getGroupCount();

	int trinkets$getTrinketSlotStart();

	int trinkets$getTrinketSlotEnd();

	boolean trinkets$isSane(SlotGroup group);
}
