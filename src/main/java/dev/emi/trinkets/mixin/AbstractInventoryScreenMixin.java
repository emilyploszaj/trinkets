package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Adds trinket slots to the PlayerContainer on initialization
 */
@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin<T extends Container> extends AbstractContainerScreen<T> {
	@Shadow
	protected boolean offsetGuiForEffects;
	public AbstractInventoryScreenMixin(T container_1, PlayerInventory playerInventory_1, Text text_1) {
		super(container_1, playerInventory_1, text_1);
	}
	@Shadow
	protected abstract void drawPotionEffects();
	/**
	 * I was getting compiler warnings without this comment
	 * @author Emi
	 * @reason Had to change render order
	 */
	@Overwrite
	public void render(int int_1, int int_2, float float_1){
		if (this.offsetGuiForEffects) {
		   this.drawPotionEffects();
		}
		super.render(int_1, int_2, float_1);
	 }
}