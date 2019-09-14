package dev.emi.trinkets.api;

import net.minecraft.inventory.BasicInventory;

/**
 * Inventory that marks its parent PlayerTrinketComponent dirty and syncs with the server when needed
 */
public class TrinketInventory extends BasicInventory{
	private PlayerTrinketComponent component;
	public TrinketInventory(PlayerTrinketComponent component, int size){
		super(size);
		this.component = component;
	}
	public PlayerTrinketComponent getComponent(){
		return component;
	}
	public void markDirty(){
		component.markDirty();
	}
}