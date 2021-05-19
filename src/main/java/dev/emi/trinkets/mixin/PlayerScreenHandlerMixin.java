package dev.emi.trinkets.mixin;

import com.mojang.datafixers.util.Function3;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.TrinketsClient;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket.SlotReference;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

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
	private final Map<SlotGroup, Integer> slotWidths = new HashMap<>();
	@Unique
	private int trinketSlotStart = 0;
	@Unique
	private int trinketSlotEnd = 0;
	@Unique
	private PlayerInventory inventory;

	protected PlayerScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	public void init(PlayerInventory playerInv, boolean onServer, PlayerEntity owner, CallbackInfo info) {
		this.inventory = playerInv;
		updateTrinketSlots();
	}

	@Override
	public void updateTrinketSlots() {
		TrinketsApi.getTrinketComponent(owner).ifPresent(trinkets -> {
			trinkets.update();
			Map<String, SlotGroup> groups = trinkets.getGroups();
			groupPos.clear();
			while (trinketSlotStart < trinketSlotEnd) {
				slots.remove(trinketSlotStart);
				trinketSlotEnd--;
			}

			for (SlotGroup group : groups.values()) {
				int id = group.getSlotId();
				if (id != -1 && this.slots.size() > id) {
					Slot slot = this.slots.get(id);
					if (slot.inventory instanceof PlayerInventory) {
						groupPos.put(group, new Pair<>(slot.x, slot.y));
					}
				}
			}

			int groupNum = 1; // Start at 1 because offhand exists
			SlotGroup hand = groups.get("hand");
			if (hand != null) { // Hardcode the hand slot group to always be above the offhand, if it exists
				groupNum++;
				groupPos.put(hand, new Pair<>(77, 44));
			}

			for (SlotGroup group : groups.values()) {
				if (!groupPos.containsKey(group)) {
					int x = 77;
					int y;
					if (groupNum >= 4) {
						x = -4 - (groupNum / 4) * 18;
						y = 7 + (groupNum % 4) * 18;
					} else {
						y = 62 - groupNum * 18;
					}
					groupPos.put(group, new Pair<>(x, y));
					groupNum++;
				}
			}

			trinketSlotStart = slots.size();
			slotWidths.clear();
			slotHeights.clear();
			for (Map.Entry<String, Map<String, TrinketInventory>> entry : trinkets.getInventory().entrySet()) {
				String groupId = entry.getKey();
				SlotGroup group = groups.get(groupId);
				int groupOffset = 1;

				if (group.getSlotId() != -1) {
					groupOffset++;
				}
				int width = 0;
				Pair<Integer, Integer> pos = getGroupPos(group);
				for (Map.Entry<String, TrinketInventory> slot : entry.getValue().entrySet()) {
					TrinketInventory stacks = slot.getValue();
					int slotOffset = 1;
					int x = (int) (pos.getLeft() + (groupOffset / 2) * 18 * Math.pow(-1, groupOffset));
					slotHeights.computeIfAbsent(group, (k) -> new ArrayList<>()).add(new Pair<>(x, stacks.size()));
					for (int i = 0; i < stacks.size(); i++) {
						int y = (int) (pos.getRight() + (slotOffset / 2) * 18 * Math.pow(-1, slotOffset));
						TrinketsMain.LOGGER.info(stacks.getSlotType().getGroup() + ":" + stacks.getSlotType().getName() + ":" + i);
						TrinketsMain.LOGGER.info("X: " + x + " Y: " + y);
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
	
	@Override
	public Pair<Integer, Integer> getGroupPos(SlotGroup group) {
		return groupPos.get(group);
	}

	@Override
	public List<Pair<Integer, Integer>> getSlotHeights(SlotGroup group) {
		return slotHeights.get(group);
	}

	@Override
	public int getSlotWidth(SlotGroup group) {
		return slotWidths.get(group);
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

		if (slot.hasStack()) {
			ItemStack stack = slot.getStack();
			if (index >= trinketSlotStart && index < trinketSlotEnd) {
				if (!this.insertItem(stack, 9, 45, false)) {
					info.setReturnValue(ItemStack.EMPTY);
				} else {
					info.setReturnValue(stack);
				}
			} else if (index >= 9 && index < 45) {
				TrinketsApi.getTrinketComponent(owner).ifPresent(trinkets ->
						trinkets.forEach((slotReference, itemStack) -> {
							int i = slotReference.index;
							if (!slots.get(trinketSlotStart + i).canInsert(stack)) {
								return;
							}

							TriState state = TriState.DEFAULT;
							SlotType type = slotReference.inventory.getSlotType();
							for (Identifier id : type.getValidators()) {
								Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> function = TrinketsApi.getValidatorPredicate(id);
								if (function.isPresent()) {
									state = function.get().apply(stack, slotReference, owner);
								}
								if (state != TriState.DEFAULT) {
									break;
								}
							}

							if (state == TriState.DEFAULT) {
								Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> quickMovePredicate =
										TrinketsApi.getQuickMovePredicate(new Identifier("trinkets", "always"));

								if (quickMovePredicate.isPresent()) {
									// FIXME: state is unused
									state = quickMovePredicate.get().apply(stack, slotReference, owner);
								}
							}

							if (this.insertItem(stack, trinketSlotStart + i, trinketSlotStart + i + 1, false)) {
								if (owner.world.isClient) {
									TrinketsClient.quickMoveTimer = 20;
									TrinketsClient.quickMoveGroup = TrinketsApi.getPlayerSlots().get(type.getGroup());

									if (i > 0) {
										TrinketsClient.quickMoveType = type;
									}
								}
							}
						})
				);
			}
		}
	}
}
