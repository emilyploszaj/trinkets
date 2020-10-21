package dev.emi.trinkets.api;

import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;

/**
 * Basic Trinkets calls to get information about players and their trinkets
 */
public class TrinketsApi {
	// TODO replace ComponentType with ComponentKey when binary compatibility is not an issue
	public static final ComponentType<TrinketComponent> TRINKETS = (ComponentType<TrinketComponent>) ComponentRegistry.getOrCreate(new Identifier("trinkets:trinkets"), TrinketComponent.class);

	/**
	 * @return An inventory holding all trinket slots indexed numerically
	 * @see {@link #getTrinketComponent(player)} and {@link dev.emi.trinkets.api.PlayerTrinketComponent#getStack(slot)} for getting slots by name
	 */
	public static Inventory getTrinketsInventory(PlayerEntity player) {
		return TRINKETS.get(player).getInventory();
	}

	/**
	 * @return The {@link TrinketComponent} associated with the given player
	 */
	public static TrinketComponent getTrinketComponent(PlayerEntity player) {
		return TRINKETS.get(player);
	}
}