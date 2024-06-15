package dev.emi.trinkets.api;

public record SlotReference(TrinketInventory inventory, int index) {
    public String getRefId() {
        return this.inventory.getSlotType().getId() + "/" + index;
    }
}