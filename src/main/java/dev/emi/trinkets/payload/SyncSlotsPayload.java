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

public record SyncSlotsPayload(Map<EntityType<?>, Map<String, SlotGroup>> map) implements CustomPayload {
	public static final PacketCodec<RegistryByteBuf, SyncSlotsPayload> CODEC = PacketCodecs.map(
			(x) -> (Map<EntityType<?>, Map<String, SlotGroup>>) new HashMap<EntityType<?>, Map<String, SlotGroup>>(x),
			PacketCodecs.registryValue(RegistryKeys.ENTITY_TYPE),
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, PacketCodecs.NBT_COMPOUND.xmap(SlotGroup::read, (x) -> {
				var nbt = new NbtCompound();
				x.write(nbt);
				return nbt;
			}))
			).xmap(SyncSlotsPayload::new, SyncSlotsPayload::map);

	@Override
	public Id<? extends CustomPayload> getId() {
		return TrinketsNetwork.SYNC_SLOTS;
	}
}
