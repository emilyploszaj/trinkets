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
import dev.emi.trinkets.api.TrinketSlots;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.util.Rect2i;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Draws trinket slot group borders
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements RecipeBookProvider {
	private static final Identifier MORE_SLOTS = new Identifier("trinkets", "textures/gui/more_slots.png");
	private Map<SlotGroup, Rect2i> boundMap = new HashMap<SlotGroup, Rect2i>();
	private Rect2i currentBound = new Rect2i(0, 0, 0, 0);
	private SlotGroup group = null;
	private int slotsWidth;

	@Shadow
	private float mouseX;
	@Shadow
	private float mouseY;

	public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
		super(screenHandler, playerInventory, text);
	}

	@Inject(at = @At("HEAD"), method = "init")
	public void init(CallbackInfo info) {
		for (SlotGroup group : TrinketSlots.getPlayerSlots().values()) {
			boundMap.put(group, new Rect2i(getGroupX(group.getName()), getGroupY(group.getName()), 16, 16));
		}
		group = null;
		currentBound = new Rect2i(0, 0, 0, 0);
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		if (group != null) {
			if (!currentBound.contains(((int) mouseX) - x, ((int) mouseY) - y)) {
				TrinketsClient.activeGroup = null;
				group = null;
			}
		}
		if (group == null) {
			for (Map.Entry<SlotGroup, Rect2i> entry : boundMap.entrySet()) {
				if (entry.getValue().contains(((int) mouseX) - x, ((int) mouseY) - y)) {
					TrinketsClient.activeGroup = entry.getKey();
					break;
				}
			}
		}
		if (group != TrinketsClient.activeGroup) {
			group = TrinketsClient.activeGroup;
			if (group != null) {
				slotsWidth = group.getSlots().values().size() + 1;
				if (group.getName().equals("hand")) slotsWidth -= 1; // TODO move this to a slot group property
				Rect2i r = boundMap.get(group);
				if (r == null) {
					currentBound = new Rect2i(0, 0, 0, 0);
				} else {
					int l = (slotsWidth - 1) / 2 * 18;
					currentBound = new Rect2i(r.getX() - l - 4, r.getY() - 4, slotsWidth * 18 + 8, 26);
				}
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "drawForeground")
	private void drawForeground(MatrixStack matrices, int mouseX, int mouseY, CallbackInfo info) {
		if (group != null) {
			RenderSystem.enableDepthTest();
			this.setZOffset(310);
			int x = getGroupX(group.getName()) - 4 - (slotsWidth - 1) / 2 * 18;
			int y = getGroupY(group.getName()) - 4;
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

	// TODO put this info somewhere else, this is for testing
	public int getGroupX(String group) {
		if (group.equals("head")) {
			return 7;
		} else if (group.equals("chest")) {
			return 7;
		} else if (group.equals("legs")) {
			return 7;
		} else if (group.equals("feet")) {
			return 7;
		} else {
			return 76;
		}
	}

	// TODO put this info somewhere else, this is for testing
	public int getGroupY(String group) {
		if (group.equals("head")) {
			return 7;
		} else if (group.equals("chest")) {
			return 25;
		} else if (group.equals("legs")) {
			return 43;
		} else if (group.equals("feet")) {
			return 61;
		} else if (group.equals("offhand")) {
			return 61;
		} else {
			return 43;
		}
	}
}
