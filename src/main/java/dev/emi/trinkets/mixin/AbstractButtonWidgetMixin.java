package dev.emi.trinkets.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketsClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Makes buttons uninteractable while trinket groups are being interacted with
 */
@Mixin(AbstractButtonWidget.class)
public class AbstractButtonWidgetMixin {

	@Shadow
	protected boolean hovered;
	
	@Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/AbstractButtonWidget;hovered:Z",
			opcode = Opcodes.PUTFIELD, ordinal = 0, shift = Shift.AFTER), method = "render")
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (TrinketsClient.activeGroup != null) {
			hovered = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "mouseClicked", cancellable = true)
	public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			info.setReturnValue(false);
		}
	}
}
