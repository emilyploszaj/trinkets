package dev.emi.trinkets.payload;

import dev.emi.trinkets.TrinketsNetwork;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;


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
