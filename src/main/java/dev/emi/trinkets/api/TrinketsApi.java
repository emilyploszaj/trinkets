package dev.emi.trinkets.api;

import dev.emi.trinkets.data.EntitySlotLoader;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

public class TrinketsApi {

	public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
			.getOrCreate(new Identifier("trinkets:trinkets"), TrinketComponent.class);

	private static final Map<Item, Trinket> TRINKETS = new HashMap<Item, Trinket>();

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
}
