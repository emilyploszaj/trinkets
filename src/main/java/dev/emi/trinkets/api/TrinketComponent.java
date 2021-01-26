package dev.emi.trinkets.api;

import java.util.List;
import java.util.function.Predicate;

import dev.emi.trinkets.api.Trinket.SlotReference;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

public interface TrinketComponent extends ComponentV3 {

	TrinketInventory getInventory();

	boolean isEquipped(Predicate<ItemStack> predicate);

	default boolean isEquipped(Item item) {
		return isEquipped(stack -> stack.getItem() == item);
	}

	List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate);

	default List<Pair<SlotReference, ItemStack>> getEquipped(Item item) {
		return getEquipped(stack -> stack.getItem() == item);
	}

	default List<Pair<SlotReference, ItemStack>> getAllEquipped() {
		return getEquipped(stack -> true);
	}
}