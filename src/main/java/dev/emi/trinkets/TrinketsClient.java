package dev.emi.trinkets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.util.Pair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.data.EntitySlotLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup;
	public static SlotType activeType;
	public static SlotGroup quickMoveGroup;
	public static SlotType quickMoveType;
	public static int quickMoveTimer;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_CONTENT, (client, handler, buf, responseSender) -> {
			int entityId = buf.readInt();
			NbtCompound data = buf.readNbt();

			if (data != null) {
				List<Pair<String, ItemStack>> entries = new ArrayList<>();
				for (String key : data.getKeys()) {
					ItemStack stack = ItemStack.fromNbt(data.getCompound(key));
					entries.add(new Pair<>(key, stack));
				}
				client.execute(() -> {
					Entity entity = client.world.getEntityById(entityId);
					if (entity instanceof LivingEntity) {
						TrinketsApi.getTrinketComponent((LivingEntity) entity).ifPresent(trinkets -> {
							for (Pair<String, ItemStack> entry : entries) {
								String[] split = entry.getLeft().split("/");
								String group = split[0];
								String slot = split[1];
								int index = Integer.parseInt(split[2]);
								Map<String, TrinketInventory> slots = trinkets.getInventory().get(group);
								if (slots != null) {
									TrinketInventory inv = slots.get(slot);
									if (inv != null && index < inv.size()) {
										inv.setStack(index, entry.getRight());
									}
								}
							}
						});
					}
				});
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_MODIFIERS, (client, handler, buf, responseSender) -> {
			int entityId = buf.readInt();
			NbtCompound data = buf.readNbt();

			if (data != null) {
				List<Triple<String, String, NbtCompound>> entries = new ArrayList<>();
				for (String key : data.getKeys()) {
					String[] split = key.split("/");
					String group = split[0];
					String slot = split[1];
					NbtCompound tag = data.getCompound(key);
					entries.add(new ImmutableTriple<>(group, slot, tag));
				}
				client.execute(() -> {
					Entity entity = client.world.getEntityById(entityId);
					if (entity instanceof LivingEntity) {
						TrinketsApi.getTrinketComponent((LivingEntity) entity).ifPresent(trinkets -> {
							for (Triple<String, String, NbtCompound> entry : entries) {
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
			NbtCompound data = buf.readNbt();

			if (data != null) {
				Map<EntityType<?>, Map<String, SlotGroup>> slots = new HashMap<>();

				for (String id : data.getKeys()) {
					Optional<EntityType<?>> maybeType = Registry.ENTITY_TYPE.getOrEmpty(Identifier.tryParse(id));
					maybeType.ifPresent(type -> {
						NbtCompound groups = data.getCompound(id);

						if (groups != null) {

							for (String groupId : groups.getKeys()) {
								NbtCompound group = groups.getCompound(groupId);

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
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.BREAK, (client, handler, buf, sender) -> {
			int entityId = buf.readInt();
			String[] split = buf.readString().split("/");
			String group = split[0];
			String slot = split[1];
			int index = buf.readInt();
			client.execute(() -> {
				Entity e = client.world.getEntityById(entityId);
				if (e instanceof LivingEntity entity) {
					TrinketsApi.getTrinketComponent(entity).ifPresent(comp -> {
						var groupMap = comp.getInventory().get(group);
						if (groupMap != null) {
							TrinketInventory inv = groupMap.get(slot);
							if (index < inv.size()) {
								ItemStack stack = inv.getStack(index);
								SlotReference ref = new SlotReference(inv, index);
								Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
								trinket.onBreak(stack, ref, entity);
							}
						}
					});
				}
			});
		});
	}
}
