package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.data.EntitySlotLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup;
	public static SlotType activeType; // TODO mostly unused
	public static SlotGroup quickMoveGroup;
	public static SlotType quickMoveType;
	public static int quickMoveTimer;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_SLOTS, (client, handler, buf, responseSender) -> {
			CompoundTag data = buf.readCompoundTag();

			if (data != null) {
				Map<EntityType<?>, Map<String, SlotGroup>> slots = new HashMap<>();

				for (String id : data.getKeys()) {
					Optional<EntityType<?>> maybeType = Registry.ENTITY_TYPE.getOrEmpty(Identifier.tryParse(id));
					maybeType.ifPresent(type -> {
						CompoundTag groups = data.getCompound(id);

						if (groups != null) {

							for (String groupId : groups.getKeys()) {
								CompoundTag group = groups.getCompound(groupId);

								if (group != null) {
									SlotGroup slotGroup = SlotGroup.read(group);
									slots.computeIfAbsent(type, (k) -> new HashMap<>()).put(groupId, slotGroup);
								}
							}
						}
					});
				}
				client.execute(() -> {
					EntitySlotLoader.INSTANCE.setSlots(slots);
					ClientPlayerEntity player = client.player;

					if (player != null) {
						((TrinketPlayerScreenHandler) player.playerScreenHandler).updateTrinketSlots();
					}
				});
			}
		});
	}
}
