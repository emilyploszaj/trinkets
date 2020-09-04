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
import java.util.Map;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

public class EntitySlotLoader extends
    SinglePreparationResourceReloadListener<Map<String, Map<String, SlotGroup>>> implements
    IdentifiableResourceReloadListener {

  public static final EntitySlotLoader INSTANCE = new EntitySlotLoader();

  private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping()
      .create();
  private static final Identifier ID = new Identifier(TrinketsMain.MOD_ID, "entities");

  private Map<String, SlotGroup> playerSlots = new HashMap<>();
  private Map<EntityType<?>, Map<String, SlotGroup>> entitySlots = new HashMap<>();

  @Override
  protected Map<String, Map<String, SlotGroup>> prepare(ResourceManager resourceManager,
      Profiler profiler) {
    Map<String, Map<String, SlotGroup>> map = new HashMap<>();
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
          Map<String, GroupData> slots = SlotLoader.INSTANCE.getSlots();
          Map<String, SlotGroup.Builder> groupBuilders = new HashMap<>();

          if (assignedSlots != null) {

            for (JsonElement assignedSlot : assignedSlots) {
              String[] parsedSlot = assignedSlot.getAsString().split("/", 2);
              String group = parsedSlot[0];
              String name = parsedSlot[1];
              GroupData groupData = slots.get(group);

              if (groupData != null) {
                SlotGroup.Builder builder = groupBuilders.computeIfAbsent(group,
                    (k) -> new SlotGroup.Builder(groupData.getDefaultSlot()));
                SlotData slotData = groupData.getSlot(name);

                if (slotData != null) {
                  builder.addSlot(name, slotData.create(name));
                }
              }
            }
          }
          JsonArray entities = JsonHelper.getArray(jsonObject, "entities", new JsonArray());

          if (!groupBuilders.isEmpty() && entities != null) {

            for (JsonElement entity : entities) {
              String name = entity.getAsString();
              Map<String, SlotGroup> createdEntitySlots = map
                  .computeIfAbsent(name, (k) -> new HashMap<>());

              if (replace) {
                createdEntitySlots.clear();
              }
              groupBuilders.forEach((groupName, builder) -> createdEntitySlots
                  .putIfAbsent(groupName, builder.build()));
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
  protected void apply(Map<String, Map<String, SlotGroup>> loader, ResourceManager manager,
      Profiler profiler) {
    loader.forEach((entityName, groups) -> {
      if (entityName.equals("player")) {
        this.playerSlots.putAll(groups);
      } else {
        Registry.ENTITY_TYPE.getOrEmpty(new Identifier(entityName))
            .ifPresent(entityType -> this.entitySlots.putIfAbsent(entityType, groups));
      }
    });
    TrinketsMain.LOGGER.info("Done");
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
