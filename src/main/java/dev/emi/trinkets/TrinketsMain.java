package dev.emi.trinkets;

import dev.emi.trinkets.api.PlayerTrinketComponent;
import dev.emi.trinkets.api.SlotGroups;
import dev.emi.trinkets.api.Slots;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import nerdhub.cardinal.components.api.event.EntityComponentCallback;
import nerdhub.cardinal.components.api.util.EntityComponents;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class TrinketsMain implements ModInitializer{
	@Override
	@SuppressWarnings("unchecked")
	public void onInitialize(){
		EntityComponentCallback.event(PlayerEntity.class).register((player, components) -> components.put(TrinketsApi.TRINKETS, new PlayerTrinketComponent(player)));
		EntityComponents.setRespawnCopyStrategy(TrinketsApi.TRINKETS, RespawnCopyStrategy.INVENTORY);
		TrinketSlots.addSubSlot(SlotGroups.CHEST, Slots.CAPE, new Identifier("trinkets", "textures/item/empty_trinket_slot_cape.png"));
		//Slots used for testing
		//TrinketSlots.addSubSlot("head", "mask", new Identifier("trinkets", "textures/item/empty_trinket_slot_mask.png"));
		//TrinketSlots.addSubSlot("chest", "necklace", new Identifier("trinkets", "textures/item/empty_trinket_slot_necklace.png"));
		//TrinketSlots.addSubSlot("legs", "belt", new Identifier("trinkets", "textures/item/empty_trinket_slot_belt.png"));
		//TrinketSlots.addSubSlot("feet", "aglet", new Identifier("trinkets", "textures/item/empty_trinket_slot_aglet.png"));
		//TrinketSlots.addSubSlot("hand", "ring", new Identifier("trinkets", "textures/item/empty_trinket_slot_ring.png"));
		//TrinketSlots.addSubSlot("hand", "gloves", new Identifier("trinkets", "textures/item/empty_trinket_slot_gloves.png"));
		//TrinketSlots.addSubSlot("offhand", "ring", new Identifier("trinkets", "textures/item/empty_trinket_slot_ring.png"));
	}
}