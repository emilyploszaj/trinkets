package dev.emi.trinkets.api;

import net.minecraft.item.Item;

public class TrinketItem extends Item implements Trinket {
	
	public TrinketItem(Item.Settings settings) {
		super(settings);
		TrinketsApi.registerTrinket(this, this);
	}
}