package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrinketFeatureRenderer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

	public TrinketFeatureRenderer(FeatureRendererContext<T, M> context) {
		super(context);
	}

	public static void update(LivingEntity livingEntity, LivingEntityRenderState entityState, float tickDelta, TrinketEntityRenderState state) {
		Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(livingEntity);
		if (component.isEmpty()) {
			state.trinkets$setState(List.of());
		} else {
			List<Pair<ItemStack, SlotReference>> items = new ArrayList<>();
			component.get().forEach((slotReference, stack) -> items.add(new Pair<>(stack, slotReference)));
			state.trinkets$setState(items);
		}
	}

	@Override
	public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, T state, float limbAngle, float limbDistance) {
		((TrinketEntityRenderState) state).trinkets$getState().forEach(pair -> {
			TrinketRendererRegistry.getRenderer(pair.getLeft().getItem()).ifPresent(renderer -> {
				matrices.push();
				renderer.render(pair.getLeft(), pair.getRight(), this.getContextModel(), matrices, queue,
						light, state, limbAngle, limbDistance);
				matrices.pop();
			});
		});
	}
}
