package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import net.fabricmc.api.ClientModInitializer;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup = null;
	public static SlotType activeType = null; // TODO mostly unused

	@Override
	public void onInitializeClient() {

	}
}
