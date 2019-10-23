package dev.emi.trinkets;

import com.mojang.blaze3d.platform.GlStateManager;

import dev.emi.trinkets.api.TrinketSlots.SlotGroup;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;

/**
 * Contains reused methods for rendering in the survival and creative inventories
 */
public class TrinketInventoryRenderer {
	public static final Identifier MORE_SLOTS_TEX = new Identifier("trinkets", "textures/gui/more_slots.png");

	public static <T extends Container> void renderGroupFront(AbstractInventoryScreen<T> screen, TextureManager manager, PlayerInventory inventory, int left, int top, SlotGroup group, int groupX, int groupY) {
		int count = group.slots.size();
		if(group.onReal) count++;
		int l = count / 2;
		int r = count - l - 1;
		GlStateManager.disableDepthTest();
		manager.bindTexture(MORE_SLOTS_TEX);
		screen.blit(left + groupX, top + groupY - 4, 4, 0, 18, 26);
		screen.blit(left + groupX - 18 * l - 4, top + groupY - 4, 0, 0, 4, 26);
		screen.blit(left + groupX + 18 * (r + 1), top + groupY - 4, 22, 0, 4, 26);
		for (int i = 0; i < l; i++) {
			screen.blit(left + groupX - 18 * (i + 1), top + groupY - 4, 4, 0, 18, 26);
		}
		for (int i = 0; i < r; i++) {
			screen.blit(left + groupX + 18 * (i + 1), top + groupY - 4, 4, 0, 18, 26);
		}
		GlStateManager.enableDepthTest();
	}

	public static <T extends Container> void renderExcessSlotGroups(AbstractInventoryScreen<T> screen, TextureManager manager, int left, int top, int lastX, int lastY) {
		int xIndex = (lastX + 15) / -18;
		int yIndex = (lastY - 7) / 18;
		manager.bindTexture(TrinketInventoryRenderer.MORE_SLOTS_TEX);
		//Top segments
		for (int i = 0; i <= xIndex; i++) {
			screen.blit(left - 15 - i * 18, top + 3, 4, 0, 18, 4);
		}
		//Top left corner
		screen.blit(left - 19 - xIndex * 18, top + 3, 0, 0, 4, 4);
		//Bottom segments
		for (int i = 0; i < xIndex; i++) {
			screen.blit(left - 15 - i * 18, top + 79, 4, 22, 18, 4);
		}
		//Furthest left side segments
		for (int i = 0; i <= yIndex; i++) {
			screen.blit(left - 19 - xIndex * 18, top + 7 + i * 18, 0, 4, 4, 18);
		}
		//Bottom left side corner 
		screen.blit(left - 19 - xIndex * 18, top + 25 + yIndex * 18, 0, 22, 4, 4);
		if (xIndex != 0) {
			//Bottom layer left corner
			screen.blit(left - 1 - xIndex * 18, top + 79, 0, 22, 4, 4);
			//Inner left side segments
			for (int i = yIndex; i < 3; i++) {
				screen.blit(left - 1 - xIndex * 18, top + 25 + i * 18, 0, 4, 4, 18);
			}
			//Left column bottom layer
			if (yIndex == 3) {
				screen.blit(left - 15 - xIndex * 18, top + 25 + yIndex * 18, 4, 22, 18, 4);
			} else {
				screen.blit(left - 15 - xIndex * 18, top + 25 + yIndex * 18, 4, 22, 15, 4);
			}
		} else {
			screen.blit(left - 15 - xIndex * 18, top + 25 + yIndex * 18, 4, 22, 18, 4);
		}
	}
}