package dev.emi.trinkets.api;

import java.util.List;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/**
 * Trinkets should extend this interface to be usable in trinket slots
 */
public interface ITrinket{
	/**
	 * @return Whether the provided slot is valid for this item
	 */
	public boolean canWearInSlot(String group, String slot);
	/**
	 * Called once per tick while being worn by a player
	 */
	public default void tick(PlayerEntity player, ItemStack stack){
	}
	/**
	 * @return Whether the itemstack can be inserted into the slot
	 */
	public default boolean canInsert(ItemStack stack){
		return true;
	}
	/**
	 * @return Whether the itemstack can be removed from the slot
	 */
	public default boolean canTake(ItemStack stack){
		return true;
	}
	/**
	 * Called when equipped by a player
	 */
	public default void onEquip(ItemStack stack){
	}
	/**
	 * Called when unequipped by a player
	 */
	public default void onUnequip(ItemStack stack){
	}
	
	//Helper stuff for creating trinkets that interact with vanilla behavior properly
	public static final DispenserBehavior TRINKET_DISPENSER_BEHAVIOR = new ItemDispenserBehavior() {
		protected ItemStack dispenseSilently(BlockPointer blockPointer_1, ItemStack itemStack_1){
			ItemStack itemStack_2 = dispenseTrinket(blockPointer_1, itemStack_1);
			return itemStack_2.isEmpty() ? super.dispenseSilently(blockPointer_1, itemStack_1) : itemStack_2;
		}
	};
	public static ItemStack dispenseTrinket(BlockPointer blockPointer_1, ItemStack itemStack_1) {
		BlockPos blockPos_1 = blockPointer_1.getBlockPos().offset((Direction)blockPointer_1.getBlockState().get(DispenserBlock.FACING));
		List<LivingEntity> list_1 = blockPointer_1.getWorld().getEntities(LivingEntity.class, new Box(blockPos_1), EntityPredicates.EXCEPT_SPECTATOR.and(new EntityPredicates.CanPickup(itemStack_1)));
		if(list_1.isEmpty()){
			return ItemStack.EMPTY;
		}else{
			LivingEntity livingEntity_1 = (LivingEntity)list_1.get(0);
			if(livingEntity_1 instanceof PlayerEntity){
				TrinketComponent comp = TrinketsApi.getTrinketComponent((PlayerEntity) livingEntity_1);
				if(comp.equip(itemStack_1)){
					itemStack_1.setCount(0);
				}
			}
			return itemStack_1;
		}
	}
	public static TypedActionResult<ItemStack> equipTrinket(PlayerEntity player, Hand hand){
		ItemStack itemStack_1 = player.getStackInHand(hand);
		TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
		if(comp.equip(itemStack_1)){
			itemStack_1.setCount(0);
			return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, itemStack_1);
		}else{
			return new TypedActionResult<ItemStack>(ActionResult.FAIL, itemStack_1);
		}
	}
}