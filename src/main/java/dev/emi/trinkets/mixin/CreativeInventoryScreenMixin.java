package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Makes trinket slots not show up in the creative inventory in weird bad places
 * 
 * @author Emi
 */
@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin {

	@Redirect(at = @At(value = "INVOKE", target = "net/minecraft/util/collection/DefaultedList.size()I"), method = "setSelectedTab")
	private int size(DefaultedList<ItemStack> list) {
		return 46;
	}
}
