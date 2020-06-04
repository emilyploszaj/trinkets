package dev.emi.trinkets.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

/**
 * Inventory that marks its parent PlayerTrinketComponent dirty and syncs with the server when needed
 */
public class TrinketInventory extends SimpleInventory {
	private PlayerTrinketComponent component;

	public TrinketInventory(PlayerTrinketComponent component, int size) {
		super(size);
		this.component = component;
	}

	public PlayerTrinketComponent getComponent(){
		return component;
	}

	@Override
	public void setStack(int i, ItemStack stack) {
		if (getStack(i).getItem() instanceof TrinketBase) {
			((TrinketBase) getStack(i).getItem()).onUnequip((PlayerEntity) component.getEntity(), getStack(i));
		}
		super.setStack(i, stack);
		if(getStack(i).getItem() instanceof TrinketBase) {
			((TrinketBase) getStack(i).getItem()).onEquip((PlayerEntity) component.getEntity(), getStack(i));
		}
	}

	@Override
	public ItemStack removeStack(int i) {
		if(getStack(i).getItem() instanceof TrinketBase){
			((TrinketBase) getStack(i).getItem()).onUnequip((PlayerEntity) component.getEntity(), getStack(i));
		}
		return super.removeStack(i);
	}

	@Override
	public ItemStack removeStack(int i, int count) {
		ItemStack stack = super.removeStack(i, count);
		if (!stack.isEmpty() && getStack(i).isEmpty() && stack.getItem() instanceof TrinketBase) {
			((TrinketBase) stack.getItem()).onUnequip((PlayerEntity) component.getEntity(), stack);
		}
		return stack;
	}

	@Override
	public void markDirty() {
		component.sync();
	}
}