package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class SlotGroup {

	private final String name;
	private final String defaultSlot;
	private final int slotId;
	private final Map<String, SlotType> slots;

	private SlotGroup(Builder builder) {
		this.name = builder.name;
		this.defaultSlot = builder.defaultSlot;
		this.slots = builder.slots;
		this.slotId = builder.slotId;
	}

	public int getSlotId() {
		return slotId;
	}

	public String getName() {
		return name;
	}

	public String getDefaultSlot() {
		return defaultSlot;
	}

	public Map<String, SlotType> getSlots() {
		return ImmutableMap.copyOf(slots);
	}

	public static class Builder {

		private final String name;
		private final String defaultSlot;
		private final int slotId;
		private final Map<String, SlotType> slots = new HashMap<>();

		public Builder(String name, String defaultSlot) {
			this.name = name;
			this.defaultSlot = defaultSlot;
			this.slotId = -1;
		}

		public Builder(int slotId) {
			this.slotId = slotId;
			this.defaultSlot = "";
		}

		public Builder addSlot(String name, SlotType slot) {
			this.slots.put(name, slot);
			return this;
		}

		public SlotGroup build() {
			return new SlotGroup(this);
		}
	}
}
