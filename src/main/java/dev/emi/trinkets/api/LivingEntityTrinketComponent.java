package dev.emi.trinkets.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.Trinket.SlotReference;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class LivingEntityTrinketComponent implements TrinketComponent, AutoSyncedComponent {

	public Map<String, Map<String, TrinketInventory>> inventory = new HashMap<>();
	public Set<TrinketInventory> trackingUpdates = new HashSet<>();
	public Map<String, SlotGroup> groups = new HashMap<>();
	public int size;
	public LivingEntity entity;

	public LivingEntityTrinketComponent(LivingEntity entity) {
		this.entity = entity;
		this.update();
	}

	@Override
	public LivingEntity getEntity() {
		return this.entity;
	}

	@Override
	public Map<String, SlotGroup> getGroups() {
		return this.groups;
	}

	@Override
	public Map<String, Map<String, TrinketInventory>> getInventory() {
		return inventory;
	}

	@Override
	public void update() {
		Map<String, SlotGroup> entitySlots = TrinketsApi.getEntitySlots(this.entity.getType());
		int count = 0;
		groups.clear();
		Map<String, Map<String, TrinketInventory>> inventory = new HashMap<>();
		for (Map.Entry<String, SlotGroup> group : entitySlots.entrySet()) {
			String groupKey = group.getKey();
			SlotGroup groupValue = group.getValue();
			Map<String, TrinketInventory> oldGroup = this.inventory.get(groupKey);
			groups.put(groupKey, groupValue);
			for (Map.Entry<String, SlotType> slot : groupValue.getSlots().entrySet()) {
				TrinketInventory inv = new TrinketInventory(slot.getValue(), this, e -> this.trackingUpdates.add(e));
				if (oldGroup != null) {
					TrinketInventory oldInv = oldGroup.get(slot.getKey());
					if (oldInv != null) {
						inv.copyFrom(oldInv);
						for (int i = 0; i < oldInv.size(); i++) {
							ItemStack stack = oldInv.getStack(i).copy();
							if (i < inv.size()) {
								inv.setStack(i, stack);
							} else {
								this.entity.dropStack(stack);
							}
						}
					}
				}
				inventory.computeIfAbsent(group.getKey(), (k) -> new HashMap<>()).put(slot.getKey(), inv);
				count += inv.size();
			}
		}
		size = count;
		this.inventory = inventory;
	}

	@Override
	public void clearCachedModifiers() {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				slotType.getValue().clearCachedModifiers();
			}
		}
	}

	@Override
	public Set<TrinketInventory> getTrackingUpdates() {
		return this.trackingUpdates;
	}

	@Override
	public void addTemporaryModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
		for (Map.Entry<String, Collection<EntityAttributeModifier>> entry : modifiers.asMap().entrySet()) {
			String[] keys = entry.getKey().split(":");
			String group = keys[0];
			String slot = keys[1];
			for (EntityAttributeModifier modifier : entry.getValue()) {
				Map<String, TrinketInventory> groupInv = this.inventory.get(group);
				if (groupInv != null) {
					TrinketInventory inv = groupInv.get(slot);
					if (inv != null) {
						inv.addModifier(modifier);
					}
				}
			}
		}
	}

	@Override
	public void addPersistentModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
		for (Map.Entry<String, Collection<EntityAttributeModifier>> entry : modifiers.asMap().entrySet()) {
			String[] keys = entry.getKey().split(":");
			String group = keys[0];
			String slot = keys[1];
			for (EntityAttributeModifier modifier : entry.getValue()) {
				Map<String, TrinketInventory> groupInv = this.inventory.get(group);
				if (groupInv != null) {
					TrinketInventory inv = groupInv.get(slot);
					if (inv != null) {
						inv.addPersistentModifier(modifier);
					}
				}
			}
		}
	}

	@Override
	public void removeModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
		for (Map.Entry<String, Collection<EntityAttributeModifier>> entry : modifiers.asMap().entrySet()) {
			String[] keys = entry.getKey().split(":");
			String group = keys[0];
			String slot = keys[1];
			for (EntityAttributeModifier modifier : entry.getValue()) {
				Map<String, TrinketInventory> groupInv = this.inventory.get(group);
				if (groupInv != null) {
					TrinketInventory inv = groupInv.get(slot);
					if (inv != null) {
						inv.removeModifier(modifier.getId());
					}
				}
			}
		}
	}

	@Override
	public Multimap<String, EntityAttributeModifier> getModifiers() {
		Multimap<String, EntityAttributeModifier> result = HashMultimap.create();
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				result.putAll(group.getKey() + ":" + slotType.getKey(), slotType.getValue().getModifiers().values());
			}
		}

		return result;
	}

	@Override
	public void clearModifiers() {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				slotType.getValue().clearModifiers();
			}
		}
	}

	@Override
	public void readFromNbt(CompoundTag tag) {
		DefaultedList<ItemStack> dropped = DefaultedList.of();
		for (String groupKey : tag.getKeys()) {
			CompoundTag groupTag = tag.getCompound(groupKey);
			if (groupTag != null) {
				Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);
				if (groupSlots != null) {
					for (String slotKey : groupTag.getKeys()) {
						CompoundTag slotTag = groupTag.getCompound(slotKey);
						ListTag list = slotTag.getList("Items", NbtType.COMPOUND);
						TrinketInventory inv = groupSlots.get(slotKey);

						if (inv != null) {
							inv.fromTag(slotTag.getCompound("Metadata"));
						}

						for (int i = 0; i < list.size(); i++) {
							CompoundTag c = list.getCompound(i);
							ItemStack stack = ItemStack.fromTag(c);
							if (inv != null && i < inv.size()) {
								inv.setStack(i, stack);
							} else {
								dropped.add(stack);
							}
						}
					}
				} else {
					for (String slotKey : groupTag.getKeys()) {
						CompoundTag slotTag = groupTag.getCompound(slotKey);
						ListTag list = slotTag.getList("Items", NbtType.COMPOUND);
						for (int i = 0; i < list.size(); i++) {
							CompoundTag c = list.getCompound(i);
							dropped.add(ItemStack.fromTag(c));
						}
					}
				}
			}
		}

		for (ItemStack itemStack : dropped) {
			this.entity.dropStack(itemStack);
		}
	}

	@Override
	public void writeToNbt(CompoundTag tag) {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			CompoundTag groupTag = new CompoundTag();
			for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
				CompoundTag slotTag = new CompoundTag();
				ListTag list = new ListTag();
				TrinketInventory inv = slot.getValue();
				for (int i = 0; i < inv.size(); i++) {
					CompoundTag c = new CompoundTag();
					inv.getStack(i).toTag(c);
					list.add(c);
				}
				slotTag.put("Metadata", inv.toTag());
				slotTag.put("Items", list);
				groupTag.put(slot.getKey(), slotTag);
			}
			tag.put(group.getKey(), groupTag);
		}
	}

	@Override
	public boolean isEquipped(Predicate<ItemStack> predicate) {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				TrinketInventory inv = slotType.getValue();
				for (int i = 0; i < inv.size(); i++) {
					if (predicate.test(inv.getStack(i))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
		List<Pair<SlotReference, ItemStack>> list = new ArrayList<>();
		forEach((slotReference, itemStack) -> {
			if (predicate.test(itemStack)) {
				list.add(new Pair<>(slotReference, itemStack));
			}
		});
		return list;
	}

	@Override
	public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotType : group.getValue().entrySet()) {
				TrinketInventory inv = slotType.getValue();
				for (int i = 0; i < inv.size(); i++) {
					consumer.accept(new SlotReference(inv, i), inv.getStack(i));
				}
			}
		}
	}
}