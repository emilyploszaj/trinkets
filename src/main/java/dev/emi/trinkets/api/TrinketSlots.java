package dev.emi.trinkets.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Registry and screenHandler of all slot groups and slots
 */
public class TrinketSlots {
	public static List<SlotGroup> slotGroups = new ArrayList<SlotGroup>();

	static {
		//Default slot groups
		TrinketSlots.addSlotGroup(SlotGroups.HEAD, 5);
		TrinketSlots.addSlotGroup(SlotGroups.CHEST, 6);
		TrinketSlots.addSlotGroup(SlotGroups.LEGS, 7);
		TrinketSlots.addSlotGroup(SlotGroups.FEET, 8);
		TrinketSlots.addSlotGroup(SlotGroups.OFFHAND, 45);
		TrinketSlots.addSlotGroup(SlotGroups.HAND, Slots.GLOVES);
	}

	/**
	 * Adds a slot group with no existing vanilla slot
	 * @param name Slot group name, standardly the name of a body part
	 * @param defaultSlot The name of the default display slot for the group, this slot is not created and must be added later
	 */
	public static void addSlotGroup(String name, String defaultSlot) {
		if (name.matches("^[a-zA-Z0-9]+$")) {
			for (SlotGroup group: slotGroups) {
				if (group.name.equals(name)) {
					return;
				}
			}
			SlotGroup group = new SlotGroup(name);
			group.defaultSlot = defaultSlot;
			slotGroups.add(group);
		} else {
			System.err.println("Failed to register slot " + name + ", identifiers must only be alphanumeric characters");
		}
	}

	private static void addSlotGroup(String name, int vanillaSlot) {
		if (name.matches("^[a-zA-Z0-9]+$")) {
			for (SlotGroup group: slotGroups) {
				if(group.name.equals(name)){
					return;
				}
			}
			SlotGroup group = new SlotGroup(name);
			group.vanillaSlot = vanillaSlot;
			group.onReal = true;
			slotGroups.add(group);
		} else {
			System.err.println("Failed to register slot " + name + ", identifiers must only be alphanumeric characters");
		}
	}

	/**
	 * Adds a new slot to an existing slot group
	 * @param groupName Name of the slot group to add the slot to
	 * @param slotName Name of the new slot
	 * @param texture The identifier representing the path to the file to be used for rendering the slot's background. Please use {@code new Identifier("trinkets", "textures/gui/blank_back.png")} if you want a blank slot
	 */
	public static void addSlot(String groupName, String slotName, Identifier texture) {
		if (slotName.matches("^[a-zA-Z0-9]+$")) {
			for (SlotGroup group: slotGroups) {
				if (group.name.equals(groupName)) {
					for (Slot slot: group.slots) {
						if (slot.name.equals(slotName)) {
							return;
						}
					}
					if (!group.onReal && slotName.equals(group.defaultSlot) && group.slots.size() > 0) {
						Slot s = group.slots.get(0);
						group.slots.set(0, new Slot(slotName, texture, group));
						group.slots.add(s);
					} else {
						group.slots.add(new Slot(slotName, texture, group));
					}
				}
			}
		} else {
			System.err.println("Failed to register slot " + groupName + ":" + slotName + ", identifiers must only be alpha characters");
		}
	}

	/**
	 * Adds a new slot to an existing slot group
	 * @param groupName Name of the slot group to add the slot to
	 * @param slotName Name of the new slot
	 * @param texture The identifier representing the path to the file to be used for rendering the slot's background. Please use {@code new Identifier("trinkets", "textures/gui/blank_back.png")} if you want a blank slot
	 * @param canEquip Function to be run to test if an ItemStack can be put into this trinket slot
	 */
	public static void addSlot(String groupName, String slotName, Identifier texture, BiFunction<Slot, ItemStack, Boolean> canEquip) {
		if (slotName.matches("^[a-zA-Z0-9]+$")) {
			for (SlotGroup group: slotGroups) {
				if (group.name.equals(groupName)) {
					for (Slot slot: group.slots) {
						if (slot.name.equals(slotName)) {
							slot.canEquip = canEquip;
							return;
						}
					}
					Slot newSlot = new Slot(slotName, texture, group);
					newSlot.canEquip = canEquip;
					if (!group.onReal && slotName.equals(group.defaultSlot) && group.slots.size() > 0) {
						Slot s = group.slots.get(0);
						group.slots.set(0, newSlot);
						group.slots.add(s);
					} else {
						group.slots.add(newSlot);
					}
				}
			}
		} else {
			System.err.println("Failed to register slot " + groupName + ":" + slotName + ", identifiers must only be alpha characters");
		}
	}

	/**
	 * @return List of {@code group:slot} names for all slots currently registered
	 */
	public static List<String> getAllSlotNames() {
		List<String> names = new ArrayList<String>();
		for(SlotGroup group: slotGroups){
			for(Slot slot: group.slots){
				names.add(group.getName() + ":" + slot.getName());
			}
		}
		return names;
	}

	/**
	 * @return List of all slots currently registered
	 */
	public static List<Slot> getAllSlots() {
		List<Slot> slots = new ArrayList<Slot>();
		for(SlotGroup group: slotGroups){
			for(Slot slot: group.slots){
				slots.add(slot);
			}
		}
		return slots;
	}

	/**
	 * @return Slot from group and slot names
	 */
	public static Slot getSlotFromName(String group, String slot) {
		for(SlotGroup g: slotGroups){
			for(Slot s: g.slots){
				if(g.name.equals(group) && s.name.equals(slot)) return s;
			}
		}
		return null;
	}

	/**
	 * @return Number of slots currently registered
	 */
	public static int getSlotCount(){
		int slotCount = 0;
		for(SlotGroup group: slotGroups){
			slotCount += group.slots.size();
		}
		return slotCount;
	}

	public static class SlotGroup {
		private String name;
		public String defaultSlot;
		public List<Slot> slots = new ArrayList<Slot>();
		public boolean onReal = false;
		public int vanillaSlot = -1;

		public SlotGroup(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public static class Slot {
		private SlotGroup group;
		private String name;
		public boolean disableQuickMove = false;
		public Identifier texture;
		public BiFunction<Slot, ItemStack, Boolean> canEquip = (slot, stack) -> {
			if (!(stack.getItem() instanceof Trinket)) return false;
			return ((Trinket) stack.getItem()).canWearInSlot(slot.getSlotGroup().getName(), slot.getName());
		};

		public Slot(String name, Identifier texture, SlotGroup group){
			this.name = name;
			this.texture = texture;
			this.group = group;
		}

		public SlotGroup getSlotGroup() {
			return group;
		}

		public String getName() {
			return name;
		}
	}
}