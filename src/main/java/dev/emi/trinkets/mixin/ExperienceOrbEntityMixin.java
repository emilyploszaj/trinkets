package dev.emi.trinkets.mixin;

import java.util.Map.Entry;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.extern.TrinketsMending;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Takes xp and puts it into mending trinkets.
 */
@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {

	public ExperienceOrbEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"), method = "onPlayerCollision")
	private Entry<EquipmentSlot, ItemStack> chooseEquipmentWith(Enchantment ench, LivingEntity entity, Predicate<ItemStack> condition) {
		return TrinketsMending.chooseEquipmentWith(ench, entity, condition);
	}
}