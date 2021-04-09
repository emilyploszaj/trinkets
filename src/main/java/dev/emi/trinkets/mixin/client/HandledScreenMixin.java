package dev.emi.trinkets.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketSlots;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Overwrites general ContainerScreen slot hover checking functionality to not let any non-active group slots be interactable 
 * as well as render trinket slots properly
 */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
	@Final @Shadow protected T handler;
	@Shadow protected Slot focusedSlot;
	@Shadow protected int x;
	@Shadow protected int y;
	
	private static final Identifier BLANK_BACK = new Identifier("trinkets", "textures/gui/blank_back.png");
	
	protected HandledScreenMixin(Text text) {
		super(text);
	}

	@Shadow protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double a, double b);
	@Shadow protected abstract void drawSlot(MatrixStack matrices, Slot slot);
	@Shadow protected abstract boolean isPointOverSlot(Slot slot, double a, double b);

	@Inject(at = @At("HEAD"), method = "isPointOverSlot", cancellable = true)
	private void isPointOverSlot(Slot slot, double a, double b, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.slotGroup == null) {
			if (slot instanceof TrinketSlot) info.setReturnValue(false);
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
		matrices.push();
		RenderSystem.disableDepthTest();
		this.setZOffset(100);
		this.itemRenderer.zOffset = 100.0F;

		this.forEachSlot(46, (s, ts) -> {
			if (!ts.getSlotGroup().onReal && ts.getSlotGroup().slots.get(0) == ts) {
				this.renderSlotBack(matrices, s, ts, this.x, this.y);
			}
		});

		this.setZOffset(0);
		this.itemRenderer.zOffset = 0.0F;
		RenderSystem.enableDepthTest();
		matrices.pop();
	}
	
	@Inject(at = @At("HEAD"), method = "drawForeground")
	private void drawForeground(MatrixStack matrices, int x, int y, CallbackInfo info) {
		RenderSystem.disableDepthTest();
		this.forEachSlot(46, (s, ts) -> {
			if (!(ts.getSlotGroup() == TrinketsClient.slotGroup || !(ts.getSlotGroup() == TrinketsClient.lastEquipped && TrinketsClient.displayEquipped > 0))) {
				renderSlot(matrices, s, ts, x, y);
			}
		});

		// Redraw only the active group slots so they're always on top
		this.forEachSlot(0, (s, ts) -> {
			if (ts.getSlotGroup() == TrinketsClient.slotGroup || (ts.getSlotGroup() == TrinketsClient.lastEquipped && TrinketsClient.displayEquipped > 0)) {
				renderSlot(matrices, s, ts, x, y);
			}
		});
		RenderSystem.enableDepthTest();
	}

	@Unique
	private void forEachSlot(int startIndex, BiConsumer<Slot, TrinketSlots.Slot> slotConsumer) {
		int trinketOffset = -1;
		List<TrinketSlots.Slot> trinketSlots = TrinketSlots.getAllSlots();

		for (; startIndex < handler.slots.size(); startIndex++) {
			if (handler.slots.get(startIndex).inventory instanceof TrinketInventory) {
				if (trinketOffset == -1) {
					trinketOffset = startIndex;
				}

				Slot slot = handler.getSlot(startIndex);
				TrinketSlots.Slot trinketSlot = trinketSlots.get(startIndex - trinketOffset);
				slotConsumer.accept(slot, trinketSlot);
			}
		}
	}

	@Unique
	private void renderSlotBack(MatrixStack matrices, Slot slot, TrinketSlots.Slot trinketSlot, int x, int y) {
		RenderSystem.disableLighting();
		if (slot.getStack().isEmpty()) {
			this.client.getTextureManager().bindTexture(trinketSlot.texture);
		} else {
			this.client.getTextureManager().bindTexture(BLANK_BACK);
		}
		DrawableHelper.drawTexture(matrices, x + slot.x, y + slot.y, 0, 0, 0, 16, 16, 16, 16);
	}

	@Unique
	private void renderSlot(MatrixStack matrices, Slot slot, TrinketSlots.Slot trinketSlot, int x, int y){
		matrices.push();
		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();
		if (slot.getStack().isEmpty()) {
			this.client.getTextureManager().bindTexture(trinketSlot.texture);
		} else {
			this.client.getTextureManager().bindTexture(BLANK_BACK);
		}
		DrawableHelper.drawTexture(matrices, slot.x, slot.y, 0, 0, 0, 16, 16, 16, 16);
		drawSlot(matrices, slot);
		if (this.isPointOverSlot(slot, x, y) && slot.doDrawHoveringEffect()) {
			this.focusedSlot = slot;
			RenderSystem.disableDepthTest();
			RenderSystem.colorMask(true, true, true, false);
			this.fillGradient(matrices, slot.x, slot.y, slot.x + 16, slot.y + 16, -2130706433, -2130706433);
			RenderSystem.colorMask(true, true, true, true);
		}
		matrices.pop();
	}
}