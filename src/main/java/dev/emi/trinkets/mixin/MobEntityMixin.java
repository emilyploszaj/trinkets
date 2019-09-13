package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin{
	@Inject(at = @At("HEAD"), method = "getPreferredEquipmentSlot", cancellable = true)
	private static void getPreferredEquipmentSlot(ItemStack itemStack_1, CallbackInfoReturnable<EquipmentSlot> info){
		if(itemStack_1.getItem() == Items.ELYTRA) info.setReturnValue(EquipmentSlot.MAINHAND);
	}
}