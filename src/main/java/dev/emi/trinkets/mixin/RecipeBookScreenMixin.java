package dev.emi.trinkets.mixin;

import dev.emi.trinkets.TrinketScreenManager;
import dev.emi.trinkets.TrinketSlot;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenMixin extends HandledScreen<AbstractRecipeScreenHandler> {
    @Unique
    private static final Identifier SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.ofVanilla("container/slot_highlight_front");

    public RecipeBookScreenMixin(AbstractRecipeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Inject(at = @At("HEAD"), method = "isClickOutsideBounds", cancellable = true)
    private void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> info) {
        if (TrinketScreenManager.isClickInsideTrinketBounds(mouseX, mouseY)) {
            info.setReturnValue(false);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/RecipeBookScreen;renderCursorStack(Lnet/minecraft/client/gui/DrawContext;II)V", shift = At.Shift.BEFORE),
            method = "render")
    private void drawForeground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (((Object) this) instanceof InventoryScreen) {
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
}
