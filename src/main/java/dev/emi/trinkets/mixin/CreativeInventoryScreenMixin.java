package dev.emi.trinkets.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.api.TrinketSlots;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.container.Slot;

/**
 * The creative inventory draws slots by starting after armor and looping through the rest. This is absurd but I have to prevent it
 */
@Environment(EnvType.CLIENT)
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {
	
	@Redirect(at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"), method = "setSelectedTab")
	public int getEquippedStackProxy(List<Slot> slots) {
		return slots.size() - TrinketSlots.getSlotCount();
	}
}