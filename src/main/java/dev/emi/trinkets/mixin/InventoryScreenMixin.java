package dev.emi.trinkets.mixin;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.util.Rect2i;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

/**
 * Draws trinket slot group borders and handles active group logic
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements RecipeBookProvider {
	private static final Identifier MORE_SLOTS = new Identifier("trinkets", "textures/gui/more_slots.png");
	private Map<SlotGroup, Pair<Integer, Integer>> slotPos = new HashMap<SlotGroup, Pair<Integer, Integer>>();
	private Map<SlotGroup, Rect2i> boundMap = new HashMap<SlotGroup, Rect2i>();
	private Rect2i currentBound = new Rect2i(0, 0, 0, 0);
	private Rect2i quickMoveBound = new Rect2i(0, 0, 0, 0);
	private SlotGroup group = null;
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
		Map<Integer, SlotGroup> ids = new HashMap<Integer, SlotGroup>(); 
		for (SlotGroup group : TrinketsApi.getPlayerSlots().values()) {
			if (group.getSlotId() != -1) {
				ids.put(group.getSlotId(), group);
			}
		}
		for (Slot slot : this.handler.slots) {
			if (ids.containsKey(slot.id) && slot.inventory instanceof PlayerInventory) {
				slotPos.put(ids.get(slot.id), new Pair<Integer, Integer>(slot.x, slot.y));
			}
		}
		int groupNum = 1; // Start at 1 because offhand exists
		if (TrinketsApi.getPlayerSlots().containsKey("hand")) { // Hardcode the hand slot group to always be above the offhand, if it exists
			groupNum++;
			slotPos.put(TrinketsApi.getPlayerSlots().get("hand"), new Pair<Integer, Integer>(77, 44));
		}
		for (SlotGroup group : TrinketsApi.getPlayerSlots().values()) {
			if (!slotPos.containsKey(group)) {
				int x = 77;
				int y = 0;
				if (groupNum >= 4) {
					x = -4 - (x / 4) * 18;
					y = 7 + (x % 4) * 18;
				} else {
					y = 62 - x * 18;
				}
				slotPos.put(group, new Pair<Integer, Integer>(x, y));
			}
		}
		for (SlotGroup group : TrinketsApi.getPlayerSlots().values()) {
			Pair<Integer, Integer> pos = slotPos.get(group);
			boundMap.put(group, new Rect2i(pos.getLeft(), pos.getRight(), 16, 16));
		}
		group = null;
		currentBound = new Rect2i(0, 0, 0, 0);
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		if (group != null) {
			if (!currentBound.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
				TrinketsClient.activeGroup = null;
				group = null;
			}
		}
		if (group == null) {
			if (quickMoveGroup != null) {
				if (quickMoveBound.contains(Math.round(mouseX) - x, Math.round(mouseY) - y)) {
					TrinketsClient.activeGroup = quickMoveGroup;
					TrinketsClient.quickMoveGroup = null;
				}
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
				int slotsWidth = group.getSlots().values().size() + 1;
				if (group.getSlotId() == -1) slotsWidth -= 1;
				Rect2i r = boundMap.get(group);
				if (r == null) {
					currentBound = new Rect2i(0, 0, 0, 0);
				} else {
					int l = (slotsWidth - 1) / 2 * 18;
					currentBound = new Rect2i(r.getX() - l - 4, r.getY() - 4, slotsWidth * 18 + 8, 26);
				}
			}
		}
		if (quickMoveGroup != TrinketsClient.quickMoveGroup) {
			quickMoveGroup = TrinketsClient.quickMoveGroup;
			if (quickMoveGroup != null) {
				int slotsWidth = quickMoveGroup.getSlots().values().size() + 1;
				if (quickMoveGroup.getSlotId() == -1) slotsWidth -= 1;
				Rect2i r = boundMap.get(quickMoveGroup);
				if (r == null) {
					quickMoveBound = new Rect2i(0, 0, 0, 0);
				} else {
					int l = (slotsWidth - 1) / 2 * 18;
					quickMoveBound = new Rect2i(r.getX() - l - 4, r.getY() - 4, slotsWidth * 18 + 8, 26);
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
		}else if (TrinketsClient.quickMoveGroup != null) {
			drawGroup(matrices, TrinketsClient.quickMoveGroup);
		}
	}

	private void drawGroup(MatrixStack matrices, SlotGroup group) {
		RenderSystem.enableDepthTest();
		this.setZOffset(310);
		Pair<Integer, Integer> pos = slotPos.get(group);
		int slotsWidth = group.getSlots().values().size() + 1;
		if (group.getSlotId() == -1) slotsWidth -= 1;
		int x = pos.getLeft() - 5 - (slotsWidth - 1) / 2 * 18;
		int y = pos.getRight() - 5;
		this.client.getTextureManager().bindTexture(MORE_SLOTS);
		drawTexture(matrices, x, y, 0, 0, 4, 26);
		for (int i = 0; i < slotsWidth; i++) {
			drawTexture(matrices, x + i * 18 + 4, y, 4, 0, 18, 26);
		}
		drawTexture(matrices, x + slotsWidth * 18 + 4, y, 22, 0, 4, 26);
		this.setZOffset(0);
		RenderSystem.disableDepthTest();
	}
}
