package dev.emi.trinkets.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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
public abstract class PlayerInventoryMixin {

	@Shadow
	@Final
	private PlayerEntity player;

	@Inject(at = @At("TAIL"), method = "updateItems")
	private void updateItems(CallbackInfo info) {
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets ->
				trinkets.forEach((slotReference, itemStack) ->
						TrinketsApi.getTrinket(itemStack.getItem()).tick(itemStack, slotReference, player)));
	}
}
