package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotReference;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import java.util.List;

public interface TrinketEntityRenderState {
    void trinkets$setState(List<Pair<ItemStack, SlotReference>> items);
    List<Pair<ItemStack, SlotReference>> trinkets$getState();
}
