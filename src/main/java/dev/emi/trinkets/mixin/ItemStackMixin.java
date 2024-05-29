package dev.emi.trinkets.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds a tooltip for trinkets describing slots and attributes
 * 
 * @author Emi
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {


	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;appendAttributeModifiersTooltip(Ljava/util/function/Consumer;Lnet/minecraft/entity/player/PlayerEntity;)V", shift = Shift.BEFORE), method = "getTooltip")
	private void getTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType tooltipType, CallbackInfoReturnable<List<Text>> info, @Local(ordinal = 0) List<Text> list) {
		TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
			ItemStack self = (ItemStack) (Object) this;
			boolean canEquipAnywhere = true;
			Set<SlotType> slots = Sets.newHashSet();
			Map<SlotType, Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> modifiers = Maps.newHashMap();
			Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> defaultModifier = null;
			boolean allModifiersSame = true;
			int slotCount = 0;

			for (Map.Entry<String, Map<String, TrinketInventory>> group : comp.getInventory().entrySet()) {
				outer:
				for (Map.Entry<String, TrinketInventory> inventory : group.getValue().entrySet()) {
					TrinketInventory trinketInventory = inventory.getValue();
					SlotType slotType = trinketInventory.getSlotType();
					slotCount++;
					boolean anywhereButHidden = false;
					for (int i = 0; i < trinketInventory.size(); i++) {
						SlotReference ref = new SlotReference(trinketInventory, i);
						boolean res = TrinketsApi.evaluatePredicateSet(slotType.getTooltipPredicates(), self, ref, player);
						boolean canInsert = TrinketSlot.canInsert(self, ref, player);
						if (res && canInsert) {
							boolean sameTranslationExists = false;
							for (SlotType t : slots) {
								if (t.getTranslation().getString().equals(slotType.getTranslation().getString())) {
									sameTranslationExists = true;
									break;
								}
							}
							if (!sameTranslationExists) {
								slots.add(slotType);
							}
							Trinket trinket = TrinketsApi.getTrinket((self).getItem());

							Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map =
									trinket.getModifiers(self, ref, player, SlotAttributes.getIdentifier(ref));
							
							if (defaultModifier == null) {
								defaultModifier = map;
							} else if (allModifiersSame) {
								allModifiersSame = areMapsEqual(defaultModifier, map);
							}

							boolean duplicate = false;
							for (Map.Entry<SlotType, Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> entry : modifiers.entrySet()) {
								if (entry.getKey().getTranslation().getString().equals(slotType.getTranslation().getString())) {
									if (areMapsEqual(entry.getValue(), map)) {
										duplicate = true;
										break;
									}
								}
							}

							if (!duplicate) {
								modifiers.put(slotType, map);
							}
							continue outer;
						} else if (canInsert) {
							anywhereButHidden = true;
						}
					}
					if (!anywhereButHidden) {
						canEquipAnywhere = false;
					}
				}
			}

			if (!self.contains(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP)) {
				if (canEquipAnywhere && slotCount > 1) {
					list.add(Text.translatable("trinkets.tooltip.slots.any").formatted(Formatting.GRAY));
				} else if (slots.size() > 1) {
					list.add(Text.translatable("trinkets.tooltip.slots.list").formatted(Formatting.GRAY));
					for (SlotType type : slots) {
						list.add(type.getTranslation().formatted(Formatting.BLUE));
					}
				} else if (slots.size() == 1) {
					// Should only run once
					for (SlotType type : slots) {
						list.add(Text.translatable("trinkets.tooltip.slots.single",
								type.getTranslation().formatted(Formatting.BLUE)).formatted(Formatting.GRAY));
					}
				}
			}

			if (!modifiers.isEmpty() && self.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT).showInTooltip()) {
				if (allModifiersSame) {
					if (defaultModifier != null && !defaultModifier.isEmpty()) {
						list.add(Text.translatable("trinkets.tooltip.attributes.all").formatted(Formatting.GRAY));
						addAttributes(list, defaultModifier);
					}
				} else {
					for (SlotType type : modifiers.keySet()) {
						list.add(Text.translatable("trinkets.tooltip.attributes.single",
								type.getTranslation().formatted(Formatting.BLUE)).formatted(Formatting.GRAY));
						addAttributes(list, modifiers.get(type));
					}
				}
			}
		});
	}

	@Unique
	private void addAttributes(List<Text> list, Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map) {
		if (!map.isEmpty()) {
			for (Map.Entry<RegistryEntry<EntityAttribute>, EntityAttributeModifier> entry : map.entries()) {
				RegistryEntry<EntityAttribute> attribute = entry.getKey();
				EntityAttributeModifier modifier = entry.getValue();
				double g = modifier.value();

				if (modifier.operation() != EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE && modifier.operation() != EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
					if (entry.getKey().equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) {
						g *= 10.0D;
					}
				} else {
					g *= 100.0D;
				}

				Text text = Text.translatable(attribute.value().getTranslationKey());
				if (attribute.hasKeyAndValue() && attribute.value() instanceof SlotAttributes.SlotEntityAttribute) {
					text = Text.translatable("trinkets.tooltip.attributes.slots", text);
				}
				if (g > 0.0D) {
					list.add(Text.translatable("attribute.modifier.plus." + modifier.operation().getId(),
							AttributeModifiersComponent.DECIMAL_FORMAT.format(g), text).formatted(Formatting.BLUE));
				} else if (g < 0.0D) {
					g *= -1.0D;
					list.add(Text.translatable("attribute.modifier.take." + modifier.operation().getId(),
							AttributeModifiersComponent.DECIMAL_FORMAT.format(g), text).formatted(Formatting.RED));
				}
			}
		}
	}

	// `equals` doesn't test thoroughly
	@Unique
	private boolean areMapsEqual(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map1, Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map2) {
		if (map1.size() != map2.size()) {
			return false;
		} else {
			for (RegistryEntry<EntityAttribute> attribute : map1.keySet()) {
				if (!map2.containsKey(attribute)) {
					return false;
				}

				Collection<EntityAttributeModifier> col1 = map1.get(attribute);
				Collection<EntityAttributeModifier> col2 = map2.get(attribute);

				if (col1.size() != col2.size()) {
					return false;
				} else {
					Iterator<EntityAttributeModifier> iter = col2.iterator();

					for (EntityAttributeModifier modifier : col1) {
						EntityAttributeModifier eam = iter.next();

						if (!modifier.toNbt().equals(eam.toNbt())) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
}
