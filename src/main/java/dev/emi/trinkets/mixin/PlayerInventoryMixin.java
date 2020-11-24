package dev.emi.trinkets.mixin;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ticks trinkets
 *
 * @author Emi
 */
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

	@Shadow
	@Final
	public PlayerEntity player;

	@Inject(at = @At("TAIL"), method = "updateItems")
	public void updateItems(CallbackInfo info) {
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
			TrinketInventory inv = trinkets.getInventory();

			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				Pair<SlotType, Integer> p = inv.posMap.get(i);
				TrinketsApi.getTrinket(stack.getItem()).ifPresent(trinket -> trinket.tick(stack, new Trinket.SlotReference(p.getLeft(), p.getRight()), player));
			}
		});
	}
}
