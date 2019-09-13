package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M>{
	public ElytraFeatureRendererMixin(FeatureRendererContext<T, M> featureRendererContext_1) {
		super(featureRendererContext_1);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"), method = "method_17161")
	public ItemStack getEquippedStackProxy(T entity, EquipmentSlot slot){
		if(entity instanceof PlayerEntity){
			PlayerEntity player = (PlayerEntity) entity;
			TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
			return comp.getStack("chest:cape");
		}else{
			return entity.getEquippedStack(slot);
		}
	}
}