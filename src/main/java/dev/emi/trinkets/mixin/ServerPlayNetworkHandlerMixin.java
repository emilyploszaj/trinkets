package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import dev.emi.trinkets.api.TrinketSlots;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;

/**
 * Modifies checked value for out of bounds creative slot indices
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements ServerPlayPacketListener {

	@ModifyConstant(method = "onCreativeInventoryAction", constant = @Constant(intValue = 45))
	public int modifyCreativeSlotMax(int value) {
		return value + TrinketSlots.getSlotCount();
	}
}