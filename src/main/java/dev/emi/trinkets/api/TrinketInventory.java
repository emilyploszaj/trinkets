package dev.emi.trinkets.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class TrinketInventory implements Inventory {
	private static final Codec<Collection<EntityAttributeModifier>> ENTITY_ATTRIBUTE_MODIFIERS_CODEC = EntityAttributeModifier.CODEC.listOf().xmap(Function.identity(), List::copyOf);

	private final SlotType slotType;
	private final int baseSize;
	private final TrinketComponent component;
	private final Map<Identifier, EntityAttributeModifier> modifiers = new HashMap<>();
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

	public Map<Identifier, EntityAttributeModifier> getModifiers() {
		return this.modifiers;
	}

	public Collection<EntityAttributeModifier> getModifiersByOperation(EntityAttributeModifier.Operation operation) {
		return this.modifiersByOperation.get(operation);
	}

	public void addModifier(EntityAttributeModifier modifier) {
		this.modifiers.put(modifier.id(), modifier);
		this.getModifiersByOperation(modifier.operation()).add(modifier);
		this.markUpdate();
	}

	public void addPersistentModifier(EntityAttributeModifier modifier) {
		this.addModifier(modifier);
		this.persistentModifiers.add(modifier);
	}

	public void removeModifier(Identifier identifier) {
		EntityAttributeModifier modifier = this.modifiers.remove(identifier);
		if (modifier != null) {
			this.persistentModifiers.remove(modifier);
			this.getModifiersByOperation(modifier.operation()).remove(modifier);
			this.markUpdate();
		}
	}

	public void clearModifiers() {
		java.util.Iterator<Identifier> iter = this.getModifiers().keySet().iterator();

		while(iter.hasNext()) {
			this.removeModifier(iter.next());
		}
	}

	public void removeCachedModifier(EntityAttributeModifier attributeModifier) {
		this.cachedModifiers.remove(attributeModifier);
	}

	public void clearCachedModifiers() {
		for (EntityAttributeModifier cachedModifier : this.cachedModifiers) {
			this.removeModifier(cachedModifier.id());
		}
		this.cachedModifiers.clear();
	}

	public void update() {
		if (this.update) {
			this.update = false;
			double baseSize = this.baseSize;
			for (EntityAttributeModifier mod : this.getModifiersByOperation(EntityAttributeModifier.Operation.ADD_VALUE)) {
				baseSize += mod.value();
			}

			double size = baseSize;
			for (EntityAttributeModifier mod : this.getModifiersByOperation(EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
				size += this.baseSize * mod.value();
			}

			for (EntityAttributeModifier mod : this.getModifiersByOperation(EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
				size *= mod.value();
			}
			LivingEntity entity = this.component.getEntity();

			if (size != this.size()) {
				DefaultedList<ItemStack> newStacks = DefaultedList.ofSize((int) size, ItemStack.EMPTY);
				for (int i = 0; i < this.stacks.size(); i++) {
					ItemStack stack = this.stacks.get(i);
					if (i < newStacks.size()) {
						newStacks.set(i, stack);
					} else {
						if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
							entity.dropStack(serverWorld, stack);
						}
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
		this.update();
	}

	public static void copyFrom(LivingEntity previous, LivingEntity current) {
		TrinketsApi.getTrinketComponent(previous).ifPresent(prevTrinkets -> {
			TrinketsApi.getTrinketComponent(current).ifPresent(currentTrinkets -> {
				Map<String, Map<String, TrinketInventory>> prevMap = prevTrinkets.getInventory();
				Map<String, Map<String, TrinketInventory>> currentMap = currentTrinkets.getInventory();
				for (Map.Entry<String, Map<String, TrinketInventory>> entry : prevMap.entrySet()) {
					Map<String, TrinketInventory> currentInvs = currentMap.get(entry.getKey());
					if (currentInvs != null) {
						for (Map.Entry<String, TrinketInventory> invEntry : entry.getValue().entrySet()) {
							TrinketInventory currentInv = currentInvs.get(invEntry.getKey());
							if (currentInv != null) {
								currentInv.copyFrom(invEntry.getValue());
							}
						}
					}
				}
			});
		});
	}

	public TrinketSaveData.Metadata toMetadata() {
		List<EntityAttributeModifier> cachedModifiers = new ArrayList<>();

		if (!this.modifiers.isEmpty()) {
			this.modifiers.forEach((uuid, modifier) -> {
				if (!this.persistentModifiers.contains(modifier)) {
					cachedModifiers.add(modifier);
				}
			});
		}
		return new TrinketSaveData.Metadata(List.copyOf(this.persistentModifiers), cachedModifiers);
	}

	public void fromMetadata(TrinketSaveData.Metadata tag) {
		tag.persistentModifiers().forEach(this::addPersistentModifier);

		if (!tag.cachedModifiers().isEmpty()) {
			for (EntityAttributeModifier modifier : tag.cachedModifiers()) {
				this.cachedModifiers.add(modifier);
				this.addModifier(modifier);
			}

			this.update();
		}
	}

	public TrinketSaveData.Metadata getSyncMetadata() {
		return new TrinketSaveData.Metadata(List.copyOf(this.modifiers.values()), List.of());
	}

	public void applySyncMetadata(TrinketSaveData.Metadata metadata) {
		this.modifiers.clear();
		this.persistentModifiers.clear();
		this.modifiersByOperation.clear();

		metadata.persistentModifiers().forEach(this::addModifier);
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