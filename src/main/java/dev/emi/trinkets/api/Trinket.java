package dev.emi.trinkets.api;

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

import java.util.Optional;
import java.util.UUID;

public interface Trinket {

	/**
	 * Called every tick on the client and server side
	 *
	 * @param stack The stack being ticked
	 * @param slot The slot the stack is equipped to
	 * @param entity The entity wearing the stack
	 */
	default void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
	}

	/**
	 * Called when an entity equips a trinket
	 *
	 * @param stack The stack being equipped
	 * @param slot The slot the stack is equipped to
	 * @param entity The entity that equipped the stack
	 */
	default void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
	}

	/**
	 * Called when an entity equips a trinket
	 *
	 * @param stack The stack being unequipped
	 * @param slot The slot the stack was unequipped from
	 * @param entity The entity that unequipped the stack
	 */
	default void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
	}

	/**
	 * Determines whether an entity can equip a trinket
	 *
	 * @param stack The stack being equipped
	 * @param slot The slot the stack is being equipped to
	 * @param entity The entity that is equipping the stack
	 * @return Whether the stack can be equipped
	 */
	default boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
		return true;
	}

	/**
	 * Determines whether an entity can unequip a trinket
	 *
	 * @param stack The stack being unequipped
	 * @param slot The slot the stack is being unequipped from
	 * @param entity The entity that is unequipping the stack
	 * @return Whether the stack can be unequipped
	 */
	default boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
		return !EnchantmentHelper.hasBindingCurse(stack);
	}

	/**
	 * Returns the Entity Attribute Modifiers for a stack in a slot. Child implementations should
	 * remain pure
	 *
	 * @param uuid The UUID to use for creating attributes
	 */
	default Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack,
			SlotReference slot, LivingEntity entity, UUID uuid) {
		Multimap<EntityAttribute, EntityAttributeModifier> map = HashMultimap.create();

		if (stack.hasTag() && stack.getTag().contains("TrinketAttributeModifiers", 9)) {
			ListTag list = stack.getTag().getList("TrinketAttributeModifiers", 10);

			for (int i = 0; i < list.size(); ++i) {
				CompoundTag tag = list.getCompound(i);

				// TODO Determine and setup a slot naming scheme for tags, probably just group:slot, and uncomment the second condition
				if (!tag.contains("Slot", 8)/* || tag.getString("Slot").equals(equipmentSlot.getName())*/) {
					Optional<EntityAttribute> optional = Registry.ATTRIBUTE
							.getOrEmpty(Identifier.tryParse(tag.getString("AttributeName")));

					if (optional.isPresent()) {
						EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromTag(tag);

						if (entityAttributeModifier != null
								&& entityAttributeModifier.getId().getLeastSignificantBits() != 0L
								&& entityAttributeModifier.getId().getMostSignificantBits() != 0L) {
							map.put(optional.get(), entityAttributeModifier);
						}
					}
				}
			}
		}
		return map;
	}

	/**
	 * Returns the Slot Modifiers for a stack in a slot. Child implementations should remain pure.
	 * Keys should be in the format "group:slot".
	 *
	 * @param uuid The UUID to use for creating attributes
	 */
	default Multimap<String, EntityAttributeModifier> getSlotModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, UUID uuid) {
		return HashMultimap.create();
	}

	default void onBreak(ItemStack stack, SlotReference slot, LivingEntity entity) {
		// TODO
	}

	default TrinketEnums.DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
		return TrinketEnums.DropRule.DEFAULT;
	}

	class SlotReference {
		public TrinketInventory inventory;
		public int index;

		public SlotReference(TrinketInventory inventory, int index) {
			this.inventory = inventory;
			this.index = index;
		}
	}
}