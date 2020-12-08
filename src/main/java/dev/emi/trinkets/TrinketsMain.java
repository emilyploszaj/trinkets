package dev.emi.trinkets;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.data.EntitySlotLoader;
import dev.emi.trinkets.data.SlotLoader;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceType;

import java.util.UUID;

import com.google.common.collect.Multimap;

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
		TrinketsApi.registerTrinket(Items.DIAMOND, new Trinket() {

			@Override
			public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot,
					LivingEntity entity, UUID uuid) {
				Multimap<EntityAttribute, EntityAttributeModifier> map = Trinket.super.getModifiers(stack, slot, entity, uuid);
				map.put(EntityAttributes.GENERIC_MAX_HEALTH, new EntityAttributeModifier(uuid, "Max health", 10.0d, Operation.ADDITION));
				return map;
			}
		});
	}

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(TrinketsApi.TRINKET_COMPONENT, entity -> {
			return new LivingEntityTrinketComponent(entity);
		}, RespawnCopyStrategy.ALWAYS_COPY);
		registry.registerFor(LivingEntity.class, TrinketsApi.TRINKET_COMPONENT, entity -> {
			return new LivingEntityTrinketComponent(entity);
		});
	}
}