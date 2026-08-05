package dev.emi.trinkets.mixin;


import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.emi.trinkets.CreativeTrinketScreen;
import dev.emi.trinkets.TrinketScreenManager;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.mixin.accessor.CreativeSlotAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * Draws trinket slot backs, adjusts z location of draw calls, and makes non-trinket slots un-interactable while a trinket slot group is focused
 * 
 * @author Emi
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
	@Shadow @Nullable protected Slot focusedSlot;
	@Shadow @Final private static Identifier SLOT_HIGHLIGHT_BACK_TEXTURE;
	@Unique
	private static final Identifier MORE_SLOTS = Identifier.of("trinkets", "textures/gui/more_slots.png");
	@Unique
	private static final Identifier BLANK_BACK = Identifier.of("trinkets", "textures/gui/blank_back.png");

	private HandledScreenMixin() {
		super(null);
	}

	@Inject(at = @At("HEAD"), method = "removed")
	private void removed(CallbackInfo info) {
		if ((Object)this instanceof InventoryScreen) {
			TrinketScreenManager.removeSelections();
		}
	}

	@WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V"),
			method = "drawSlots")
	private boolean preventDrawingSlots(HandledScreen instance, DrawContext context, Slot slot, int mouseX, int mouseY) {
		return !(slot instanceof TrinketSlot trinketSlot) || !trinketSlot.renderAfterRegularSlots();
	}

	@Inject(at = @At("HEAD"), method = "drawSlot")
	private void drawSlotBackground(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (slot instanceof TrinketSlot ts) {
			assert this.client != null;
			Identifier slotTextureId = ts.getBackgroundIdentifier();

			if (!slot.getStack().isEmpty() || slotTextureId == null) {
				slotTextureId = BLANK_BACK;
			}

			if (ts.isTrinketFocused()) {
				context.drawTexture(RenderPipelines.GUI_TEXTURED, slotTextureId, slot.x, slot.y, 0, 0, 16, 16, 16, 16);
				if (this.focusedSlot == slot && this.focusedSlot.canBeHighlighted()) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_TEXTURE, this.focusedSlot.x - 4, this.focusedSlot.y - 4, 24, 24);
				}
			} else {
				context.drawTexture(RenderPipelines.GUI_TEXTURED, slotTextureId, slot.x, slot.y, 0, 0, 16, 16, 16, 16);
				context.drawTexture(RenderPipelines.GUI_TEXTURED, MORE_SLOTS, slot.x - 1, slot.y - 1, 4, 4, 18, 18, 256, 256);
			}
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;renderMain(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER), method = "render")
	private void renderCreativeSlots(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		if (this instanceof CreativeTrinketScreen screen) {
			screen.trinkets$renderCreative(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Inject(at = @At("HEAD"), method = "isPointOverSlot", cancellable = true)
	private void isPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			if (slot instanceof TrinketSlot ts) {
				if (!ts.isTrinketFocused()) {
					info.setReturnValue(false);
				}
			} else {
				if (slot instanceof CreativeSlot cs) {
					if (((CreativeSlotAccessor) cs).getSlot().id != TrinketsClient.activeGroup.getSlotId()) {
						info.setReturnValue(false);
					}
				} else if (slot.id != TrinketsClient.activeGroup.getSlotId()) {
					info.setReturnValue(false);
				}
			}
		}
	}
}
