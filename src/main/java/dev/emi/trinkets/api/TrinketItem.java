package dev.emi.trinkets.api;

import dev.emi.trinkets.TrinketSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * A convenient base class for trinket items that automatically registers itself and
 */
public class TrinketItem extends Item implements Trinket {

	public TrinketItem(Item.Settings settings) {
		super(settings);
		TrinketsApi.registerTrinket(this, this);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (equipItem(user, stack)) {
			return ActionResult.SUCCESS;
		}
		return super.use(world, user, hand);
	}

	public static boolean equipItem(PlayerEntity user, ItemStack stack) {
		return equipItem((LivingEntity) user, stack);
	}

	public static boolean equipItem(LivingEntity user, ItemStack stack) {
		Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(user);
		if (optional.isPresent()) {
			TrinketComponent comp = optional.get();
			for (Map<String, TrinketInventory> group : comp.getInventory().values()) {
				for (TrinketInventory inv : group.values()) {
					for (int i = 0; i < inv.size(); i++) {
						if (inv.getStack(i).isEmpty()) {
							SlotReference ref = new SlotReference(inv, i);
							if (TrinketSlot.canInsert(stack, ref, user)) {
								ItemStack newStack = stack.copy();
								inv.setStack(i, newStack);
								Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
								RegistryEntry<SoundEvent> soundEvent = trinket.getEquipSound(stack, ref, user);
								if (!stack.isEmpty() && soundEvent != null) {
								   user.emitGameEvent(GameEvent.EQUIP);
								   user.playSound(soundEvent.value(), 1.0F, 1.0F);
								}
								stack.setCount(0);
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