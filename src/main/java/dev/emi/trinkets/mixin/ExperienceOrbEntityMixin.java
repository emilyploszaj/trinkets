package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Takes xp and puts it into mending trinkets. (Disabled when SmarterMending is loaded)
 */
@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {

	public ExperienceOrbEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"), method = "onPlayerCollision")
	private Entry<EquipmentSlot, ItemStack> chooseEquipmentWith(Enchantment ench, LivingEntity entity, Predicate<ItemStack> condition) {
		Map<EquipmentSlot, ItemStack> map = ench.getEquipment(entity);
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		if (entity instanceof PlayerEntity) {//Should be
			TrinketComponent comp = TrinketsApi.getTrinketComponent((PlayerEntity) entity);
			for (int i = 0; i < comp.getInventory().size(); i++) {
				ItemStack stack = comp.getInventory().getStack(i);
				if (!stack.isEmpty() && EnchantmentHelper.getLevel(ench, stack) > 0) {
					stacks.add(stack);
				}
			}
		}
		if (map.isEmpty()) {
			return null;
		} else {
			List<Entry<EquipmentSlot, ItemStack>> list = new ArrayList<Entry<EquipmentSlot, ItemStack>>();
			Iterator<Entry<EquipmentSlot, ItemStack>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<EquipmentSlot, ItemStack> entry = (Entry<EquipmentSlot, ItemStack>) iterator.next();
				ItemStack stack = (ItemStack) entry.getValue();
				if (!stack.isEmpty() && EnchantmentHelper.getLevel(ench, stack) > 0) {
					list.add(entry);
				}
			}
			if (list.size() + stacks.size() == 0) return null;
			int i = entity.getRandom().nextInt(list.size() + stacks.size());
			if (i < list.size()) {
				return list.get(i);
			} else {
				i -= list.size();
				map.put(EquipmentSlot.MAINHAND, stacks.get(i));
				iterator = map.entrySet().iterator();
				while (iterator.hasNext() ){
					Entry<EquipmentSlot, ItemStack> entry = (Entry<EquipmentSlot, ItemStack>) iterator.next();
					if (entry.getKey() == EquipmentSlot.MAINHAND) return entry;
				}
			}
		}
		return null;
	}
}