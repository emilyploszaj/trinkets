package dev.emi.trinkets;

import java.util.List;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for putting methods onto the player's screen handler
 */
public interface TrinketPlayerScreenHandler {

	/**
	 * Called to inform the player's slot handler that it needs to remove and re-add its trinket slots to reflect new changes
	 */
	void trinkets$updateTrinketSlots(boolean slotsChanged);

	int trinkets$getGroupNum(SlotGroup group);

	@Nullable
	Point trinkets$getGroupPos(SlotGroup group);

	@NotNull
	List<Point> trinkets$getSlotHeights(SlotGroup group);

	@Nullable
	Point trinkets$getSlotHeight(SlotGroup group, int i);

	@NotNull
	List<SlotType> trinkets$getSlotTypes(SlotGroup group);

	int trinkets$getSlotWidth(SlotGroup group);

	int trinkets$getGroupCount();

	int trinkets$getTrinketSlotStart();

	int trinkets$getTrinketSlotEnd();
}
