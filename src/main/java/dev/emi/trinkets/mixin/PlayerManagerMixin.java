package dev.emi.trinkets.mixin;


import dev.emi.trinkets.data.EntitySlotLoader;
import net.minecraft.network.ClientConnection;
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

	@Inject(at = @At("TAIL"), method = "onPlayerConnect")
	private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo cb) {
		EntitySlotLoader.INSTANCE.sync(player);
	}
}
