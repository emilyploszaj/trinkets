package dev.emi.trinkets;

import dev.emi.trinkets.api.ITrinket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.container.Slot;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TrinketSlot extends Slot{
	public String major, minor;
	public boolean keepVisible;
	public TrinketSlot(Inventory inventory_1, int int_1, int int_2, int int_3, String major, String minor){
		super(inventory_1, int_1, int_2, int_3);
		this.major = major;
		this.minor = minor;
	}
	public boolean canInsert(ItemStack itemStack){
		if(major == "chest" && minor == "cape" && itemStack.getItem() == Items.ELYTRA) return true;
		if(itemStack.getItem() instanceof ITrinket){
			ITrinket trinket = (ITrinket) itemStack.getItem();
			return trinket.canWearInSlot(major, minor) && trinket.canInsert(itemStack);
		}
		return false;
	}
	@Override
	public boolean canTakeItems(PlayerEntity player){
		ItemStack stack = this.getStack();
		if(EnchantmentHelper.hasBindingCurse(stack)){
			return false;
		}
		if(stack.getItem() instanceof ITrinket){
			return ((ITrinket) stack.getItem()).canTake(stack);
		}
		return super.canTakeItems(player);
	}
	@Override
	public void setStack(ItemStack stack){
		super.setStack(stack);
	}
	@Override
	public ItemStack takeStack(int int_1){
		return super.takeStack(int_1);
	}
	@Override
	@Environment(EnvType.CLIENT)
	public String getBackgroundSprite(){
		return "trinkets:item/empty";
	}
}