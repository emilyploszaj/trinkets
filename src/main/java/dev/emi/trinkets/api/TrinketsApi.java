package dev.emi.trinkets.api;

import com.mojang.datafixers.util.Function3;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.Trinket.SlotReference;
import dev.emi.trinkets.data.EntitySlotLoader;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TrinketsApi {

	public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
			.getOrCreate(new Identifier(TrinketsMain.MOD_ID, "trinkets"), TrinketComponent.class);
	private static final Map<Identifier, Function3<ItemStack, SlotReference, LivingEntity, TriState>> QUICK_MOVE_PREDICATES
			= new HashMap<>();
	private static final Map<Identifier, Function3<ItemStack, SlotReference, LivingEntity, TriState>> VALIDATOR_PREDICATES
			= new HashMap<>();

	private static final Map<Item, Trinket> TRINKETS = new HashMap<>();

	public static void registerTrinket(Item item, Trinket trinket) {
		TRINKETS.put(item, trinket);
	}

	public static Optional<Trinket> getTrinket(Item item) {
		return Optional.ofNullable(TRINKETS.get(item));
	}

	public static Optional<TrinketComponent> getTrinketComponent(LivingEntity livingEntity) {
		return TRINKET_COMPONENT.maybeGet(livingEntity);
	}

	public static Map<String, SlotGroup> getPlayerSlots() {
		return getEntitySlots(EntityType.PLAYER);
	}

	public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
		return EntitySlotLoader.INSTANCE.getEntitySlots(type);
	}

	public static void registerQuickMovePredicate(Identifier id, Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate) {
		QUICK_MOVE_PREDICATES.put(id, predicate);
	}

	public static void registerValidatorPredicate(Identifier id, Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate) {
		VALIDATOR_PREDICATES.put(id, predicate);
	}

	public static Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> getQuickMovePredicate(Identifier id) {
		if (QUICK_MOVE_PREDICATES.containsKey(id)) {
			return Optional.of(QUICK_MOVE_PREDICATES.get(id));
		}
		return Optional.empty();
	}

	public static Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> getValidatorPredicate(Identifier id) {
		if (VALIDATOR_PREDICATES.containsKey(id)) {
			return Optional.of(VALIDATOR_PREDICATES.get(id));
		}
		return Optional.empty();
	}

	static {
		TrinketsApi.registerQuickMovePredicate(new Identifier(TrinketsMain.MOD_ID, "always"), (stack, slot, entity) -> TriState.TRUE);
		TrinketsApi.registerQuickMovePredicate(new Identifier(TrinketsMain.MOD_ID, "never"), (stack, slot, entity) -> TriState.FALSE);
		TrinketsApi.registerValidatorPredicate(new Identifier(TrinketsMain.MOD_ID, "tag"), (stack, slot, entity) -> {
			SlotType slotType = slot.inventory.getSlotType();
			Tag<Item> tag = ItemTags.getTagGroup().getTagOrEmpty(new Identifier("trinkets", slotType.getGroup() + "/" + slotType.getName()));
			Tag<Item> all = ItemTags.getTagGroup().getTagOrEmpty(new Identifier("trinkets", "all"));
			if (tag.contains(stack.getItem()) || all.contains(stack.getItem())) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		TrinketsApi.registerValidatorPredicate(new Identifier(TrinketsMain.MOD_ID, "all"), (stack, slot, entity) -> TriState.TRUE);
		TrinketsApi.registerValidatorPredicate(new Identifier(TrinketsMain.MOD_ID, "none"), (stack, slot, entity) -> TriState.FALSE);
	}
}
