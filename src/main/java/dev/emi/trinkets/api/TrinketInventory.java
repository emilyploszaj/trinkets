package dev.emi.trinkets.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.item.ItemStack;

/**
 * Inventory that marks its parent PlayerTrinketComponent dirty and syncs with the server when needed
 */
public class TrinketInventory extends BasicInventory {
	private PlayerTrinketComponent component;

	public TrinketInventory(PlayerTrinketComponent component, int size) {
		super(size);
		this.component = component;
	}

	public PlayerTrinketComponent getComponent(){
		return component;
	}

	@Override
	public void setInvStack(int i, ItemStack stack) {
		if (getInvStack(i).getItem() instanceof ITrinket) {
			((ITrinket) getInvStack(i).getItem()).onUnequip((PlayerEntity) component.getEntity(), getInvStack(i));
		}
		super.setInvStack(i, stack);
		if(getInvStack(i).getItem() instanceof ITrinket) {
			((ITrinket) getInvStack(i).getItem()).onEquip((PlayerEntity) component.getEntity(), getInvStack(i));
		}
	}

	@Override
	public ItemStack removeInvStack(int i) {
		if(getInvStack(i).getItem() instanceof ITrinket){
			((ITrinket) getInvStack(i).getItem()).onUnequip((PlayerEntity) component.getEntity(), getInvStack(i));
		}
		return super.removeInvStack(i);
	}

	@Override
	public ItemStack takeInvStack(int i, int count) {
		ItemStack stack = super.takeInvStack(i, count);
		if (!stack.isEmpty() && getInvStack(i).isEmpty() && stack.getItem() instanceof ITrinket) {
			((ITrinket) stack.getItem()).onUnequip((PlayerEntity) component.getEntity(), stack);
		}
		return stack;
	}

	@Override
	public void markDirty() {
		component.markDirty();
	}
}