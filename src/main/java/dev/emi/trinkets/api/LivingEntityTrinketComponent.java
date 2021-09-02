package dev.emi.trinkets.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

public class LivingEntityTrinketComponent implements TrinketComponent, AutoSyncedComponent {

	public Map<String, Map<String, TrinketInventory>> inventory = new HashMap<>();
	public Set<TrinketInventory> trackingUpdates = new HashSet<>();
	public Map<String, SlotGroup> groups = new HashMap<>();
	public int size;
	public LivingEntity entity;

	private boolean syncing;

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
								if (this.entity instanceof PlayerEntity player) {
									player.getInventory().offerOrDrop(stack);
								} else {
									this.entity.dropStack(stack);
								}
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
			String[] keys = entry.getKey().split("/");
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
			String[] keys = entry.getKey().split("/");
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
			String[] keys = entry.getKey().split("/");
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
				result.putAll(group.getKey() + "/" + slotType.getKey(), slotType.getValue().getModifiers().values());
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
	public void readFromNbt(NbtCompound tag) {
		DefaultedList<ItemStack> dropped = DefaultedList.of();
		for (String groupKey : tag.getKeys()) {
			NbtCompound groupTag = tag.getCompound(groupKey);
			if (groupTag != null) {
				Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);
				if (groupSlots != null) {
					for (String slotKey : groupTag.getKeys()) {
						NbtCompound slotTag = groupTag.getCompound(slotKey);
						NbtList list = slotTag.getList("Items", NbtType.COMPOUND);
						TrinketInventory inv = groupSlots.get(slotKey);

						if (inv != null) {
							inv.fromTag(slotTag.getCompound("Metadata"));
						}

						for (int i = 0; i < list.size(); i++) {
							NbtCompound c = list.getCompound(i);
							ItemStack stack = ItemStack.fromNbt(c);
							if (inv != null && i < inv.size()) {
								inv.setStack(i, stack);
							} else {
								dropped.add(stack);
							}
						}
					}
				} else {
					for (String slotKey : groupTag.getKeys()) {
						NbtCompound slotTag = groupTag.getCompound(slotKey);
						NbtList list = slotTag.getList("Items", NbtType.COMPOUND);
						for (int i = 0; i < list.size(); i++) {
							NbtCompound c = list.getCompound(i);
							dropped.add(ItemStack.fromNbt(c));
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
	public void applySyncPacket(PacketByteBuf buf) {
		NbtCompound tag = buf.readNbt();

		if (tag != null) {

			for (String groupKey : tag.getKeys()) {
				NbtCompound groupTag = tag.getCompound(groupKey);

				if (groupTag != null) {
					Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);

					if (groupSlots != null) {

						for (String slotKey : groupTag.getKeys()) {
							NbtCompound slotTag = groupTag.getCompound(slotKey);
							NbtList list = slotTag.getList("Items", NbtType.COMPOUND);
							TrinketInventory inv = groupSlots.get(slotKey);

							if (inv != null) {
								inv.applySyncTag(slotTag.getCompound("Metadata"));
							}

							for (int i = 0; i < list.size(); i++) {
								NbtCompound c = list.getCompound(i);
								ItemStack stack = ItemStack.fromNbt(c);
								if (inv != null && i < inv.size()) {
									inv.setStack(i, stack);
								}
							}
						}
					}
				}
			}

			if (this.entity instanceof PlayerEntity player) {
				((TrinketPlayerScreenHandler) player.playerScreenHandler).updateTrinketSlots(false);
			}
		}
	}

	@Override
	public void writeToNbt(NbtCompound tag) {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			NbtCompound groupTag = new NbtCompound();
			for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
				NbtCompound slotTag = new NbtCompound();
				NbtList list = new NbtList();
				TrinketInventory inv = slot.getValue();
				for (int i = 0; i < inv.size(); i++) {
					NbtCompound c = inv.getStack(i).writeNbt(new NbtCompound());
					list.add(c);
				}
				slotTag.put("Metadata", this.syncing ? inv.getSyncTag() : inv.toTag());
				slotTag.put("Items", list);
				groupTag.put(slot.getKey(), slotTag);
			}
			tag.put(group.getKey(), groupTag);
		}
	}

	@Override
	public void writeSyncPacket(PacketByteBuf buf, ServerPlayerEntity recipient) {
		this.syncing = true;
		NbtCompound tag = new NbtCompound();
		this.writeToNbt(tag);
		this.syncing = false;
		buf.writeNbt(tag);
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