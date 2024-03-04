package dev.emi.trinkets.compat;

import dev.emi.trinkets.TrinketScreen;
import java.util.stream.Collectors;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;

public class TrinketsREIPlugin implements REIClientPlugin {

	@Override
	public void registerExclusionZones(ExclusionZones zones) {
		zones.register(TrinketScreen.class,
			screen -> ExclusionCreator.createExclusionAreas(screen).stream().map(
				rect2i -> new Rectangle(rect2i.getX(), rect2i.getY(), rect2i.getWidth(),
					rect2i.getHeight())).collect(Collectors.toList()));
	}
}
