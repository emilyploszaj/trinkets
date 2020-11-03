package dev.emi.trinkets;

import java.util.List;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class TrinketFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context;

	public TrinketFeatureRenderer(
			FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
		super(context);
		this.context = context;
	}

	@Override
	public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumer, int light, AbstractClientPlayerEntity player, float a, float b, float c, float d, float headYaw, float headPitch) {
		TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
		List<String> names = TrinketSlots.getAllSlotNames();
		for (int i = 0; i < comp.getInventory().size(); i++) {
			matrixStack.push();
			ItemStack stack = comp.getInventory().getStack(i);
			if (stack.getItem() instanceof Trinket) {
				((Trinket) stack.getItem()).render(names.get(i), matrixStack, vertexConsumer, light, context.getModel(), player, headYaw, headPitch);
			}
			matrixStack.pop();
		}
	}
}
