package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> entityType_1, World world_1) {
		super(entityType_1, world_1);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"), method = "initAi")
	public ItemStack getEquippedStackProxy(LivingEntity entity, EquipmentSlot slot){
		if(entity instanceof PlayerEntity){
			PlayerEntity player = (PlayerEntity) entity;
			TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
			return comp.getStack("chest:cape");
		}else{
			return entity.getEquippedStack(slot);
		}
	}
}