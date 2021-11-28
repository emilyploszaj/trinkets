package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.emi.trinkets.mixin.accessor.ScreenHandlerAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
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

	@Unique
	private final Map<SlotGroup, Pair<Integer, Integer>> groupPos = new HashMap<>();
	@Unique
	private final Map<SlotGroup, List<Pair<Integer, Integer>>> slotHeights = new HashMap<>();
	@Unique
	private final Map<SlotGroup, List<SlotType>> slotTypes = new HashMap<>();
	@Unique
	private final Map<SlotGroup, Integer> slotWidths = new HashMap<>();
	@Unique
	private int trinketSlotStart = 0;
	@Unique
	private int trinketSlotEnd = 0;
	@Unique
	private int groupCount = 0;
	@Unique
	private PlayerInventory inventory;

	private PlayerScreenHandlerMixin() {
		super(null, 0);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	private void init(PlayerInventory playerInv, boolean onServer, PlayerEntity owner, CallbackInfo info) {
		this.inventory = playerInv;
		updateTrinketSlots(true);
	}

	@Override
	public void updateTrinketSlots(boolean slotsChanged) {
		TrinketsApi.getTrinketComponent(owner).ifPresent(trinkets -> {
			if (slotsChanged) trinkets.update();
			Map<String, SlotGroup> groups = trinkets.getGroups();
			groupPos.clear();
			while (trinketSlotStart < trinketSlotEnd) {
				slots.remove(trinketSlotStart);
				((ScreenHandlerAccessor) (this)).getTrackedStacks().remove(trinketSlotStart);
				((ScreenHandlerAccessor) (this)).getPreviousTrackedStacks().remove(trinketSlotStart);
				trinketSlotEnd--;
			}

			int groupNum = 1; // Start at 1 because offhand exists

			for (SlotGroup group : groups.values().stream().sorted(Comparator.comparing(SlotGroup::getOrder)).toList()) {
				if (!hasSlots(trinkets, group)) {
					continue;
				}
				int id = group.getSlotId();
				if (id != -1) {
					if (this.slots.size() > id) {
						Slot slot = this.slots.get(id);
						if (!(slot instanceof TrinketSlot)) {
							groupPos.put(group, new Pair<>(slot.x, slot.y));
						}
					}
				} else {
					int x = 77;
					int y;
					if (groupNum >= 4) {
						x = 4 - (groupNum / 4) * 18;
						y = 8 + (groupNum % 4) * 18;
					} else {
						y = 62 - groupNum * 18;
					}
					groupPos.put(group, new Pair<>(x, y));
					groupNum++;
				}
			}
			if (groupNum > 4) {
				groupCount = groupNum - 4;
			}

			trinketSlotStart = slots.size();
			slotWidths.clear();
			slotHeights.clear();
			slotTypes.clear();
			for (Map.Entry<String, Map<String, TrinketInventory>> entry : trinkets.getInventory().entrySet()) {
				String groupId = entry.getKey();
				SlotGroup group = groups.get(groupId);
				int groupOffset = 1;

				if (group.getSlotId() != -1) {
					groupOffset++;
				}
				int width = 0;
				Pair<Integer, Integer> pos = getGroupPos(group);
				if (pos == null) {
					continue;
				}
				for (Map.Entry<String, TrinketInventory> slot : entry.getValue().entrySet().stream().sorted((a, b) ->
						Integer.compare(a.getValue().getSlotType().getOrder(), b.getValue().getSlotType().getOrder())).toList()) {
					TrinketInventory stacks = slot.getValue();
					if (stacks.size() == 0) {
						continue;
					}
					int slotOffset = 1;
					int x = (int) (pos.getLeft() + (groupOffset / 2) * 18 * Math.pow(-1, groupOffset));
					slotHeights.computeIfAbsent(group, (k) -> new ArrayList<>()).add(new Pair<>(x, stacks.size()));
					slotTypes.computeIfAbsent(group, (k) -> new ArrayList<>()).add(stacks.getSlotType());
					for (int i = 0; i < stacks.size(); i++) {
						int y = (int) (pos.getRight() + (slotOffset / 2) * 18 * Math.pow(-1, slotOffset));
						this.addSlot(new TrinketSlot(stacks, i, x, y, group, stacks.getSlotType(), i, groupOffset == 1 && i == 0));
						slotOffset++;
					}
					groupOffset++;
					width++;
				}
				slotWidths.put(group, width);
			}

			trinketSlotEnd = slots.size();
		});
	}

	@Unique
	private boolean hasSlots(TrinketComponent comp, SlotGroup group) {
		for (TrinketInventory inv : comp.getInventory().get(group.getName()).values()) {
			if (inv.size() > 0) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Pair<Integer, Integer> getGroupPos(SlotGroup group) {
		return groupPos.get(group);
	}

	@Override
	public List<Pair<Integer, Integer>> getSlotHeights(SlotGroup group) {
		return slotHeights.get(group);
	}

	@Override
	public List<SlotType> getSlotTypes(SlotGroup group) {
		return slotTypes.get(group);
	}

	@Override
	public int getSlotWidth(SlotGroup group) {
		return slotWidths.get(group);
	}

	@Override
	public int getGroupCount() {
		return groupCount;
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
	private void transferSlot(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> info) {
		Slot slot = slots.get(index);

		if (slot.hasStack()) {
			ItemStack stack = slot.getStack();
			if (index >= trinketSlotStart && index < trinketSlotEnd) {
				if (!this.insertItem(stack, 9, 45, false)) {
					info.setReturnValue(ItemStack.EMPTY);
				} else {
					info.setReturnValue(stack);
				}
			} else if (index >= 9 && index < 45) {
				TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
						for (int i = trinketSlotStart; i < trinketSlotEnd; i++) {
							Slot s = slots.get(i);
							if (!(s instanceof TrinketSlot) || !s.canInsert(stack)) {
								continue;
							}
							
							TrinketSlot ts = (TrinketSlot) s;
							SlotType type = ts.getType();
							SlotReference ref = new SlotReference((TrinketInventory) ts.inventory, ts.getIndex());

							boolean res = TrinketsApi.evaluatePredicateSet(type.getQuickMovePredicates(), stack, ref, player);

							if (res) {
								if (this.insertItem(stack, i, i + 1, false)) {
									if (player.world.isClient) {
										TrinketsClient.quickMoveTimer = 20;
										TrinketsClient.quickMoveGroup = TrinketsApi.getPlayerSlots().get(type.getGroup());
										if (ref.index() > 0) {
											TrinketsClient.quickMoveType = type;
										} else {
											TrinketsClient.quickMoveType = null;
										}
									}
								}
							}
						}
					}
				);
			}
		}
	}
}
