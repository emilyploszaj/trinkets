package dev.emi.trinkets;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public class TestTrinket extends TrinketItem {

	public TestTrinket(Settings settings) {
		super(settings);
	}

	@Override
	public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, UUID uuid) {
		Multimap<EntityAttribute, EntityAttributeModifier> modifiers = super.getModifiers(stack, slot, entity, uuid);
		EntityAttributeModifier speedModifier = new EntityAttributeModifier(uuid, "trinkets-testmod:movement_speed",
				0.4, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		modifiers.put(EntityAttributes.GENERIC_MOVEMENT_SPEED, speedModifier);
		return modifiers;
	}
}
