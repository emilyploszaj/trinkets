package dev.emi.trinkets;

import dev.emi.trinkets.data.EntitySlotLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup;
	public static SlotType activeType;
	public static SlotGroup quickMoveGroup;
	public static SlotType quickMoveType;
	public static int quickMoveTimer;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_INVENTORY, (client, handler, buf, responseSender) -> {
			int entityId = buf.readInt();
			NbtCompound inventoryData = buf.readNbt();
			NbtCompound contentData = buf.readNbt();
			List<Pair<String, ItemStack>> contentUpdates = new ArrayList<>();
			List<Triple<String, String, NbtCompound>> inventoryUpdates = new ArrayList<>();

			if (inventoryData != null) {
				for (String key : inventoryData.getKeys()) {
					String[] split = key.split("/");
					String group = split[0];
					String slot = split[1];
					NbtCompound tag = inventoryData.getCompound(key);
					inventoryUpdates.add(new ImmutableTriple<>(group, slot, tag));
				}
			}

			if (contentData != null) {
				for (String key : contentData.getKeys()) {
					ItemStack stack = ItemStack.fromNbt(contentData.getCompound(key));
					contentUpdates.add(new Pair<>(key, stack));
				}
			}

			client.execute(() -> {
				Entity entity = client.world.getEntityById(entityId);
				if (entity instanceof LivingEntity) {
					TrinketsApi.getTrinketComponent((LivingEntity) entity).ifPresent(trinkets -> {
						for (Triple<String, String, NbtCompound> entry : inventoryUpdates) {
							Map<String, TrinketInventory> slots = trinkets.getInventory().get(entry.getLeft());
							if (slots != null) {
								TrinketInventory inv = slots.get(entry.getMiddle());
								if (inv != null) {
									inv.applySyncTag(entry.getRight());
								}
							}
						}

						if (entity instanceof PlayerEntity && ((PlayerEntity) entity).playerScreenHandler instanceof TrinketPlayerScreenHandler screenHandler) {
							screenHandler.trinkets$updateTrinketSlots(false);
							if (TrinketScreenManager.currentScreen != null) {
								TrinketScreenManager.currentScreen.trinkets$updateTrinketSlots();
							}
						}

						for (Pair<String, ItemStack> entry : contentUpdates) {
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
		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_SLOTS, (client, handler, buf, responseSender) -> {
			NbtCompound data = buf.readNbt();

			if (data != null) {
				Map<EntityType<?>, Map<String, SlotGroup>> slots = new HashMap<>();

				for (String id : data.getKeys()) {
					Optional<EntityType<?>> maybeType = Registries.ENTITY_TYPE.getOrEmpty(Identifier.tryParse(id));
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
					EntitySlotLoader.CLIENT.setSlots(slots);
					ClientPlayerEntity player = client.player;

					if (player != null) {
						((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(true);

						if (client.currentScreen instanceof TrinketScreen trinketScreen) {
							trinketScreen.trinkets$updateTrinketSlots();
						}

						for (AbstractClientPlayerEntity clientWorldPlayer : player.clientWorld.getPlayers()) {
							((TrinketPlayerScreenHandler) clientWorldPlayer.playerScreenHandler).trinkets$updateTrinketSlots(true);
						}
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
