package dev.emi.trinkets.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Function3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.Trinket.SlotReference;
import dev.emi.trinkets.api.TrinketInventory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

/**
 * Adds trinket slots to the player's screen handler
 *
 * @author Emi
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandler implements TrinketPlayerScreenHandler {

	@Shadow @Final
	private PlayerEntity owner;

	// TODO store this exclusively in the screen handler and mixin an interface to get it from the screen
	private Map<SlotGroup, Pair<Integer, Integer>> groupPos = new HashMap<SlotGroup, Pair<Integer, Integer>>();
	public int trinketSlotStart = 0;
	public int trinketSlotEnd = 0;

	protected PlayerScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	public void init(PlayerInventory playerInv, boolean onServer, PlayerEntity owner, CallbackInfo info) {
		updateTrinketSlots();
	}

	@Override
	public void updateTrinketSlots() {
		groupPos.clear();
		while (trinketSlotStart < trinketSlotEnd) {
			slots.remove(trinketSlotStart);
			trinketSlotEnd--;
		}
		Map<Integer, SlotGroup> ids = new HashMap<Integer, SlotGroup>(); 
		for (SlotGroup group : TrinketsApi.getPlayerSlots().values()) {
			if (group.getSlotId() != -1) {
				ids.put(group.getSlotId(), group);
			}
		}
		for (Slot slot : this.slots) {
			if (ids.containsKey(slot.id) && slot.inventory instanceof PlayerInventory) {
				groupPos.put(ids.get(slot.id), new Pair<Integer, Integer>(slot.x, slot.y));
			}
		}
		int groupNum = 1; // Start at 1 because offhand exists
		if (TrinketsApi.getPlayerSlots().containsKey("hand")) { // Hardcode the hand slot group to always be above the offhand, if it exists
			groupNum++;
			groupPos.put(TrinketsApi.getPlayerSlots().get("hand"), new Pair<Integer, Integer>(77, 44));
		}
		for (SlotGroup group : TrinketsApi.getPlayerSlots().values()) {
			if (!groupPos.containsKey(group)) {
				int x = 77;
				int y = 0;
				if (groupNum >= 4) {
					x = -4 - (x / 4) * 18;
					y = 7 + (x % 4) * 18;
				} else {
					y = 62 - x * 18;
				}
				groupPos.put(group, new Pair<Integer, Integer>(x, y));
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
				Pair<Integer, Integer> pos = getGroupPos(group);
				this.addSlot(new TrinketSlot(inv, i, pos.getLeft() + groupPos * 18, pos.getRight(), group, p.getLeft(), p.getRight(), groupPos == 0, p.getRight() == 0));
			}
		});
		trinketSlotEnd = slots.size();
	}
	
	@Override
	public Pair<Integer, Integer> getGroupPos(SlotGroup group) {
		return groupPos.get(group);
	}

	@Inject(at = @At("HEAD"), method = "close")
	private void close(PlayerEntity player, CallbackInfo info) {
		if (player.world.isClient) {
			TrinketsClient.activeGroup = null;
			TrinketsClient.activeType = null;
			TrinketsClient.quickMoveGroup = null;
		}
	}

	@Inject(at = @At("HEAD"), method = "transferSlot", cancellable = true)
	public void transferSlot(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> info) {
		Slot slot = slots.get(index);
		if (slot != null && slot.hasStack()) {
			ItemStack stack = slot.getStack();
			if (index >= trinketSlotStart && index < trinketSlotEnd) {
				if (!this.insertItem(stack, 9, 45, false)) {
					info.setReturnValue(ItemStack.EMPTY);
				} else {
					info.setReturnValue(stack);
				}
			} else if (index >= 9 && index < 45) {
				TrinketsApi.getTrinketComponent(owner).ifPresent(comp -> {
					TrinketInventory inv = comp.getInventory();
					for (int i = 0; i < inv.size(); i++) {
						if (!slots.get(trinketSlotStart + i).canInsert(stack)) {
							continue;
						}
						Pair<SlotType, Integer> pair = inv.posMap.get(i);
						SlotType type = pair.getLeft();
						SlotReference ref = new SlotReference(type, pair.getRight());
						TriState state = TriState.DEFAULT;
						for (Identifier id : type.getValidators()) {
							Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> function = TrinketsApi.getValidatorPredicator(id);
							if (function.isPresent()) {
								state = function.get().apply(stack, ref, owner);
							}
							if (state != TriState.DEFAULT) {
								break;
							}
						}
						if (state == TriState.DEFAULT) {
							state = TrinketsApi.getQuickMovePredicate(new Identifier("trinkets", "always")).get().apply(stack, ref, owner);
						}
						if (this.insertItem(stack, trinketSlotStart + i, trinketSlotStart + i + 1, false)) {
							if (owner.world.isClient) {
								TrinketsClient.quickMoveTimer = 20;
								TrinketsClient.quickMoveGroup = TrinketsApi.getPlayerSlots().get(type.getGroup());
								if (ref.index > 0) {
									TrinketsClient.quickMoveType = type;
								}
							}
						}
					}
				});
			}
		}
	}
}
