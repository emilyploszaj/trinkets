package dev.emi.trinkets;

import com.mojang.blaze3d.platform.GlStateManager;

import dev.emi.trinkets.api.ITrinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.item.ItemStack;

public class TrinketFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> featureContext;
	public TrinketFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> featureRendererContext_1) {
		super(featureRendererContext_1);
		featureContext = featureRendererContext_1;
	}
	@Override
	public boolean hasHurtOverlay() {
		return false;
	}
	@Override
	public void render(AbstractClientPlayerEntity var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8){
		TrinketComponent comp = TrinketsApi.getTrinketComponent(var1);
		for(int i = 0; i < comp.getInventory().getInvSize(); i++){
			GlStateManager.pushMatrix();
			ItemStack stack = comp.getInventory().getInvStack(i);
			if(stack.getItem() instanceof ITrinket){
				((ITrinket) stack.getItem()).render(featureContext.getModel(), var1, var6, var7);
			}
			GlStateManager.popMatrix();
		}
	}
}