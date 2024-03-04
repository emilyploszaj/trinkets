package dev.emi.trinkets.compat;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketScreen;
import dev.emi.trinkets.TrinketScreenManager;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.minecraft.client.util.math.Rect2i;

public class TrinketsREIPlugin implements REIClientPlugin {

	@Override
	public void registerExclusionZones(ExclusionZones zones) {
		zones.register(TrinketScreen.class, screen -> {
			List<Rectangle> rects = new ArrayList<>();
			Rect2i rect = TrinketScreenManager.currentBounds;
			int x = screen.trinkets$getX();
			int y = screen.trinkets$getY();
			rects.add(
				new Rectangle(rect.getX() + x, rect.getY() + y, rect.getWidth(), rect.getHeight()));
			TrinketPlayerScreenHandler handler = screen.trinkets$getHandler();
			int groupCount = handler.trinkets$getGroupCount();

			if (groupCount <= 0 || screen.trinkets$isRecipeBookOpen()) {
				return List.of();
			}
			int width = groupCount / 4;
			int height = groupCount % 4;
			if (width > 0) {
				rects.add(new Rectangle(-4 - 18 * width + x, y, 7 + 18 * width, 86));
			}
			if (height > 0) {
				rects.add(new Rectangle(-22 - 18 * width + x, y, 25, 14 + 18 * height));
			}
			return rects;
		});
	}
}
