package dev.emi.trinkets.extern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Two different mods (and potentially more) want to mess with how mending items are picked, and so does trinkets
 * As a solution, trinkets' implementation is put in an open file that can have other mixins applied to it
 */
public class TrinketsMending {
	
	public static Entry<EquipmentSlot, ItemStack> chooseEquipmentWith(Enchantment ench, LivingEntity entity, Predicate<ItemStack> condition) {
		List<Entry<EquipmentSlot, ItemStack>> list = getAllValidEquipment(ench, entity, condition);

		// Pick one at random
		return list.isEmpty() ? null : list.get(entity.getRandom().nextInt(list.size()));
	}

	public static List<Entry<EquipmentSlot, ItemStack>> getAllValidEquipment(Enchantment ench, LivingEntity entity, Predicate<ItemStack> condition) {
		Map<EquipmentSlot, ItemStack> map = ench.getEquipment(entity);
		// Trinkets will all be in the EquipmentSlot.MAINHAND since there really is no other option
		List<Entry<EquipmentSlot, ItemStack>> list = new ArrayList<Entry<EquipmentSlot, ItemStack>>();
		
		// Add all damaged equipment with mending to the list
		Iterator<Entry<EquipmentSlot, ItemStack>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<EquipmentSlot, ItemStack> entry = (Entry<EquipmentSlot, ItemStack>) iterator.next();
			ItemStack stack = (ItemStack) entry.getValue();
			if (!stack.isEmpty() && EnchantmentHelper.getLevel(ench, stack) > 0 && condition.test(stack)) {
				list.add(entry);
			}
		}
		
		// Add all damaged trinkets with mending to the list
		if (entity instanceof PlayerEntity) { // Should be
			Map<EquipmentSlot, ItemStack> dummy = new HashMap<EquipmentSlot, ItemStack>();
			TrinketComponent comp = TrinketsApi.getTrinketComponent((PlayerEntity) entity);
			for (int i = 0; i < comp.getInventory().size(); i++) {
				ItemStack stack = comp.getInventory().getStack(i);
				if (!stack.isEmpty() && EnchantmentHelper.getLevel(ench, stack) > 0 && condition.test(stack)) {
					dummy.put(EquipmentSlot.MAINHAND, stack);
					list.add(dummy.entrySet().iterator().next());
				}
			}
		}

		return list;
	}
}