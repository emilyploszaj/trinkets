package dev.emi.trinkets.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.util.Rect2i;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final Map<SlotGroup, Rect2i> boundMap = new HashMap<>();
	@Unique
	private final List<Rect2i> currentBounds = new ArrayList<>();
	@Unique
	private final List<Rect2i> quickMoveBounds = new ArrayList<>();
	@Unique
	private SlotGroup group = null;
	@Unique
	private SlotGroup quickMoveGroup = null;

	@Shadow
	private float mouseX;
	@Shadow
	private float mouseY;

	public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
		super(screenHandler, playerInventory, text);
	}

	@Inject(at = @At("HEAD"), method = "init")
	public void init(CallbackInfo info) {
		TrinketsApi.getTrinketComponent(this.playerInventory.player).ifPresent(trinkets -> {
			for (SlotGroup group : trinkets.getGroups().values()) {
				Pair<Integer, Integer> pos = ((TrinketPlayerScreenHandler) handler).getGroupPos(group);
				if (pos != null) {
					boundMap.put(group, new Rect2i(pos.getLeft(), pos.getRight(), 16, 16));
				}
			}
		});
		group = null;
		currentBounds.clear();
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		if (group != null) {
			boolean flag = true;
			for (Rect2i currentBound : currentBounds) {
				if (currentBound.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
					flag = false;
					break;
				}
			}

			if (flag) {
				TrinketsClient.activeGroup = null;
				group = null;
			}
		}

		if (group == null && quickMoveGroup != null) {
			boolean flag = true;
			for (Rect2i quickMoveBound : quickMoveBounds) {
				if (quickMoveBound.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
					flag = false;
					break;
				}
			}

			if (flag) {
				TrinketsClient.activeGroup = quickMoveGroup;
				TrinketsClient.quickMoveGroup = null;
			}
		}

		if (group == null) {
			for (Map.Entry<SlotGroup, Rect2i> entry : boundMap.entrySet()) {
				if (entry.getValue().contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
					TrinketsClient.activeGroup = entry.getKey();
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
				Rect2i r = boundMap.get(group);
				currentBounds.clear();

				if (r != null) {
					int l = (slotsWidth - 1) / 2 * 18;

					if (slotsWidth > 1) {
						currentBounds.add(new Rect2i(r.getX() - l - 5, r.getY() - 5, slotsWidth * 18 + 8, 26));
						List<Pair<Integer, Integer>> slotHeights = ((TrinketPlayerScreenHandler) handler).getSlotHeights(group);
						if (slotHeights != null) {
							for (Pair<Integer, Integer> slotHeight : slotHeights) {
								int height = slotHeight.getRight();
								if (height > 1) {
									currentBounds.add(new Rect2i(slotHeight.getLeft() - 5, r.getY() - (height - 1) / 2 * 18 - 5, 26, height * 18 + 8));
								}
							}
						}
					} else {
						currentBounds.add(new Rect2i(r.getX() - l, r.getY(), 18, 18));
					}
				}
			}
		}

		if (quickMoveGroup != TrinketsClient.quickMoveGroup) {
			quickMoveGroup = TrinketsClient.quickMoveGroup;

			if (quickMoveGroup != null) {
				int slotsWidth = ((TrinketPlayerScreenHandler) handler).getSlotWidth(quickMoveGroup) + 1;

				if (quickMoveGroup.getSlotId() == -1) slotsWidth -= 1;
				Rect2i r = boundMap.get(quickMoveGroup);
				quickMoveBounds.clear();

				if (r != null) {
					int l = (slotsWidth - 1) / 2 * 18;
					quickMoveBounds.add(new Rect2i(r.getX() - l - 5, r.getY() - 5, slotsWidth * 18 + 8, 26));
					List<Pair<Integer, Integer>> slotHeights = ((TrinketPlayerScreenHandler) handler).getSlotHeights(group);
					for (Pair<Integer, Integer> slotHeight : slotHeights) {
						int height = slotHeight.getRight();
						quickMoveBounds.add(new Rect2i(slotHeight.getLeft() - 5, r.getY() - (height - 1) / 2 * 18 - 5, 26, height + 8));
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

	@Inject(at = @At("TAIL"), method = "drawForeground")
	private void drawForeground(MatrixStack matrices, int mouseX, int mouseY, CallbackInfo info) {
		if (TrinketsClient.activeGroup != null) {
			drawGroup(matrices, TrinketsClient.activeGroup);
		} else if (TrinketsClient.quickMoveGroup != null) {
			drawGroup(matrices, TrinketsClient.quickMoveGroup);
		}
	}

	@Unique
	private void drawGroup(MatrixStack matrices, SlotGroup group) {
		RenderSystem.enableDepthTest();
		this.setZOffset(305);

		Pair<Integer, Integer> pos = ((TrinketPlayerScreenHandler) handler).getGroupPos(group);
		int slotsWidth = ((TrinketPlayerScreenHandler) handler).getSlotWidth(group) + 1;
		List<Pair<Integer, Integer>> slotHeights = ((TrinketPlayerScreenHandler) handler).getSlotHeights(group);
		if (group.getSlotId() == -1) slotsWidth -= 1;
		int x = pos.getLeft() - 5 - (slotsWidth - 1) / 2 * 18;
		int y = pos.getRight() - 5;
		this.client.getTextureManager().bindTexture(MORE_SLOTS);

		if (slotsWidth > 1 || slotHeights != null) {
			drawTexture(matrices, x, y, 0, 0, 4, 26);

			for (int i = 0; i < slotsWidth; i++) {
				drawTexture(matrices, x + i * 18 + 4, y, 4, 0, 18, 26);
			}

			drawTexture(matrices, x + slotsWidth * 18 + 4, y, 22, 0, 4, 26);
			if (slotHeights != null) {
				for (Pair<Integer, Integer> slotHeight : slotHeights) {
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
				for (Pair<Integer, Integer> slotHeight : slotHeights) {
					int height = slotHeight.getRight();
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
	protected void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> info) {
		for (Rect2i currentBound : currentBounds) {
			if (currentBound.contains((int) (Math.round(mouseX) - x), (int) (Math.round(mouseY) - y))) {
				info.setReturnValue(false);
				break;
			}
		}
	}
}
