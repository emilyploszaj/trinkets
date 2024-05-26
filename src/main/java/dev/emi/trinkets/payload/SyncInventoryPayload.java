package dev.emi.trinkets.payload;

import dev.emi.trinkets.TrinketsNetwork;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashMap;
import java.util.Map;

public record SyncInventoryPayload(int entityId, Map<String, ItemStack> contentUpdates, Map<String, NbtCompound> inventoryUpdates) implements CustomPayload {
	public static final PacketCodec<RegistryByteBuf, SyncInventoryPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			SyncInventoryPayload::entityId,
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, ItemStack.OPTIONAL_PACKET_CODEC),
			SyncInventoryPayload::contentUpdates,
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, PacketCodecs.NBT_COMPOUND),
			SyncInventoryPayload::inventoryUpdates,
			SyncInventoryPayload::new);
	@Override
	public Id<? extends CustomPayload> getId() {
		return TrinketsNetwork.SYNC_INVENTORY;
	}
}
