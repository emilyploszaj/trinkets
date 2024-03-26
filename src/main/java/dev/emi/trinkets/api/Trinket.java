package dev.emi.trinkets.api;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import dev.emi.trinkets.mixin.accessor.LivingEntityAccessor;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

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
		return !EnchantmentHelper.hasBindingCurse(stack) || (entity instanceof PlayerEntity player && player.isCreative());
	}

	/**
	 * Returns the Entity Attribute Modifiers for a stack in a slot. Child implementations should
	 * remain pure
	 * <p>
	 * If modifiers do not change based on stack, slot, or entity, caching based on passed UUID
	 * should be considered
	 *
	 * @param uuid The UUID to use for creating attributes
	 */
	default Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack,
			SlotReference slot, LivingEntity entity, UUID uuid) {
		Multimap<EntityAttribute, EntityAttributeModifier> map = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);

		if (stack.hasNbt() && stack.getNbt().contains("TrinketAttributeModifiers", 9)) {
			NbtList list = stack.getNbt().getList("TrinketAttributeModifiers", 10);

			for (int i = 0; i < list.size(); i++) {
				NbtCompound tag = list.getCompound(i);

				if (!tag.contains("Slot", NbtType.STRING) || tag.getString("Slot")
						.equals(slot.inventory().getSlotType().getGroup() + "/" + slot.inventory().getSlotType().getName())) {
					Optional<EntityAttribute> optional = Registries.ATTRIBUTE
							.getOrEmpty(Identifier.tryParse(tag.getString("AttributeName")));

					if (optional.isPresent()) {
						EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromNbt(tag);

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
	 * Called by Trinkets when a trinket is broken on the client if {@link TrinketsApi#onTrinketBroken}
	 * is called by the consumer in {@link ItemStack#damage(int, LivingEntity, Consumer)} server side
	 * <p>
	 * The default implementation works the same as breaking vanilla equipment, a sound is played and
	 * particles are spawned based on the item
	 *
	 * @param stack The stack being broken
	 * @param slot The slot the stack is being broken in
	 * @param entity The entity that is breaking the stack
	 */
	default void onBreak(ItemStack stack, SlotReference slot, LivingEntity entity) {
		((LivingEntityAccessor) entity).invokePlayEquipmentBreakEffects(stack);
	}

	default TrinketEnums.DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
		return TrinketEnums.DropRule.DEFAULT;
	}
}