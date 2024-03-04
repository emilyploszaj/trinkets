package dev.emi.trinkets.compat;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketScreen;
import dev.emi.trinkets.TrinketScreenManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.Rect2i;

public class ExclusionCreator {

	public static List<Rect2i> createExclusionAreas(TrinketScreen screen) {
		List<Rect2i> rects = new ArrayList<>();
		Rect2i rect = TrinketScreenManager.currentBounds;
		int x = screen.trinkets$getX();
		int y = screen.trinkets$getY();
		rects.add(new Rect2i(rect.getX() + x, rect.getY() + y, rect.getWidth(), rect.getHeight()));
		TrinketPlayerScreenHandler handler = screen.trinkets$getHandler();
		int groupCount = handler.trinkets$getGroupCount();

		if (groupCount <= 0 || screen.trinkets$isRecipeBookOpen()) {
			return List.of();
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
}
