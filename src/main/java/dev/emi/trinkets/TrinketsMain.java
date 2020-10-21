package dev.emi.trinkets;

import dev.emi.trinkets.api.*;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class TrinketsMain implements ModInitializer, EntityComponentInitializer {
	
	@Override
	public void onInitialize() {
		TrinketSlots.addSlot(SlotGroups.CHEST, Slots.CAPE, new Identifier("trinkets", "textures/item/empty_trinket_slot_cape.png"), (slot, stack) -> {
			if (!(stack.getItem() instanceof Trinket)) {
				return stack.getItem() == Items.ELYTRA;
			}
			return ((Trinket) stack.getItem()).canWearInSlot(slot.getSlotGroup().getName(), slot.getName());
		});
		//Slots used for testing
		//TrinketSlots.addSlot("head", "mask", new Identifier("trinkets", "textures/item/empty_trinket_slot_mask.png"));
		//TrinketSlots.addSlot("chest", "necklace", new Identifier("trinkets", "textures/item/empty_trinket_slot_necklace.png"));
		//TrinketSlots.addSlot("legs", "belt", new Identifier("trinkets", "textures/item/empty_trinket_slot_belt.png"));
		//TrinketSlots.addSlot("feet", "aglet", new Identifier("trinkets", "textures/item/empty_trinket_slot_aglet.png"));
		//TrinketSlots.addSlot("hand", "ring", new Identifier("trinkets", "textures/item/empty_trinket_slot_ring.png"));
		//TrinketSlots.addSlot("hand", "gloves", new Identifier("trinkets", "textures/item/empty_trinket_slot_gloves.png"));
		//TrinketSlots.addSlot("offhand", "ring", new Identifier("trinkets", "textures/item/empty_trinket_slot_ring.png"));
	}

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(TrinketsApi.TRINKETS, PlayerTrinketComponent::new, RespawnCopyStrategy.INVENTORY);
	}
}