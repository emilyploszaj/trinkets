package dev.emi.trinkets.mixin;

import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.PlayerTrinketComponent;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketSlots.Slot;
import dev.emi.trinkets.api.TrinketSlots.SlotGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Adds trinket slots to the PlayerContainer on initialization
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends AbstractRecipeScreenHandler<CraftingInventory> {

	public PlayerScreenHandlerMixin(ScreenHandlerType<?> type, int i) {
		super(type, i);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	protected void init(PlayerInventory inventory, boolean b, PlayerEntity player, CallbackInfo info) {
		Inventory inv = TrinketsApi.getTrinketsInventory(player);
		int i = 0;
		for (SlotGroup group: TrinketSlots.slotGroups) {
			int j = 0;
			for (Slot slot: group.slots) {
				TrinketSlot ts;
				ts = new TrinketSlot(inv, i, Integer.MIN_VALUE, 8, group.getName(), slot.getName());
				if (j == 0 && !group.onReal) {
					ts.keepVisible = true;
				}
				addSlot(ts);
				i++;
				j++;
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "transferSlot", cancellable = true)
	public void transferSlot(PlayerEntity player, int i, CallbackInfoReturnable<ItemStack> info) {
		net.minecraft.screen.slot.Slot slot = (net.minecraft.screen.slot.Slot) this.slots.get(i);
		if (i > 45) {
			if(slot != null && slot.hasStack()){
				ItemStack stack = slot.getStack();
				ItemStack copy = stack.copy();
				if(!this.insertItem(stack, 9, 45, false)){
					info.setReturnValue(ItemStack.EMPTY);
				}else{
					if(copy.getItem() instanceof Trinket){
						TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
						((Trinket) copy.getItem()).onUnequip((PlayerEntity) ((PlayerTrinketComponent) comp).getEntity(), copy);
					}
					info.setReturnValue(stack);
				}
			}
		} else if(slot != null && slot.hasStack()) {
			ItemStack stack = slot.getStack();
			TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
			if (comp.equip(stack, true)) {
				stack.setCount(0);
				info.setReturnValue(stack);
			}
		}
	}
}