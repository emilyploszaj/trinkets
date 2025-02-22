package dev.emi.trinkets.mixin;

import dev.emi.trinkets.TrinketEntityRenderState;
import dev.emi.trinkets.TrinketFeatureRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the trinket feature renderer to the list of living entity features
 *
 * @author C4
 * @author powerboat9
 */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Invoker("addFeature")
    public abstract boolean invokeAddFeature(FeatureRenderer<?, ?> feature);

	@Inject(at = @At("RETURN"), method = "<init>")
	public void init(EntityRendererFactory.Context ctx, EntityModel<?> model, float shadowRadius, CallbackInfo info) {
        this.invokeAddFeature(new TrinketFeatureRenderer<>((FeatureRendererContext) (Object) this));
	}

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void updateTrinketsRenderState(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f, CallbackInfo ci) {
        var state = (TrinketEntityRenderState) livingEntityRenderState;
        TrinketFeatureRenderer.update(livingEntity, livingEntityRenderState, f, state);
    }
}
