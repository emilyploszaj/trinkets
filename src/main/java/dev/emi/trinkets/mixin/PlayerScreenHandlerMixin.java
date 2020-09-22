package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.TrinketInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

/**
 * Adds trinket slots to the player's screen handler
 * 
 * @author Emi 
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandler {
	public int trinketSlotStart;

	protected PlayerScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	public void init(PlayerInventory playerInv, boolean onServer, PlayerEntity owner, CallbackInfo info) {
		trinketSlotStart = slots.size();
		TrinketInventory inv = TrinketsMain.TRINKETS.get(owner).getInventory();
		for (int i = 0; i < inv.size(); i++) {
			this.addSlot(new Slot(inv, i, i * 16, 0)); // TODO actually do something reasonable with the slots
		}
	}
}
