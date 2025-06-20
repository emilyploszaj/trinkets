package dev.emi.trinkets.mixin;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.CreativeTrinketSlot;
import dev.emi.trinkets.Point;
import dev.emi.trinkets.SurvivalTrinketSlot;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketScreen;
import dev.emi.trinkets.TrinketScreenManager;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

/**
 * Delegates drawing and slot group selection logic
 * 
 * @author Emi
 */
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends HandledScreen<CreativeScreenHandler> implements TrinketScreen {
	@Unique
	private static final Identifier SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.ofVanilla("container/slot_highlight_front");
	@Shadow
	private static ItemGroup selectedTab;
	@Shadow
	protected abstract void setSelectedTab(ItemGroup group);

	private CreativeInventoryScreenMixin() {
		super(null, null, null);
	}

	@Redirect(at = @At(value = "INVOKE", target = "net/minecraft/util/collection/DefaultedList.size()I"), method = "setSelectedTab")
	private int size(DefaultedList<ItemStack> list) {
		return 46;
	}

	@Inject(at = @At("HEAD"), method = "setSelectedTab")
	private void setSelectedTab(ItemGroup g, CallbackInfo info) {
		if (g.getType() != ItemGroup.Type.INVENTORY) {
			TrinketScreenManager.removeSelections();
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "net/minecraft/screen/slot/Slot.<init>(Lnet/minecraft/inventory/Inventory;III)V"), method = "setSelectedTab")
	private void addCreativeTrinketSlots(ItemGroup g, CallbackInfo info) {
		TrinketPlayerScreenHandler handler = trinkets$getHandler();
		for (int i = handler.trinkets$getTrinketSlotStart(); i < handler.trinkets$getTrinketSlotEnd(); i++) {
			Slot slot = this.client.player.playerScreenHandler.slots.get(i);
			if (slot instanceof SurvivalTrinketSlot ts) {
				SlotGroup group = TrinketsApi.getPlayerSlots(this.client.player).get(ts.getType().getGroup());
				Rect2i rect = trinkets$getGroupRect(group);
				Point pos = trinkets$getHandler().trinkets$getGroupPos(group);
				if (pos == null) {
					return;
				}
				int xOff = rect.getX() + 1 - pos.x();
				int yOff = rect.getY() + 1 - pos.y();
				((CreativeScreenHandler) this.handler).slots.add(new CreativeTrinketSlot(ts, ts.getIndex(), ts.x + xOff, ts.y + yOff));
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "init")
	private void init(CallbackInfo info) {
		TrinketScreenManager.init(this);
	}

	@Inject(at = @At("HEAD"), method = "removed")
	private void removed(CallbackInfo info) {
		TrinketScreenManager.removeSelections();
	}

	@Inject(at = @At("TAIL"), method = "handledScreenTick")
	private void tick(CallbackInfo info) {
		TrinketScreenManager.tick();
	}

	@Inject(at = @At("HEAD"), method = "render")
	private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
			TrinketScreenManager.update(mouseX, mouseY);
		}
	}

	@Inject(at = @At("RETURN"), method = "drawBackground")
	private void drawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo info) {
		if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
			TrinketScreenManager.drawExtraGroups(context);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER),
			method = "render")
	private void drawForeground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(this.x, this.y);
			TrinketScreenManager.drawActiveGroup(context);

			for (Slot slot : this.handler.slots) {
				if (slot instanceof TrinketSlot trinketSlot && trinketSlot.renderAfterRegularSlots() && slot.isEnabled()) {
					this.drawSlot(context, slot);
					if (slot == this.focusedSlot && slot.canBeHighlighted()) {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_TEXTURE, this.focusedSlot.x - 4, this.focusedSlot.y - 4, 24, 24);
					}
				}
			}
			context.getMatrices().popMatrix();
		}
	}

	@Inject(at = @At("HEAD"), method = "isClickOutsideBounds", cancellable = true)
	private void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> info) {
		if (selectedTab.getType() == ItemGroup.Type.INVENTORY && TrinketScreenManager.isClickInsideTrinketBounds(mouseX, mouseY)) {
			info.setReturnValue(false);
		}
	}

	@Inject(at = @At("HEAD"), method = "isClickInTab", cancellable = true)
	private void isClickInTab(ItemGroup group, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			info.setReturnValue(false);
		}
	}
	
	@Inject(at = @At("HEAD"), method = "renderTabTooltipIfHovered", cancellable = true)
	private void renderTabTooltipIfHovered(DrawContext context, ItemGroup group, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> info) {
		if (TrinketsClient.activeGroup != null) {
			info.setReturnValue(false);
		}
	}

	@Override
	public TrinketPlayerScreenHandler trinkets$getHandler() {
		return (TrinketPlayerScreenHandler) this.client.player.playerScreenHandler;
	}

	@Override
	public Rect2i trinkets$getGroupRect(SlotGroup group) {
		int groupNum = trinkets$getHandler().trinkets$getGroupNum(group);
		if (groupNum <= 3) {
			// Look what else do you want me to do
			return switch (groupNum) {
				case 1 -> new Rect2i(15, 19, 17, 17);
				case 2 -> new Rect2i(126, 19, 17, 17);
				case 3 -> new Rect2i(145, 19, 17, 17);
				case -5 -> new Rect2i(53, 5, 17, 17);
				case -6 -> new Rect2i(53, 32, 17, 17);
				case -7 -> new Rect2i(107, 5, 17, 17);
				case -8 -> new Rect2i(107, 32, 17, 17);
				case -45 -> new Rect2i(34, 19, 17, 17);
				default -> new Rect2i(0, 0, 0, 0);
			};
		}
		Point pos = trinkets$getHandler().trinkets$getGroupPos(group);
		if (pos != null) {
			return new Rect2i(pos.x() - 1, pos.y() - 1, 17, 17);
		}
		return new Rect2i(0, 0, 0, 0);
	}

	@Override
	public Slot trinkets$getFocusedSlot() {
		return this.focusedSlot;
	}

	@Override
	public int trinkets$getX() {
		return this.x;
	}

	@Override
	public int trinkets$getY() {
		return this.y;
	}

	@Override
	public boolean trinkets$isRecipeBookOpen() {
		return false;
	}

	@Override
	public void trinkets$updateTrinketSlots() {
		setSelectedTab(selectedTab);
	}
}
