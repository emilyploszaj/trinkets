package dev.emi.trinkets.api;

import java.util.HashMap;
import java.util.Map;

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
}
