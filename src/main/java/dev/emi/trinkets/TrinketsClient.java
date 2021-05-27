package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.data.EntitySlotLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup;
	public static SlotType activeType; // TODO mostly unused
	public static SlotGroup quickMoveGroup;
	public static SlotType quickMoveType;
	public static int quickMoveTimer;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_MODIFIERS, (client, handler, buf, responseSender) -> {
			int entityId = buf.readInt();
			CompoundTag data = buf.readCompoundTag();

			if (data != null) {
				List<Triple<String, String, CompoundTag>> entries = new ArrayList<>();
				for (String key : data.getKeys()) {
					String[] split = key.split(":");
					String group = split[0];
					String slot = split[1];
					CompoundTag tag = data.getCompound(key);
					entries.add(new ImmutableTriple<>(group, slot, tag));
				}
				client.execute(() -> {
					Entity entity = client.world.getEntityById(entityId);
					if (entity instanceof LivingEntity) {
						TrinketsApi.getTrinketComponent((LivingEntity) entity).ifPresent(trinkets -> {
							for (Triple<String, String, CompoundTag> entry : entries) {
								Map<String, TrinketInventory> slots = trinkets.getInventory().get(entry.getLeft());
								if (slots != null) {
									TrinketInventory inv = slots.get(entry.getMiddle());
									if (inv != null) {
										inv.applySyncTag(entry.getRight());
									}
								}
							}

							if (entity == client.player) {
								((TrinketPlayerScreenHandler) client.player.playerScreenHandler).updateTrinketSlots(false);
							}
						});
					}
				});
			}
		});
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
						((TrinketPlayerScreenHandler) player.playerScreenHandler).updateTrinketSlots(true);
					}
				});
			}
		});
	}
}
