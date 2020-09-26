package dev.emi.trinkets.mixin;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trinket dropping on death
 *
 * @author Emi
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

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
}
