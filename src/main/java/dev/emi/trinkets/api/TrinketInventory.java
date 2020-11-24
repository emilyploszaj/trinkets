package dev.emi.trinkets.api;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

public class TrinketInventory implements Inventory {

	public LivingEntityTrinketComponent component;
	public DefaultedList<ItemStack> stacks;
	// This is a really weird solution to the problem of quickly getting slot info, I plan on changing it to something more reasonable
	public Map<SlotType, Integer> slotMap = new HashMap<SlotType, Integer>();
	public Map<SlotType, Integer> groupOffsetMap = new HashMap<SlotType, Integer>();
	public Map<SlotGroup, Integer> groupOccupancyMap = new HashMap<SlotGroup, Integer>();
	public Map<Integer, Pair<SlotType, Integer>> posMap = new HashMap<Integer, Pair<SlotType, Integer>>();
	// Four of them? This is getting out of hand

	public int size;

	public TrinketInventory(LivingEntityTrinketComponent comp) {
		this.component = comp;
		Map<String, SlotGroup> groups = TrinketsApi.getEntitySlots(comp.entity.getType());
		int i = 0;
		for (Map.Entry<String, SlotGroup> group : groups.entrySet()) {
			int groupOffset = 0;
			Map<String, SlotType> slots = group.getValue().getSlots();
			for (Map.Entry<String, SlotType> slot : slots.entrySet()) {
				slotMap.put(slot.getValue(), i);
				groupOffsetMap.put(slot.getValue(), groupOffset);
				for (int j = 0; j < slot.getValue().getAmount(); j++) {
					posMap.put(i + j, new Pair<SlotType, Integer>(slot.getValue(), j));
				}
				i += slot.getValue().getAmount();
				groupOffset += slot.getValue().getAmount();
			}
			groupOccupancyMap.put(group.getValue(), groupOffset);
		}
		size = i;
		stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
	}

	@Override
	public void clear() {
		for (int i = 0; i < size; i++) {
			stacks.set(i, ItemStack.EMPTY);
		}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < size; i++) {
			if (!stacks.get(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return stacks.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return Inventories.splitStack(stacks, slot, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(stacks, slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		stacks.set(slot, stack);
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}
}