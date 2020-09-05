package dev.emi.trinkets.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.data.SlotLoader.GroupData;
import dev.emi.trinkets.data.SlotLoader.SlotData;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

public class EntitySlotLoader extends
    SinglePreparationResourceReloadListener<Map<String, Map<String, Set<String>>>> implements
    IdentifiableResourceReloadListener {

  public static final EntitySlotLoader INSTANCE = new EntitySlotLoader();

  private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping()
      .create();
  private static final Identifier ID = new Identifier(TrinketsMain.MOD_ID, "entities");

  private Map<String, SlotGroup> playerSlots = new HashMap<>();
  private Map<EntityType<?>, Map<String, SlotGroup>> entitySlots = new HashMap<>();

  @Override
  protected Map<String, Map<String, Set<String>>> prepare(ResourceManager resourceManager,
      Profiler profiler) {
    Map<String, Map<String, Set<String>>> map = new HashMap<>();
    String dataType = "entities";

    for (Identifier identifier : resourceManager
        .findResources(dataType, (stringx) -> stringx.endsWith(".json"))) {
      try {
        InputStreamReader reader = new InputStreamReader(
            resourceManager.getResource(identifier).getInputStream());
        JsonObject jsonObject = JsonHelper.deserialize(GSON, reader, JsonObject.class);

        if (jsonObject != null) {
          boolean replace = JsonHelper.getBoolean(jsonObject, "replace", false);
          JsonArray assignedSlots = JsonHelper.getArray(jsonObject, "slots", new JsonArray());
          Map<String, Set<String>> groups = new HashMap<>();

          if (assignedSlots != null) {

            for (JsonElement assignedSlot : assignedSlots) {
              String[] parsedSlot = assignedSlot.getAsString().split("/", 2);
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
              groups.forEach(
                  (groupName, slotNames) -> slots.computeIfAbsent(groupName, (k) -> new HashSet<>())
                      .addAll(slotNames));
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return map;
  }

  @Override
  protected void apply(Map<String, Map<String, Set<String>>> loader, ResourceManager manager,
      Profiler profiler) {
    Map<String, GroupData> slots = SlotLoader.INSTANCE.getSlots();
    Map<String, Map<String, SlotGroup.Builder>> groupBuilders = new HashMap<>();

    loader.forEach((entityName, groups) -> {
      Map<String, SlotGroup.Builder> builders = groupBuilders
          .computeIfAbsent(entityName, (k) -> new HashMap<>());
      groups.forEach((groupName, slotNames) -> {
        GroupData group = slots.get(groupName);

        if (group != null) {
          SlotGroup.Builder builder = builders
              .computeIfAbsent(groupName, (k) -> new SlotGroup.Builder(group.getDefaultSlot()));
          slotNames.forEach(slotName -> {
            SlotData slotData = group.getSlot(slotName);

            if (slotData != null) {
              builder.addSlot(slotName, slotData.create(slotName));
            }
          });
        }
      });
    });
    this.playerSlots.clear();
    this.entitySlots.clear();

    groupBuilders.forEach((entityName, groups) -> {
      Map<String, SlotGroup> existing = entityName.equals("player") ? this.playerSlots
          : Registry.ENTITY_TYPE.getOrEmpty(new Identifier(entityName))
              .map(type -> this.entitySlots.computeIfAbsent(type, (k) -> new HashMap<>()))
              .orElse(null);

      if (existing != null) {
        groups.forEach(
            (groupName, groupBuilder) -> existing.putIfAbsent(groupName, groupBuilder.build()));
      }
    });
  }

  public Map<String, SlotGroup> getPlayerSlots() {
    return ImmutableMap.copyOf(this.playerSlots);
  }

  public Map<String, SlotGroup> getEntitySlots(EntityType<?> entityType) {
    return ImmutableMap.copyOf(this.entitySlots.get(entityType));
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
