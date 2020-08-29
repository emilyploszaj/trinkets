package dev.emi.trinkets.api;

import java.util.Iterator;
import java.util.List;

import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.TrinketSlots.Slot;
import dev.emi.trinkets.api.TrinketSlots.SlotGroup;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.util.sync.EntitySyncedComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

/**
 * TrinketComponent implemented for players
 */
public class PlayerTrinketComponent implements TrinketComponent, EntitySyncedComponent {
	private Inventory inventory;
	private PlayerEntity player;

	public PlayerTrinketComponent(PlayerEntity player) {
		inventory = new TrinketInventory(this, TrinketSlots.getSlotCount());
		this.player = player;
	}

	@Override
	public void fromTag(CompoundTag tag) {
		List<String> keys = TrinketSlots.getAllSlotNames();
		Iterator<String> iterator = tag.getKeys().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (keys.contains(key)) {
				int keyPos = keys.indexOf(key);
				inventory.setStack(keyPos, ItemStack.fromTag(tag.getCompound(key)));
			} else if(!key.equals("componentId")) {
				System.out.println("[trinkets] Found item in slot that doesn't exist! " + key);
				ItemStack stack = ItemStack.fromTag(tag.getCompound(key));
				if (!equip(stack)) {
					if (!player.giveItemStack(stack)) {
						player.dropItem(stack, false);
					}
				}
			}
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		List<String> keys = TrinketSlots.getAllSlotNames();
		for (int i = 0; i < keys.size(); i++) {
			tag.put(keys.get(i), inventory.getStack(i).toTag(new CompoundTag()));
		}
		return tag;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}

	/**
	 * Gets a stack from a {@code group:slot} identifier
	 */
	@Override
	public ItemStack getStack(String slot) {
		int i = 0;
		for (String s: TrinketSlots.getAllSlotNames()) {
			if (s.equals(slot)) return getInventory().getStack(i);
			i++;
		}
		return ItemStack.EMPTY;
	}

	/**
	 * Gets a stack from a group and slot
	 */
	@Override
	public ItemStack getStack(String group, String slot) {
		return getStack(group + ":" + slot);
	}

	/**
	 * Attempts to equip a trinket into any available trinket slots
	 * @param stack The itemstack to equip, note that this is copied but not modified even on success
	 * @return {@code true} if successfully equipped
	 */
	@Override
	public boolean equip(ItemStack stack, boolean shiftClick) {
		int i = 0;
		for (SlotGroup group: TrinketSlots.slotGroups) {
			for (Slot slot: group.slots) {
				if (shiftClick && slot.disableQuickMove) continue;
				if (slot.canEquip.apply(slot, stack)) {
					if (getInventory().getStack(i).isEmpty()) {
						getInventory().setStack(i, stack.copy());
						//Makes a 16 tick popup appear showing the player where their item went when shift clicking
						TrinketsClient.lastEquipped = group;
						TrinketsClient.displayEquipped = 16;
						return true;
					}
				}
				i++;
			}
		}
		return false;
	}

	@Override
	public ComponentType<?> getComponentType() {
		return TrinketsApi.TRINKETS;
	}

	@Override
	public Entity getEntity() {
		return player;
	}
}