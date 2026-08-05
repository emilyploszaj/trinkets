package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketSaveData;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.data.EntitySlotLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup;
	public static SlotType activeType;
	public static SlotGroup quickMoveGroup;
	public static SlotType quickMoveType;
	public static int quickMoveTimer;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_INVENTORY, (payload, context) -> {
			MinecraftClient client = context.client();
			Entity entity = client.world.getEntityById(payload.entityId());
			if (entity instanceof LivingEntity) {
				TrinketsApi.getTrinketComponent((LivingEntity) entity).ifPresent(trinkets -> {
					for (Map.Entry<String, TrinketSaveData.Metadata> entry : payload.inventoryUpdates().entrySet()) {
						String[] split = entry.getKey().split("/");
						String group = split[0];
						String slot = split[1];
						Map<String, TrinketInventory> slots = trinkets.getInventory().get(group);
						if (slots != null) {
							TrinketInventory inv = slots.get(slot);
							if (inv != null) {
								inv.applySyncMetadata(entry.getValue());
							}
						}
					}

					if (entity instanceof PlayerEntity && ((PlayerEntity) entity).playerScreenHandler instanceof TrinketPlayerScreenHandler screenHandler) {
						screenHandler.trinkets$updateTrinketSlots(false);
						TrinketScreenManager.tryUpdateTrinketsSlot();
					}

					for (Map.Entry<String, ItemStack> entry : payload.contentUpdates().entrySet()) {
						String[] split = entry.getKey().split("/");
						String group = split[0];
						String slot = split[1];
						int index = Integer.parseInt(split[2]);
						Map<String, TrinketInventory> slots = trinkets.getInventory().get(group);
						if (slots != null) {
							TrinketInventory inv = slots.get(slot);
							if (inv != null && index < inv.size()) {
								inv.setStack(index, entry.getValue());
							}
						}
					}
				});
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_SLOTS, (payload, context) -> {

			EntitySlotLoader.CLIENT.setSlots(payload.map());
			ClientPlayerEntity player = context.player();

			if (player != null) {
				((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(true);

				MinecraftClient client = context.client();
				if (client.currentScreen instanceof TrinketScreen trinketScreen) {
					trinketScreen.trinkets$updateTrinketSlots();
				}

				for (PlayerEntity clientWorldPlayer : context.player().getEntityWorld().getPlayers()) {
					((TrinketPlayerScreenHandler) clientWorldPlayer.playerScreenHandler).trinkets$updateTrinketSlots(true);
				}
			}

		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.BREAK, (payload, context) -> {

			MinecraftClient client = context.client();
			Entity e = client.world.getEntityById(payload.entityId());
			if (e instanceof LivingEntity entity) {
				TrinketsApi.getTrinketComponent(entity).ifPresent(comp -> {
					Map<String, TrinketInventory> groupMap = comp.getInventory().get(payload.group());
					if (groupMap != null) {
						TrinketInventory inv = groupMap.get(payload.slot());
						if (payload.index() < inv.size()) {
							ItemStack stack = inv.getStack(payload.index());
							SlotReference ref = new SlotReference(inv, payload.index());
							Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
							trinket.onBreak(stack, ref, entity);
						}
					}
				});
			}

		});
	}
}