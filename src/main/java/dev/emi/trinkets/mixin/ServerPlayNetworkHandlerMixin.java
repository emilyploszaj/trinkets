package dev.emi.trinkets.mixin;

import dev.emi.trinkets.api.SlotGroups;
import dev.emi.trinkets.api.Slots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements ServerPlayPacketListener{
	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"), method = "onClientCommand")
	public ItemStack getEquippedStackProxy(ServerPlayerEntity player, EquipmentSlot slot){
		TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
		return comp.getStack(SlotGroups.CHEST, Slots.CAPE);
	}
}