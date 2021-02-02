package dev.emi.trinkets.client;

import dev.emi.trinkets.TrinketsTest;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;

public class TrinketsTestClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		TrinketRendererRegistry.registerRenderer(TrinketsTest.TEST_TRINKET, (TrinketRenderer) TrinketsTest.TEST_TRINKET);
	}
}
