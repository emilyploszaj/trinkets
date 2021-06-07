package dev.emi.trinkets;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsTest implements ModInitializer {

	public static final String MOD_ID = "trinkets-testmod";
	public static final Logger LOGGER = LogManager.getLogger();
	public static Item TEST_TRINKET;

	@Override
	public void onInitialize() {
		LOGGER.info("[Trinkets Testmod] test mod was initialized!");
		TEST_TRINKET = Registry.register(Registry.ITEM, identifier("test"), new TestTrinket(new Item.Settings().maxCount(1).maxDamage(100)));
	}

	private static Identifier identifier(String id) {
		return new Identifier(MOD_ID, id);
	}
}
