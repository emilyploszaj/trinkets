package dev.emi.trinkets.mixin;

import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

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
import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.gui.screen.ingame.ContainerProvider;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Overwrites general AbstractContainerScreen slot hover checking functionality to not let any non-active group slots be interactable 
 * as well as render trinket slots properly
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends Container> extends Screen implements ContainerProvider<T> {
	@Shadow
	protected int left, top;
	@Shadow
	public T container;
	@Shadow
	protected Slot focusedSlot;
	
	private static final Identifier BLANK_BACK = new Identifier("trinkets", "textures/gui/blank_back.png");
	
	protected AbstractContainerScreenMixin(Text text) {
		super(text);
	}

	@Shadow
	protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double a, double b);
	
	@Shadow
	protected abstract void drawSlot(Slot slot);

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
					this.isPointWithinBounds(slot.xPosition, slot.yPosition, 16, 16, a, b);
					return;
				}
			}
		}
		info.setReturnValue(false);
	}
	
	@Inject(at = @At("HEAD"), method = "drawForeground")
	private void drawForeground(int x, int y, CallbackInfo info) {
		GlStateManager.disableDepthTest();
		List<TrinketSlots.Slot> trinketSlots = TrinketSlots.getAllSlots();
		for (int i = 0; i < container.slotList.size(); i++) {
			if (!(container.slotList.get(i).inventory instanceof TrinketInventory)) continue;
			Slot ts = container.getSlot(i);
			TrinketSlots.Slot s = trinketSlots.get(i - 46);
			renderSlot(ts, s, x, y);
		}
		//Redraw only the active group slots so they're always on top
		for (int i = 0; i < container.slotList.size(); i++) {
			if (!(container.slotList.get(i).inventory instanceof TrinketInventory)) continue;
			Slot ts = container.getSlot(i);
			TrinketSlots.Slot s = trinketSlots.get(i - 46);
			if (s.getSlotGroup() == TrinketsClient.slotGroup) renderSlot(ts, s, x, y);
		}
		GlStateManager.enableDepthTest();
	}

	public void renderSlot(Slot ts, TrinketSlots.Slot s, int x, int y){
		GuiLighting.disable();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();
		GlStateManager.pushMatrix();
		if (ts.getStack().isEmpty()) {
			this.minecraft.getTextureManager().bindTexture(s.texture);
		} else {
			this.minecraft.getTextureManager().bindTexture(BLANK_BACK);
		}
		DrawableHelper.blit(ts.xPosition, ts.yPosition, 0, 0, 0, 16, 16, 16, 16);
		drawSlot(ts);
		if (this.isPointOverSlot(ts, x, y) && ts.doDrawHoveringEffect()) {
			this.focusedSlot = ts;
			GlStateManager.disableDepthTest();
			GlStateManager.disableLighting();
			GlStateManager.colorMask(true, true, true, false);
			this.fillGradient(ts.xPosition, ts.yPosition, ts.xPosition + 16, ts.yPosition + 16, -2130706433, -2130706433);
			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.enableLighting();
		}
		GlStateManager.popMatrix();
	}
}