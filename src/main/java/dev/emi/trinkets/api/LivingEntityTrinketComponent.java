package dev.emi.trinkets.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.SlotAttributes.SlotEntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

public class LivingEntityTrinketComponent implements TrinketComponent, AutoSyncedComponent, RespawnableComponent {

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
		Map<String, SlotGroup> entitySlots = TrinketsApi.getEntitySlots(this.entity);
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
								this.processSlotModifiers(new SlotReference(oldInv, i), stack, ItemStack.EMPTY);
								if (this.entity instanceof PlayerEntity player) {
									player.getInventory().offerOrDrop(stack);
								} else if (this.entity.getWorld() instanceof ServerWorld serverWorld) {
									this.entity.dropStack(serverWorld, stack);
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
						inv.removeModifier(modifier.id());
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

	public void processSlotModifiers(SlotReference ref, ItemStack oldStack, ItemStack newStack) {
		Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> removeModifiers = TrinketModifiers.get(oldStack, ref, entity);
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> addModifiers = TrinketModifiers.get(newStack, ref, entity);
		Multimap<String, EntityAttributeModifier> removeSlotMap = HashMultimap.create(), addSlotMap = HashMultimap.create();

        // MC-272769 Mitigation.
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> existsElsewhere = HashMultimap.create();
        this.forEach(((slotReference, itemStack) -> {
            if (!slotReference.equals(ref) && !itemStack.isEmpty()) {
                existsElsewhere.putAll(TrinketModifiers.get(itemStack, slotReference, entity));
            }
        }));
        existsElsewhere.forEach(removeModifiers::remove);

		Set<RegistryEntry<EntityAttribute>> toRemove = Sets.newHashSet();
		for (RegistryEntry<EntityAttribute> attr : removeModifiers.keySet()) {
			if (attr.hasKeyAndValue() && attr.value() instanceof SlotEntityAttribute slotAttr) {
				removeSlotMap.putAll(slotAttr.slot, removeModifiers.get(attr));
				toRemove.add(attr);
			}
		}
        for (RegistryEntry<EntityAttribute> attr : addModifiers.keySet()) {
            if (attr.hasKeyAndValue() && attr.value() instanceof SlotEntityAttribute slotAttr) {
                addSlotMap.putAll(slotAttr.slot, addModifiers.get(attr));
                toRemove.add(attr);
            }
        }

		for (RegistryEntry<EntityAttribute> attr : toRemove) {
			removeModifiers.removeAll(attr);
            addModifiers.removeAll(attr);
		}
		//this.getEntity().getAttributes().removeModifiers(map);
		removeModifiers.asMap().forEach((attribute, modifiers) -> {
			EntityAttributeInstance entityAttributeInstance = this.getEntity().getAttributes().getCustomInstance(attribute);
			if (entityAttributeInstance != null) {
				modifiers.forEach(modifier -> entityAttributeInstance.removeModifier(modifier.id()) );
			}
		});
        //this.getEntity().getAttributes().addTemporaryModifiers(map);
        addModifiers.forEach((attribute, attributeModifier) -> {
            EntityAttributeInstance entityAttributeInstance = this.getEntity().getAttributes().getCustomInstance(attribute);
            if (entityAttributeInstance != null) {
                entityAttributeInstance.removeModifier(attributeModifier.id());
                entityAttributeInstance.addTemporaryModifier(attributeModifier);
            }

        });
        this.removeModifiers(removeSlotMap);
        this.addTemporaryModifiers(addSlotMap);
	}

	@Override
	public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
		DefaultedList<ItemStack> dropped = DefaultedList.of();
		for (String groupKey : tag.getKeys()) {
			NbtCompound groupTag = tag.getCompoundOrEmpty(groupKey);
			if (groupTag != null) {
				Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);
				if (groupSlots != null) {
					for (String slotKey : groupTag.getKeys()) {
						NbtCompound slotTag = groupTag.getCompoundOrEmpty(slotKey);
						NbtList list = slotTag.getListOrEmpty("Items");
						TrinketInventory inv = groupSlots.get(slotKey);

						if (inv != null) {
							inv.fromTag(slotTag.getCompoundOrEmpty("Metadata"));
						}

						for (int i = 0; i < list.size(); i++) {
							Optional<NbtCompound> c = list.getCompound(i);
							ItemStack stack = c.isPresent() && !c.get().isEmpty() ? ItemStack.fromNbt(lookup, c.get()).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
							if (inv != null && i < inv.size()) {
								inv.setStack(i, stack);
							} else {
								dropped.add(stack);
							}
						}
					}
				} else {
					for (String slotKey : groupTag.getKeys()) {
						NbtCompound slotTag = groupTag.getCompoundOrEmpty(slotKey);
						NbtList list = slotTag.getListOrEmpty("Items");
						for (int i = 0; i < list.size(); i++) {
							Optional<NbtCompound> c = list.getCompound(i);
							ItemStack stack = c.isPresent() && !c.get().isEmpty() ? ItemStack.fromNbt(lookup, c.get()).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
							dropped.add(stack);
						}
					}
				}
			}
		}
		if (this.entity.getWorld() instanceof ServerWorld serverWorld) {
			for (ItemStack itemStack : dropped) {
				this.entity.dropStack(serverWorld, itemStack);
			}
		}
		Multimap<String, EntityAttributeModifier> slotMap = HashMultimap.create();
		this.forEach((ref, stack) -> {
			if (!stack.isEmpty()) {
				Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = TrinketModifiers.get(stack, ref, entity);
				for (RegistryEntry<EntityAttribute> entityAttribute : map.keySet()) {
					if (entityAttribute.hasKeyAndValue() && entityAttribute.value() instanceof SlotAttributes.SlotEntityAttribute slotEntityAttribute) {
						slotMap.putAll(slotEntityAttribute.slot, map.get(entityAttribute));
					}
				}
			}
		});
		for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : this.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotEntry : groupEntry.getValue().entrySet()) {
				String group = groupEntry.getKey();
				String slot = slotEntry.getKey();
				String key = group + "/" + slot;
				Collection<EntityAttributeModifier> modifiers = slotMap.get(key);
				TrinketInventory inventory = slotEntry.getValue();
				for (EntityAttributeModifier modifier : modifiers) {
					inventory.removeCachedModifier(modifier);
				}
				inventory.clearCachedModifiers();
			}
		}
	}

	@Override
	public void applySyncPacket(RegistryByteBuf buf) {
		NbtCompound tag = buf.readNbt();

		if (tag != null) {

			for (String groupKey : tag.getKeys()) {
				NbtCompound groupTag = tag.getCompoundOrEmpty(groupKey);

				if (groupTag != null) {
					Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);

					if (groupSlots != null) {

						for (String slotKey : groupTag.getKeys()) {
							NbtCompound slotTag = groupTag.getCompoundOrEmpty(slotKey);
							NbtList list = slotTag.getListOrEmpty("Items");
							TrinketInventory inv = groupSlots.get(slotKey);

							if (inv != null) {
								inv.applySyncTag(slotTag.getCompoundOrEmpty("Metadata"));
							}

							for (int i = 0; i < list.size(); i++) {
								Optional<NbtCompound> c = list.getCompound(i);
								ItemStack stack = c.isPresent() && !c.get().isEmpty() ? ItemStack.fromNbt(buf.getRegistryManager(), c.get()).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
								if (inv != null && i < inv.size()) {
									inv.setStack(i, stack);
								}
							}
						}
					}
				}
			}

			if (this.entity instanceof PlayerEntity player) {
				((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(false);
			}
		}
	}

	@Override
	public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			NbtCompound groupTag = new NbtCompound();
			for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
				NbtCompound slotTag = new NbtCompound();
				NbtList list = new NbtList();
				TrinketInventory inv = slot.getValue();
				for (int i = 0; i < inv.size(); i++) {
					NbtCompound c = inv.getStack(i).isEmpty() ? new NbtCompound() : (NbtCompound) inv.getStack(i).toNbt(lookup);
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
	public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
		this.syncing = true;
		NbtCompound tag = new NbtCompound();
		this.writeToNbt(tag, buf.getRegistryManager());
		this.syncing = false;
		buf.writeNbt(tag);
	}

	@Override
	public boolean shouldCopyForRespawn(boolean lossless, boolean keepInventory, boolean sameCharacter) {
		return lossless || keepInventory;
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