package dev.emi.trinkets;

import net.minecraft.client.gui.DrawContext;

public interface CreativeTrinketScreen {
    void trinkets$renderCreative(DrawContext context, int mouseX, int mouseY, float deltaTicks);
}
