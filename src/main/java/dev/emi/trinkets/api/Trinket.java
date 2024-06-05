package dev.emi.trinkets.api;

import java.util.ArrayList;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import dev.emi.trinkets.mixin.accessor.LivingEntityAccessor;
import java.util.function.Consumer;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Equipment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public interface Trinket {

	/**
	 * Called every tick on the client and server side
	 *
	 * @param stack The stack being ticked
	 * @param slot The slot the stack is ticking in
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
		return !EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE) || (entity instanceof PlayerEntity player && player.isCreative());
	}

	/**
	 * Determines whether a trinket can automatically attempt to equip into the first available
	 * slot when used
	 *
	 * @param stack The stack being equipped
	 * @param entity The entity that is using the stack
	 * @return Whether the stack can be equipped from use
	 */
	default boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
		return false;
	}

	/**
	 * Determines the equip sound of a trinket
	 *
	 * @param stack The stack for the equip sound
	 * @param slot The slot the stack is being equipped to
	 * @param entity The entity that is equipping the stack
	 * @return The {@link SoundEvent} to play for equipping
	 */
	default RegistryEntry<SoundEvent> getEquipSound(ItemStack stack, SlotReference slot, LivingEntity entity) {
		return stack.getItem() instanceof Equipment eq ? eq.getEquipSound() : null;
	}

	/**
	 * Returns the Entity Attribute Modifiers for a stack in a slot. Child implementations should
	 * remain pure
	 * <p>
	 * If modifiers do not change based on stack, slot, or entity, caching based on passed UUID
	 * should be considered
	 *
	 * @param slotIdentifier The Identifier to use for creating attributes
	 */
	default Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, Identifier slotIdentifier) {
		Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);

		if (stack.contains(TrinketsAttributeModifiersComponent.TYPE)) {
			for (var entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
				if (entry.slot().isEmpty() || entry.slot().get().equals(slot.inventory().getSlotType().getId())) {
					map.put(entry.attribute(), entry.modifier());
				}
			}
		}
		return map;
	}

	/**
	 * Called by Trinkets when a trinket is broken on the client if {@link TrinketsApi#onTrinketBroken}
	 * is called by the callback in {@link ItemStack#damage(int, ServerWorld, ServerPlayerEntity, Consumer)} server side
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