package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import java.util.Optional;

import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

/**
 * Applies mending to trinkets (fairly)
 * 
 * @author Emi
 */
@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbEntityMixin {
	@Unique
	private PlayerEntity mendingPlayer;
	
	@Inject(at = @At("HEAD"), method = "repairPlayerGears")
	private void repairPlayerGears(PlayerEntity player, int amount, CallbackInfoReturnable<Integer> info) {
		mendingPlayer = player;
	}

	@ModifyVariable(at = @At(value = "INVOKE_ASSIGN", target = "net/minecraft/enchantment/EnchantmentHelper.chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"),
		method = "repairPlayerGears")
	private Entry<EquipmentSlot, ItemStack> modifyEntry(Entry<EquipmentSlot, ItemStack> entry) {
		Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(mendingPlayer);
		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			Predicate<ItemStack> predicate = stack -> !stack.isEmpty() && stack.isDamaged() && EnchantmentHelper.getLevel(Enchantments.MENDING, stack) > 0;
			List<Pair<SlotReference, ItemStack>> list = comp.getEquipped(predicate);
			int totalSize = list.size();

			if (entry != null) {
				Map<EquipmentSlot, ItemStack> map = Enchantments.MENDING.getEquipment(mendingPlayer);
				// The map contains ALL equipped items, so we need to filter for Mending candidates specifically
				ArrayList<Entry<EquipmentSlot, ItemStack>> originalList = new ArrayList<>();
				for (Map.Entry<EquipmentSlot, ItemStack> ent : map.entrySet()) {
					if (predicate.test(ent.getValue())) {
						originalList.add(ent);
					}
				}
				totalSize += originalList.size();
			}

			if (totalSize == 0) {
				return entry;
			}
			int selected = mendingPlayer.getRandom().nextInt(totalSize);
			if (selected < list.size()) {
				Pair<SlotReference, ItemStack> pair = list.get(selected);
				Map<EquipmentSlot, ItemStack> dummyMap = Maps.newHashMap();
				dummyMap.put(EquipmentSlot.MAINHAND, pair.getRight());
				entry = dummyMap.entrySet().iterator().next();
			}
		}
		mendingPlayer = null;
		return entry;
	}
}
