package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Say that the elytra does not go into the chest slot
 */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin{

	@Inject(at = @At("HEAD"), method = "getPreferredEquipmentSlot", cancellable = true)
	private static void getPreferredEquipmentSlot(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> info) { 
		if(stack.getItem() == Items.ELYTRA) info.setReturnValue(EquipmentSlot.MAINHAND);
	}
}