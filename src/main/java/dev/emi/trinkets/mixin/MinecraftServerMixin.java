package dev.emi.trinkets.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.ITrinket;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;

/**
 * Tick handling for trinkets
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Shadow
	private PlayerManager playerManager;

	@Inject(at = @At("TAIL"), method = "tick")
	protected void tick(BooleanSupplier supplier, CallbackInfo info) {
		for (PlayerEntity p: playerManager.getPlayerList()) {
			Inventory inv = TrinketsApi.getTrinketsInventory(p);
			for (int i = 0; i < inv.getInvSize(); i++) {
				ItemStack stack = inv.getInvStack(i);
				if (stack.getItem() instanceof ITrinket) {
					ITrinket trinket = (ITrinket) stack.getItem();
					trinket.tick(p, stack);
				}
			}
		}
	}
}