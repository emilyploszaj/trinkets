package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

public class TrinketFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

	public TrinketFeatureRenderer(FeatureRendererContext<T, M> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		TrinketsApi.getTrinketComponent(entity).ifPresent(component -> {
			TrinketInventory inv = component.getInventory();

			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				Pair<SlotType, Integer> p = inv.posMap.get(i);

				TrinketRendererRegistry.getRenderer(stack.getItem()).ifPresent(renderer -> {
					matrices.push();
					renderer.render(stack, new Trinket.SlotReference(p.getLeft(), p.getRight()), this.getContextModel(),
							matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta,
							animationProgress, headYaw, headPitch);
					matrices.pop();
				});
			}
		});
	}
}
