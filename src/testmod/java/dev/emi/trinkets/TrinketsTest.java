package dev.emi.trinkets;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsTest implements ModInitializer {

	public static final String MOD_ID = "trinkets-testmod";
	public static final Logger LOGGER = LogManager.getLogger();
	public static Item TEST_TRINKET;

	@Override
	public void onInitialize() {
		LOGGER.info("[Trinkets Testmod] test mod was initialized!");
		TEST_TRINKET = Registry.register(Registries.ITEM, identifier("test"), new TestTrinket(new Item.Settings().maxCount(1).maxDamage(100)));
	}

	private static Identifier identifier(String id) {
		return Identifier.of(MOD_ID, id);
	}
}
