package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

/**
 * Ticks trinkets
 * 
 * @author Emi
 */
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
	@Shadow @Final
	public PlayerEntity player;
	
	@Inject(at = @At("TAIL"), method = "updateItems")
	public void updateItems(CallbackInfo info) {
		TrinketInventory inv = TrinketsMain.TRINKETS.get(player).getInventory();
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			if (TrinketsApi.hasTrinket(stack.getItem())) {
				Pair<SlotType, Integer> p = inv.posMap.get(i);
				TrinketsApi.getTrinket(stack.getItem()).tick(stack, new Trinket.SlotReference(p.getLeft(), p.getRight()), player);
			}
		}
	}
}
