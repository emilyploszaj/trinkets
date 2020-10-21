package dev.emi.trinkets.api;

import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Base TrinketComponent interface, currently only implemented by PlayerTrinketComponent
 */
public interface TrinketComponent extends Component {
	
	public Inventory getInventory();

	public ItemStack getStack(String slot);

	public default ItemStack getStack(String group, String slot) {
		return getStack(group + ':' + slot);
	}

	public default boolean equip(ItemStack stack) {
		return equip(stack, false);
	}

	public boolean equip(ItemStack stack, boolean shiftClick);
}