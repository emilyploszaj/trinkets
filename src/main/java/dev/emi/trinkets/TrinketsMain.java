package dev.emi.trinkets;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.data.EntitySlotLoader;
import dev.emi.trinkets.data.SlotLoader;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsMain implements ModInitializer, EntityComponentInitializer {

	public static final String MOD_ID = "trinkets";
	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitialize() {
		ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper.get(ResourceType.SERVER_DATA);
		resourceManagerHelper.registerReloadListener(SlotLoader.INSTANCE);
		resourceManagerHelper.registerReloadListener(EntitySlotLoader.INSTANCE);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
			(server, serverResourceManager, success) -> EntitySlotLoader.INSTANCE.sync(server.getPlayerManager().getPlayerList()));
	}

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(TrinketsApi.TRINKET_COMPONENT, LivingEntityTrinketComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
		registry.registerFor(LivingEntity.class, TrinketsApi.TRINKET_COMPONENT, LivingEntityTrinketComponent::new);
	}
}