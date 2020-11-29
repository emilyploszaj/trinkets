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
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceType;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

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
		TrinketsApi.registerTrinket(Items.DIAMOND, new Trinket() {
			
			@Override
			public boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
				return true;
			}
		});
	}

	public void registerDefaultPredicates() {
		TrinketsApi.registerQuickMovePredicate(new Identifier(MOD_ID, "always"), (stack, slot, entity) -> {
			return TriState.TRUE;
		});
		TrinketsApi.registerQuickMovePredicate(new Identifier(MOD_ID, "never"), (stack, slot, entity) -> {
			return TriState.FALSE;
		});
		TrinketsApi.registerValidatorPredicate(new Identifier(MOD_ID, "tag"), (stack, slot, entity) -> {
			Tag<Item> tag = entity.world.getTagManager().getItems().getTagOrEmpty(new Identifier("trinkets", slot.slot.getGroup() + "/" + slot.slot.getName()));
			if (tag.contains(stack.getItem())) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		TrinketsApi.registerValidatorPredicate(new Identifier(MOD_ID, "all"), (stack, slot, entity) -> {
			return TriState.TRUE;
		});
		TrinketsApi.registerValidatorPredicate(new Identifier(MOD_ID, "none"), (stack, slot, entity) -> {
			return TriState.FALSE;
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