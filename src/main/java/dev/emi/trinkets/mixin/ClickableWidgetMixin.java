package dev.emi.trinkets.mixin;

import dev.emi.trinkets.TrinketsClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes buttons uninteractable while trinket groups are being interacted with
 * 
 * @author Emi
 */
@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin {

	@Shadow
	protected boolean hovered;
	
	@Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/ClickableWidget;hovered:Z",
			opcode = Opcodes.PUTFIELD, ordinal = 0, shift = Shift.AFTER), method = "render")
	private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (TrinketsClient.activeGroup != null) {
			hovered = false;
		}
	}

	@Inject(at = @At("HEAD"), method = "mouseClicked", cancellable = true)
	private void mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			info.setReturnValue(false);
		}
	}
}
