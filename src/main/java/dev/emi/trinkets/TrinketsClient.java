package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import net.fabricmc.api.ClientModInitializer;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup = null;
	public static SlotType activeType = null; // TODO mostly unused
	public static SlotGroup quickMoveGroup = null;
	public static SlotType quickMoveType = null;
	public static int quickMoveTimer = 0;

	@Override
	public void onInitializeClient() {

	}
}
