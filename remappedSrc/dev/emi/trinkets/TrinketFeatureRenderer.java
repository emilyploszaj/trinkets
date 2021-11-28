package dev.emi.trinkets;

import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

public class TrinketFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

	public TrinketFeatureRenderer(FeatureRendererContext<T, M> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		TrinketsApi.getTrinketComponent(entity).ifPresent(component ->
				component.forEach((slotReference, stack) ->
						TrinketRendererRegistry.getRenderer(stack.getItem()).ifPresent(renderer -> {
							matrices.push();
							renderer.render(stack, slotReference, this.getContextModel(), matrices, vertexConsumers,
									light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
							matrices.pop();
						})
				)
		);
	}
}
