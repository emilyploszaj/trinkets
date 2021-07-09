package dev.emi.trinkets.mixin;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

/**
 * Draws trinket slot group borders and handles active group logic
 * 
 * @author Emi
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements RecipeBookProvider {
	@Unique
	private static final Identifier MORE_SLOTS = new Identifier("trinkets", "textures/gui/more_slots.png");
	@Unique
	private Rect2i currentBounds = new Rect2i(0, 0, 0, 0);
	@Unique
	private Rect2i typeBounds = new Rect2i(0, 0, 0, 0);
	@Unique
	private Rect2i quickMoveBounds = new Rect2i(0, 0, 0, 0);
	@Unique
	private Rect2i quickMoveTypeBounds = new Rect2i(0, 0, 0, 0);
	@Unique
	private SlotGroup group = null;
	@Unique
	private SlotGroup quickMoveGroup = null;

	@Shadow
	private float mouseX;
	@Shadow
	private float mouseY;

	private InventoryScreenMixin() {
		super(null, null, null);
	}

	@Inject(at = @At("HEAD"), method = "init")
	private void init(CallbackInfo info) {
		group = null;
		currentBounds = new Rect2i(0, 0, 0, 0);
	}

	@Inject(at = @At("TAIL"), method = "handledScreenTick")
	private void tick(CallbackInfo info) {
		if (group != null) {
			if (TrinketsClient.activeType != null) {
				if (!typeBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
					TrinketsClient.activeType = null;
				} else if (focusedSlot != null) {
					if (!(focusedSlot instanceof TrinketSlot ts && ts.getType() == TrinketsClient.activeType)) {
						TrinketsClient.activeType = null;
					}
				}
			}
			if (TrinketsClient.activeType == null) {
				if (!currentBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
					TrinketsClient.activeGroup = null;
					group = null;
				} else {
					if (focusedSlot instanceof TrinketSlot ts) {
						int i = ((TrinketPlayerScreenHandler) handler).getSlotTypes(group).indexOf(ts.getType());
						if (i >= 0) {
							Pair<Integer, Integer> slotHeight = ((TrinketPlayerScreenHandler) handler).getSlotHeights(group).get(i);
							Rect2i r = getGroupRect(group);
							int height = slotHeight.getRight();
							if (height > 1) {
								TrinketsClient.activeType = ts.getType();
								typeBounds = new Rect2i(slotHeight.getLeft() - 3, r.getY() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
							}
						}
					}
				}
			}
		}

		if (group == null && quickMoveGroup != null) {
			if (quickMoveTypeBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
				TrinketsClient.activeGroup = quickMoveGroup;
				TrinketsClient.activeType = TrinketsClient.quickMoveType;
				int i = ((TrinketPlayerScreenHandler) handler).getSlotTypes(TrinketsClient.activeGroup).indexOf(TrinketsClient.activeType);
				if (i >= 0) {
					Pair<Integer, Integer> slotHeight = ((TrinketPlayerScreenHandler) handler).getSlotHeights(TrinketsClient.activeGroup).get(i);
					Rect2i r = getGroupRect(TrinketsClient.activeGroup);
					int height = slotHeight.getRight();
					if (height > 1) {
						typeBounds = new Rect2i(slotHeight.getLeft() - 3, r.getY() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
					}
				}
				TrinketsClient.quickMoveGroup = null;
			} else if (quickMoveBounds.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
				TrinketsClient.activeGroup = quickMoveGroup;
				TrinketsClient.quickMoveGroup = null;
			}
		}

		if (group == null) {
			for (SlotGroup g : TrinketsApi.getPlayerSlots().values()) {
				Rect2i r = getGroupRect(g);
				if (r.getX() < 0 && this.getRecipeBookWidget().isOpen()) {
					continue;
				}
				if (r.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
					TrinketsClient.activeGroup = g;
					TrinketsClient.quickMoveGroup = null;
					break;
				}
			}
		}

		if (group != TrinketsClient.activeGroup) {
			group = TrinketsClient.activeGroup;

			if (group != null) {
				int slotsWidth = ((TrinketPlayerScreenHandler) handler).getSlotWidth(group) + 1;
				if (group.getSlotId() == -1) slotsWidth -= 1;
				Rect2i r = getGroupRect(group);
				currentBounds = new Rect2i(0, 0, 0, 0);

				if (r != null) {
					int l = (slotsWidth - 1) / 2 * 18;

					if (slotsWidth > 1) {
						currentBounds = new Rect2i(r.getX() - l - 3, r.getY() - 3, slotsWidth * 18 + 5, 23);
					} else {
						currentBounds = r;
					}

					if (focusedSlot instanceof TrinketSlot ts) {
						int i = ((TrinketPlayerScreenHandler) handler).getSlotTypes(group).indexOf(ts.getType());
						if (i >= 0) {
							Pair<Integer, Integer> slotHeight = ((TrinketPlayerScreenHandler) handler).getSlotHeights(group).get(i);
							int height = slotHeight.getRight();
							if (height > 1) {
								TrinketsClient.activeType = ts.getType();
								typeBounds = new Rect2i(slotHeight.getLeft() - 3, r.getY() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
							}
						}
					}
				}
			}
		}

		if (quickMoveGroup != TrinketsClient.quickMoveGroup) {
			quickMoveGroup = TrinketsClient.quickMoveGroup;

			if (quickMoveGroup != null) {
				int slotsWidth = ((TrinketPlayerScreenHandler) handler).getSlotWidth(quickMoveGroup) + 1;

				if (quickMoveGroup.getSlotId() == -1) slotsWidth -= 1;
				Rect2i r = getGroupRect(quickMoveGroup);
				quickMoveBounds = new Rect2i(0, 0, 0, 0);

				if (r != null) {
					int l = (slotsWidth - 1) / 2 * 18;
					quickMoveBounds = new Rect2i(r.getX() - l - 5, r.getY() - 5, slotsWidth * 18 + 8, 26);
					if (TrinketsClient.quickMoveType != null) {
						int i = ((TrinketPlayerScreenHandler) handler).getSlotTypes(quickMoveGroup).indexOf(TrinketsClient.quickMoveType);
						if (i >= 0) {
							Pair<Integer, Integer> slotHeight = ((TrinketPlayerScreenHandler) handler).getSlotHeights(quickMoveGroup).get(i);
							int height = slotHeight.getRight();
							quickMoveTypeBounds = new Rect2i(slotHeight.getLeft() - 4, r.getY() - (height - 1) / 2 * 18 - 4, 26, height * 18 + 8);
						}
					}
				}
			}
		}

		if (TrinketsClient.quickMoveTimer > 0) {
			TrinketsClient.quickMoveTimer--;

			if (TrinketsClient.quickMoveTimer <= 0) {
				TrinketsClient.quickMoveGroup = null;
			}
		}
	}

	@Unique
	private Rect2i getGroupRect(SlotGroup group) {
		Pair<Integer, Integer> pos = ((TrinketPlayerScreenHandler) handler).getGroupPos(group);
		if (pos != null) {
			return new Rect2i(pos.getLeft() - 1, pos.getRight() - 1, 17, 17);
		}
		return new Rect2i(0, 0, 0, 0);
	}

	@Inject(at = @At(value = "INVOKE", target = "net/minecraft/client/gui/screen/ingame/InventoryScreen.drawEntity(IIIFFLnet/minecraft/entity/LivingEntity;)V"), method = "drawBackground")
	private void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY, CallbackInfo info) {
		int groupCount = ((TrinketPlayerScreenHandler) handler).getGroupCount();
		if (groupCount <= 0 || this.getRecipeBookWidget().isOpen()) {
			return;
		}
		int width = groupCount / 4;
		int height = groupCount % 4;
		if (height == 0) {
			height = 4;
			width--;
		}
		RenderSystem.setShaderTexture(0, MORE_SLOTS);
		drawTexture(matrices, x + 3, y,      7, 26, 1, 7);
		// Repeated tops and bottoms
		for (int i = 0; i < width; i++) {
			drawTexture(matrices, x - 15 - 18 * i, y,      7, 26, 18, 7);
			drawTexture(matrices, x - 15 - 18 * i, y + 79, 7, 51, 18, 7);
		}
		// Top and bottom
		drawTexture(matrices, x - 15 - 18 * width, y,                   7, 26, 18, 7);
		drawTexture(matrices, x - 15 - 18 * width, y + 7 + 18 * height, 7, 51, 18, 7);
		// Corners
		drawTexture(matrices, x - 22 - 18 * width, y,                   0, 26, 7, 7);
		drawTexture(matrices, x - 22 - 18 * width, y + 7 + 18 * height, 0, 51, 7, 7);
		// Outer sides
		for (int i = 0; i < height; i++) {
			drawTexture(matrices, x - 22 - 18 * width, y + 7 + 18 * i, 0, 34, 7, 18);
		}
		// Inner sides
		if (width > 0) {
			for (int i = height; i < 4; i++) {
				drawTexture(matrices, x - 4 - 18 * width, y + 7 + 18 * i, 0, 34, 7, 18);
			}
		}
		if (width > 0 && height < 4) {
			// Bottom corner
			drawTexture(matrices, x - 4 - 18 * width, y + 79, 0, 51, 7, 7);
			// Inner corner
			drawTexture(matrices, x - 4 - 18 * width, y + 7 + 18 * height, 0, 58, 7, 7);
		}
		if (width > 0 || height == 4) {
			// Inner corner
			drawTexture(matrices, x, y + 79, 0, 58, 3, 7);
		}
	}

	@Inject(at = @At("TAIL"), method = "drawForeground")
	private void drawForeground(MatrixStack matrices, int mouseX, int mouseY, CallbackInfo info) {
		if (TrinketsClient.activeGroup != null) {
			drawGroup(matrices, TrinketsClient.activeGroup, TrinketsClient.activeType);
		} else if (TrinketsClient.quickMoveGroup != null) {
			drawGroup(matrices, TrinketsClient.quickMoveGroup, TrinketsClient.quickMoveType);
		}
	}

	@Unique
	private void drawGroup(MatrixStack matrices, SlotGroup group, SlotType type) {
		RenderSystem.enableDepthTest();
		this.setZOffset(305);

		Pair<Integer, Integer> pos = ((TrinketPlayerScreenHandler) handler).getGroupPos(group);
		int slotsWidth = ((TrinketPlayerScreenHandler) handler).getSlotWidth(group) + 1;
		List<Pair<Integer, Integer>> slotHeights = ((TrinketPlayerScreenHandler) handler).getSlotHeights(group);
		List<SlotType> slotTypes = ((TrinketPlayerScreenHandler) handler).getSlotTypes(group);
		if (group.getSlotId() == -1) slotsWidth -= 1;
		int x = pos.getLeft() - 5 - (slotsWidth - 1) / 2 * 18;
		int y = pos.getRight() - 5;
		RenderSystem.setShaderTexture(0, MORE_SLOTS);

		if (slotsWidth > 1 || type != null) {
			drawTexture(matrices, x, y, 0, 0, 4, 26);

			for (int i = 0; i < slotsWidth; i++) {
				drawTexture(matrices, x + i * 18 + 4, y, 4, 0, 18, 26);
			}

			drawTexture(matrices, x + slotsWidth * 18 + 4, y, 22, 0, 4, 26);
			if (slotHeights != null) {
				for (int s = 0; s < slotHeights.size(); s++) {
					if (slotTypes.get(s) != type) {
						continue;
					}
					Pair<Integer, Integer> slotHeight = slotHeights.get(s);
					int height = slotHeight.getRight();
					if (height > 1) {
						int top = (height - 1) / 2;
						int bottom = height / 2;
						int slotX = slotHeight.getLeft() - 5;
						if (height > 2) {
							drawTexture(matrices, slotX, y - top * 18, 0, 0, 26, 4);
						}

						for (int i = 1; i < top + 1; i++) {
							drawTexture(matrices, slotX, y - i * 18 + 4, 0, 4, 26, 18);
						}

						for (int i = 1; i < bottom + 1; i++) {
							drawTexture(matrices, slotX, y + i * 18 + 4, 0, 4, 26, 18);
						}

						drawTexture(matrices, slotX, y + 18 + bottom * 18 + 4, 0, 22, 26, 4);
					}
				}

				// The rest of this is just to re-render a portion of the top and bottom slot borders so that corners
				// between slot types on the GUI look nicer
				for (int s = 0; s < slotHeights.size(); s++) {
					Pair<Integer, Integer> slotHeight = slotHeights.get(s);
					int height = slotHeight.getRight();
					if (slotTypes.get(s) != type) {
						height = 1;
					}
					int slotX = slotHeight.getLeft();
					int top = (height - 1) / 2;
					int bottom = height / 2;
					drawTexture(matrices, slotX, y - top * 18 + 1, 4, 1, 16, 3);
					drawTexture(matrices, slotX, y + (bottom + 1) * 18 + 4, 4, 22, 16, 3);
				}

				// Because pre-existing slots are not part of the slotHeights list
				if (group.getSlotId() != -1) {
					drawTexture(matrices, pos.getLeft(), y + 1, 4, 1, 16, 3);
					drawTexture(matrices, pos.getLeft(), y + 22, 4, 22, 14, 3);
				}
			}
		} else {
			drawTexture(matrices, x + 4, y + 4, 4, 4, 18, 18);
		}

		this.setZOffset(0);
		RenderSystem.disableDepthTest();
	}

	@Inject(at = @At("HEAD"), method = "isClickOutsideBounds", cancellable = true)
	private void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> info) {
		int mx = (int) (Math.round(mouseX) - x);
		int my = (int) (Math.round(mouseY) - y);
		if (currentBounds.contains(mx, my)) {
			info.setReturnValue(false);
		}
		int groupCount = ((TrinketPlayerScreenHandler) handler).getGroupCount();
		if (groupCount <= 0 || this.getRecipeBookWidget().isOpen()) {
			return;
		}
		int width = groupCount / 4;
		int height = groupCount % 4;
		if (width > 0) {
			if (new Rect2i(-4 - 18 * width, 0, 7 + 18 * width, 86).contains(mx, my)) {
				info.setReturnValue(false);
			}
		}
		if (height > 0) {
			if (new Rect2i(-22 - 18 * width, 0, 25, 14 + 18 * height).contains(mx, my)) {
				info.setReturnValue(false);
			}
		}
	}
}
