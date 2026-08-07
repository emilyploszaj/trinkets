package dev.emi.trinkets.api;

import net.minecraft.item.ItemStack;

public record SlotReference(TrinketInventory inventory, int index) {

    public String getId() {
        return this.inventory.getSlotType().getId() + "/" + index;
    }

    public ItemStack get() {
        return inventory.getStack(index);
    }

    public boolean set(ItemStack itemStack) {
        inventory.setStack(index, itemStack);
        return true;
    }

    public SlotType getSlotType() {
        return inventory.getSlotType();
    }
}