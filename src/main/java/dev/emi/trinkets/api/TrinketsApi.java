package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Function3;
import dev.emi.trinkets.TrinketSlotTarget;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.data.EntitySlotLoader;
import dev.emi.trinkets.payload.BreakPayload;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.server.world.ServerWorld;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.world.World;

public class TrinketsApi {
	public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
			.getOrCreate(Identifier.of(TrinketsMain.MOD_ID, "trinkets"), TrinketComponent.class);
	private static final Map<Identifier, Function3<ItemStack, SlotReference, LivingEntity, TriState>> PREDICATES = new HashMap<>();
	
	private static final Map<Item, Trinket> TRINKETS = new HashMap<>();
	private static final Trinket DEFAULT_TRINKET;

	/**
	 * Registers a trinket for the provided item, {@link TrinketItem} will do this
	 * automatically.
	 */
	public static void registerTrinket(Item item, Trinket trinket) {
		TRINKETS.put(item, trinket);
	}

	public static Trinket getTrinket(Item item) {
		return TRINKETS.getOrDefault(item, DEFAULT_TRINKET);
	}

	public static Trinket getDefaultTrinket() {
		return DEFAULT_TRINKET;
	}

	/**
	 * @return The trinket component for this entity, if available
	 */
	public static Optional<TrinketComponent> getTrinketComponent(LivingEntity livingEntity) {
		return TRINKET_COMPONENT.maybeGet(livingEntity);
	}

	/**
	 * Called to sync a trinket breaking event with clients. Should generally be
	 * called in the callback of {@link ItemStack#damage(int, ServerWorld, ServerPlayerEntity, Consumer)}
	 */
	public static void onTrinketBroken(ItemStack stack, SlotReference ref, LivingEntity entity) {
		World world = entity.getWorld();
		if (!world.isClient) {
			var packet = new BreakPayload(entity.getId(), ref.inventory().getSlotType().getGroup(), ref.inventory().getSlotType().getName(), ref.index());
			if (entity instanceof ServerPlayerEntity player) {
				ServerPlayNetworking.send(player, packet);
			}
			PlayerLookup.tracking(entity).forEach(watcher -> {
				ServerPlayNetworking.send(watcher, packet);
			});
		}
	}

	/**
	 * @deprecated Use world-sensitive alternative {@link TrinketsApi#getPlayerSlots(World)}
	 * @return A map of slot group names to slot groups available for players
	 */
	@Deprecated
	public static Map<String, SlotGroup> getPlayerSlots() {
		return getEntitySlots(EntityType.PLAYER);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for players
	 */
	public static Map<String, SlotGroup> getPlayerSlots(World world) {
		return getEntitySlots(world, EntityType.PLAYER);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for players
	 */
	public static Map<String, SlotGroup> getPlayerSlots(PlayerEntity player) {
		return getEntitySlots(player);
	}

	/**
	 * @deprecated Use world-sensitive alternative {@link TrinketsApi#getEntitySlots(World, EntityType)}
	 * @return A map of slot group names to slot groups available for the provided
	 * entity type
	 */
	@Deprecated
	public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
		EntitySlotLoader loader = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
		return loader.getEntitySlots(type);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for the provided
	 * entity type
	 */
	public static Map<String, SlotGroup> getEntitySlots(World world, EntityType<?> type) {
		EntitySlotLoader loader = world.isClient() ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
		return loader.getEntitySlots(type);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for the provided
	 * entity
	 */
	public static Map<String, SlotGroup> getEntitySlots(Entity entity) {
		if (entity != null) {
			return getEntitySlots(entity.getWorld(), entity.getType());
		}
		return ImmutableMap.of();
	}

	/**
	 * Registers a predicate to be referenced in slot data
	 */
	public static void registerTrinketPredicate(Identifier id, Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate) {
		PREDICATES.put(id, predicate);
	}

	public static Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> getTrinketPredicate(Identifier id) {
		return Optional.ofNullable(PREDICATES.get(id));
	}

	public static boolean evaluatePredicateSet(Set<Identifier> set, ItemStack stack, SlotReference ref, LivingEntity entity) {
		TriState state = TriState.DEFAULT;
		for (Identifier id : set) {
			var function = getTrinketPredicate(id);
			if (function.isPresent()) {
				state = function.get().apply(stack, ref, entity);
			}
			if (state != TriState.DEFAULT) {
				break;
			}
		}
		return state.get();
	}

	public static Enchantment.Definition withTrinketSlots(Enchantment.Definition definition, Set<String> slots) {
		var def = new Enchantment.Definition(definition.supportedItems(), definition.primaryItems(), definition.weight(), definition.maxLevel(),
				definition.minCost(), definition.maxCost(), definition.anvilCost(), definition.slots());

		((TrinketSlotTarget) (Object) def).trinkets$slots(slots);
		return def;
	}

	static {
		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "all"), (stack, ref, entity) -> TriState.TRUE);
		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "none"), (stack, ref, entity) -> TriState.FALSE);
		TagKey<Item> trinketsAll = TagKey.of(RegistryKeys.ITEM, Identifier.of("trinkets", "all"));

		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "tag"), (stack, ref, entity) -> {
			SlotType slot = ref.inventory().getSlotType();
			TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, Identifier.of("trinkets", slot.getId()));

			if (stack.isIn(tag) || stack.isIn(trinketsAll)) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "relevant"), (stack, ref, entity) -> {
			var map = TrinketsApi.getTrinket(stack.getItem()).getModifiers(stack, ref, entity, SlotAttributes.getIdentifier(ref));
			if (!map.isEmpty()) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		DEFAULT_TRINKET = new Trinket() {

		};
	}
}
