package dev.emi.trinkets;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.data.EntitySlotLoader;
import dev.emi.trinkets.data.SlotLoader;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsMain implements ModInitializer, EntityComponentInitializer {

	public static final String MOD_ID = "trinkets";
	public static final Logger LOGGER = LogManager.getLogger();

	public static final ComponentKey<TrinketComponent> TRINKETS = ComponentRegistryV3.INSTANCE
			.getOrCreate(new Identifier("trinkets:trinkets"), TrinketComponent.class);

	@Override
	public void onInitialize() {
		ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper
				.get(ResourceType.SERVER_DATA);
		resourceManagerHelper.registerReloadListener(SlotLoader.INSTANCE);
		resourceManagerHelper.registerReloadListener(EntitySlotLoader.INSTANCE);
		/*TrinketsApi.registerTrinket(Items.DIAMOND, new Trinket(){
			
			@Override
			public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
				System.out.println(slot.slot.getName());
			}
		});*/
	}

  	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(TRINKETS, entity -> {
			return new LivingEntityTrinketComponent(entity);
		}, RespawnCopyStrategy.ALWAYS_COPY);
		registry.registerFor(LivingEntity.class, TRINKETS, entity -> {
			return new LivingEntityTrinketComponent(entity);
		});
	}
}