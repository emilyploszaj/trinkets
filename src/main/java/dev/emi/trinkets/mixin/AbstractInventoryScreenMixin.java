package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Changes the rendering order of potion effects
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin<T extends Container> extends ContainerScreen<T> {
	@Shadow
	protected boolean offsetGuiForEffects;
	
	public AbstractInventoryScreenMixin(T container, PlayerInventory inventory, Text text) {
		super(container, inventory, text);
	}
	
	@Shadow
	protected abstract void drawStatusEffects();

	/**
	 * I was getting compiler warnings without this comment
	 * @author Emi
	 * @reason Had to change render order
	 */
	@Overwrite
	public void render(int left, int top, float f) {
		if (this.offsetGuiForEffects) {
		   this.drawStatusEffects();
		}
		super.render(left, top, f);
	 }
}