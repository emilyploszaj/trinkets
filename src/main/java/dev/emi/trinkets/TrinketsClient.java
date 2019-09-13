package dev.emi.trinkets;

import java.util.List;

import dev.emi.trinkets.api.TrinketSlots.SlotGroup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.container.Slot;
import net.minecraft.util.Identifier;

public class TrinketsClient extends TrinketsMain implements ClientModInitializer{
	public static List<Slot> activeSlots;
	public static SlotGroup slotGroup = null;
	public static SlotGroup lastEquipped = null;
	public static int displayEquipped = 0;
	@Override
	public void onInitializeClient(){
		ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEX).register((spriteAtlasTexture, registry) -> {
			registry.register(new Identifier("trinkets", "item/empty"));
		});
	}
}
