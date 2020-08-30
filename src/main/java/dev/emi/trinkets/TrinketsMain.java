package dev.emi.trinkets;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.TrinketComponent;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public class TrinketsMain implements ModInitializer, EntityComponentInitializer {
	public static final ComponentKey<TrinketComponent> TRINKETS = ComponentRegistryV3.INSTANCE.getOrCreate(new Identifier("trinkets:trinkets"), TrinketComponent.class);

	@Override
	public void onInitialize() {
		
	}

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerFor(LivingEntity.class, TRINKETS, entity -> {
			return new LivingEntityTrinketComponent(entity);
		});
	}
}