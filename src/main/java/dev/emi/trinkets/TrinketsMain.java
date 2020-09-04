package dev.emi.trinkets;

import dev.emi.trinkets.data.SlotLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrinketsMain implements ModInitializer {

  public static final String MOD_ID = "trinkets";
  public static final Logger LOGGER = LogManager.getLogger();

  @Override
  public void onInitialize() {
    ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(SlotLoader.INSTANCE);
  }
}