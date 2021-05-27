package dev.emi.trinkets.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;
import java.util.function.Consumer;

public class TrinketInventory implements Inventory {

	private final SlotType slotType;
	private final int baseSize;
	private final TrinketComponent component;
	private final Map<UUID, EntityAttributeModifier> modifiers = new HashMap<>();
	private final Set<EntityAttributeModifier> persistentModifiers = new HashSet<>();
	private final Set<EntityAttributeModifier> cachedModifiers = new HashSet<>();
	private final Multimap<EntityAttributeModifier.Operation, EntityAttributeModifier> modifiersByOperation = HashMultimap.create();
	private final Consumer<TrinketInventory> updateCallback;

	private DefaultedList<ItemStack> stacks;
	private boolean update = false;

	public TrinketInventory(SlotType slotType, TrinketComponent comp, Consumer<TrinketInventory> updateCallback) {
		this.component = comp;
		this.slotType = slotType;
		this.baseSize = slotType.getAmount();
		this.stacks = DefaultedList.ofSize(this.baseSize, ItemStack.EMPTY);
		this.updateCallback = updateCallback;
	}

	public SlotType getSlotType() {
		return this.slotType;
	}

	public TrinketComponent getComponent() {
		return this.component;
	}

	@Override
	public void clear() {
		for (int i = 0; i < this.size(); i++) {
			stacks.set(i, ItemStack.EMPTY);
		}
	}

	@Override
	public int size() {
		this.update();
		return this.stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < this.size(); i++) {
			if (!stacks.get(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		this.update();
		return stacks.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return Inventories.splitStack(stacks, slot, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(stacks, slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		this.update();
		stacks.set(slot, stack);
	}

	@Override
	public void markDirty() {
		// NO-OP
	}

	public void markUpdate() {
		this.update = true;
		this.updateCallback.accept(this);
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	public Map<UUID, EntityAttributeModifier> getModifiers() {
		return this.modifiers;
	}

	public Collection<EntityAttributeModifier> getModifiersByOperation(EntityAttributeModifier.Operation operation) {
		return this.modifiersByOperation.get(operation);
	}

	public void addModifier(EntityAttributeModifier modifier) {
		this.modifiers.put(modifier.getId(), modifier);
		this.getModifiersByOperation(modifier.getOperation()).add(modifier);
		this.markUpdate();
	}

	public void addPersistentModifier(EntityAttributeModifier modifier) {
		this.addModifier(modifier);
		this.persistentModifiers.add(modifier);
	}

	public void removeModifier(UUID uuid) {
		EntityAttributeModifier modifier = this.modifiers.remove(uuid);
		if (modifier != null) {
			this.persistentModifiers.remove(modifier);
			this.getModifiersByOperation(modifier.getOperation()).remove(modifier);
			this.markUpdate();
		}
	}

	public void clearModifiers() {
		Iterator<UUID> iter = this.getModifiers().keySet().iterator();

		while(iter.hasNext()) {
			this.removeModifier(iter.next());
		}
	}

	public void clearCachedModifiers() {
		for (EntityAttributeModifier cachedModifier : this.cachedModifiers) {
			this.removeModifier(cachedModifier.getId());
		}
		this.cachedModifiers.clear();
	}

	public void update() {
		if (this.update) {
			this.update = false;
			double baseSize = this.baseSize;
			for (EntityAttributeModifier mod : this.getModifiersByOperation(EntityAttributeModifier.Operation.ADDITION)) {
				baseSize += mod.getValue();
			}

			double size = baseSize;
			for (EntityAttributeModifier mod : this.getModifiersByOperation(EntityAttributeModifier.Operation.MULTIPLY_BASE)) {
				size += this.baseSize * mod.getValue();
			}

			for (EntityAttributeModifier mod : this.getModifiersByOperation(EntityAttributeModifier.Operation.MULTIPLY_TOTAL)) {
				size *= mod.getValue();
			}
			LivingEntity entity = this.component.getEntity();

			if (size != this.size()) {
				DefaultedList<ItemStack> newStacks = DefaultedList.ofSize((int) size, ItemStack.EMPTY);
				for (int i = 0; i < this.stacks.size(); i++) {
					ItemStack stack = this.stacks.get(i);
					if (i < newStacks.size()) {
						newStacks.set(i, stack);
					} else {
						entity.dropStack(stack);
					}
				}

				this.stacks = newStacks;
			}
		}
	}

	public void copyFrom(TrinketInventory other) {
		this.modifiers.clear();
		this.modifiersByOperation.clear();
		this.persistentModifiers.clear();
		other.modifiers.forEach((uuid, modifier) -> this.addModifier(modifier));
		for (EntityAttributeModifier persistentModifier : other.persistentModifiers) {
			this.addPersistentModifier(persistentModifier);
		}
	}

	public CompoundTag toTag() {
		CompoundTag compoundTag = new CompoundTag();
		if (!this.persistentModifiers.isEmpty()) {
			ListTag listTag = new ListTag();
			for (EntityAttributeModifier entityAttributeModifier : this.persistentModifiers) {
				listTag.add(entityAttributeModifier.toTag());
			}

			compoundTag.put("PersistentModifiers", listTag);
		}
		if (!this.modifiers.isEmpty()) {
			ListTag listTag = new ListTag();
			this.modifiers.forEach((uuid, modifier) -> {
				if (!this.persistentModifiers.contains(modifier)) {
					listTag.add(modifier.toTag());
				}
			});

			compoundTag.put("CachedModifiers", listTag);
		}
		return compoundTag;
	}

	public void fromTag(CompoundTag tag) {
		if (tag.contains("PersistentModifiers", 9)) {
			ListTag listTag = tag.getList("PersistentModifiers", 10);

			for(int i = 0; i < listTag.size(); ++i) {
				EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromTag(listTag.getCompound(i));
				if (entityAttributeModifier != null) {
					this.addPersistentModifier(entityAttributeModifier);
				}
			}
		}

		if (tag.contains("CachedModifiers", 9)) {
			ListTag listTag = tag.getList("CachedModifiers", 10);

			for (int i = 0; i < listTag.size(); ++i) {
				EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromTag(listTag.getCompound(i));
				if (entityAttributeModifier != null) {
					this.cachedModifiers.add(entityAttributeModifier);
					this.addModifier(entityAttributeModifier);
				}
			}

			this.update();
		}
	}

	public CompoundTag getSyncTag() {
		CompoundTag compoundTag = new CompoundTag();
		if (!this.modifiers.isEmpty()) {
			ListTag listTag = new ListTag();
			for (Map.Entry<UUID, EntityAttributeModifier> modifier : this.modifiers.entrySet()) {
				listTag.add(modifier.getValue().toTag());
			}

			compoundTag.put("Modifiers", listTag);
		}
		return compoundTag;
	}

	public void applySyncTag(CompoundTag tag) {
		this.modifiers.clear();
		this.persistentModifiers.clear();
		this.modifiersByOperation.clear();
		if (tag.contains("Modifiers", 9)) {
			ListTag listTag = tag.getList("Modifiers", 10);

			for (int i = 0; i < listTag.size(); ++i) {
				EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromTag(listTag.getCompound(i));
				if (entityAttributeModifier != null) {
					this.addModifier(entityAttributeModifier);
				}
			}
		}
		this.markUpdate();
		this.update();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TrinketInventory that = (TrinketInventory) o;
		return slotType.equals(that.slotType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(slotType);
	}
}