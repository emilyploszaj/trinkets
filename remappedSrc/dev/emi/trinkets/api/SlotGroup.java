package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;

public final class SlotGroup {

	private final String name;
	private final int slotId;
	private final int order;
	private final Map<String, SlotType> slots;

	private SlotGroup(Builder builder) {
		this.name = builder.name;
		this.slots = builder.slots;
		this.slotId = builder.slotId;
		this.order = builder.order;
	}

	public int getSlotId() {
		return slotId;
	}

	public int getOrder() {
		return order;
	}

	public String getName() {
		return name;
	}

	public Map<String, SlotType> getSlots() {
		return ImmutableMap.copyOf(slots);
	}

	public void write(NbtCompound data) {
		NbtCompound tag = new NbtCompound();
		tag.putString("Name", name);
		tag.putInt("SlotId", slotId);
		tag.putInt("Order", order);
		NbtCompound typesTag = new NbtCompound();

		slots.forEach((id, slot) -> {
			NbtCompound typeTag = new NbtCompound();
			slot.write(typeTag);
			typesTag.put(id, typeTag);
		});
		tag.put("SlotTypes", typesTag);
		data.put("GroupData", tag);
	}

	public static SlotGroup read(NbtCompound data) {
		NbtCompound groupData = data.getCompound("GroupData");
		String name = groupData.getString("Name");
		int slotId = groupData.getInt("SlotId");
		int order = groupData.getInt("Order");
		NbtCompound typesTag = groupData.getCompound("SlotTypes");
		Builder builder = new Builder(name, slotId, order);

		for (String id : typesTag.getKeys()) {
			NbtCompound tag = (NbtCompound) typesTag.get(id);

			if (tag != null) {
				builder.addSlot(id, SlotType.read(tag));
			}
		}
		return builder.build();
	}

	public static class Builder {

		private final String name;
		private final int slotId;
		private final int order;
		private final Map<String, SlotType> slots = new HashMap<>();

		public Builder(String name, int slotId, int order) {
			this.name = name;
			this.slotId = slotId;
			this.order = order;
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
