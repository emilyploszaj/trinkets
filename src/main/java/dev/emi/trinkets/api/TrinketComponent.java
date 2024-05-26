package dev.emi.trinkets.api;

import com.google.common.collect.Multimap;

import org.ladysnake.cca.api.v3.component.ComponentV3;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface TrinketComponent extends ComponentV3 {

	LivingEntity getEntity();

	/**
	 * @return A map of names to slot groups available to the entity
	 */
	Map<String, SlotGroup> getGroups();

	/**
	 * @return A map of slot group names, to slot names, to trinket inventories
	 * for the entity. Inventories will respect EAM slot count modifications for
	 * the entity.
	 */
	Map<String, Map<String, TrinketInventory>> getInventory();

	void update();

	void addTemporaryModifiers(Multimap<String, EntityAttributeModifier> modifiers);

	void addPersistentModifiers(Multimap<String, EntityAttributeModifier> modifiers);

	void removeModifiers(Multimap<String, EntityAttributeModifier> modifiers);

	void clearModifiers();

	Multimap<String, EntityAttributeModifier> getModifiers();

	/**
	 * @return Whether the predicate matches any slots available to the entity
	 */
	boolean isEquipped(Predicate<ItemStack> predicate);

	/**
	 * @return Whether the item is in any slots available to the entity
	 */
	default boolean isEquipped(Item item) {
		return isEquipped(stack -> stack.getItem() == item);
	}

	/**
	 * @return All slots that match the provided predicate
	 */
	List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate);

	/**
	 * @return All slots that contain the provided item
	 */
	default List<Pair<SlotReference, ItemStack>> getEquipped(Item item) {
		return getEquipped(stack -> stack.getItem() == item);
	}

	/**
	 * @return All non-empty slots
	 */
	default List<Pair<SlotReference, ItemStack>> getAllEquipped() {
		return getEquipped(stack -> !stack.isEmpty());
	}

	/**
	 * Iterates over every slot available to the entity
	 */
	void forEach(BiConsumer<SlotReference, ItemStack> consumer);

	Set<TrinketInventory> getTrackingUpdates();

	void clearCachedModifiers();
}