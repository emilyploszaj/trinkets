package dev.emi.trinkets.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.trinkets.TrinketScreenManager;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookScreen.class)
public class RecipeBookScreenMixin {
    @Inject(at = @At("HEAD"), method = "isClickOutsideBounds", cancellable = true)
    private void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> info) {
        if (TrinketScreenManager.isClickInsideTrinketBounds(mouseX, mouseY)) {
            info.setReturnValue(false);
        }
    }

    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;mouseClicked(DDI)Z"), method = "Lnet/minecraft/client/gui/screen/ingame/RecipeBookScreen;mouseClicked(DDI)Z")
    private boolean overrideRecipeBookClick(RecipeBookWidget<?> instance, double mouseX, double mouseY, int button, Operation<Boolean> original) {
        if (TrinketScreenManager.isClickInsideTrinketBounds(mouseX, mouseY)) {
            return false;
        }
        return original.call(instance, mouseX, mouseY, button);
    }
}
