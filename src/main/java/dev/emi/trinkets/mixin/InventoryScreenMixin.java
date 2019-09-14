package dev.emi.trinkets.mixin;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.container.PlayerContainer;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketSlots.SlotGroup;

/**
 * Rendering, logic, etc. for slot groups and extra slots
 */
@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerContainer> implements RecipeBookProvider{
	@Shadow
	private float mouseX;
	@Shadow
	private float mouseY;
	@Shadow
	private static final Identifier RECIPE_BUTTON_TEX = new Identifier("textures/gui/recipe_button.png");
	private static final Identifier MORE_SLOTS_TEX = new Identifier("trinkets", "textures/gui/more_slots.png");
	private static final Identifier BLANK_BACK = new Identifier("trinkets", "textures/gui/blank_back.png");
	private List<TrinketSlot> invSlots;
	public InventoryScreenMixin(PlayerContainer container_1, PlayerInventory playerInventory_1, Text text_1) {
		super(container_1, playerInventory_1, text_1);
	}
	@Inject(at = @At("RETURN"), method = "init")
	public void init(CallbackInfo info){
		TrinketsClient.displayEquipped = 0;
		invSlots = new ArrayList<>();
		for(Slot slot: this.container.slotList){
			if(slot instanceof TrinketSlot){
				invSlots.add((TrinketSlot) slot);
				if(!((TrinketSlot) slot).keepVisible) slot.xPosition = Integer.MIN_VALUE;
			}
		}
	}
	@Inject(at = @At(value = "RETURN"), method = "tick")
	protected void tick(CallbackInfo info){
		float relX = this.mouseX - this.left;
		float relY = this.mouseY - this.top;
		if(TrinketsClient.slotGroup == null || !TrinketsClient.slotGroup.inBounds(relX, relY, true)){
			if(TrinketsClient.slotGroup != null){
				for(TrinketSlot ts: invSlots){
					if(ts.major == TrinketsClient.slotGroup.getName() && !ts.keepVisible) ts.xPosition = Integer.MIN_VALUE;
				}
			}
			TrinketsClient.slotGroup = null;
			for(SlotGroup group: TrinketSlots.slotGroups){
				if(group.inBounds(relX, relY, false) && group.slots.size() > 0){
					TrinketsClient.displayEquipped = 0;
					TrinketsClient.slotGroup = group;
					List<TrinketSlot> tSlots = new ArrayList<TrinketSlot>();
					for(TrinketSlot ts: invSlots){
						if(ts.major == group.getName()) tSlots.add(ts);
					}
					int count = group.slots.size();
					int offset = 1;
					if(group.onReal){
						count++;
						offset = 0;
					}else{
						tSlots.get(0).xPosition = group.x + 1;
						tSlots.get(0).yPosition = group.y + 1;
					}
					int l = count / 2;
					int r = count - l - 1;
					if(tSlots.size() == 0) break;
					for(int i = 0; i < l; i++){
						tSlots.get(i + offset).xPosition = group.x - (i + 1) * 18 + 1;
						tSlots.get(i + offset).yPosition = group.y + 1;
					}
					for(int i = 0; i < r; i++){
						tSlots.get(i + l + offset).xPosition = group.x + (i + 1) * 18 + 1;
						tSlots.get(i + l + offset).yPosition = group.y + 1;
					}
					TrinketsClient.activeSlots = new ArrayList<Slot>();
					if(group.vanillaSlot != -1){
						TrinketsClient.activeSlots.add(this.container.getSlot(group.vanillaSlot));
					}
					for(TrinketSlot ts: tSlots){
						TrinketsClient.activeSlots.add(ts);
					}
					break;
				}
			}
		}
		if(TrinketsClient.displayEquipped > 0){
			TrinketsClient.displayEquipped--;
			if(TrinketsClient.slotGroup == null){
				SlotGroup group = TrinketsClient.lastEquipped;
				if(group != null){
					List<TrinketSlot> tSlots = new ArrayList<TrinketSlot>();
					for(TrinketSlot ts: invSlots){
						if(ts.major == group.getName()) tSlots.add(ts);
					}
					int count = group.slots.size();
					int offset = 1;
					if(group.onReal){
						count++;
						offset = 0;
					}else{
						tSlots.get(0).xPosition = group.x + 1;
						tSlots.get(0).yPosition = group.y + 1;
					}
					int l = count / 2;
					int r = count - l - 1;
					for(int i = 0; i < l; i++){
						tSlots.get(i + offset).xPosition = group.x - (i + 1) * 18 + 1;
						tSlots.get(i + offset).yPosition = group.y + 1;
					}
					for(int i = 0; i < r; i++){
						tSlots.get(i + l + offset).xPosition = group.x + (i + 1) * 18 + 1;
						tSlots.get(i + l + offset).yPosition = group.y + 1;
					}
					TrinketsClient.activeSlots = new ArrayList<Slot>();
					if(group.vanillaSlot != -1){
						TrinketsClient.activeSlots.add(this.container.getSlot(group.vanillaSlot));
					}
					for(TrinketSlot ts: tSlots){
						TrinketsClient.activeSlots.add(ts);
					}
				}
			}
		}
		for(TrinketSlot ts: invSlots){
			if(((TrinketsClient.lastEquipped == null || TrinketsClient.displayEquipped <= 0 || ts.major != TrinketsClient.lastEquipped.getName()) && (TrinketsClient.slotGroup == null || ts.major != TrinketsClient.slotGroup.getName())) && !ts.keepVisible){
				ts.xPosition = Integer.MIN_VALUE;
			}
		}
	}
	@Inject(at = @At("RETURN"), method = "drawBackground")
	protected void drawBackground(CallbackInfo info){
		if(TrinketsClient.slotGroup != null){
			renderGroupBack(TrinketsClient.slotGroup);
		}else if(TrinketsClient.displayEquipped > 0 && TrinketsClient.lastEquipped != null){
			renderGroupBack(TrinketsClient.lastEquipped);
		}
		TrinketComponent comp = TrinketsApi.getTrinketComponent(this.playerInventory.player);
		for(SlotGroup group: TrinketSlots.slotGroups){
			if(!group.onReal && group.slots.size() > 0){
				if(comp.getStack(group.getName() + ":" + group.slots.get(0).getName()).isEmpty()){
					this.minecraft.getTextureManager().bindTexture(group.slots.get(0).texture);
				}else{
					this.minecraft.getTextureManager().bindTexture(BLANK_BACK);
				}
				DrawableHelper.blit(this.left + group.x + 1, this.top + group.y + 1, 0, 0, 0, 16, 16, 16, 16);
				this.minecraft.getTextureManager().bindTexture(MORE_SLOTS_TEX);
				this.blit(this.left + group.x, this.top + group.y, 4, 4, 18, 18);
			}
		}
	}
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawMouseoverTooltip(II)V"), method = "render")
	protected void drawMouseoverTooltip(int int_1, int int_2, float float_1, CallbackInfo info){
		if(TrinketsClient.slotGroup != null){
			renderGroupFront(TrinketsClient.slotGroup);
		}else if(TrinketsClient.displayEquipped > 0 && TrinketsClient.lastEquipped != null){
			renderGroupFront(TrinketsClient.lastEquipped);
		}else{
			return;
		}
	}
	@Inject(at = @At(value = "TAIL"), method = "render")
	protected void render(int int_1, int int_2, float float_1, CallbackInfo info){
		PlayerInventory playerInventory_1 = this.minecraft.player.inventory;
		ItemStack itemStack_1 = playerInventory_1.getCursorStack();
		if (!itemStack_1.isEmpty()) {
			try {
				drawItem(itemStack_1, int_1 - 8, int_2 - 8, null);
			} catch (Exception e) {
				e.printStackTrace();
				//Nice
			}
		}
	}
	public void drawItem(ItemStack itemStack_1, int int_1, int int_2, String string_1) {
		GlStateManager.translatef(0.0F, 0.0F, 32.0F);
		this.blitOffset = 200;
		this.itemRenderer.zOffset = 200.0F;
		this.itemRenderer.renderGuiItem(itemStack_1, int_1, int_2);
		this.itemRenderer.renderGuiItemOverlay(this.font, itemStack_1, int_1, int_2, string_1);
		this.blitOffset = 0;
		this.itemRenderer.zOffset = 0.0F;
	 }
	public void renderGroupBack(SlotGroup group){
		int count = group.slots.size();
		int offset = 1;
		GlStateManager.disableDepthTest();
		TrinketComponent comp = TrinketsApi.getTrinketComponent(this.playerInventory.player);
		if(group.onReal){
			count++;
			offset = 0;
		}else{
			if(comp.getStack(group.getName() + ":" + group.slots.get(0).getName()).isEmpty()){
				this.minecraft.getTextureManager().bindTexture(group.slots.get(0).texture);
			}else{
				this.minecraft.getTextureManager().bindTexture(BLANK_BACK);
			}
			DrawableHelper.blit(this.left + group.x + 1, this.top + group.y + 1, 0, 0, 0, 16, 16, 16, 16);
		}
		int l = count / 2;
		int r = count - l - 1;
		for(int i = 0; i < l; i++){
			if(comp.getStack(group.getName() + ":" + group.slots.get(i + offset).getName()).isEmpty()){
				this.minecraft.getTextureManager().bindTexture(group.slots.get(i + offset).texture);
			}else{
				this.minecraft.getTextureManager().bindTexture(BLANK_BACK);
			}
			DrawableHelper.blit(this.left + group.x - 18 * (i + 1) + 1, this.top + group.y + 1, 0, 0, 0, 16, 16, 16, 16);
		}
		for(int i = 0; i < r; i++){
			if(comp.getStack(group.getName() + ":" + group.slots.get(i + l + offset).getName()).isEmpty()){
				this.minecraft.getTextureManager().bindTexture(group.slots.get(i + l + offset).texture);
			}else{
				this.minecraft.getTextureManager().bindTexture(BLANK_BACK);
			}
			DrawableHelper.blit(this.left + group.x + 18 * (i + 1) + 1, this.top + group.y + 1, 0, 0, 0, 16, 16, 16, 16);
		}
		GlStateManager.enableDepthTest();
	}
	public void renderGroupFront(SlotGroup group){
		int count = group.slots.size();
		if(group.onReal) count++;
		int l = count / 2;
		int r = count - l - 1;
		GlStateManager.disableDepthTest();
		this.minecraft.getTextureManager().bindTexture(MORE_SLOTS_TEX);
		this.blit(this.left + group.x, this.top + group.y - 4, 4, 0, 18, 26);
		this.blit(this.left + group.x - 18 * l - 4, this.top + group.y - 4, 0, 0, 4, 26);
		this.blit(this.left + group.x + 18 * (r + 1), this.top + group.y - 4, 22, 0, 4, 26);
		for(int i = 0; i < l; i++){
			this.blit(this.left + group.x - 18 * (i + 1), this.top + group.y - 4, 4, 0, 18, 26);
		}
		for(int i = 0; i < r; i++){
			this.blit(this.left + group.x + 18 * (i + 1), this.top + group.y - 4, 4, 0, 18, 26);
		}
		GlStateManager.enableDepthTest();
	}
	@Inject(at = @At("HEAD"), method = "isClickOutsideBounds", cancellable = true)
	protected void isClickOutsideBounds(double double_1, double double_2, int int_1, int int_2, int int_3, CallbackInfoReturnable<Boolean> info){
		if(TrinketsClient.slotGroup != null && TrinketsClient.slotGroup.inBounds((float) double_1 - left, (float) double_2 - top, true)) info.setReturnValue(false);
	}
}