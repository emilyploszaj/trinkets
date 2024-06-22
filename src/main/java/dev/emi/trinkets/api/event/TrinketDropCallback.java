package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface TrinketDropCallback {
	Event<TrinketDropCallback> EVENT = EventFactory.createArrayBacked(TrinketDropCallback.class,
	listeners -> (rule, stack, ref, entity) -> {
		for (TrinketDropCallback listener : listeners) {
			rule = listener.drop(rule, stack, ref, entity);
		}
		return rule;
	});

	DropRule drop(DropRule rule, ItemStack stack, SlotReference ref, LivingEntity entity);
}