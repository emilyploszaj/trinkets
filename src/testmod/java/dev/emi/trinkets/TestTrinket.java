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

	private static final EntityAttributeModifier SPEED_BOOST_MODIFIER = new EntityAttributeModifier(UUID.fromString("ac7ab816-2b08-46b6-879d-e5dea34ff305"), "trinkets-testmod:movement_speed", 0.4, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

	public TestTrinket(Settings settings) {
		super(settings);
	}

	@Override
	public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, UUID uuid) {
		Multimap<EntityAttribute, EntityAttributeModifier> modifiers = super.getModifiers(stack, slot, entity, uuid);
		modifiers.put(EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_BOOST_MODIFIER);
		return modifiers;
	}
}
