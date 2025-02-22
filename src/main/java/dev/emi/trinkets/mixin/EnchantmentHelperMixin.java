package dev.emi.trinkets.mixin;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.TrinketSlotTarget;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows enchantments to work on trinket items when used in global entity context
 *
 * @author Patbox
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

	@Inject(at = @At("TAIL"), method = "forEachEnchantment(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/enchantment/EnchantmentHelper$ContextAwareConsumer;)V")
	private static void forEachTrinket(LivingEntity entity, EnchantmentHelper.ContextAwareConsumer contextAwareConsumer, CallbackInfo info) {
		Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(entity);
		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			comp.forEach((ref, stack) -> {
				if (!stack.isEmpty()) {
					ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
					if (enchantments != null && !enchantments.isEmpty()) {
						EnchantmentEffectContext context = new EnchantmentEffectContext(stack, null, entity, (item) -> {
							TrinketsApi.onTrinketBroken(stack, ref, entity);
						});

						for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
							RegistryEntry<Enchantment> registryEntry = entry.getKey();
							List<AttributeModifierSlot> slots = registryEntry.value().definition().slots();
							Set<String> trinketSlots = ((TrinketSlotTarget) (Object) registryEntry.value().definition()).trinkets$slots();

							if (slots.contains(AttributeModifierSlot.ANY) || slots.contains(AttributeModifierSlot.ARMOR) || trinketSlots.contains(ref.inventory().getSlotType().getId())) {
								contextAwareConsumer.accept(registryEntry, entry.getIntValue(), context);
							}
						}
					}
				}
			});
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), method = "chooseEquipmentWith")
	private static void addTrinketsAsChoices(ComponentType<?> componentType, LivingEntity entity, Predicate<ItemStack> stackPredicate, CallbackInfoReturnable<Optional<EnchantmentEffectContext>> info, @Local List<EnchantmentEffectContext> list) {
		Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(entity);
		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			comp.forEach((ref, stack) -> {
				if (stackPredicate.test(stack)) {
					ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
					for(Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
						RegistryEntry<Enchantment> registryEntry = entry.getKey();
						List<AttributeModifierSlot> slots = registryEntry.value().definition().slots();
						Set<String> trinketSlots = ((TrinketSlotTarget) (Object) registryEntry.value().definition()).trinkets$slots();

						if (registryEntry.value().effects().contains(componentType)
								&& (slots.contains(AttributeModifierSlot.ANY) || slots.contains(AttributeModifierSlot.ARMOR)
								|| trinketSlots.contains(ref.inventory().getSlotType().getId()))
						) {
							list.add(new EnchantmentEffectContext(stack, null, entity, (item) -> {
								TrinketsApi.onTrinketBroken(stack, ref, entity);
							}));
						}
					}
				}
			});
		}
	}
}