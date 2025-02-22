package dev.emi.trinkets.mixin;

import dev.emi.trinkets.TrinketEntityRenderState;
import dev.emi.trinkets.api.SlotReference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderState.class)
public class LivingEntityStateRenderMixin implements TrinketEntityRenderState {

    @Unique
    private List<Pair<ItemStack, SlotReference>> trinketsState;

    @Override
    public void trinkets$setState(List<Pair<ItemStack, SlotReference>> items) {
        this.trinketsState = items;
    }

    @Override
    public List<Pair<ItemStack, SlotReference>> trinkets$getState() {
        return this.trinketsState;
    }
}
