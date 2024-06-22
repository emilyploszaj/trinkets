package dev.emi.trinkets;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class TrinketModifiers {

    //internalizes getTrinket and slotIdentifier, both which typically are generated just before the modifiers call anyway
    public static Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> get(ItemStack stack, SlotReference slot, LivingEntity entity){
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = TrinketsApi.getTrinket(stack.getItem()).getModifiers(stack, slot, entity, SlotAttributes.getIdentifier(slot));
        if (stack.contains(TrinketsAttributeModifiersComponent.TYPE)) {
            for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
                if (entry.slot().isEmpty() || entry.slot().get().equals(slot.inventory().getSlotType().getId())) {
                    map.put(entry.attribute(), toSlotReferencedModifier(entry.modifier(), slot));
                }
            }
        }
        return map;
    }

    //overload if a custom method for retrieving the trinket is used. Also exposes the slotIdentifier if custom on that is needed
    public static Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> get(Trinket trinket, ItemStack stack, SlotReference slot, LivingEntity entity, Identifier slotIdentifier){
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = trinket.getModifiers(stack, slot, entity, slotIdentifier);
        if (stack.contains(TrinketsAttributeModifiersComponent.TYPE)) {
            for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
                if (entry.slot().isEmpty() || entry.slot().get().equals(slot.inventory().getSlotType().getId())) {
                    map.put(entry.attribute(), toSlotReferencedModifier(entry.modifier(), slot));
                }
            }
        }
        return map;
    }

    public static EntityAttributeModifier toSlotReferencedModifier(EntityAttributeModifier modifier, SlotReference ref){
        return new EntityAttributeModifier(modifier.id().withSuffixedPath("/" + ref.getId()), modifier.value(), modifier.operation());
    }
}