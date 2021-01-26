package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

public class TrinketFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

	private final FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context;

	public TrinketFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
		super(context);
		this.context = context;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumer, int light, AbstractClientPlayerEntity player, float limbAngle,
			float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
			TrinketInventory inv = trinkets.getInventory();

			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				Pair<SlotType, Integer> p = inv.posMap.get(i);
				TrinketRendererRegistry.getRenderer(stack.getItem()).ifPresent(renderer -> {
					matrices.push();
					renderer.render(stack, new Trinket.SlotReference(p.getLeft(), p.getRight()), matrices, vertexConsumer, light, context.getModel(), player,
							limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
					matrices.pop();
				});
			}
		});
	}
}
