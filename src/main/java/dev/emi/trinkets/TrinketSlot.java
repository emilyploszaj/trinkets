package dev.emi.trinkets;

import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketSlots;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * A slot for a TrinketInventory, only properly utilized in the survival inventory due to restrictions in the creative inventory
 */
public class TrinketSlot extends Slot {
	public String group, slot;
	public boolean keepVisible;

	public TrinketSlot(Inventory inventory, int id, int x, int y, String group, String slot) {
		super(inventory, id, x, y);
		this.group = group;
		this.slot = slot;
	}

	public boolean canInsert(ItemStack stack) {
		TrinketSlots.Slot s = TrinketSlots.getSlotFromName(group, slot);
		return s.canEquip.apply(s, stack);
	}

	@Override
	public boolean canTakeItems(PlayerEntity player) {
		ItemStack stack = this.getStack();
		if (EnchantmentHelper.hasBindingCurse(stack)) {
			return false;
		}
		if (stack.getItem() instanceof TrinketItem) {
			return ((TrinketItem) stack.getItem()).canTake(stack);
		}
		return super.canTakeItems(player);
	}

	@Override
	public void setStack(ItemStack stack) {
		super.setStack(stack);
	}

	@Override
	public ItemStack takeStack(int i) {
		return super.takeStack(i);
	}
}