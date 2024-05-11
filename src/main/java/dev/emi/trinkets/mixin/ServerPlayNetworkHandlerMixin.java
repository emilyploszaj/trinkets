package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Allows mutation of trinket slots (and all other valid slots) in the creative inventory
 * 
 * @author Emi
 */
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;
	
	@ModifyConstant(method = "onCreativeInventoryAction", constant = @Constant(intValue = 45))
	public int modifyCreativeSlotMax(int value) {
		return player.playerScreenHandler.slots.size();
	}
}
