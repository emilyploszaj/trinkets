package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface TrinketEquipCallback {
	Event<TrinketEquipCallback> EVENT = EventFactory.createArrayBacked(TrinketEquipCallback.class,
	listeners -> (stack, slot, entity) -> {
		for (TrinketEquipCallback listener: listeners){
			listener.onEquip(stack, slot, entity);
		}
	});

	/**
	 * Called when an entity equips a trinket, after the {@link Trinket#onEquip} method of the Trinket
	 *
	 * @param stack The stack being equipped
	 * @param slot The slot the stack is equipped to
	 * @param entity The entity that equipped the stack
	 */
	void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity);
}