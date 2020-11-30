package dev.emi.trinkets.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketInventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Pair;

/**
 * Adds trinket slots to the player's screen handler
 *
 * @author Emi
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandler {

	// TODO store this exclusively in the screen handler and mixin an interface to get it from the screen
	private Map<SlotGroup, Pair<Integer, Integer>> slotPos = new HashMap<SlotGroup, Pair<Integer, Integer>>();
	public int trinketSlotStart;

	protected PlayerScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	public void init(PlayerInventory playerInv, boolean onServer, PlayerEntity owner, CallbackInfo info) {
		Map<Integer, SlotGroup> ids = new HashMap<Integer, SlotGroup>(); 
		for (SlotGroup group : TrinketsApi.getPlayerSlots().values()) {
			if (group.getSlotId() != -1) {
				ids.put(group.getSlotId(), group);
			}
		}
		for (Slot slot : this.slots) {
			if (ids.containsKey(slot.id) && slot.inventory instanceof PlayerInventory) {
				slotPos.put(ids.get(slot.id), new Pair<Integer, Integer>(slot.x, slot.y));
			}
		}
		int groupNum = 1; // Start at 1 because offhand exists
		if (TrinketsApi.getPlayerSlots().containsKey("hand")) { // Hardcode the hand slot group to always be above the offhand, if it exists
			groupNum++;
			slotPos.put(TrinketsApi.getPlayerSlots().get("hand"), new Pair<Integer, Integer>(77, 44));
		}
		for (SlotGroup group : TrinketsApi.getPlayerSlots().values()) {
			if (!slotPos.containsKey(group)) {
				int x = 77;
				int y = 0;
				if (groupNum >= 4) {
					x = -4 - (x / 4) * 18;
					y = 7 + (x % 4) * 18;
				} else {
					y = 62 - x * 18;
				}
				slotPos.put(group, new Pair<Integer, Integer>(x, y));
			}
		}
		trinketSlotStart = slots.size();
		TrinketsApi.getTrinketComponent(owner).ifPresent(trinkets -> {
			TrinketInventory inv = trinkets.getInventory();
			for (int i = 0; i < inv.size(); i++) {
				Pair<SlotType, Integer> p = inv.posMap.get(i);
				SlotGroup group = TrinketsApi.getPlayerSlots().get(p.getLeft().getGroup());
				int groupPos = inv.groupOffsetMap.get(p.getLeft()) + p.getRight();
				int groupAmount = inv.groupOccupancyMap.get(group);
				if (group.getSlotId() == -1) {
					groupAmount += 1;
					groupPos += 1;
				}
				groupPos = groupPos - groupAmount / 2;
				if (group.getSlotId() != -1 && groupPos >= 0) groupPos++;
				Pair<Integer, Integer> pos = slotPos.get(group);
				this.addSlot(new TrinketSlot(inv, i, pos.getLeft() + groupPos * 18, pos.getRight(), group, p.getLeft(), p.getRight(), groupPos == 0, p.getRight() == 0));
			}
		});
	}

	@Inject(at = @At("HEAD"), method = "close")
	private void close(PlayerEntity player, CallbackInfo info) {
		if (player.world.isClient) {
			TrinketsClient.activeGroup = null;
			TrinketsClient.activeType = null;
		}
	}
}
