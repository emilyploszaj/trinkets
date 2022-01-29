package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotType;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.util.Identifier;

/**
 * A gui slot for a trinket slot in the creative inventory
 */
public class CreativeTrinketSlot extends CreativeSlot implements TrinketSlot {
	private final SurvivalTrinketSlot original;

	public CreativeTrinketSlot(SurvivalTrinketSlot original, int s, int x, int y) {
		super(original, s, x, y);
		this.original = original;
	}

	@Override
	public boolean isTrinketFocused() {
		return original.isTrinketFocused();
	}

	@Override
	public Identifier getBackgroundIdentifier() {
		return original.getBackgroundIdentifier();
	}

	@Override
	public SlotType getType() {
		return original.getType();
	}
}
