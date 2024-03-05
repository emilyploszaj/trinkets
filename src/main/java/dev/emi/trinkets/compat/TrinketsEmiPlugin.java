package dev.emi.trinkets.compat;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.Rect2i;

public class TrinketsEmiPlugin implements EmiPlugin {

	@Override
	public void register(EmiRegistry registry) {
		registry.addExclusionArea(InventoryScreen.class, (screen, consumer) -> {
			for (Rect2i rect2i : TrinketsExclusionAreas.create(screen)) {
				consumer.accept(new Bounds(rect2i.getX(), rect2i.getY(), rect2i.getWidth(),
					rect2i.getHeight()));
			}
		});
		registry.addExclusionArea(CreativeInventoryScreen.class, (screen, consumer) -> {
			for (Rect2i rect2i : TrinketsExclusionAreas.create(screen)) {
				consumer.accept(new Bounds(rect2i.getX(), rect2i.getY(), rect2i.getWidth(),
					rect2i.getHeight()));
			}
		});
	}
}
