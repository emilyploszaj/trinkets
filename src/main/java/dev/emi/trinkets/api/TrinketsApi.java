package dev.emi.trinkets.api;

import dev.emi.trinkets.data.EntitySlotLoader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;

public class TrinketsApi {

	private static final Map<Item, Trinket> TRINKETS = new HashMap<Item, Trinket>();

	public static void registerTrinket(Item item, Trinket trinket) {
		TRINKETS.put(item, trinket);
	}

	public static boolean hasTrinket(Item item) {
		return TRINKETS.containsKey(item);
	}

	public static Trinket getTrinket(Item item) {
		return TRINKETS.get(item);
	}

	public static Map<String, SlotGroup> getPlayerSlots() {
		return getEntitySlots(EntityType.PLAYER);
	}

	public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
		return EntitySlotLoader.INSTANCE.getEntitySlots(type);
	}
}
