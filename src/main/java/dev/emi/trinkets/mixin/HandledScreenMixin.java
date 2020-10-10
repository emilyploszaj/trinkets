package dev.emi.trinkets.mixin;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
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
	@Shadow
	protected int x;
	@Shadow
	protected int y;
	
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
	
	@Inject(at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/util/math/MatrixStack;FII)V"), method = "render")
	private void drawBackground(MatrixStack matrices, int x, int y, float delta, CallbackInfo info) {
		RenderSystem.pushMatrix();
		RenderSystem.disableDepthTest();
		List<TrinketSlots.Slot> trinketSlots = TrinketSlots.getAllSlots();
		this.setZOffset(100);
		this.itemRenderer.zOffset = 100.0F;
		int trinketOffset = -1;
		for (int i = 46; i < handler.slots.size(); i++) {
			if (!(handler.slots.get(i).inventory instanceof TrinketInventory)) continue;
			if (trinketOffset == -1) { trinketOffset = i; }
			Slot ts = handler.getSlot(i);
			TrinketSlots.Slot s = trinketSlots.get(i - trinketOffset);
			if (!s.getSlotGroup().onReal && s.getSlotGroup().slots.get(0) == s) {
				renderSlotBack(matrices, ts, s, this.x, this.y);
			}
		}
		this.setZOffset(0);
		this.itemRenderer.zOffset = 0.0F;
		RenderSystem.enableDepthTest();
		RenderSystem.popMatrix();
	}
	
	@Inject(at = @At("HEAD"), method = "drawForeground")
	private void drawForeground(MatrixStack matrices, int x, int y, CallbackInfo info) {
		RenderSystem.disableDepthTest();
		List<TrinketSlots.Slot> trinketSlots = TrinketSlots.getAllSlots();
		int trinketOffset = -1;
		for (int i = 46; i < handler.slots.size(); i++) {
			if (!(handler.slots.get(i).inventory instanceof TrinketInventory)) continue;
			if (trinketOffset == -1) { trinketOffset = i; }
			Slot ts = handler.getSlot(i);
			TrinketSlots.Slot s = trinketSlots.get(i - trinketOffset);
			if (!(s.getSlotGroup() == TrinketsClient.slotGroup || !(s.getSlotGroup() == TrinketsClient.lastEquipped && TrinketsClient.displayEquipped > 0))) renderSlot(matrices, ts, s, x, y);
		}
		//Redraw only the active group slots so they're always on top
		trinketOffset = -1;
		for (int i = 0; i < handler.slots.size(); i++) {
			if (!(handler.slots.get(i).inventory instanceof TrinketInventory)) continue;
			if (trinketOffset == -1) { trinketOffset = i; }
			Slot ts = handler.getSlot(i);
			TrinketSlots.Slot s = trinketSlots.get(i - trinketOffset);
			if (s.getSlotGroup() == TrinketsClient.slotGroup || (s.getSlotGroup() == TrinketsClient.lastEquipped && TrinketsClient.displayEquipped > 0)) renderSlot(matrices, ts, s, x, y);
		}
		RenderSystem.enableDepthTest();
	}

	public void renderSlotBack(MatrixStack matrices, Slot ts, TrinketSlots.Slot s, int x, int y) {
		RenderSystem.disableLighting();
		if (ts.getStack().isEmpty()) {
			this.client.getTextureManager().bindTexture(s.texture);
		} else {
			this.client.getTextureManager().bindTexture(BLANK_BACK);
		}
		DrawableHelper.drawTexture(matrices, x + ts.x, y + ts.y, 0, 0, 0, 16, 16, 16, 16);
	}

	public void renderSlot(MatrixStack matrices, Slot ts, TrinketSlots.Slot s, int x, int y){
		matrices.push();
		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();
		if (ts.getStack().isEmpty()) {
			this.client.getTextureManager().bindTexture(s.texture);
		} else {
			this.client.getTextureManager().bindTexture(BLANK_BACK);
		}
		DrawableHelper.drawTexture(matrices, ts.x, ts.y, 0, 0, 0, 16, 16, 16, 16);
		drawSlot(matrices, ts);
		if (this.isPointOverSlot(ts, x, y) && ts.doDrawHoveringEffect()) {
			this.focusedSlot = ts;
			RenderSystem.disableDepthTest();
			RenderSystem.colorMask(true, true, true, false);
			this.fillGradient(matrices, ts.x, ts.y, ts.x + 16, ts.y + 16, -2130706433, -2130706433);
			RenderSystem.colorMask(true, true, true, true);
		}
		matrices.pop();
	}
}