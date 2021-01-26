package dev.emi.trinkets.mixin;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.Trinket.SlotReference;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trinket dropping on death, trinket EAMs, and trinket equip/unequip calls
 *
 * @author Emi
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Unique
	private final Map<Integer, ItemStack> lastEquippedTrinkets = new HashMap<>();
	
	@Shadow
	public abstract AttributeContainer getAttributes();

	protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(at = @At("TAIL"), method = "dropInventory")
	public void dropInventory(CallbackInfo info) {
		boolean keepInv = this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
		LivingEntity entity = (LivingEntity) (Object) this;

		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> {
			TrinketInventory inv = trinkets.getInventory();

			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				if (stack.isEmpty()) {
					continue;
				}

				Pair<SlotType, Integer> p = inv.posMap.get(i);
				TrinketEnums.DropRule dropRule = TrinketsApi.getTrinket(stack.getItem())
						.map(trinket -> trinket.getDropRule(stack, new Trinket.SlotReference(p.getLeft(), p.getRight()), entity)).orElse(DropRule.DEFAULT);

				if (dropRule == TrinketEnums.DropRule.DEFAULT) {
					dropRule = p.getLeft().getDropRule();
				}

				if (dropRule == TrinketEnums.DropRule.DEFAULT) {
					if (keepInv && this.getType() == EntityType.PLAYER) {
						dropRule = TrinketEnums.DropRule.ALWAYS_KEEP;
					} else {
						if (EnchantmentHelper.hasVanishingCurse(stack)) {
							dropRule = TrinketEnums.DropRule.DESTROY;
						} else {
							dropRule = TrinketEnums.DropRule.ALWAYS_DROP;
						}
					}
				}

				switch (dropRule) {
					case ALWAYS_DROP:
						dropStack(stack);
						// Fallthrough
					case DESTROY:
						inv.setStack(i, ItemStack.EMPTY);
						break;
					default:
						break;
				}
			}
		});
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;
		Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(entity);

		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			TrinketInventory inv = comp.getInventory();

			for (int i = 0; i < inv.size(); i++) {
				ItemStack oldStack = getOldStack(i);
				ItemStack newStack = inv.getStack(i);

				if (!ItemStack.areEqual(newStack, oldStack)) {
					Pair<SlotType, Integer> pair = inv.posMap.get(i);
					SlotReference ref = new SlotReference(pair.getLeft(), pair.getRight());

					if (!this.world.isClient) {
						UUID uuid = UUID.nameUUIDFromBytes((ref.index + ref.slot.getName() + ref.slot.getGroup()).getBytes());

						if (!oldStack.isEmpty()) {
							Optional<Trinket> trinket = TrinketsApi.getTrinket(oldStack.getItem());
							trinket.ifPresent(value -> this.getAttributes().removeModifiers(value.getModifiers(oldStack, ref, entity, uuid)));
						}

						if (!newStack.isEmpty()) {
							Optional<Trinket> trinket = TrinketsApi.getTrinket(newStack.getItem());
							trinket.ifPresent(value -> this.getAttributes().addTemporaryModifiers(value.getModifiers(newStack, ref, entity, uuid)));
						}
					}
					lastEquippedTrinkets.put(i, newStack.copy());

					if (!newStack.isItemEqual(oldStack)) {
						TrinketsApi.getTrinket(oldStack.getItem()).ifPresent(trinket -> trinket.onUnequip(oldStack, ref, entity));
						TrinketsApi.getTrinket(newStack.getItem()).ifPresent(trinket -> trinket.onEquip(newStack, ref, entity));
					}
				}
			}
		}
	}

	@Unique
	private ItemStack getOldStack(int i) {
		if (lastEquippedTrinkets.containsKey(i)) {
			ItemStack stack = lastEquippedTrinkets.get(i);
			if (stack != null) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}
}
