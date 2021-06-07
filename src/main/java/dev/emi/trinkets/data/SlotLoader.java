package dev.emi.trinkets.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import dev.emi.trinkets.data.SlotLoader.GroupData;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;

public class SlotLoader extends SinglePreparationResourceReloader<Map<String, GroupData>> implements IdentifiableResourceReloadListener {

	public static final SlotLoader INSTANCE = new SlotLoader();

	static final Identifier ID = new Identifier(TrinketsMain.MOD_ID, "slots");

	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	private static final int FILE_SUFFIX_LENGTH = ".json".length();

	private Map<String, GroupData> slots = new HashMap<>();

	@Override
	protected Map<String, GroupData> prepare(ResourceManager resourceManager, Profiler profiler) {
		Map<String, GroupData> map = new HashMap<>();
		String dataType = "slots";

		for (Identifier identifier : resourceManager.findResources(dataType, (stringx) -> stringx.endsWith(".json"))) {

			try {
				InputStreamReader reader = new InputStreamReader(resourceManager.getResource(identifier).getInputStream());
				JsonObject jsonObject = JsonHelper.deserialize(GSON, reader, JsonObject.class);

				if (jsonObject != null) {
					String path = identifier.getPath();
					String[] parsed = path.substring(dataType.length() + 1, path.length() - FILE_SUFFIX_LENGTH).split("/");
					String groupName = parsed[0];
					String fileName = parsed[parsed.length - 1];
					GroupData group = map.computeIfAbsent(groupName, (k) -> new GroupData());

					try {
						if (fileName.equals("group")) {
							group.read(jsonObject);
						} else {
							SlotData slot = group.slots.computeIfAbsent(fileName, (k) -> new SlotData());
							slot.read(jsonObject);
						}
					} catch (JsonSyntaxException e) {
						TrinketsMain.LOGGER.error("[trinkets] Syntax error while reading data for " + path);
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
	protected void apply(Map<String, GroupData> loader, ResourceManager manager, Profiler profiler) {
		this.slots = loader;
	}

	public Map<String, GroupData> getSlots() {
		return ImmutableMap.copyOf(this.slots);
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	static class GroupData {

		private int slotId = -1;
		private int order = 0;
		private final Map<String, SlotData> slots = new HashMap<>();

		void read(JsonObject jsonObject) {
			slotId = JsonHelper.getInt(jsonObject, "slot_id", slotId);
			order = JsonHelper.getInt(jsonObject, "order", order);
		}

		int getSlotId() {
			return slotId;
		}

		int getOrder() {
			return order;
		}

		SlotData getSlot(String name) {
			return slots.get(name);
		}
	}

	static class SlotData {
		private static final Set<Identifier> DEFAULT_QUICK_MOVE_PREDICATES = ImmutableSet.of(new Identifier("trinkets", "all"));
		private static final Set<Identifier> DEFAULT_VALIDATOR_PREDICATES = ImmutableSet.of(new Identifier("trinkets", "tag"));
		private static final Set<Identifier> DEFAULT_TOOLTIP_PREDICATES = ImmutableSet.of(new Identifier("trinkets", "all"));

		private int order = 0;
		private int amount = -1;
		private String icon = "";
		private final Set<String> quickMovePredicates = new HashSet<>();
		private final Set<String> validatorPredicates = new HashSet<>();
		private final Set<String> tooltipPredicates = new HashSet<>();
		private String dropRule = DropRule.DEFAULT.toString();

		SlotType create(String group, String name) {
			Identifier finalIcon = new Identifier(icon);
			finalIcon = new Identifier(finalIcon.getNamespace(), "textures/" + finalIcon.getPath() + ".png");
			Set<Identifier> finalValidatorPredicates = validatorPredicates.stream().map(Identifier::new).collect(Collectors.toSet());
			Set<Identifier> finalQuickMovePredicates = quickMovePredicates.stream().map(Identifier::new).collect(Collectors.toSet());
			Set<Identifier> finalTooltipPredicates = tooltipPredicates.stream().map(Identifier::new).collect(Collectors.toSet());
			if (finalValidatorPredicates.isEmpty()) {
				finalValidatorPredicates = DEFAULT_VALIDATOR_PREDICATES;
			}
			if (finalQuickMovePredicates.isEmpty()) {
				finalQuickMovePredicates = DEFAULT_QUICK_MOVE_PREDICATES;
			}
			if (finalTooltipPredicates.isEmpty()) {
				finalTooltipPredicates = DEFAULT_TOOLTIP_PREDICATES;
			}
			if (amount == -1) {
				amount = 1;
			}
			return new SlotType(group, name, order, amount, finalIcon, finalQuickMovePredicates, finalValidatorPredicates,
				finalTooltipPredicates, DropRule.valueOf(dropRule));
		}

		void read(JsonObject jsonObject) {
			boolean replace = JsonHelper.getBoolean(jsonObject, "replace", false);

			order = JsonHelper.getInt(jsonObject, "order", order);

			int jsonAmount = JsonHelper.getInt(jsonObject, "amount", amount);
			amount = replace ? jsonAmount : Math.max(jsonAmount, amount);

			icon = JsonHelper.getString(jsonObject, "icon", icon);

			JsonArray jsonQuickMovePredicates = JsonHelper.getArray(jsonObject, "quick_move_predicates", new JsonArray());

			if (jsonQuickMovePredicates != null) {

				if (replace && jsonQuickMovePredicates.size() > 0) {
					quickMovePredicates.clear();
				}

				for (JsonElement jsonQuickMovePredicate : jsonQuickMovePredicates) {
					quickMovePredicates.add(jsonQuickMovePredicate.getAsString());
				}
			}

			String jsonDropRule = JsonHelper.getString(jsonObject, "drop_rule", dropRule).toUpperCase();

			if (DropRule.has(jsonDropRule)) {
				dropRule = jsonDropRule;
			}
			JsonArray jsonValidatorPredicates = JsonHelper.getArray(jsonObject, "validator_predicates", new JsonArray());

			if (jsonValidatorPredicates != null) {

				if (replace && jsonValidatorPredicates.size() > 0) {
					validatorPredicates.clear();
				}

				for (JsonElement jsonValidatorPredicate : jsonValidatorPredicates) {
					validatorPredicates.add(jsonValidatorPredicate.getAsString());
				}
			}

			JsonArray jsonTooltipPredicates = JsonHelper.getArray(jsonObject, "tooltip_predicates", new JsonArray());

			if (jsonTooltipPredicates != null) {

				if (replace && jsonTooltipPredicates.size() > 0) {
					tooltipPredicates.clear();
				}

				for (JsonElement jsonTooltipPredicate : jsonTooltipPredicates) {
					tooltipPredicates.add(jsonTooltipPredicate.getAsString());
				}
			}
		}
	}
}
