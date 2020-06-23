package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.TrinketInventoryRenderer;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketSlots.SlotGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Trinkets slots in the creative inventory, noticably grosser than the survival inventory due to slot restrictions
 */
@Environment(EnvType.CLIENT)
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {
	@Shadow
	private static int selectedTab;

	private List<Slot> creativeSlots = new ArrayList<Slot>();
	private int mouseX, mouseY;

	public CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory inventory, Text text) {
		super(screenHandler, inventory, text);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	public void constructor(PlayerEntity player, CallbackInfo info) {
		this.playerInventoryTitleX = -100000;
	}

	@Inject(at = @At("TAIL"), method = "init")
	public void init(CallbackInfo info) {
		if(creativeSlots.size() == 0) return;
		TrinketsClient.displayEquipped = 0;
		List<TrinketSlots.Slot> trinketSlots = TrinketSlots.getAllSlots();
		for (int i = 0; i < trinketSlots.size(); i++) {
			if (!trinketSlots.get(i).getSlotGroup().onReal && trinketSlots.get(i).getSlotGroup().slots.get(0).equals(trinketSlots.get(i))) {
				((SlotMixin) (Object) creativeSlots.get(i)).setXPosition(getGroupX(trinketSlots.get(i).getSlotGroup()) + 1);
				((SlotMixin) (Object) creativeSlots.get(i)).setYPosition(getGroupY(trinketSlots.get(i).getSlotGroup()) + 1);
			} else {
				((SlotMixin) (Object) creativeSlots.get(i)).setXPosition(Integer.MIN_VALUE);
			}
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "tick")
	protected void tick(CallbackInfo info) {
		if (selectedTab != ItemGroup.INVENTORY.getIndex()) return;
		List<TrinketSlots.Slot> trinketSlots = TrinketSlots.getAllSlots();
		float relX = mouseX - this.x;
		float relY = mouseY - this.y;
		if (TrinketsClient.slotGroup == null || !inBounds(TrinketsClient.slotGroup, relX, relY, true)) {
			if (TrinketsClient.slotGroup != null) {
				for (int i = 0; i < trinketSlots.size(); i++) {
					if (trinketSlots.get(i).getSlotGroup().getName().equals(TrinketsClient.slotGroup.getName())
							&& (TrinketsClient.slotGroup.onReal || TrinketsClient.slotGroup.slots.get(0) != trinketSlots.get(i))) {
						((SlotMixin) (Object) creativeSlots.get(i)).setXPosition(Integer.MIN_VALUE);
					}
				}
			}
			TrinketsClient.slotGroup = null;
			for (SlotGroup group : TrinketSlots.slotGroups) {
				if (inBounds(group, relX, relY, false) && group.slots.size() > 0) {
					TrinketsClient.displayEquipped = 0;
					TrinketsClient.slotGroup = group;
					List<Slot> tSlots = new ArrayList<Slot>();
					for (int i = 0; i < trinketSlots.size(); i++) {
						if (trinketSlots.get(i).getSlotGroup() == group)
							tSlots.add(creativeSlots.get(i));
					}
					if (tSlots.size() == 0) return;
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
						TrinketsClient.activeSlots.add(this.getScreenHandler().getSlot(group.vanillaSlot));
					}
					for (Slot ts : tSlots) {
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
					List<Slot> tSlots = new ArrayList<Slot>();
					for (int i = 0; i < trinketSlots.size(); i++) {
						if (trinketSlots.get(i).getSlotGroup().getName().equals(group.getName())) {
							tSlots.add(creativeSlots.get(i));
						}
					}
					if (tSlots.size() == 0) return;
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
						TrinketsClient.activeSlots.add(this.getScreenHandler().getSlot(group.vanillaSlot));
					}
					for (Slot ts : tSlots) {
						TrinketsClient.activeSlots.add(ts);
					}
				}
			}
		}
		for (int i = 0; i < trinketSlots.size(); i++) {
			if (((TrinketsClient.lastEquipped == null || TrinketsClient.displayEquipped <= 0 || !trinketSlots.get(i).getSlotGroup().getName().equals(TrinketsClient.lastEquipped.getName()))
					&& (TrinketsClient.slotGroup == null || !trinketSlots.get(i).getSlotGroup().equals(TrinketsClient.slotGroup)))
					&& (trinketSlots.get(i).getSlotGroup().onReal || trinketSlots.get(i).getSlotGroup().slots.get(0) != trinketSlots.get(i))) {
				((SlotMixin) (Object) creativeSlots.get(i)).setXPosition(Integer.MIN_VALUE);
			}
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "drawBackground")
	protected void drawBackground(MatrixStack matrices, float f, int x, int y, CallbackInfo info) {
		if (selectedTab != ItemGroup.INVENTORY.getIndex()) return;
		GlStateManager.disableLighting();
		SlotGroup lastGroup = TrinketSlots.slotGroups.get(TrinketSlots.slotGroups.size() - 1);
		int lastX = getGroupX(lastGroup);
		int lastY = getGroupY(lastGroup);
		if (lastX < 0) {
			TrinketInventoryRenderer.renderExcessSlotGroups(matrices, this, this.client.getTextureManager(), this.x, this.y, lastX, lastY);
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "drawForeground")
	protected void drawForeground(MatrixStack matrices, int x, int y, CallbackInfo info) {
		if (selectedTab != ItemGroup.INVENTORY.getIndex()) return;
		super.drawForeground(matrices, x, y);
		GlStateManager.disableLighting();
		for (SlotGroup group : TrinketSlots.slotGroups) {
			if (!group.onReal && group.slots.size() > 0) {
				this.client.getTextureManager().bindTexture(TrinketInventoryRenderer.MORE_SLOTS_TEX);
				this.drawTexture(matrices, getGroupX(group), getGroupY(group), 4, 4, 18, 18);
			}
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen;drawMouseoverTooltip(Lnet/minecraft/client/util/math/MatrixStack;II)V"), method = "render")
	protected void drawMouseoverTooltip(MatrixStack matrices, int x, int y, float f, CallbackInfo info) {
		if (selectedTab != ItemGroup.INVENTORY.getIndex()) return;
		if (TrinketsClient.slotGroup != null) {
			TrinketInventoryRenderer.renderGroupFront(matrices, this, this.client.getTextureManager(), this.playerInventory,
					this.x, this.y, TrinketsClient.slotGroup, getGroupX(TrinketsClient.slotGroup),
					getGroupY(TrinketsClient.slotGroup));
		} else if (TrinketsClient.displayEquipped > 0 && TrinketsClient.lastEquipped != null) {
			TrinketInventoryRenderer.renderGroupFront(matrices, this, this.client.getTextureManager(), this.playerInventory,
					this.x, this.y, TrinketsClient.lastEquipped, getGroupX(TrinketsClient.lastEquipped),
					getGroupY(TrinketsClient.lastEquipped));
		} else {
			return;
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "render")
	protected void render(MatrixStack matrices, int x, int y, float f, CallbackInfo info) {
		if (selectedTab != ItemGroup.INVENTORY.getIndex()) return;
		mouseX = x;
		mouseY = y;
		PlayerInventory inventory = this.client.player.inventory;
		ItemStack stack = inventory.getCursorStack();
		if (!stack.isEmpty()) {
			try {
				drawItem(stack, x - 8, y - 8, null);
			} catch (Exception e) {
				e.printStackTrace();
				// Nice
			}
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "setSelectedTab")
	private void setSelectedTab(ItemGroup itemGroup, CallbackInfo info) {
		if (itemGroup == ItemGroup.INVENTORY) {
			ScreenHandler screenHandler = this.getScreenHandler();
			creativeSlots.clear();
			for (int i = 46; i < screenHandler.slots.size(); i++) {
				if(screenHandler.getSlot(i).inventory instanceof TrinketInventory) creativeSlots.add(screenHandler.getSlot(i));
			}
			init(info);
		}
	}

	public boolean inBounds(SlotGroup group, float x, float y, boolean focused) {
		int groupX = getGroupX(group);
		int groupY = getGroupY(group);
		if (focused) {
			int count = group.slots.size();
			if (group.onReal) count++;
			int l = count / 2;
			int r = count - l - 1;
			return x > groupX - l * 18 - 4 && y > groupY - 4 && x < groupX + r * 18 + 22 && y < groupY + 22;
		} else {
			return x > groupX && y > groupY && x < groupX + 18 && y < groupY + 18;
		}
	}

	public int getGroupX(SlotGroup group) {
		if (group.vanillaSlot == 5) return 53;
		if (group.vanillaSlot == 6) return 53;
		if (group.vanillaSlot == 7) return 107;
		if (group.vanillaSlot == 8) return 107;
		if (group.vanillaSlot == 45) return 34;
		if (group.getName().equals("hand")) return 15;
		int j = 0;
		if (TrinketSlots.slotGroups.get(5).slots.size() == 0) j = -1;
		for (int i = 6; i < TrinketSlots.slotGroups.size(); i++) {
			if (TrinketSlots.slotGroups.get(i) == group) {
				j += i;
				if (j == 5) return 15;
				if (j == 6) return 126;
				if (j == 7) return 145;
				return -15 - ((j - 8) / 4) * 18;
			} else if (TrinketSlots.slotGroups.get(i).slots.size() == 0) j--;
		}
		return 0;
	}

	public int getGroupY(SlotGroup group) {
		if (group.vanillaSlot == 5) return 5;
		if (group.vanillaSlot == 6) return 32;
		if (group.vanillaSlot == 7) return 5;
		if (group.vanillaSlot == 8) return 32;
		if (group.vanillaSlot == 45) return 19;
		if (group.getName().equals("hand")) return 19;
		int j = 0;
		if (TrinketSlots.slotGroups.get(5).slots.size() == 0) j = -1;
		for (int i = 6; i < TrinketSlots.slotGroups.size(); i++) {
			if (TrinketSlots.slotGroups.get(i) == group) {
				j += i;
				if (j < 8) return 19;
				return 7 + ((j - 8) % 4) * 18;
			} else if (TrinketSlots.slotGroups.get(i).slots.size() == 0) j--;
		}
		return 0;
	}

	public void drawItem(ItemStack stack, int x, int y, String string) {
		GlStateManager.translatef(0.0F, 0.0F, 32.0F);
		this.setZOffset(200);
		this.itemRenderer.zOffset = 200.0F;
		this.itemRenderer.renderGuiItemIcon(stack, x, y);
		this.itemRenderer.renderGuiItemOverlay(this.textRenderer, stack, x, y, string);
		this.setZOffset(0);
		this.itemRenderer.zOffset = 0.0F;
	}
}