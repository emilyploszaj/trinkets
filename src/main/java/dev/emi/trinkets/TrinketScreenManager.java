package dev.emi.trinkets;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class TrinketScreenManager {
	private static final Identifier MORE_SLOTS = new Identifier("trinkets", "textures/gui/more_slots.png");
	public static TrinketScreen currentScreen;
	public static Rect2i currentBounds = new Rect2i(0, 0, 0, 0);
	public static Rect2i typeBounds = new Rect2i(0, 0, 0, 0);
	public static Rect2i quickMoveBounds = new Rect2i(0, 0, 0, 0);
	public static Rect2i quickMoveTypeBounds = new Rect2i(0, 0, 0, 0);
	public static SlotGroup group = null;
	public static SlotGroup quickMoveGroup = null;

	public static void init(TrinketScreen screen) {
		currentScreen = screen;
		group = null;
		currentBounds = new Rect2i(0, 0, 0, 0);
	}

	public static void removeSelections() {
		TrinketsClient.activeGroup = null;
		TrinketsClient.quickMoveGroup = null;
	}

	public static void update(float mouseX, float mouseY) {
		TrinketPlayerScreenHandler handler = currentScreen.trinkets$getHandler();

		if((group != null && !handler.trinkets$isSane(group))
			|| (quickMoveGroup != null && !handler.trinkets$isSane(quickMoveGroup))
			|| (TrinketsClient.activeGroup != null && !handler.trinkets$isSane(TrinketsClient.activeGroup))
			|| (TrinketsClient.quickMoveGroup != null && !handler.trinkets$isSane(TrinketsClient.quickMoveGroup))){
			return;
		}

		Slot focusedSlot = currentScreen.trinkets$getFocusedSlot();
		int x = currentScreen.trinkets$getX();
		int y = currentScreen.trinkets$getY();
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
						int i = handler.trinkets$getSlotTypes(group).indexOf(ts.getType());
						if (i >= 0) {
							Point slotHeight = handler.trinkets$getSlotHeights(group).get(i);
							Rect2i r = currentScreen.trinkets$getGroupRect(group);
							int height = slotHeight.y();
							if (height > 1) {
								TrinketsClient.activeType = ts.getType();
								typeBounds = new Rect2i(r.getX() + slotHeight.x() - 2, r.getY() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
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
				int i = handler.trinkets$getSlotTypes(TrinketsClient.activeGroup).indexOf(TrinketsClient.activeType);
				if (i >= 0) {
					Point slotHeight = handler.trinkets$getSlotHeights(TrinketsClient.activeGroup).get(i);
					Rect2i r = currentScreen.trinkets$getGroupRect(TrinketsClient.activeGroup);
					int height = slotHeight.y();
					if (height > 1) {
						typeBounds = new Rect2i(r.getX() + slotHeight.x() - 2, r.getY() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
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
				Rect2i r = currentScreen.trinkets$getGroupRect(g);
				if (r.getX() < 0 && currentScreen.trinkets$isRecipeBookOpen()) {
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
				if(!handler.trinkets$isSane(group)){
					return;
				}

				int slotsWidth = handler.trinkets$getSlotWidth(group) + 1;
				if (group.getSlotId() == -1) slotsWidth -= 1;
				Rect2i r = currentScreen.trinkets$getGroupRect(group);
				currentBounds = new Rect2i(0, 0, 0, 0);

				if (r != null) {
					int l = (slotsWidth - 1) / 2 * 18;

					if (slotsWidth > 1) {
						currentBounds = new Rect2i(r.getX() - l - 3, r.getY() - 3, slotsWidth * 18 + 5, 23);
					} else {
						currentBounds = r;
					}

					if (focusedSlot instanceof TrinketSlot ts) {
						int i = handler.trinkets$getSlotTypes(group).indexOf(ts.getType());
						if (i >= 0) {
							Point slotHeight = handler.trinkets$getSlotHeights(group).get(i);
							int height = slotHeight.y();
							if (height > 1) {
								TrinketsClient.activeType = ts.getType();
								typeBounds = new Rect2i(r.getX() + slotHeight.x() - 2, r.getY() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
							}
						}
					}
				}
			}
		}

		if (quickMoveGroup != TrinketsClient.quickMoveGroup) {
			quickMoveGroup = TrinketsClient.quickMoveGroup;

			if (quickMoveGroup != null) {
				if(!handler.trinkets$isSane(quickMoveGroup)){
					return;
				}

				int slotsWidth = handler.trinkets$getSlotWidth(quickMoveGroup) + 1;

				if (quickMoveGroup.getSlotId() == -1) slotsWidth -= 1;
				Rect2i r = currentScreen.trinkets$getGroupRect(quickMoveGroup);
				quickMoveBounds = new Rect2i(0, 0, 0, 0);

				if (r != null) {
					int l = (slotsWidth - 1) / 2 * 18;
					quickMoveBounds = new Rect2i(r.getX() - l - 5, r.getY() - 5, slotsWidth * 18 + 8, 26);
					if (TrinketsClient.quickMoveType != null) {
						int i = handler.trinkets$getSlotTypes(quickMoveGroup).indexOf(TrinketsClient.quickMoveType);
						if (i >= 0) {
							Point slotHeight = handler.trinkets$getSlotHeights(quickMoveGroup).get(i);
							int height = slotHeight.y();
							quickMoveTypeBounds = new Rect2i(r.getX() + slotHeight.x() - 2, r.getY() - (height - 1) / 2 * 18 - 3, 23, height * 18 + 5);
						}
					}
				}
			}
		}
	}

	public static void tick() {
		if (TrinketsClient.quickMoveTimer > 0) {
			TrinketsClient.quickMoveTimer--;

			if (TrinketsClient.quickMoveTimer <= 0) {
				TrinketsClient.quickMoveGroup = null;
			}
		}
	}

	public static void drawGroup(DrawableHelper helper, MatrixStack matrices, SlotGroup group, SlotType type) {
		TrinketPlayerScreenHandler handler = currentScreen.trinkets$getHandler();

		if (!handler.trinkets$isSane(group)) {
			return;
		}

		RenderSystem.enableDepthTest();
		helper.setZOffset(305);

		Rect2i r = currentScreen.trinkets$getGroupRect(group);
		int slotsWidth = handler.trinkets$getSlotWidth(group) + 1;
		List<Point> slotHeights = handler.trinkets$getSlotHeights(group);
		List<SlotType> slotTypes = handler.trinkets$getSlotTypes(group);
		if (group.getSlotId() == -1) slotsWidth -= 1;
		int x = r.getX() - 4 - (slotsWidth - 1) / 2 * 18;
		int y = r.getY() - 4;
		RenderSystem.setShaderTexture(0, MORE_SLOTS);

		if (slotsWidth > 1 || type != null) {
			helper.drawTexture(matrices, x, y, 0, 0, 4, 26);

			for (int i = 0; i < slotsWidth; i++) {
				helper.drawTexture(matrices, x + i * 18 + 4, y, 4, 0, 18, 26);
			}

			helper.drawTexture(matrices, x + slotsWidth * 18 + 4, y, 22, 0, 4, 26);
			if (slotHeights != null) {
				for (int s = 0; s < slotHeights.size(); s++) {
					if (slotTypes.get(s) != type) {
						continue;
					}
					Point slotHeight = slotHeights.get(s);
					int height = slotHeight.y();
					if (height > 1) {
						int top = (height - 1) / 2;
						int bottom = height / 2;
						int slotX = slotHeight.x() - 4 + r.getX();
						if (height > 2) {
							helper.drawTexture(matrices, slotX, y - top * 18, 0, 0, 26, 4);
						}

						for (int i = 1; i < top + 1; i++) {
							helper.drawTexture(matrices, slotX, y - i * 18 + 4, 0, 4, 26, 18);
						}

						for (int i = 1; i < bottom + 1; i++) {
							helper.drawTexture(matrices, slotX, y + i * 18 + 4, 0, 4, 26, 18);
						}

						helper.drawTexture(matrices, slotX, y + 18 + bottom * 18 + 4, 0, 22, 26, 4);
					}
				}


				// The rest of this is just to re-render a portion of the top and bottom slot borders so that corners
				// between slot types on the GUI look nicer
				for (int s = 0; s < slotHeights.size(); s++) {
					Point slotHeight = slotHeights.get(s);
					int height = slotHeight.y();
					if (slotTypes.get(s) != type) {
						height = 1;
					}
					int slotX = slotHeight.x() + r.getX() + 1;
					int top = (height - 1) / 2;
					int bottom = height / 2;
					helper.drawTexture(matrices, slotX, y - top * 18 + 1, 4, 1, 16, 3);
					helper.drawTexture(matrices, slotX, y + (bottom + 1) * 18 + 4, 4, 22, 16, 3);
				}

				// Because pre-existing slots are not part of the slotHeights list
				if (group.getSlotId() != -1) {
					helper.drawTexture(matrices, r.getX() + 1, y + 1, 4, 1, 16, 3);
					helper.drawTexture(matrices, r.getX() + 1, y + 22, 4, 22, 16, 3);
				}
			}
		} else {
			helper.drawTexture(matrices, x + 4, y + 4, 4, 4, 18, 18);
		}

		helper.setZOffset(0);
		RenderSystem.disableDepthTest();
	}

	public static void drawActiveGroup(DrawableHelper helper, MatrixStack matrices) {
		if (TrinketsClient.activeGroup != null) {
			TrinketScreenManager.drawGroup(helper, matrices, TrinketsClient.activeGroup, TrinketsClient.activeType);
		} else if (TrinketsClient.quickMoveGroup != null) {
			TrinketScreenManager.drawGroup(helper, matrices, TrinketsClient.quickMoveGroup, TrinketsClient.quickMoveType);
		}
	}

	public static void drawExtraGroups(DrawableHelper helper, MatrixStack matrices) {
		TrinketPlayerScreenHandler handler = currentScreen.trinkets$getHandler();
		int x = currentScreen.trinkets$getX();
		int y = currentScreen.trinkets$getY();
		int groupCount = handler.trinkets$getGroupCount();
		if (groupCount <= 0 || currentScreen.trinkets$isRecipeBookOpen()) {
			return;
		}
		int width = groupCount / 4;
		int height = groupCount % 4;
		if (height == 0) {
			height = 4;
			width--;
		}
		RenderSystem.setShaderTexture(0, MORE_SLOTS);
		helper.drawTexture(matrices, x + 3, y,      7, 26, 1, 7);
		// Repeated tops and bottoms
		for (int i = 0; i < width; i++) {
			helper.drawTexture(matrices, x - 15 - 18 * i, y,      7, 26, 18, 7);
			helper.drawTexture(matrices, x - 15 - 18 * i, y + 79, 7, 51, 18, 7);
		}
		// Top and bottom
		helper.drawTexture(matrices, x - 15 - 18 * width, y,                   7, 26, 18, 7);
		helper.drawTexture(matrices, x - 15 - 18 * width, y + 7 + 18 * height, 7, 51, 18, 7);
		// Corners
		helper.drawTexture(matrices, x - 22 - 18 * width, y,                   0, 26, 7, 7);
		helper.drawTexture(matrices, x - 22 - 18 * width, y + 7 + 18 * height, 0, 51, 7, 7);
		// Outer sides
		for (int i = 0; i < height; i++) {
			helper.drawTexture(matrices, x - 22 - 18 * width, y + 7 + 18 * i, 0, 34, 7, 18);
		}
		// Inner sides
		if (width > 0) {
			for (int i = height; i < 4; i++) {
				helper.drawTexture(matrices, x - 4 - 18 * width, y + 7 + 18 * i, 0, 34, 7, 18);
			}
		}
		if (width > 0 && height < 4) {
			// Bottom corner
			helper.drawTexture(matrices, x - 4 - 18 * width, y + 79, 0, 51, 7, 7);
			// Inner corner
			helper.drawTexture(matrices, x - 4 - 18 * width, y + 7 + 18 * height, 0, 58, 7, 7);
		}
		if (width > 0 || height == 4) {
			// Inner corner
			helper.drawTexture(matrices, x, y + 79, 0, 58, 3, 7);
		}
	}

	public static boolean isClickInsideTrinketBounds(double mouseX, double mouseY) {
		TrinketPlayerScreenHandler handler = currentScreen.trinkets$getHandler();
		int x = currentScreen.trinkets$getX();
		int y = currentScreen.trinkets$getY();
		int mx = (int) (Math.round(mouseX) - x);
		int my = (int) (Math.round(mouseY) - y);
		if (TrinketScreenManager.currentBounds.contains(mx, my)) {
			return true;
		}
		int groupCount = handler.trinkets$getGroupCount();
		if (groupCount <= 0 || currentScreen.trinkets$isRecipeBookOpen()) {
			return false;
		}
		int width = groupCount / 4;
		int height = groupCount % 4;
		if (width > 0) {
			if (new Rect2i(-4 - 18 * width, 0, 7 + 18 * width, 86).contains(mx, my)) {
				return true;
			}
		}
		if (height > 0) {
			if (new Rect2i(-22 - 18 * width, 0, 25, 14 + 18 * height).contains(mx, my)) {
				return true;
			}
		}
		return false;
	}
}
