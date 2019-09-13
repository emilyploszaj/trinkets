package dev.emi.trinkets.api;

import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;

/**
 * Basic Trinkets calls to get information about players
 */
public class TrinketsApi{
	public static final ComponentType<TrinketComponent> TRINKETS = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("trinkets:trinkets"), TrinketComponent.class);
	/**
	 * @return An inventory holding all trinket slots indexed numerically
	 * @see {@link #getTrinketComponent(player)} and {@link dev.emi.trinkets.api.PlayerTrinketComponent#getStack(slot)} for getting slots by name
	 */
	public static Inventory getTrinketsInventory(PlayerEntity player){
		return TRINKETS.get(player).getInventory();
	}
	public static TrinketComponent getTrinketComponent(PlayerEntity player){
		return TRINKETS.get(player);
	}
}