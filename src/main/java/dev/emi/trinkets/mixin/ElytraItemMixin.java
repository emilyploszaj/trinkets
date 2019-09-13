package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.api.ITrinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
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

@Mixin(ElytraItem.class)
public abstract class ElytraItemMixin{
	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/block/DispenserBlock;registerBehavior(Lnet/minecraft/item/ItemConvertible;Lnet/minecraft/block/dispenser/DispenserBehavior;)V"), method = "<init>")
	private void registerBehaviorProxy(ItemConvertible item, DispenserBehavior behavior){
		DispenserBlock.registerBehavior(item, ITrinket.TRINKET_DISPENSER_BEHAVIOR);
	}
	@Inject(at = @At("HEAD"), method = "use", cancellable = true)
	public void use(World world_1, PlayerEntity playerEntity_1, Hand hand_1, CallbackInfoReturnable<TypedActionResult<ItemStack>> info){
		ItemStack itemStack_1 = playerEntity_1.getStackInHand(hand_1);
		TrinketComponent comp = TrinketsApi.getTrinketComponent(playerEntity_1);
		if(comp.equip(itemStack_1)){
			itemStack_1.setCount(0);
			info.setReturnValue(new TypedActionResult<ItemStack>(ActionResult.SUCCESS, itemStack_1));
		}else{
			info.setReturnValue(new TypedActionResult<ItemStack>(ActionResult.FAIL, itemStack_1));
		}
	 }
}