package dev.emi.trinkets.mixin;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.mojang.datafixers.util.Function3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.Trinket.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;

/**
 * Adds a tooltip for trinkets describing slots and attributes
 * 
 * @author Emi
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

	// Couldn't shadow for some reason
	private static final DecimalFormat FORMAT = Util.make(new DecimalFormat("#.##"), (decimalFormat) -> {
		decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
	});
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isSectionHidden(ILnet/minecraft/item/ItemStack$TooltipSection;)Z",
		ordinal = 3, shift = Shift.BEFORE), method = "getTooltip", locals = LocalCapture.CAPTURE_FAILHARD)
	private void getTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> info, List<Text> list) {
		TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
			TrinketInventory inv = comp.getInventory();
			Set<SlotType> slots = Sets.newHashSet();
			UUID uuid = UUID.randomUUID();
			Map<SlotType, Multimap<EntityAttribute, EntityAttributeModifier>> attributes = Maps.newHashMap();
			Multimap<EntityAttribute, EntityAttributeModifier> defaultAttribute = null;
			boolean allAttributesSame = true;
			int slotCount = 0;
			for (int i = 0; i < inv.size(); i++) {
				Pair<SlotType, Integer> pair = inv.posMap.get(i);
				if (canInsert(player, pair.getLeft(), pair.getRight(), (ItemStack) (Object) this)) {
					slots.add(pair.getLeft());
					Optional<Trinket> optional = TrinketsApi.getTrinket(((ItemStack) (Object) this).getItem());
					if (optional.isPresent()) {
						Trinket trinket = optional.get();
						Multimap<EntityAttribute, EntityAttributeModifier> map =
							trinket.getModifiers((ItemStack) (Object) this, new SlotReference(pair.getLeft(), pair.getRight()), player, uuid);
						if (defaultAttribute == null) {
							defaultAttribute = map;
						} else if (allAttributesSame) {
							allAttributesSame = areMapsEqual(defaultAttribute, map);
						}
						attributes.put(pair.getLeft(), map);
					}
				}
				if (pair.getRight() == 0) {
					slotCount++;
				}
			}
			if (slots.size() == slotCount && slotCount > 1) {
				list.add(new TranslatableText("trinkets.tooltip.slots.any").formatted(Formatting.GRAY));
			} else if (slots.size() > 1) {
				list.add(new TranslatableText("trinkets.tooltip.slots.list").formatted(Formatting.GRAY));
				for (SlotType type : slots) {
					list.add(new TranslatableText(getTranslationKey(type)).formatted(Formatting.BLUE));
				}
			} else if (slots.size() == 1) {
				// Should only run once
				for (SlotType type : slots) {
					list.add(new TranslatableText("trinkets.tooltip.slots.single",
					new TranslatableText(getTranslationKey(type)).formatted(Formatting.BLUE)).formatted(Formatting.GRAY));
				}
			}
			if (attributes.size() > 0) {
				if (allAttributesSame) {
					list.add(new TranslatableText("trinkets.tooltip.attributes.all").formatted(Formatting.GRAY));
					addAttributes(list, defaultAttribute);
				} else {
					for (SlotType type : attributes.keySet()) {
						if (attributes.get(type).isEmpty()) continue;
						list.add(new TranslatableText("trinkets.tooltip.attributes.single",
							new TranslatableText(getTranslationKey(type)).formatted(Formatting.BLUE)).formatted(Formatting.GRAY));
						addAttributes(list, attributes.get(type));
					}
				}
			}
		});
	}

	// `equals` doesn't test thoroughly
	private void addAttributes(List<Text> list, Multimap<EntityAttribute, EntityAttributeModifier> map) {
		if (!map.isEmpty()) {
			for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : map.entries()) {
				EntityAttributeModifier modifier = entry.getValue();
				double g = modifier.getValue();
				if (modifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_BASE && modifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
					if (entry.getKey().equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) {
						g *= 10.0D;
					}
				} else {
					g *= 100.0D;
				}
				if (g > 0.0D) {
					list.add(new TranslatableText("attribute.modifier.plus." + modifier.getOperation().getId(), new Object[]{FORMAT.format(g), new TranslatableText(entry.getKey().getTranslationKey())}).formatted(Formatting.BLUE));
				} else if (g < 0.0D) {
					g *= -1.0D;
					list.add(new TranslatableText("attribute.modifier.take." + modifier.getOperation().getId(), new Object[]{FORMAT.format(g), new TranslatableText(entry.getKey().getTranslationKey())}).formatted(Formatting.RED));
				}
			}
		}
	}

	private String getTranslationKey(SlotType type) {
		return "trinkets.slot." + type.getGroup() + "." + type.getName();
	}

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
						if (!modifier.toTag().equals(eam.toTag())) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private boolean canInsert(PlayerEntity player, SlotType type, int off, ItemStack stack) {
		SlotReference reference = new SlotReference(type, off);
		TriState state = TriState.DEFAULT;
		for (Identifier id : type.getValidators()) {
			Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> function = TrinketsApi.getValidatorPredicate(id);
			if (function.isPresent()) {
				state = function.get().apply(stack, reference, player);
			}
			if (state != TriState.DEFAULT) {
				break;
			}
		}
		if (state == TriState.DEFAULT) {
			state = TrinketsApi.getValidatorPredicate(new Identifier("trinkets", "tag")).get().apply(stack, reference, player);
		}
		if (state.get()) {
			Optional<Trinket> trinket = TrinketsApi.getTrinket(stack.getItem());
			if (trinket.isPresent()) {
				return trinket.get().canEquip(stack, reference, player);
			} else {
				return true;
			}
		}
		return false;
	}
}
