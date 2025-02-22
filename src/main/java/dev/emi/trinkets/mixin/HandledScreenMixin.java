package dev.emi.trinkets.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.trinkets.TrinketScreenManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderLayer;
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

import java.util.function.Function;

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

	@Inject(at = @At(value = "INVOKE", target = "net/minecraft/client/util/math/MatrixStack.translate(FFF)V"),
		method = "drawSlot")
	private void changeZ(DrawContext context, Slot slot, CallbackInfo info) {
		// Items are drawn at z + 150 (normal items are drawn at 250)
		// Item tooltips (count, item bar) are drawn at z + 200 (normal itmes are drawn at 300)
		// Inventory tooltip is drawn at 400
		if (slot instanceof TrinketSlot ts) {
			assert this.client != null;
			Identifier slotTextureId = ts.getBackgroundIdentifier();

			if (!slot.getStack().isEmpty() || slotTextureId == null) {
				slotTextureId = BLANK_BACK;
			}

			if (ts.isTrinketFocused()) {
				// Thus, I need to draw trinket slot backs over normal items at z 300 (310 was chosen)
				context.getMatrices().translate(0, 0, 310);
				context.drawTexture(RenderLayer::getGuiTextured, slotTextureId, slot.x, slot.y, 0, 0, 16, 16, 16, 16);
				if (this.focusedSlot == slot && this.focusedSlot.canBeHighlighted()) {
					context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT_HIGHLIGHT_BACK_TEXTURE, this.focusedSlot.x - 4, this.focusedSlot.y - 4, 24, 24);
				}
				context.getMatrices().translate(0, 0, -310);
				// I also need to draw items in trinket slots *above* 310 but *below* 400, (320 for items and 370 for tooltips was chosen)
				context.getMatrices().translate(0, 0, 70);
			} else {
				context.drawTexture(RenderLayer::getGuiTextured, slotTextureId, slot.x, slot.y, 0, 0, 16, 16, 16, 16);
				context.drawTexture(RenderLayer::getGuiTextured, MORE_SLOTS, slot.x - 1, slot.y - 1, 4, 4, 18, 18, 256, 256);
			}
		}
	}

	@WrapOperation(method = "drawSlotHighlightFront", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"))
	private void changeZForHighlightFront(DrawContext context, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
		assert this.focusedSlot != null;
		if (this.focusedSlot instanceof TrinketSlot) {
			context.getMatrices().push();
			context.getMatrices().translate(0, 0, 100 + 310 + 70 + 70 + 1);
			original.call(context, renderLayers, sprite, x, y, width, height);
			context.getMatrices().pop();
		} else if (this.focusedSlot instanceof TrinketSlot) {
			context.getMatrices().push();
			context.getMatrices().translate(0, 0, 100 + 310 + 70 + 70 + 1);
			original.call(context, renderLayers, sprite, x, y, width, height);
			context.getMatrices().pop();
		} else {
			original.call(context, renderLayers, sprite, x, y, width, height);
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
