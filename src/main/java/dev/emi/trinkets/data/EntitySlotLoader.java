package dev.emi.trinkets.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.data.SlotLoader.GroupData;
import dev.emi.trinkets.data.SlotLoader.SlotData;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloadListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class EntitySlotLoader extends SinglePreparationResourceReloadListener<Map<String, Map<String, Set<String>>>> implements IdentifiableResourceReloadListener {

	public static final EntitySlotLoader INSTANCE = new EntitySlotLoader();

	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	private static final Identifier ID = new Identifier(TrinketsMain.MOD_ID, "entities");

	private final Map<EntityType<?>, Map<String, SlotGroup>> slots = new HashMap<>();

	@Override
	protected Map<String, Map<String, Set<String>>> prepare(ResourceManager resourceManager, Profiler profiler) {
		Map<String, Map<String, Set<String>>> map = new HashMap<>();
		String dataType = "entities";

		for (Identifier identifier : resourceManager.findResources(dataType, (stringx) -> stringx.endsWith(".json"))) {
			try {
				InputStreamReader reader = new InputStreamReader(resourceManager.getResource(identifier).getInputStream());
				JsonObject jsonObject = JsonHelper.deserialize(GSON, reader, JsonObject.class);

				if (jsonObject != null) {

					try {
						boolean replace = JsonHelper.getBoolean(jsonObject, "replace", false);
						JsonArray assignedSlots = JsonHelper.getArray(jsonObject, "slots", new JsonArray());
						Map<String, Set<String>> groups = new HashMap<>();

						if (assignedSlots != null) {

							for (JsonElement assignedSlot : assignedSlots) {
								String slot = assignedSlot.getAsString();
								String[] parsedSlot = slot.split("/");

								if (parsedSlot.length > 2) {
									TrinketsMain.LOGGER.error("Detected malformed slot assignment " + slot
											+ "! Slots should be in the format 'group/slot'.");
									continue;
								}
								String group = parsedSlot[0];
								String name = parsedSlot[1];
								groups.computeIfAbsent(group, (k) -> new HashSet<>()).add(name);
							}
						}
						JsonArray entities = JsonHelper.getArray(jsonObject, "entities", new JsonArray());

						if (!groups.isEmpty() && entities != null) {

							for (JsonElement entity : entities) {
								String name = entity.getAsString();
								Map<String, Set<String>> slots = map.computeIfAbsent(name, (k) -> new HashMap<>());

								if (replace) {
									slots.clear();
								}
								groups.forEach((groupName, slotNames) -> slots.computeIfAbsent(groupName, (k) -> new HashSet<>())
										.addAll(slotNames));
							}
						}
					} catch (JsonSyntaxException e) {
						TrinketsMain.LOGGER.error("[trinkets] Syntax error while reading data for " + identifier.getPath());
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				TrinketsMain.LOGGER.error("[trinkets] Unknown IO error while reading slot data!");
				e.printStackTrace();
			}
		}
		return map;
	}

	@Override
	protected void apply(Map<String, Map<String, Set<String>>> loader, ResourceManager manager, Profiler profiler) {
		Map<String, GroupData> slots = SlotLoader.INSTANCE.getSlots();
		Map<String, Map<String, SlotGroup.Builder>> groupBuilders = new HashMap<>();

		loader.forEach((entityName, groups) -> {
			Map<String, SlotGroup.Builder> builders = groupBuilders.computeIfAbsent(entityName, (k) -> new HashMap<>());
			groups.forEach((groupName, slotNames) -> {
				GroupData group = slots.get(groupName);

				if (group != null) {
					SlotGroup.Builder builder = builders.computeIfAbsent(groupName, (k) -> {
						if (group.getSlotId() == -1) {
							return new SlotGroup.Builder(groupName, group.getDefaultSlot());
						}
						return new SlotGroup.Builder(groupName, group.getSlotId());
					});
					slotNames.forEach(slotName -> {
						SlotData slotData = group.getSlot(slotName);

						if (slotData != null) {
							builder.addSlot(slotName, slotData.create(groupName, slotName));
						} else {
							TrinketsMain.LOGGER.error("[trinkets] Attempted to assign unknown slot " + slotName);
						}
					});
				} else {
					TrinketsMain.LOGGER.error("[trinkets] Attempted to assign slot from unknown group " + groupName);
				}
			});
		});
		this.slots.clear();

		groupBuilders.forEach((entityName, groups) -> {
			EntityType<?> type = Registry.ENTITY_TYPE.getOrEmpty(new Identifier(entityName)).orElse(null);

			if (type != null) {
				Map<String, SlotGroup> entitySlots = this.slots.computeIfAbsent(type, (k) -> new HashMap<>());
				groups.forEach((groupName, groupBuilder) -> entitySlots.putIfAbsent(groupName, groupBuilder.build()));
			} else {
				TrinketsMain.LOGGER.error("[trinkets] Attempted to assign slots to unknown entity " + entityName);
			}
		});
	}

	public Map<String, SlotGroup> getEntitySlots(EntityType<?> entityType) {
		if (this.slots.containsKey(entityType)) {
			return ImmutableMap.copyOf(this.slots.get(entityType));
		}
		return ImmutableMap.of();
	}

	public void setSlots(Map<EntityType<?>, Map<String, SlotGroup>> slots) {
		this.slots.clear();
		this.slots.putAll(slots);
	}

	public void sync(ServerPlayerEntity playerEntity) {
		PacketByteBuf buf = getSlotsPacket();
		ServerPlayNetworking.send(playerEntity, TrinketsNetwork.SYNC_SLOTS, buf);
	}

	public void sync(List<? extends ServerPlayerEntity> players) {
		PacketByteBuf buf = getSlotsPacket();
		players.forEach(player -> ServerPlayNetworking.send(player, TrinketsNetwork.SYNC_SLOTS, buf));
	}

	private PacketByteBuf getSlotsPacket() {
		CompoundTag tag = new CompoundTag();

		this.slots.forEach((entity, slotMap) -> {
			CompoundTag slotsTag = new CompoundTag();

			slotMap.forEach((id, slotGroup) -> {
				CompoundTag groupTag = new CompoundTag();
				slotGroup.write(groupTag);
				slotsTag.put(id, groupTag);
			});

			tag.put(Registry.ENTITY_TYPE.getId(entity).toString(), slotsTag);
		});

		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeCompoundTag(tag);
		return buf;
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return Collections.singletonList(SlotLoader.ID);
	}
}
