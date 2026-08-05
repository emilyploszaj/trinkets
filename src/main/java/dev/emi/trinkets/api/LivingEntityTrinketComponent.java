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

import com.mojang.serialization.DynamicOps;
import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
								if (this.entity instanceof PlayerEntity player) {
									player.getInventory().offerOrDrop(stack);
								} else if (this.entity.getEntityWorld() instanceof ServerWorld serverWorld) {
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

	@Override
	public void readData(ReadView view) {
		Optional<TrinketSaveData> optional = view.read(TrinketSaveData.MAP_CODEC);
		DefaultedList<ItemStack> dropped = DefaultedList.of();
		if (optional.isPresent()) {
			TrinketSaveData data = optional.orElseThrow();
			for (String groupKey : data.data().keySet()) {
				Map<String, TrinketSaveData.InventoryData> groupTag = data.data().get(groupKey);
				if (groupTag != null) {
					Map<String, TrinketInventory> groupSlots = this.inventory.get(groupKey);
					if (groupSlots != null) {
						for (String slotKey : groupTag.keySet()) {
							TrinketSaveData.InventoryData slotTag = groupTag.get(slotKey);
							TrinketInventory inv = groupSlots.get(slotKey);

							if (inv != null) {
								inv.fromMetadata(slotTag.metadata());
							}

							for (int i = 0; i < slotTag.items().size(); i++) {
								ItemStack stack = slotTag.items().get(i);
								if (inv != null && i < inv.size()) {
									inv.setStack(i, stack);
								} else {
									dropped.add(stack);
								}
							}
						}
					} else {
						for (String slotKey : groupTag.keySet()) {
							dropped.addAll(groupTag.get(slotKey).items());
						}
					}
				}
			}
		}
		if (this.entity.getEntityWorld() instanceof ServerWorld serverWorld) {
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
			DynamicOps<NbtElement> ops = buf.getRegistryManager().getOps(NbtOps.INSTANCE);
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
								inv.applySyncMetadata(slotTag.get("Metadata", TrinketSaveData.Metadata.CODEC, ops).orElse(TrinketSaveData.Metadata.EMPTY));
							}

							for (int i = 0; i < list.size(); i++) {
								NbtCompound c = list.getCompoundOrEmpty(i);
								ItemStack stack = ItemStack.OPTIONAL_CODEC.decode(ops, c).result().map(com.mojang.datafixers.util.Pair::getFirst).orElse(ItemStack.EMPTY);
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
	public void writeData(WriteView view) {
		TrinketSaveData data = new TrinketSaveData(new HashMap<>());
		for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
			Map<String, TrinketSaveData.InventoryData> groupTag = new HashMap<>();
			for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
				TrinketInventory inv = slot.getValue();

				List<ItemStack> items = new ArrayList<>();
				for (int i = 0; i < inv.size(); i++) {
					items.add(inv.getStack(i).copy());
				}
				TrinketSaveData.Metadata metadata = this.syncing ? inv.getSyncMetadata() : inv.toMetadata();
				groupTag.put(slot.getKey(), new TrinketSaveData.InventoryData(metadata, items));
			}
			data.data().put(group.getKey(), groupTag);
		}
		view.put(TrinketSaveData.MAP_CODEC, data);
	}

	@Override
	public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
		this.syncing = true;
		NbtWriteView tag = NbtWriteView.create(ErrorReporter.EMPTY);
		this.writeData(tag);
		this.syncing = false;
		buf.writeNbt(tag.getNbt());
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