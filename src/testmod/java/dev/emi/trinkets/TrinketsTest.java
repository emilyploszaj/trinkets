package dev.emi.trinkets;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsTest implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitialize() {
		LOGGER.info("[Trinkets Testmod] test mod was initialized!");
	}
}
