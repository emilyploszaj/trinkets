package dev.emi.trinkets.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	
	@Invoker("playEquipmentBreakEffects")
	public void invokePlayEquipmentBreakEffects(ItemStack stack);
}
