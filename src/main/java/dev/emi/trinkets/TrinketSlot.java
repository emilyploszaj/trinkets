package dev.emi.trinkets;

import com.mojang.datafixers.util.Function3;
import dev.emi.trinkets.api.*;
import dev.emi.trinkets.api.Trinket.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * A gui slot for a trinket slot
 */
public class TrinketSlot extends Slot {
	private final SlotGroup group;
	private final SlotType type;
	private final boolean alwaysVisible;
	private final boolean baseType;
	private final int slotOffset;

	public TrinketSlot(Inventory inventory, int index, int x, int y, SlotGroup group, SlotType type, int slotOffset, boolean alwaysVisible, boolean baseType) {
		super(inventory, index, x, y);
		this.group = group;
		this.type = type;
		this.slotOffset = slotOffset;
		this.alwaysVisible = alwaysVisible;
		this.baseType = baseType;
	}

	public boolean isTrinketFocused() {
		if (TrinketsClient.activeGroup == group) {
			return baseType || TrinketsClient.activeType == type;
		}
		if (TrinketsClient.quickMoveGroup == group) {
			return baseType || TrinketsClient.quickMoveType == type;
		}
		return false;
	}

	public Identifier getBackgroundIdentifier() {
		return type.getIcon();
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		LivingEntity entity = ((TrinketInventory) inventory).component.entity;
		return canInsert(stack, new SlotReference(type, slotOffset), entity);
	}

	public static boolean canInsert(ItemStack stack, SlotReference slotRef, LivingEntity entity) {
		TriState state = TriState.DEFAULT;

		for (Identifier id : slotRef.slot.getValidators()) {
			Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> function = TrinketsApi.getValidatorPredicate(id);

			if (function.isPresent()) {
				state = function.get().apply(stack, slotRef, entity);
			}

			if (state != TriState.DEFAULT) {
				break;
			}
		}

		if (state == TriState.DEFAULT) {
			Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> validatorPredicate =
					TrinketsApi.getValidatorPredicate(new Identifier("trinkets", "tag"));

			if (validatorPredicate.isPresent()) {
				state = validatorPredicate.get().apply(stack, slotRef, entity);
			}
		}

		if (state.get()) {
			Optional<Trinket> trinket = TrinketsApi.getTrinket(stack.getItem());
			return trinket.map(value -> value.canEquip(stack, slotRef, entity)).orElse(true);
		}

		return false;
	}

	@Override
	public boolean canTakeItems(PlayerEntity player) {
		ItemStack stack = this.getStack();
		Optional<Trinket> trinket = TrinketsApi.getTrinket(stack.getItem());
		return trinket.map(value -> value.canUnequip(stack, new SlotReference(type, slotOffset), player)).orElse(true);
	}

	@Override
	public boolean doDrawHoveringEffect() {
		if (alwaysVisible) {
			return true;
		}
		return isTrinketFocused();
	}
}
