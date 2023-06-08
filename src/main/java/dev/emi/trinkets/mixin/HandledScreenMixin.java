package dev.emi.trinkets.mixin;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.trinkets.TrinketScreenManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.mixin.accessor.CreativeSlotAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * Draws trinket slot backs, adjusts z location of draw calls, and makes non-trinket slots un-interactable while a trinket slot group is focused
 * 
 * @author Emi
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
	@Unique
	private static final Identifier MORE_SLOTS = new Identifier("trinkets", "textures/gui/more_slots.png");
	@Unique
	private static final Identifier BLANK_BACK = new Identifier("trinkets", "textures/gui/blank_back.png");

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

			RenderSystem.enableDepthTest();

			if (ts.isTrinketFocused()) {
				// Thus, I need to draw trinket slot backs over normal items at z 300 (310 was chosen)
				context.drawTexture(slotTextureId, slot.x, slot.y, 310, 0, 0, 16, 16, 16, 16);
				// I also need to draw items in trinket slots *above* 310 but *below* 400, (320 for items and 370 for tooltips was chosen)
				context.getMatrices().translate(0, 0, 70);
			} else {
				context.drawTexture(slotTextureId, slot.x, slot.y, 0, 0, 0, 16, 16, 16, 16);
				context.drawTexture(MORE_SLOTS, slot.x - 1, slot.y - 1, 0, 4, 4, 18, 18, 256, 256);
			}
		}
		if (TrinketsClient.activeGroup != null && TrinketsClient.activeGroup.getSlotId() == slot.id) {
			context.getMatrices().translate(0, 0, 70);
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
