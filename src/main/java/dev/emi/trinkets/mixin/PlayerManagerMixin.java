package dev.emi.trinkets.mixin;


import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.data.EntitySlotLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.emi.trinkets.payload.SyncInventoryPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Syncs slot data to player's client on login
 *
 * @author C4
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V"), method = "onPlayerConnect")
	private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
		EntitySlotLoader.SERVER.sync(player);
		((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(false);
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
			var tag = new HashMap<String, NbtCompound>();
			Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

			for (TrinketInventory trinketInventory : inventoriesToSend) {
				tag.put(trinketInventory.getSlotType().getId(), trinketInventory.getSyncTag());
			}
			ServerPlayNetworking.send(player, new SyncInventoryPayload(player.getId(), Map.of(), tag));
			inventoriesToSend.clear();
		});
	}
}
