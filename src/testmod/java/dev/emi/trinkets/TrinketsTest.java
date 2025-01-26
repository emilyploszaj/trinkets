package dev.emi.trinkets;

import dev.emi.trinkets.api.event.TrinketEquipCallback;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsTest implements ModInitializer {

	public static final String MOD_ID = "trinkets-testmod";
	public static final Logger LOGGER = LogManager.getLogger();
	public static Item TEST_TRINKET;
	public static Item TEST_TRINKET_2;

	@Override
	public void onInitialize() {
		LOGGER.info("[Trinkets Testmod] test mod was initialized!");
		TEST_TRINKET = Registry.register(Registries.ITEM, identifier("test"), new TestTrinket(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, identifier("test"))).maxCount(1).maxDamage(100)));
		TEST_TRINKET_2 = Registry.register(Registries.ITEM, identifier("test2"), new TestTrinket2(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, identifier("test2"))).maxCount(1)));
		TrinketEquipCallback.EVENT.register(((stack, slot, entity) -> {
			if(stack.isOf(TEST_TRINKET_2)){
				entity.getWorld().playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1f, 1f);
			}
		}));
		TrinketUnequipCallback.EVENT.register(((stack, slot, entity) -> {
			if(stack.isOf(TEST_TRINKET_2)){
				entity.getWorld().playSound(null, entity.getBlockPos(), SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.PLAYERS, 0.5f, 1f);
			}
		}));
	}

	private static Identifier identifier(String id) {
		return Identifier.of(MOD_ID, id);
	}
}