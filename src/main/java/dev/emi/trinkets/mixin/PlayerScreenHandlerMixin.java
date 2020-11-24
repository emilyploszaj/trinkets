package dev.emi.trinkets.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketSlots;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Pair;

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
		TrinketsApi.getTrinketComponent(owner).ifPresent(trinkets -> {
			TrinketInventory inv = trinkets.getInventory();
			for (int i = 0; i < inv.size(); i++) {
				Pair<SlotType, Integer> p = inv.posMap.get(i);
				String group = p.getLeft().getGroup();
				int groupPos = inv.groupOffsetMap.get(p.getLeft()) + p.getRight();
				int groupAmount = inv.groupOccupancyMap.get(TrinketSlots.getPlayerSlots().get(group));
				if (group.equals("hand")) { // TODO move to slot group
					groupAmount += 1;
					groupPos += 1;
				}
				groupPos = groupPos - groupAmount / 2;
				if (!group.equals("hand") && groupPos >= 0) groupPos++; // TODO only slot groups not based on vanilla slots
				int x = getGroupX(group) + 1;
				int y = getGroupY(group) + 1;
				x += groupPos * 18;
				this.addSlot(new TrinketSlot(inv, i, x, y, TrinketSlots.getPlayerSlots().get(group), p.getLeft(), groupPos == 0, p.getRight() == 0));
			}
		});
	}

	// TODO put this info somewhere else, this is for testing
	public int getGroupX(String group) {
		if (group.equals("head")) {
			return 7;
		} else if (group.equals("chest")) {
			return 7;
		} else if (group.equals("legs")) {
			return 7;
		} else if (group.equals("feet")) {
			return 7;
		} else {
			return 76;
		}
	}

	// TODO put this info somewhere else, this is for testing
	public int getGroupY(String group) {
		if (group.equals("head")) {
			return 7;
		} else if (group.equals("chest")) {
			return 25;
		} else if (group.equals("legs")) {
			return 43;
		} else if (group.equals("feet")) {
			return 61;
		} else if (group.equals("offhand")) {
			return 61;
		} else {
			return 43;
		}
	}
}
