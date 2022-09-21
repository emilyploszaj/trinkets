package dev.emi.trinkets.api;

import dev.emi.trinkets.TrinketSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * A convenient base class for trinket items that automatically registers itself and
 */
public class TrinketItem extends Item implements Trinket {

	public TrinketItem(Item.Settings settings) {
		super(settings);
		TrinketsApi.registerTrinket(this, this);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (equipItem(user, stack)) {
			return TypedActionResult.success(stack, world.isClient());
		}
		return super.use(world, user, hand);
	}

	public static boolean equipItem(PlayerEntity user, ItemStack stack) {
		var optional = TrinketsApi.getTrinketComponent(user);
		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			for (var group : comp.getInventory().values()) {
				for (TrinketInventory inv : group.values()) {
					for (int i = 0; i < inv.size(); i++) {
						if (inv.getStack(i).isEmpty()) {
							SlotReference ref = new SlotReference(inv, i);
							if (TrinketSlot.canInsert(stack, ref, user)) {
								Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
								ItemStack inserted = stack.split(trinket.getMaxCount(stack, ref));
								inv.setStack(i, inserted);
								SoundEvent soundEvent = inserted.getEquipSound();
								if (!inserted.isEmpty() && soundEvent != null) {
								   user.emitGameEvent(GameEvent.EQUIP);
								   user.playSound(soundEvent, 1.0F, 1.0F);
								}
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}
