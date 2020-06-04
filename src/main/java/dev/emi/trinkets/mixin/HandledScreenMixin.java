package dev.emi.trinkets.mixin;

import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketSlots;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Overwrites general ContainerScreen slot hover checking functionality to not let any non-active group slots be interactable 
 * as well as render trinket slots properly
 */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
	@Shadow
	public T handler;
	@Shadow
	protected Slot focusedSlot;
	
	private static final Identifier BLANK_BACK = new Identifier("trinkets", "textures/gui/blank_back.png");
	
	protected HandledScreenMixin(Text text) {
		super(text);
	}

	@Shadow
	protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double a, double b);
	
	@Shadow
	protected abstract void drawSlot(MatrixStack matrices, Slot slot);

	@Shadow
	protected abstract boolean isPointOverSlot(Slot slot, double a, double b);

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
					this.isPointWithinBounds(slot.x, slot.y, 16, 16, a, b);
					return;
				}
			}
		}
		info.setReturnValue(false);
	}
	
	@Inject(at = @At("HEAD"), method = "drawForeground")
	private void drawForeground(MatrixStack matrices, int x, int y, CallbackInfo info) {
		GlStateManager.disableDepthTest();
		List<TrinketSlots.Slot> trinketSlots = TrinketSlots.getAllSlots();
		for (int i = 0; i < handler.slots.size(); i++) {
			if (!(handler.slots.get(i).inventory instanceof TrinketInventory)) continue;
			Slot ts = handler.getSlot(i);
			TrinketSlots.Slot s = trinketSlots.get(i - 46);
			if (!(s.getSlotGroup() == TrinketsClient.slotGroup || (s.getSlotGroup() == TrinketsClient.lastEquipped && TrinketsClient.displayEquipped > 0))) renderSlot(matrices, ts, s, x, y);
		}
		//Redraw only the active group slots so they're always on top
		for (int i = 0; i < handler.slots.size(); i++) {
			if (!(handler.slots.get(i).inventory instanceof TrinketInventory)) continue;
			Slot ts = handler.getSlot(i);
			TrinketSlots.Slot s = trinketSlots.get(i - 46);
			if (s.getSlotGroup() == TrinketsClient.slotGroup || (s.getSlotGroup() == TrinketsClient.lastEquipped && TrinketsClient.displayEquipped > 0)) renderSlot(matrices, ts, s, x, y);
		}
		GlStateManager.enableDepthTest();
	}

	public void renderSlot(MatrixStack matrices, Slot ts, TrinketSlots.Slot s, int x, int y){
		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();
		if (ts.getStack().isEmpty()) {
			this.client.getTextureManager().bindTexture(s.texture);
		} else {
			this.client.getTextureManager().bindTexture(BLANK_BACK);
		}
		DrawableHelper.drawTexture(matrices, ts.x, ts.y, 0, 0, 0, 16, 16, 16, 16);
		drawSlot(matrices, ts);
		if (this.isPointOverSlot(ts, x, y) && ts.doDrawHoveringEffect()) {
			this.focusedSlot = ts;
			GlStateManager.disableDepthTest();
			GlStateManager.disableLighting();
			GlStateManager.colorMask(true, true, true, false);
			this.fillGradient(matrices, ts.x, ts.y, ts.x + 16, ts.y + 16, -2130706433, -2130706433);
			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.enableLighting();
		}
		GlStateManager.popMatrix();
	}
}