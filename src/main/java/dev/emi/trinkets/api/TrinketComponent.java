package dev.emi.trinkets.api;

import nerdhub.cardinal.components.api.component.Component;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Base TrinketComponent interface, currently only implemented by PlayerTrinketComponent
 */
public interface TrinketComponent extends Component{
	public Inventory getInventory();
	public ItemStack getStack(String slot);
	public boolean equip(ItemStack stack);
}