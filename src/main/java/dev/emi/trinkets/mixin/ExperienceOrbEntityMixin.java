package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

@Mixin(priority = 999, value = ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {
	public ExperienceOrbEntityMixin(EntityType<?> entityType_1, World world_1) {
		super(entityType_1, world_1);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getRandomEnchantedEquipment(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;)Ljava/util/Map$Entry;"), method = "onPlayerCollision")
	private Entry<EquipmentSlot, ItemStack> getRandomEnchantedEquipment(Enchantment ench, LivingEntity entity){
		Map<EquipmentSlot, ItemStack> map = ench.getEquipment(entity);
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		if(entity instanceof PlayerEntity){//Should be
			TrinketComponent comp = TrinketsApi.getTrinketComponent((PlayerEntity) entity);
			for(int i = 0; i < comp.getInventory().getInvSize(); i++){
				ItemStack stack = comp.getInventory().getInvStack(i);
				if(!stack.isEmpty() && EnchantmentHelper.getLevel(ench, stack) > 0){
					stacks.add(stack);
				}
			}
		}
		if(map.isEmpty()){
			return null;
		}else{
			List<Entry<EquipmentSlot, ItemStack>> list = new ArrayList<Entry<EquipmentSlot, ItemStack>>();
			Iterator<Entry<EquipmentSlot, ItemStack>> var4 = map.entrySet().iterator();
			while(var4.hasNext()){
				Entry<EquipmentSlot, ItemStack> entry = (Entry<EquipmentSlot, ItemStack>) var4.next();
				ItemStack stack = (ItemStack) entry.getValue();
				if(!stack.isEmpty() && EnchantmentHelper.getLevel(ench, stack) > 0){
					list.add(entry);
				}
			}
			if(list.size() + stacks.size() == 0) return null;
			int i = entity.getRand().nextInt(list.size() + stacks.size());
			if(i < list.size()){
				return list.get(i);
			}else{
				i -= list.size();
				map.put(EquipmentSlot.MAINHAND, stacks.get(i));
				var4 = map.entrySet().iterator();
				while(var4.hasNext()){
					Entry<EquipmentSlot, ItemStack> entry = (Entry<EquipmentSlot, ItemStack>) var4.next();
					if(entry.getKey() == EquipmentSlot.MAINHAND) return entry;
				}
			}
		}
		return null;
	}
}