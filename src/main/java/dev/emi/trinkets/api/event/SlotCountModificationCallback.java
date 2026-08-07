package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.Set;

/**
 * Event called upon slot modification by attributes.
 * Does not trigger upon Datapack Reload.
 */

public interface SlotCountModificationCallback {
	Event<SlotCountModificationCallback> EVENT = EventFactory.createArrayBacked(SlotCountModificationCallback.class,
	listeners -> (component, inventories) -> {
		for (var listener : listeners) {
			listener.onChange(component, inventories);
		}
	});

	/**
	 * @param component The TrinketComponent the inventories belong to.
	 * @param inventories A set of inventories affected by EAM modifications.
	 */
	void onChange(TrinketComponent component, Set<TrinketInventory> inventories);
}
