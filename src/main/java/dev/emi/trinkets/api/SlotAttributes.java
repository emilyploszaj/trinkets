package dev.emi.trinkets.api;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;

public class SlotAttributes {
	private static Map<String, UUID> CACHED_UUIDS= Maps.newHashMap();
	private static Map<String, RegistryEntry<EntityAttribute>> CACHED_ATTRIBUTES = Maps.newHashMap();
	
	/**
	 * Adds an Entity Attribute Nodifier for slot count to the provided multimap
	 */
	public static void addSlotModifier(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map, String slot, UUID uuid, double amount,
			EntityAttributeModifier.Operation operation) {
		CACHED_ATTRIBUTES.putIfAbsent(slot, RegistryEntry.of(new SlotEntityAttribute(slot)));
		map.put(CACHED_ATTRIBUTES.get(slot), new EntityAttributeModifier(uuid, slot, amount, operation));
	}

	public static UUID getUuid(SlotReference ref) {
		String key = ref.inventory().getSlotType().getGroup() + "/" + ref.inventory().getSlotType().getName() + "/" + ref.index();
		CACHED_UUIDS.putIfAbsent(key, UUID.nameUUIDFromBytes(key.getBytes()));
		return CACHED_UUIDS.get(key);
	}

	public static class SlotEntityAttribute extends EntityAttribute {
		public String slot; 

		private SlotEntityAttribute(String slot) {
			super("trinkets.slot." + slot.replace("/", "."), 0);
			this.slot = slot;
		}
	}
}
