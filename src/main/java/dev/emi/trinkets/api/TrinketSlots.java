package dev.emi.trinkets.api;

import dev.emi.trinkets.data.EntitySlotLoader;
import java.util.Map;
import net.minecraft.entity.EntityType;

public class TrinketSlots {

  public static Map<String, SlotGroup> getPlayerSlots() {
    return EntitySlotLoader.INSTANCE.getPlayerSlots();
  }

  public static Map<String, SlotGroup> getEntitySlots(EntityType<?> entityType) {
    return EntitySlotLoader.INSTANCE.getEntitySlots(entityType);
  }
}
