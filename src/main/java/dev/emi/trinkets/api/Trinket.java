package dev.emi.trinkets.api;

import java.util.Optional;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public interface Trinket {

	/**
	 * Called every tick on the client and server side
	 */
	public default void tick(ItemStack stack, TrinketSlot slot, LivingEntity entity) {
	}

	/**
	 * Called when an entity equips a trinket
	 * 
	 * @param stack		The stack being equipped
	 */
	public default void onEquip(ItemStack stack, TrinketSlot slot, LivingEntity entity) {
	}
	
	/**
	 * Called when an entity equips a trinket
	 * 
	 * @param stack		The stack being unequipped
	 */
	public default void onUnequip(ItemStack stack, TrinketSlot slot, LivingEntity entity) {
	}

	public default boolean canEquip(ItemStack stack, TrinketSlot slot, LivingEntity entity) {
		return true;
	}

	public default boolean canUnequip(ItemStack stack, TrinketSlot slot, LivingEntity entity) {
		return !EnchantmentHelper.hasBindingCurse(stack);
	}

	/**
	 * Returns the Entity Attribute Modifiers for a stack in a slot.
	 * Super implementations should remain pure
	 */
	public default Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, TrinketSlot slot, LivingEntity entity) {
		Multimap<EntityAttribute, EntityAttributeModifier> map = HashMultimap.create();
		if (stack.hasTag() && stack.getTag().contains("TrinketAttributeModifiers", 9)) {
			ListTag list = stack.getTag().getList("TrinketAttributeModifiers", 10);

			for(int i = 0; i < list.size(); ++i) {
				CompoundTag tag = list.getCompound(i);
				// TODO Determine and setup a slot naming scheme for tags, probably just group:slot, and uncomment the second condition
				if (!tag.contains("Slot", 8)/* || tag.getString("Slot").equals(equipmentSlot.getName())*/) {
					Optional<EntityAttribute> optional = Registry.ATTRIBUTE.getOrEmpty(Identifier.tryParse(tag.getString("AttributeName")));
					if (optional.isPresent()) {
						EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromTag(tag);
						if (entityAttributeModifier != null && entityAttributeModifier.getId().getLeastSignificantBits() != 0L && entityAttributeModifier.getId().getMostSignificantBits() != 0L) {
							map.put(optional.get(), entityAttributeModifier);
						}
					}
				}
			}
		}
		return map;
	}

	public default void onBreak(ItemStack stack, TrinketSlot slot, LivingEntity entity) {
		// TODO
	}

	// TODO getDropRule

	class TrinketSlot {
		// Temporary dummy type 
	}
}