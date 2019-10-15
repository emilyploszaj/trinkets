package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.gui.screen.ingame.ContainerProvider;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.text.Text;

/**
 * Overwrites general AbstractContainerScreen slot hover checking functionality to not let any non-active group slots be interactable
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends Container> extends Screen implements ContainerProvider<T> {
	
	protected AbstractContainerScreenMixin(Text text) {
		super(text);
	}

	@Shadow
	protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double a, double b);

	@Inject(at = @At("HEAD"), method = "isPointOverSlot", cancellable = true)
	private void isPointOverSlot(Slot slot, double a, double b, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.slotGroup == null) {
			if(slot != null && slot instanceof TrinketSlot) info.setReturnValue(false);
			return;
		}
		if (TrinketsClient.activeSlots != null) {
			for (Slot s: TrinketsClient.activeSlots) {
				if (s == null) continue;
				if (s == slot) {
					this.isPointWithinBounds(slot.xPosition, slot.yPosition, 16, 16, a, b);
					return;
				}
			}
		}
		info.setReturnValue(false);
	 }
}