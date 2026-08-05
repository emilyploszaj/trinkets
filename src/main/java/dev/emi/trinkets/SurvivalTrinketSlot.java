package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.mixin.accessor.RecipeBookScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * A gui slot for a trinket slot, used in the survival inventory, but suited for any case
 */
public class SurvivalTrinketSlot extends Slot implements TrinketSlot {
	private final SlotGroup group;
	private final SlotType type;
	private final boolean alwaysVisible;
	private final int slotOffset;
	private final TrinketInventory trinketInventory;

	public SurvivalTrinketSlot(TrinketInventory inventory, int index, int x, int y, SlotGroup group, SlotType type, int slotOffset,
			boolean alwaysVisible) {
		super(inventory, index, x, y);
		this.group = group;
		this.type = type;
		this.slotOffset = slotOffset;
		this.alwaysVisible = alwaysVisible;
		this.trinketInventory = inventory;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return TrinketSlot.canInsert(stack, new SlotReference(trinketInventory, slotOffset), trinketInventory.getComponent().getEntity());
	}

	@Override
	public boolean canTakeItems(PlayerEntity player) {
		ItemStack stack = this.getStack();
		return TrinketsApi.getTrinket(stack.getItem())
			.canUnequip(stack, new SlotReference(trinketInventory, slotOffset), player);
	}

	@Override
	public boolean isEnabled() {
		if (alwaysVisible) {
			if (x < 0) {
				World world = trinketInventory.getComponent().getEntity().getEntityWorld();
				if (world.isClient()) {
					MinecraftClient client = MinecraftClient.getInstance();
					Screen s = client.currentScreen;
					if (s instanceof InventoryScreen screen) {
						if (((RecipeBookScreenAccessor) screen).getRecipeBook().isOpen()) {
							return false;
						}
					}
				}
			}
			return true;
		}
		return isTrinketFocused();
	}

	@Override
	public boolean isTrinketFocused() {
		if (TrinketsClient.activeGroup == group) {
			return slotOffset == 0 || TrinketsClient.activeType == type;
		} else if (TrinketsClient.quickMoveGroup == group) {
			return slotOffset == 0 || TrinketsClient.quickMoveType == type && TrinketsClient.quickMoveTimer > 0;
		}
		return false;
	}

	@Override
	public boolean renderAfterRegularSlots() {
		return slotOffset != 0 || !this.alwaysVisible;
	}

	@Override
	public Identifier getBackgroundIdentifier() {
		return type.getIcon();
	}

	@Override
	public SlotType getType() {
		return type;
	}
}
