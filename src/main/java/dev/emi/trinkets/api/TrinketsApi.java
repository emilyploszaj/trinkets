package dev.emi.trinkets.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.datafixers.util.Function3;

import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.data.EntitySlotLoader;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

public class TrinketsApi {
	public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
			.getOrCreate(new Identifier(TrinketsMain.MOD_ID, "trinkets"), TrinketComponent.class);
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
	 * called in the callback of {@link ItemStack#damage(int, LivingEntity, Consumer)}
	 */
	public static void onTrinketBroken(ItemStack stack, SlotReference ref, LivingEntity entity) {
		if (!entity.world.isClient) {
			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			buf.writeInt(entity.getId());
			buf.writeString(ref.inventory().getSlotType().getGroup() + "/" + ref.inventory().getSlotType().getName());
			buf.writeInt(ref.index());
			if (entity instanceof ServerPlayerEntity player) {
				ServerPlayNetworking.send(player, TrinketsNetwork.BREAK, buf);
			}
			PlayerLookup.tracking(entity).forEach(watcher -> {
				ServerPlayNetworking.send(watcher, TrinketsNetwork.BREAK, buf);
			});
		}
	}

	/**
	 * @return A map of slot group names to slot groups available for players
	 */
	public static Map<String, SlotGroup> getPlayerSlots() {
		return getEntitySlots(EntityType.PLAYER);
	}

	/**
	 * @return A map of slot group names to slot groups available for the provided
	 * entity type
	 */
	public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
		return EntitySlotLoader.INSTANCE.getEntitySlots(type);
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

	static {
		TrinketsApi.registerTrinketPredicate(new Identifier("trinkets", "all"), (stack, ref, entity) -> TriState.TRUE);
		TrinketsApi.registerTrinketPredicate(new Identifier("trinkets", "none"), (stack, ref, entity) -> TriState.FALSE);
		Identifier trinketsAll = new Identifier("trinkets", "all");
		TrinketsApi.registerTrinketPredicate(new Identifier("trinkets", "tag"), (stack, ref, entity) -> {
			SlotType slot = ref.inventory().getSlotType();
			Tag<Item> tag = ItemTags.getTagGroup().getTagOrEmpty(new Identifier("trinkets", slot.getGroup() + "/" + slot.getName()));
			Tag<Item> all = ItemTags.getTagGroup().getTagOrEmpty(trinketsAll);
			if (tag.contains(stack.getItem()) || all.contains(stack.getItem())) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		TrinketsApi.registerTrinketPredicate(new Identifier("trinkets", "relevant"), (stack, ref, entity) -> {
			var map = TrinketsApi.getTrinket(stack.getItem()).getModifiers(stack, ref, entity, SlotAttributes.getUuid(ref));
			if (!map.isEmpty()) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		DEFAULT_TRINKET = new Trinket() {

		};
	}
}
