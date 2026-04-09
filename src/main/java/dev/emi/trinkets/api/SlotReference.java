package dev.emi.trinkets.api;

public record SlotReference(TrinketInventory inventory, int index) {
    public String getId() {
        return inventory.getSlotType().getGroup() + "/" + inventory.getSlotType().getName() + "/" + index;
    }
}