package dev.emi.trinkets.payload;

import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.SlotGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;

import java.util.HashMap;
import java.util.Map;

public record BreakPayload(int entityId, String group, String slot, int index) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, BreakPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            BreakPayload::entityId,
            PacketCodecs.STRING,
            BreakPayload::group,
            PacketCodecs.STRING,
            BreakPayload::slot,
            PacketCodecs.VAR_INT,
            BreakPayload::index,
            BreakPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return TrinketsNetwork.BREAK;
    }
}
