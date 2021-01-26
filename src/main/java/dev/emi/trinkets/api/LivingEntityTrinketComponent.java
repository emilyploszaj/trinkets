package dev.emi.trinkets.api;

import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.Trinket.SlotReference;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class LivingEntityTrinketComponent implements TrinketComponent, AutoSyncedComponent {

	public TrinketInventory inventory;
	public LivingEntity entity;

	public LivingEntityTrinketComponent(LivingEntity entity) {
		this.entity = entity;
		this.inventory = new TrinketInventory(this);
	}

	@Override
	public void readFromNbt(CompoundTag tag) {
		Set<String> keys = tag.getKeys();
		for (Map.Entry<SlotType, Integer> entry : inventory.slotMap.entrySet()) {
			String name = entry.getKey().getGroup() + ":" + entry.getKey().getName();
			if (tag.contains(name, 9)) { // ListTag
				ListTag list = tag.getList(name, 10);
				int offset = entry.getValue();
				for (int i = 0; i < list.size(); i++) {
					CompoundTag c = list.getCompound(i);
					ItemStack stack = ItemStack.fromTag(c);
					if (i >= entry.getKey().getAmount()) {
						if (!stack.isEmpty()) {
							// If amount is lowered between loads of the entity drop excess
							TrinketsMain.LOGGER
									.info("[trinkets] Found item in slot that doesn't exist! Dropping on ground.");
							entity.dropStack(stack);
						}
					} else {
						inventory.setStack(offset + i, stack);
					}
				}
			}
			keys.remove(name);
		}
		for (String key : keys) {
			if (tag.getType(key) == 9) { // ListTag
				ListTag list = tag.getList(key, 10);
				for (int i = 0; i < list.size(); i++) {
					CompoundTag c = list.getCompound(i);
					ItemStack stack = ItemStack.fromTag(c);
					if (!stack.isEmpty()) {
						// If slot is removed between loads of the entity drop items
						TrinketsMain.LOGGER
								.info("[trinkets] Found item in slot that doesn't exist! Dropping on ground.");
						entity.dropStack(stack);
					}
				}
			}
		}
	}

	@Override
	public void writeToNbt(CompoundTag tag) {
		for (Map.Entry<SlotType, Integer> entry : inventory.slotMap.entrySet()) {
			ListTag list = new ListTag();
			int offset = entry.getValue();
			for (int i = 0; i < entry.getKey().getAmount(); i++) {
				CompoundTag c = new CompoundTag();
				inventory.getStack(offset + i).toTag(c);
				list.add(c);
			}
			tag.put(entry.getKey().getGroup() + ":" + entry.getKey().getName(), list);
		}
	}

	@Override
	public TrinketInventory getInventory() {
		return inventory;
	}

	@Override
	public boolean isEquipped(Predicate<ItemStack> predicate) {
		for (int i = 0; i < inventory.size(); i++) {
			if (predicate.test(inventory.getStack(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
		List<Pair<SlotReference, ItemStack>> list = new ArrayList<>();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (predicate.test(stack)) {
				Pair<SlotType, Integer> pair = inventory.posMap.get(i);
				list.add(new Pair<>(new SlotReference(pair.getLeft(), pair.getRight()), stack));
			}
		}
		return list;
	}
}