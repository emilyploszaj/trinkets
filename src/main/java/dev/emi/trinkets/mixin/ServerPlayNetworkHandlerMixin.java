package dev.emi.trinkets.mixin;

import dev.emi.trinkets.api.SlotGroups;
import dev.emi.trinkets.api.Slots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Makes the elytra check for flight use the trinkets chest:cape slot and modifies checked value for out of bounds creative slot indices
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements ServerPlayPacketListener {

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"), method = "onClientCommand")
	public ItemStack getEquippedStackProxy(ServerPlayerEntity player, EquipmentSlot slot) {
		TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
		return comp.getStack(SlotGroups.CHEST, Slots.CAPE);
	}

	@ModifyConstant(method = "onCreativeInventoryAction", constant = @Constant(intValue = 45))
	public int modifyCreativeSlotMax(int value) {
		return value + TrinketSlots.getSlotCount();
	}
}