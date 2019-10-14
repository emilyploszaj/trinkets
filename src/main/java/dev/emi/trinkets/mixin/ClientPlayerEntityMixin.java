package dev.emi.trinkets.mixin;

import com.mojang.authlib.GameProfile;

import dev.emi.trinkets.api.SlotGroups;
import dev.emi.trinkets.api.Slots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Clean up logic to not mess with slots if hovering over a group on gui close
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
	public ClientPlayerEntityMixin(ClientWorld clientWorld_1, GameProfile gameProfile_1) {
		super(clientWorld_1, gameProfile_1);
	}
	@Inject(at = @At("TAIL"), method = "closeScreen")
	public void closeScreen(CallbackInfo info){
		TrinketsClient.slotGroup = null;
	}
	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"), method = "tickMovement")
	public ItemStack getEquippedStackProxy(ClientPlayerEntity player, EquipmentSlot slot){
		TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
		return comp.getStack(SlotGroups.CHEST + ':' + Slots.CAPE);
	}
}