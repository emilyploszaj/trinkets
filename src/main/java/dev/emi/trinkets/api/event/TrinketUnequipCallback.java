package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface TrinketUnequipCallback {
	Event<TrinketUnequipCallback> EVENT = EventFactory.createArrayBacked(TrinketUnequipCallback.class,
	listeners -> (stack, slot, entity) -> {
		for (TrinketUnequipCallback listener: listeners){
			listener.onUnequip(stack, slot, entity);
		}
	});

	/**
	 * Called when an entity un-equips a trinket, after the {@link Trinket#onUnequip} method of the Trinket
	 *
	 * @param stack The stack being unequipped
	 * @param slot The slot the stack was unequipped from
	 * @param entity The entity that unequipped the stack
	 */
	void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity);
}