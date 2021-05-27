package dev.emi.trinkets.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class TrinketInventory implements Inventory {

	private final SlotType slotType;
	private final TrinketComponent component;

	public DefaultedList<ItemStack> stacks;

	public TrinketInventory(SlotType slotType, TrinketComponent comp) {
		this.component = comp;
		this.slotType = slotType;
		this.stacks = DefaultedList.ofSize(slotType.getAmount(), ItemStack.EMPTY);
	}

	public SlotType getSlotType() {
		return this.slotType;
	}

	public TrinketComponent getComponent() {
		return this.component;
	}

	@Override
	public void clear() {
		for (int i = 0; i < this.size(); i++) {
			stacks.set(i, ItemStack.EMPTY);
		}
	}

	@Override
	public int size() {
		return this.stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < this.size(); i++) {
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