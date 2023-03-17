package dev.emi.trinkets.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isSectionVisible(ILnet/minecraft/item/ItemStack$TooltipSection;)Z",
		ordinal = 4, shift = Shift.BEFORE), method = "getTooltip", locals = LocalCapture.CAPTURE_FAILHARD)
	private void getTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> info, List<Text> list) {
		TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
			ItemStack self = (ItemStack) (Object) this;
			boolean canEquipAnywhere = true;
			Set<SlotType> slots = Sets.newHashSet();
			Map<SlotType, Multimap<EntityAttribute, EntityAttributeModifier>> modifiers = Maps.newHashMap();
			Multimap<EntityAttribute, EntityAttributeModifier> defaultModifier = null;
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

							Multimap<EntityAttribute, EntityAttributeModifier> map =
									trinket.getModifiers(self, ref, player, SlotAttributes.getUuid(ref));
							
							if (defaultModifier == null) {
								defaultModifier = map;
							} else if (allModifiersSame) {
								allModifiersSame = areMapsEqual(defaultModifier, map);
							}

							boolean duplicate = false;
							for (var entry : modifiers.entrySet()) {
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

			if (modifiers.size() > 0) {
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
	private void addAttributes(List<Text> list, Multimap<EntityAttribute, EntityAttributeModifier> map) {
		if (!map.isEmpty()) {
			for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : map.entries()) {
				EntityAttribute attribute = entry.getKey();
				EntityAttributeModifier modifier = entry.getValue();
				double g = modifier.getValue();

				if (modifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_BASE && modifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
					if (entry.getKey().equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) {
						g *= 10.0D;
					}
				} else {
					g *= 100.0D;
				}

				Text text = Text.translatable(attribute.getTranslationKey());
				if (attribute instanceof SlotAttributes.SlotEntityAttribute) {
					text = Text.translatable("trinkets.tooltip.attributes.slots", text);
				}
				if (g > 0.0D) {
					list.add(Text.translatable("attribute.modifier.plus." + modifier.getOperation().getId(),
						ItemStack.MODIFIER_FORMAT.format(g), text).formatted(Formatting.BLUE));
				} else if (g < 0.0D) {
					g *= -1.0D;
					list.add(Text.translatable("attribute.modifier.take." + modifier.getOperation().getId(),
						ItemStack.MODIFIER_FORMAT.format(g), text).formatted(Formatting.RED));
				}
			}
		}
	}

	// `equals` doesn't test thoroughly
	@Unique
	private boolean areMapsEqual(Multimap<EntityAttribute, EntityAttributeModifier> map1, Multimap<EntityAttribute, EntityAttributeModifier> map2) {
		if (map1.size() != map2.size()) {
			return false;
		} else {
			for (EntityAttribute attribute : map1.keySet()) {
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
