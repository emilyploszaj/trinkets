package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public interface TrinketSlot {

	public boolean isTrinketFocused();

	public boolean renderAfterRegularSlots();

	public Identifier getBackgroundIdentifier();

	public SlotType getType();

	public static boolean canInsert(ItemStack stack, SlotReference slotRef, LivingEntity entity) {
		boolean res = TrinketsApi.evaluatePredicateSet(slotRef.inventory().getSlotType().getValidatorPredicates(),
			stack, slotRef, entity);

		if (res) {
			return TrinketsApi.getTrinket(stack.getItem()).canEquip(stack, slotRef, entity);
		}

		return false;
	}
}
