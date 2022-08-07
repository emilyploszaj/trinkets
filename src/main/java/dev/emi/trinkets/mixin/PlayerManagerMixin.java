package dev.emi.trinkets.mixin;


import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.data.EntitySlotLoader;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
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
	private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo cb) {
		EntitySlotLoader.SERVER.sync(player);
		((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(false);
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeInt(player.getId());
			NbtCompound tag = new NbtCompound();
			Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

			for (TrinketInventory trinketInventory : inventoriesToSend) {
				tag.put(trinketInventory.getSlotType().getGroup() + "/" + trinketInventory.getSlotType().getName(), trinketInventory.getSyncTag());
			}
			buf.writeNbt(tag);
			buf.writeNbt(new NbtCompound());
			ServerPlayNetworking.send(player, TrinketsNetwork.SYNC_INVENTORY, buf);
			inventoriesToSend.clear();
		});
	}
}
