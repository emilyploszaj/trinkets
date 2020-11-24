package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * A gui slot for a trinket slot
 */
public class TrinketSlot extends Slot {
	private SlotGroup group;
	private SlotType type;
	private boolean alwaysVisible;
	private boolean baseType;

	public TrinketSlot(Inventory inventory, int index, int x, int y, SlotGroup group, SlotType type, boolean alwaysVisible, boolean baseType) {
		super(inventory, index, x, y);
		this.group = group;
		this.type = type;
		this.alwaysVisible = alwaysVisible;
		this.baseType = baseType;
	}

	public boolean isTrinketFocused() {
		if (TrinketsClient.activeGroup == group) {
			return baseType || TrinketsClient.activeType == type;
		}
		return false;
	}

	public Identifier getBackgroundIdentifier() {
		return type.getIcon();
	}

	@Override
	public boolean doDrawHoveringEffect() {
		if (alwaysVisible) {
			return true;
		}
		return isTrinketFocused();
	}
}
