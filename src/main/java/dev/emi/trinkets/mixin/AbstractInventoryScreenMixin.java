package dev.emi.trinkets.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Changes the rendering order of potion effects
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin<T extends ScreenHandler> extends HandledScreen<T> {
	@Shadow
	protected boolean drawStatusEffects;
	
	public AbstractInventoryScreenMixin(T screenHandler, PlayerInventory inventory, Text text) {
		super(screenHandler, inventory, text);
	}
	
	@Shadow
	protected abstract void drawStatusEffects(MatrixStack matrices);

	/**
	 * I was getting compiler warnings without this comment
	 * @author Emi
	 * @reason Had to change render order
	 */
	@Overwrite
	public void render(MatrixStack matrices, int left, int top, float f) {
		if (this.drawStatusEffects) {
		   this.drawStatusEffects(matrices);
		}
		super.render(matrices, left, top, f);
	 }
}