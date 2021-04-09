package dev.emi.trinkets;

import java.util.List;

import dev.emi.trinkets.api.TrinketSlots.SlotGroup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class TrinketsClient extends TrinketsMain implements ClientModInitializer {
	// Slots in the current hovered slot group
	public static List<Slot> activeSlots;
	// Current hovered slot group
	public static SlotGroup slotGroup = null;
	// Last slot group that had a trinket equipped through shift clicking
	public static SlotGroup lastEquipped = null;
	// Ticks left to slot the last equipped slot
	public static int displayEquipped = 0;

	@Override
	public void onInitializeClient(){
		//Is this needed?
		ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).register((spriteAtlasTexture, registry) -> {
			registry.register(new Identifier("trinkets", "item/empty"));
		});
	}
}
