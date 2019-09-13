package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.gui.screen.ingame.ContainerProvider;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.text.Text;

/**
 * Overwrites general AbstractContainerScreen slot picking functionality to not let any non-active group slots be interactable
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends Container> extends Screen implements ContainerProvider<T>{
	protected AbstractContainerScreenMixin(Text text_1) {
		super(text_1);
	}
	@Shadow
	protected boolean isPointWithinBounds(int int_1, int int_2, int int_3, int int_4, double double_1, double double_2){
		return false;
	}
	@Inject(at = @At("HEAD"), method = "isPointOverSlot", cancellable = true)
	private void isPointOverSlot(Slot slot_1, double double_1, double double_2, CallbackInfoReturnable<Boolean> info) {
		if(TrinketsClient.slotGroup == null){
			if(slot_1 != null && slot_1 instanceof TrinketSlot) info.setReturnValue(false);
			return;
		}
		if(TrinketsClient.activeSlots != null){
			for(Slot slot: TrinketsClient.activeSlots){
				if(slot == null) continue;
				if(slot == slot_1){
					this.isPointWithinBounds(slot_1.xPosition, slot_1.yPosition, 16, 16, double_1, double_2);
					return;
				}
			}
		}
		info.setReturnValue(false);
	 }

}