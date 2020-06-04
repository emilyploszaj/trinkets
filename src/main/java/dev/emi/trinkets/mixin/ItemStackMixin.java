package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import net.minecraft.entity.attribute.EntityAttribute;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.api.TrinketBase;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketSlots.Slot;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

/**
 * Adds attribute and slot text
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	@Inject(at = @At("RETURN"), method = "getTooltip", cancellable = true)
	public void getTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> info) {
		List<Text> list = info.getReturnValue();
		List<Slot> slots = new ArrayList<Slot>();
		List<Pair<Slot, Multimap<EntityAttribute, EntityAttributeModifier>>> eams = Lists.newArrayList();
		UUID uuid = UUID.randomUUID();//Constant UUID for comparison
		for (Slot s : TrinketSlots.getAllSlots()) {
			ItemStack stack = ((ItemStack) (Object) this);
			if (s.canEquip.apply(s, stack)) {
				slots.add(s);
				if (stack.getItem() instanceof TrinketBase) {
					TrinketBase trinket = (TrinketBase) stack.getItem();
					Multimap<EntityAttribute, EntityAttributeModifier> e = trinket.getTrinketModifiers(s.getSlotGroup().getName(), s.getName(), uuid, stack);
					if (e.size() > 0) {
						eams.add(Pair.of(s, e));
					}
				}
			}
		}
		if (slots.size() > 0) {
			if (slots.size() > 1 && slots.size() == TrinketSlots.getAllSlots().size()) {
				list.add((new LiteralText("")));
				list.add((new LiteralText("Equippable in ").formatted(Formatting.GRAY))
					.append(new LiteralText("all ").formatted(Formatting.BLUE))
					.append(new LiteralText("trinket slots").formatted(Formatting.GRAY)));
			} else if (slots.size() == 1) {
				list.add((new LiteralText("")));
				list.add((new LiteralText("Equippable in the ")).formatted(Formatting.GRAY)
					.append(new LiteralText(StringUtils.capitalize(slots.get(0).getName())).formatted(Formatting.BLUE))
					.append(new LiteralText(" trinket slot")));
			} else {
				list.add((new LiteralText("")));
				list.add((new LiteralText("Equippable in trinket slots:")).formatted(Formatting.GRAY));
				for (Slot s : slots) {
					list.add((new LiteralText(StringUtils.capitalize(s.getName())).formatted(Formatting.BLUE)));
				}
			}
		}
		if (eams.size() > 0) {
			Multimap<EntityAttribute, EntityAttributeModifier> base;
			base = eams.get(0).getValue();
			boolean unique = false;
			for (int i = 0; i < eams.size(); i++) {
				if (!eams.get(i).getValue().equals(base)) {
					unique = true;
					break;
				}
			}
			if (!unique && eams.size() == slots.size()) {
				list.add((new LiteralText("When equiped in ")).formatted(Formatting.GRAY)
					.append(new LiteralText("any").formatted(Formatting.BLUE))
					.append(new LiteralText(" trinket slot:")).formatted(Formatting.GRAY));
				Iterator<Map.Entry<EntityAttribute, EntityAttributeModifier>> iterator = base.entries().iterator();
				while(iterator.hasNext()) {
					Map.Entry<EntityAttribute, EntityAttributeModifier> entry = iterator.next();
					EntityAttributeModifier eam = entry.getValue();
					double d = eam.getValue();
					if (eam.getOperation() == EntityAttributeModifier.Operation.MULTIPLY_BASE || eam.getOperation() == EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
						d = d * 100.0D;
					}
					if (d > 0.0D) {
						list.add((new TranslatableText("attribute.modifier.plus." + eam.getOperation().getId(), new Object[]{ItemStack.MODIFIER_FORMAT.format(d), new TranslatableText("attribute.name." + entry.getKey(), new Object[0])})).formatted(Formatting.BLUE));
					} else if (d < 0.0D) {
						d *= -1.0D;
						list.add((new TranslatableText("attribute.modifier.take." + eam.getOperation().getId(), new Object[]{ItemStack.MODIFIER_FORMAT.format(d), new TranslatableText("attribute.name." + entry.getKey(), new Object[0])})).formatted(Formatting.RED));
					}
				}
			} else {
				for (int i = 0; i < eams.size(); i++) {
					list.add((new LiteralText("When equiped in the ")).formatted(Formatting.GRAY)
						.append(new LiteralText(StringUtils.capitalize(eams.get(i).getKey().getName())).formatted(Formatting.BLUE))
						.append(new LiteralText(" trinket slot:")).formatted(Formatting.GRAY));
					Iterator<Map.Entry<EntityAttribute, EntityAttributeModifier>> iterator = eams.get(i).getValue().entries().iterator();
					while(iterator.hasNext()) {
						Map.Entry<EntityAttribute, EntityAttributeModifier> entry = iterator.next();
						EntityAttributeModifier eam = entry.getValue();
						double d = eam.getValue();
						if (eam.getOperation() == EntityAttributeModifier.Operation.MULTIPLY_BASE || eam.getOperation() == EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
							d = d * 100.0D;
						}
						if (d > 0.0D) {
							list.add((new TranslatableText("attribute.modifier.plus." + eam.getOperation().getId(), new Object[]{ItemStack.MODIFIER_FORMAT.format(d), new TranslatableText("attribute.name." + entry.getKey(), new Object[0])})).formatted(Formatting.BLUE));
						} else if (d < 0.0D) {
							d *= -1.0D;
							list.add((new TranslatableText("attribute.modifier.take." + eam.getOperation().getId(), new Object[]{ItemStack.MODIFIER_FORMAT.format(d), new TranslatableText("attribute.name." + entry.getKey(), new Object[0])})).formatted(Formatting.RED));
						}
					}
				}
			}
		}
	 }
}