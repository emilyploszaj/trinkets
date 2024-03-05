package dev.emi.trinkets.compat;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketScreen;
import dev.emi.trinkets.TrinketScreenManager;
import dev.emi.trinkets.TrinketsClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.util.math.Rect2i;

public class TrinketsExclusionAreas {

	public static List<Rect2i> create(Screen screen) {
		if (screen instanceof TrinketScreen trinketScreen) {
			if (screen instanceof CreativeInventoryScreen creativeInventoryScreen &&
				!creativeInventoryScreen.isInventoryTabSelected()) {
				return List.of();
			}
			List<Rect2i> rects = new ArrayList<>();
			int x = trinketScreen.trinkets$getX();
			int y = trinketScreen.trinkets$getY();
			TrinketPlayerScreenHandler handler = trinketScreen.trinkets$getHandler();
			int groupCount = handler.trinkets$getGroupCount();
			if (groupCount <= 0 || trinketScreen.trinkets$isRecipeBookOpen()) {
				return List.of();
			}

			if (TrinketsClient.activeGroup != null) {
				Rect2i rect = TrinketScreenManager.currentBounds;
				rects.add(
					new Rect2i(rect.getX() + x, rect.getY() + y, rect.getWidth(),
						rect.getHeight()));
			}
			int width = groupCount / 4;
			int height = groupCount % 4;
			if (width > 0) {
				rects.add(new Rect2i(-4 - 18 * width + x, y, 7 + 18 * width, 86));
			}

			if (height > 0) {
				rects.add(new Rect2i(-22 - 18 * width + x, y, 25, 14 + 18 * height));
			}
			return rects;
		}
		return List.of();
	}
}
