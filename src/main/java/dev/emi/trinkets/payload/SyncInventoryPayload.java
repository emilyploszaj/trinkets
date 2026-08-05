package dev.emi.trinkets.payload;

import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.TrinketSaveData;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashMap;
import java.util.Map;

public record SyncInventoryPayload(int entityId, Map<String, ItemStack> contentUpdates, Map<String, TrinketSaveData.Metadata> inventoryUpdates) implements CustomPayload {
	public static final PacketCodec<RegistryByteBuf, SyncInventoryPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			SyncInventoryPayload::entityId,
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, ItemStack.OPTIONAL_PACKET_CODEC),
			SyncInventoryPayload::contentUpdates,
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, TrinketSaveData.Metadata.PACKET_CODEC_PERSISTENT_ONLY),
			SyncInventoryPayload::inventoryUpdates,
			SyncInventoryPayload::new);
	@Override
	public Id<? extends CustomPayload> getId() {
		return TrinketsNetwork.SYNC_INVENTORY;
	}
}
