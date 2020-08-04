package dev.emi.trinkets.mixin;

import dev.emi.trinkets.api.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Changes dispenser and right click equipping to use trinket slots
 */
@Mixin(ElytraItem.class)
public abstract class ElytraItemMixin implements Trinket {

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/block/DispenserBlock;registerBehavior(Lnet/minecraft/item/ItemConvertible;Lnet/minecraft/block/dispenser/DispenserBehavior;)V"), method = "<init>")
	private void registerBehaviorProxy(ItemConvertible item, DispenserBehavior behavior) {
		DispenserBlock.registerBehavior(item, TrinketItem.TRINKET_DISPENSER_BEHAVIOR);
	}

	@Inject(at = @At("HEAD"), method = "use", cancellable = true)
	public void use(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> info) {
		ItemStack stack = player.getStackInHand(hand);
		TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
		if (comp.equip(stack)) {
			stack.setCount(0);
			info.setReturnValue(new TypedActionResult<ItemStack>(ActionResult.SUCCESS, stack));
		} else {
			info.setReturnValue(new TypedActionResult<ItemStack>(ActionResult.FAIL, stack));
		}
	 }

	public boolean canWearInSlot(String group, String slot) {
		return group.equals(SlotGroups.CHEST) && slot.equals(Slots.CAPE);
	}
}