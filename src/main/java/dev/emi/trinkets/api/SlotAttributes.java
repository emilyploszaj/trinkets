package dev.emi.trinkets.api;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class SlotAttributes {
	private static final Map<String, Identifier> CACHED_IDS = Maps.newHashMap();
	private static final Map<String, RegistryEntry<EntityAttribute>> CACHED_ATTRIBUTES = Maps.newHashMap();

	/**
	 * Adds an Entity Attribute Modifier for slot count to the provided multimap
	 */
	public static void addSlotModifier(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map, String slot, Identifier identifier, double amount,
			EntityAttributeModifier.Operation operation) {
		CACHED_ATTRIBUTES.putIfAbsent(slot, RegistryEntry.of(new SlotEntityAttribute(slot)));
		map.put(CACHED_ATTRIBUTES.get(slot), new EntityAttributeModifier(identifier, amount, operation));
	}

	public static Identifier getIdentifier(SlotReference ref) {
		String key = ref.getId();
		return CACHED_IDS.computeIfAbsent(key, Identifier::of);
	}

	public static class SlotEntityAttribute extends EntityAttribute {
		public String slot;

		private SlotEntityAttribute(String slot) {
			super("trinkets.slot." + slot.replace("/", "."), 0);
			this.slot = slot;
		}
	}
}