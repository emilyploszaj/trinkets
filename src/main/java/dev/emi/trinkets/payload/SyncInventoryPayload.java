package dev.emi.trinkets.payload;

import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.SlotGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
