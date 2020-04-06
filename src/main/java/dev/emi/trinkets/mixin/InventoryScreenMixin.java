package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketInventoryRenderer;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.SlotGroups;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketSlots.SlotGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.container.PlayerContainer;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Rendering, logic, etc. for slot groups and extra slots
 */
@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerContainer> implements RecipeBookProvider {
	@Shadow
	private float mouseX;
	@Shadow
	private float mouseY;
	@Shadow
	private static final Identifier RECIPE_BUTTON_TEX = new Identifier("textures/gui/recipe_button.png");
	
	private List<TrinketSlot> invSlots;

	public InventoryScreenMixin(PlayerContainer container, PlayerInventory inventory, Text text) {
		super(container, inventory, text);
	}

	@Inject(at = @At("TAIL"), method = "init")
	public void init(CallbackInfo info){
		TrinketsClient.displayEquipped = 0;
		invSlots = new ArrayList<>();
		for (Slot slot: this.container.slots) {
			if (slot instanceof TrinketSlot) {
				TrinketSlot ts = (TrinketSlot) slot;
				invSlots.add(ts);
				if (!ts.keepVisible) {
					((SlotMixin) (Object) slot).setXPosition(Integer.MIN_VALUE);
				} else {
					((SlotMixin) (Object) ts).setXPosition(getGroupX(TrinketSlots.getSlotFromName(ts.group, ts.slot).getSlotGroup()) + 1);
					((SlotMixin) (Object) ts).setYPosition(getGroupY(TrinketSlots.getSlotFromName(ts.group, ts.slot).getSlotGroup()) + 1);
				}
			}
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "tick")
	protected void tick(CallbackInfo info) {
		float relX = this.mouseX - this.x;
		float relY = this.mouseY - this.y;
		if (TrinketsClient.slotGroup == null || !inBounds(TrinketsClient.slotGroup, relX, relY, true)) {
			if (TrinketsClient.slotGroup != null) {
				for (TrinketSlot ts: invSlots) {
					if (ts.group.equals(TrinketsClient.slotGroup.getName()) && !ts.keepVisible) {
						((SlotMixin) (Object) ts).setXPosition(Integer.MIN_VALUE);
					}
				}
			}
			TrinketsClient.slotGroup = null;
			for (SlotGroup group: TrinketSlots.slotGroups) {
				if (inBounds(group, relX, relY, false) && group.slots.size() > 0) {
					TrinketsClient.displayEquipped = 0;
					TrinketsClient.slotGroup = group;
					List<TrinketSlot> tSlots = new ArrayList<TrinketSlot>();
					for (TrinketSlot ts: invSlots) {
						if(ts.group.equals(group.getName())) tSlots.add(ts);
					}
					int groupX = getGroupX(group);
					int groupY = getGroupY(group);
					int count = group.slots.size();
					int offset = 1;
					if (group.onReal) {
						count++;
						offset = 0;
					} else {
						((SlotMixin) (Object) tSlots.get(0)).setXPosition(groupX + 1);
						((SlotMixin) (Object) tSlots.get(0)).setYPosition(groupY + 1);
					}
					int l = count / 2;
					int r = count - l - 1;
					if(tSlots.size() == 0) break;
					for (int i = 0; i < l; i++) {
						((SlotMixin) (Object) tSlots.get(i + offset)).setXPosition(groupX - (i + 1) * 18 + 1);
						((SlotMixin) (Object) tSlots.get(i + offset)).setYPosition(groupY + 1);
					}
					for (int i = 0; i < r; i++) {
						((SlotMixin) (Object) tSlots.get(i + l + offset)).setXPosition(groupX + (i + 1) * 18 + 1);
						((SlotMixin) (Object) tSlots.get(i + l + offset)).setYPosition(groupY + 1);
					}
					TrinketsClient.activeSlots = new ArrayList<Slot>();
					if (group.vanillaSlot != -1) {
						TrinketsClient.activeSlots.add(this.container.getSlot(group.vanillaSlot));
					}
					for (TrinketSlot ts: tSlots) {
						TrinketsClient.activeSlots.add(ts);
					}
					break;
				}
			}
		}
		if (TrinketsClient.displayEquipped > 0) {
			TrinketsClient.displayEquipped--;
			if (TrinketsClient.slotGroup == null) {
				SlotGroup group = TrinketsClient.lastEquipped;
				if (group != null) {
					List<TrinketSlot> tSlots = new ArrayList<TrinketSlot>();
					for (TrinketSlot ts: invSlots) {
						if (ts.group == group.getName()) tSlots.add(ts);
					}
					int groupX = getGroupX(group);
					int groupY = getGroupY(group);
					int count = group.slots.size();
					int offset = 1;
					if (group.onReal) {
						count++;
						offset = 0;
					} else {
						((SlotMixin) (Object) tSlots.get(0)).setXPosition(groupX + 1);
						((SlotMixin) (Object) tSlots.get(0)).setYPosition(groupY + 1);
					}
					int l = count / 2;
					int r = count - l - 1;
					for (int i = 0; i < l; i++) {
						((SlotMixin) (Object) tSlots.get(i + offset)).setXPosition(groupX - (i + 1) * 18 + 1);
						((SlotMixin) (Object) tSlots.get(i + offset)).setYPosition(groupY + 1);
					}
					for (int i = 0; i < r; i++) {
						((SlotMixin) (Object) tSlots.get(i + l + offset)).setXPosition(groupX + (i + 1) * 18 + 1);
						((SlotMixin) (Object) tSlots.get(i + l + offset)).setYPosition(groupY + 1);
					}
					TrinketsClient.activeSlots = new ArrayList<Slot>();
					if (group.vanillaSlot != -1) {
						TrinketsClient.activeSlots.add(this.container.getSlot(group.vanillaSlot));
					}
					for (TrinketSlot ts: tSlots) {
						TrinketsClient.activeSlots.add(ts);
					}
				}
			}
		}
		for (TrinketSlot ts: invSlots) {
			if (((TrinketsClient.lastEquipped == null || TrinketsClient.displayEquipped <= 0 || ts.group != TrinketsClient.lastEquipped.getName())
					&& (TrinketsClient.slotGroup == null || ts.group != TrinketsClient.slotGroup.getName())) && !ts.keepVisible) {
				((SlotMixin) (Object) ts).setXPosition(Integer.MIN_VALUE);
			}
		}
		for (TrinketSlot ts : invSlots) {
			int groupX = getGroupX(TrinketSlots.getSlotFromName(ts.group, ts.slot).getSlotGroup());
			if (ts.keepVisible && groupX < 0) {
				if (getRecipeBookWidget().isOpen()) {
					((SlotMixin) (Object) ts).setXPosition(Integer.MIN_VALUE);
				} else {
					((SlotMixin) (Object) ts).setXPosition(groupX + 1);
				}
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "drawBackground")
	protected void drawBackground(float f, int x, int y, CallbackInfo info) {
		SlotGroup lastGroup = TrinketSlots.slotGroups.get(TrinketSlots.slotGroups.size() - 1);
		int lastX = getGroupX(lastGroup);
		int lastY = getGroupY(lastGroup);
		if (!getRecipeBookWidget().isOpen() && lastX < 0) {
			TrinketInventoryRenderer.renderExcessSlotGroups(this, this.minecraft.getTextureManager(), this.x, this.y, lastX, lastY);
		}
		for (SlotGroup group: TrinketSlots.slotGroups) {
			if (!group.onReal && group.slots.size() > 0) {
				if (getRecipeBookWidget().isOpen() && getGroupX(group) < 0) continue;
				this.minecraft.getTextureManager().bindTexture(TrinketInventoryRenderer.MORE_SLOTS_TEX);
				this.blit(this.x + getGroupX(group), this.y + getGroupY(group), 4, 4, 18, 18);
			}
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "drawForeground")
	protected void drawForeground(int x, int y, CallbackInfo info) {
		super.drawForeground(x, y);
		GlStateManager.disableLighting();
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawMouseoverTooltip(II)V"), method = "render")
	protected void drawMouseoverTooltip(int x, int y, float f, CallbackInfo info) {
		if (TrinketsClient.slotGroup != null) {
			TrinketInventoryRenderer.renderGroupFront(this, this.minecraft.getTextureManager(), this.playerInventory, this.x, this.y, TrinketsClient.slotGroup, getGroupX(TrinketsClient.slotGroup), getGroupY(TrinketsClient.slotGroup));
		} else if (TrinketsClient.displayEquipped > 0 && TrinketsClient.lastEquipped != null) {
			TrinketInventoryRenderer.renderGroupFront(this, this.minecraft.getTextureManager(), this.playerInventory, this.x, this.y, TrinketsClient.lastEquipped, getGroupX(TrinketsClient.lastEquipped), getGroupY(TrinketsClient.lastEquipped));
		} else {
			return;
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "render")
	protected void render(int x, int y, float f, CallbackInfo info) {
		PlayerInventory inventory = this.minecraft.player.inventory;
		ItemStack stack = inventory.getCursorStack();
		if (!stack.isEmpty()) {
			try {
				GlStateManager.enableLighting();
				drawItem(stack, x - 8, y - 8, null);
			} catch (Exception e) {
				e.printStackTrace();
				//Nice
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "isClickOutsideBounds", cancellable = true)
	protected void isClickOutsideBounds(double x, double y, int i, int j, int k, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.slotGroup != null && inBounds(TrinketsClient.slotGroup, (float) x - this.x, (float) y - this.y, true)) info.setReturnValue(false);
	}

	public boolean inBounds(SlotGroup group, float x, float y, boolean focused) {
		if (getRecipeBookWidget().isOpen() && getGroupX(group) < 0) return false;
		int groupX = getGroupX(group);
		int groupY = getGroupY(group);
		if (focused) {
			int count = group.slots.size();
			if (group.onReal)
				count++;
			int l = count / 2;
			int r = count - l - 1;
			return x > groupX - l * 18 - 4 && y > groupY - 4 && x < groupX + r * 18 + 22 && y < groupY + 22;
		} else {
			return x > groupX && y > groupY && x < groupX + 18 && y < groupY + 18;
		}
	}

	public int getGroupX(SlotGroup group) {
		if (group.vanillaSlot == 5) return 7;
		if (group.vanillaSlot == 6) return 7;
		if (group.vanillaSlot == 7) return 7;
		if (group.vanillaSlot == 8) return 7;
		if (group.vanillaSlot == 45) return 76;
		if (group.getName().equals(SlotGroups.HAND)) return 76;
		int j = 0;
		if (TrinketSlots.slotGroups.get(5).slots.size() == 0) j = -1;
		for (int i = 6; i < TrinketSlots.slotGroups.size(); i++) {
			if (TrinketSlots.slotGroups.get(i) == group) {
				j += i;
				if (j < 8) return 76;
				return -15 - ((j - 8) / 4) * 18;
			} else if (TrinketSlots.slotGroups.get(i).slots.size() == 0) j--;
		}
		return 0;
	}

	public int getGroupY(SlotGroup group) {
		if (group.vanillaSlot == 5) return 7;
		if (group.vanillaSlot == 6) return 25;
		if (group.vanillaSlot == 7) return 43;
		if (group.vanillaSlot == 8) return 61;
		if (group.vanillaSlot == 45) return 61;
		if (group.getName().equals(SlotGroups.HAND)) return 43;
		int j = 0;
		if (TrinketSlots.slotGroups.get(5).slots.size() == 0) j = -1;
		for (int i = 6; i < TrinketSlots.slotGroups.size(); i++) {
			if (TrinketSlots.slotGroups.get(i) == group) {
				j += i;
				if (j == 5) return 43;
				if (j == 6) return 25;
				if (j == 7) return 7;
				return 7 + ((j - 8) % 4) * 18;
			} else if (TrinketSlots.slotGroups.get(i).slots.size() == 0) j--;
		}
		return 0;
	}

	public void drawItem(ItemStack stack, int x, int y, String string) {
		GlStateManager.translatef(0.0F, 0.0F, 32.0F);
		setBlitOffset(200);
		this.itemRenderer.zOffset = 200.0F;
		this.itemRenderer.renderGuiItem(stack, x, y);
		this.itemRenderer.renderGuiItemOverlay(this.font, stack, x, y, string);
		setBlitOffset(0);
		this.itemRenderer.zOffset = 0.0F;
	}
}