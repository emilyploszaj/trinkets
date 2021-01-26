package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;

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

	public void write(CompoundTag data) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Name", name);
		tag.putString("DefaultSlot", defaultSlot);
		tag.putInt("SlotId", slotId);
		CompoundTag typesTag = new CompoundTag();

		slots.forEach((id, slot) -> {
			CompoundTag typeTag = new CompoundTag();
			slot.write(typeTag);
			typesTag.put(id, typeTag);
		});
		tag.put("SlotTypes", typesTag);
		data.put("GroupData", tag);
	}

	public static SlotGroup read(CompoundTag data) {
		CompoundTag groupData = data.getCompound("GroupData");
		String name = groupData.getString("Name");
		String defaultSlot = groupData.getString("DefaultSlot");
		int slotId = groupData.getInt("SlotId");
		CompoundTag typesTag = groupData.getCompound("SlotTypes");
		Builder builder = defaultSlot.isEmpty() ? new Builder(name, slotId) : new Builder(name, defaultSlot);

		for (String id : typesTag.getKeys()) {
			CompoundTag tag = (CompoundTag) typesTag.get(id);

			if (tag != null) {
				builder.addSlot(id, SlotType.read(tag));
			}
		}
		return builder.build();
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

		public Builder(String name, int slotId) {
			this.name = name;
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
