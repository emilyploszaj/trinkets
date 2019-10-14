package dev.emi.trinkets.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Identifier;

public class TrinketSlots{
	public static List<SlotGroup> slotGroups = new ArrayList<SlotGroup>();

	static{
		//Default slot groups
		TrinketSlots.addSlotGroup(SlotGroups.HEAD, 7, 7, 5);
		TrinketSlots.addSlotGroup(SlotGroups.CHEST, 7, 25, 6);
		TrinketSlots.addSlotGroup(SlotGroups.LEGS, 7, 43, 7);
		TrinketSlots.addSlotGroup(SlotGroups.FEET, 7, 61, 8);
		TrinketSlots.addSlotGroup(SlotGroups.HAND, 76, 43, "gloves");
		TrinketSlots.addSlotGroup(SlotGroups.OFFHAND, 76, 61, 45);
	}
	/**
	 * Adds a slot group with no existing vanilla slot
	 * @param name Slot group name, standardly the name of a body part
	 * @param x	Screen x coordinate relative to the left of the survival inventory
	 * @param y Screen y coordinate relative to the top of the survival inventory
	 * @param defaultSlot The name of the default display slot for the group, this slot is not created and must be added later
	 */
	public static void addSlotGroup(String name, int x, int y, String defaultSlot){
		if(name.matches("^[a-zA-Z]+$")){
			for(SlotGroup group: slotGroups){
				if(group.name.equals(name)){
					return;
				}
			}
			SlotGroup group = new SlotGroup(name, x, y);
			group.defaultSlot = defaultSlot;
			slotGroups.add(group);
		}else{
			System.err.println("Failed to register slot " + name + ", identifiers must only be alpha characters");
		}
	}
	/**
	 * Adds a slot group over an existing vanilla slot
	 * @param name Slot group name, standardly the name of a body part
	 * @param x	Screen x coordinate relative to the left of the survival inventory
	 * @param y Screen y coordinate relative to the top of the survival inventory
	 * @param vanillaSlot The id in the survival inventory of the slot to place over
	 */
	public static void addSlotGroup(String name, int x, int y, int vanillaSlot){
		if(name.matches("^[a-zA-Z]+$")){
			for(SlotGroup group: slotGroups){
				if(group.name.equals(name)){
					return;
				}
			}
			SlotGroup group = new SlotGroup(name, x, y);
			group.vanillaSlot = vanillaSlot;
			group.onReal = true;
			slotGroups.add(group);
		}else{
			System.err.println("Failed to register slot " + name + ", identifiers must only be alpha characters");
		}
	}
	/**
	 * Adds a new slot to an existing slot group
	 * @param groupName Name of the slot group to add the slot to
	 * @param slotName Name of the new slot
	 * @param texture The identifier representing the path to the file to be used for rendering the slot's background. Please use {@code new Identifier("trinkets", "textures/gui/blank_back.png")} if you want a blank slot
	 */
	public static void addSubSlot(String groupName, String slotName, Identifier texture){
		if(slotName.matches("^[a-zA-Z]+$")){
			for(SlotGroup group: slotGroups){
				if(group.name == groupName){
					for(Slot slot: group.slots){
						if(slot.name.equals(slotName)){
							return;
						}
					}
					if(!group.onReal && slotName.equals(group.defaultSlot) && group.slots.size() > 0){
						Slot s = group.slots.get(0);
						group.slots.set(0, new Slot(slotName, texture, group));
						group.slots.add(s);
					}else{
						group.slots.add(new Slot(slotName, texture, group));
					}
				}
			}
		}else{
			System.err.println("Failed to register slot " + groupName + ":" + slotName + ", identifiers must only be alpha characters");
		}
	}
	/**
	 * @return List of {@code group:slot} names for all slots currently registered
	 */
	public static List<String> getAllSlotNames(){
		List<String> names = new ArrayList<String>();
		for(SlotGroup group: slotGroups){
			for(Slot slot: group.slots){
				names.add(group.getName() + ":" + slot.getName());
			}
		}
		return names;
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

	public static class SlotGroup{
		private String name;
		public String defaultSlot;
		public List<Slot> slots = new ArrayList<Slot>();
		public boolean onReal = false;
		public int x, y, vanillaSlot = -1;
		public SlotGroup(String name, int x, int y){
			this.name = name;
			this.x = x;
			this.y = y;
		}
		public String getName(){
			return name;
		}
		public boolean inBounds(float cursorX, float cursorY, boolean focused){
			if(focused){
				int count = slots.size();
				if(onReal) count++;
				int l = count / 2;
				int r = count - l - 1;
				return cursorX > x - l * 18 - 4 && cursorY > y - 4 && cursorX < x + r * 18 + 22 && cursorY < y + 22;
			}else{
				return cursorX > x && cursorY > y && cursorX < x + 18 && cursorY < y + 18;
			}
		}
	}
	public static class Slot{
		private SlotGroup group;
		private String name;
		public Identifier texture;
		public Slot(String name, Identifier texture, SlotGroup group){
			this.name = name;
			this.texture = texture;
			this.group = group;
		}
		public SlotGroup getSlotGroup(){
			return group;
		}
		public String getName(){
			return name;
		}
	}
}