package dev.emi.trinkets.api;

import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
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
	 * @return Whether the itemstack can be inserted into a slot
	 */
	public default boolean canInsert(ItemStack stack){
		return true;
	}
	/**
	 * @return Whether the itemstack can be removed from a slot
	 */
	public default boolean canTake(ItemStack stack){
		return true;
	}
	/**
	 * Called when equipped by a player
	 */
	public default void onEquip(PlayerEntity player, ItemStack stack){
	}
	/**
	 * Called when unequipped by a player
	 */
	public default void onUnequip(PlayerEntity player, ItemStack stack){
	}
	/**
	 * Called to render the trinket
	 * @see {@link #translateToFace(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 * @see {@link #translateToChest(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 * @see {@link #translateToRightArm(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 * @see {@link #translateToLeftArm(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 * @see {@link #translateToRightLeg(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 * @see {@link #translateToLeftLeg(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 */
	public default void render(PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch){
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
	//Helper stuff for rendering
	/**
	 * Translates the rendering context to the center of the player's face, parameters should be passed from {@link #render(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 */
	public static void translateToFace(PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch){
		if(player.isInSwimmingPose() || player.isFallFlying()){
			GlStateManager.rotatef(model.headwear.roll, 0.0F, 0.0F, 1.0F);
			GlStateManager.rotatef(headYaw, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotatef(-45.0F, 1.0F, 0.0F, 0.0F);
		}else{
			if(player.isInSneakingPose() && !model.isRiding){
				GlStateManager.translatef(0.0F, 0.25F, 0.0F);
			}
			GlStateManager.rotatef(headYaw, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotatef(headPitch, 1.0F, 0.0F, 0.0F);
		}
		GlStateManager.translatef(0.0F, -0.25F, -0.3F);
	}
	/**
	 * Translates the rendering context to the center of the player's chest/torso segment, parameters should be passed from {@link #render(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 */
	public static void translateToChest(PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch){
		if(player.isInSneakingPose() && !model.isRiding && !player.isSwimming()){
			GlStateManager.translatef(0.0F, 0.2F, 0.0F);
			GlStateManager.rotatef(model.body.pitch * 60, 1.0F, 0.0F, 0.0F);
		}
		GlStateManager.rotatef(model.body.yaw * 60, 0.0F, 1.0F, 0.0F);
		GlStateManager.translatef(0.0F, 0.4F, -0.16F);
	}
	/**
	 * Translates the rendering context to the center of the bottom of the player's right arm, parameters should be passed from {@link #render(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 */
	public static void translateToRightArm(PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch){
		if(player.isInSneakingPose() && !model.isRiding && !player.isSwimming()){
			GlStateManager.translatef(0.0F, 0.2F, 0.0F);
		}
		GlStateManager.rotatef(model.body.yaw * 60, 0.0F, 1.0F, 0.0F);
		GlStateManager.translatef(-0.3125F, 0.15625F, 0.0F);
		GlStateManager.rotatef(model.rightArm.roll * 57.5F, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotatef(model.rightArm.yaw * 57.5F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(model.rightArm.pitch * 57.5F, 1.0F, 0.0F, 0.0F);
		GlStateManager.translatef(-0.0625F, 0.625F, 0.0F);
	}
	/**
	 * Translates the rendering context to the center of the bottom of the player's left arm, parameters should be passed from {@link #render(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 */
	public static void translateToLeftArm(PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch){
		if(player.isInSneakingPose() && !model.isRiding && !player.isSwimming()){
			GlStateManager.translatef(0.0F, 0.2F, 0.0F);
		}
		GlStateManager.rotatef(model.body.yaw * 60, 0.0F, 1.0F, 0.0F);
		GlStateManager.translatef(0.3125F, 0.15625F, 0.0F);
		GlStateManager.rotatef(model.leftArm.roll * 57.5F, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotatef(model.leftArm.yaw * 57.5F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(model.leftArm.pitch * 57.5F, 1.0F, 0.0F, 0.0F);
		GlStateManager.translatef(0.0625F, 0.625F, 0.0F);
	}
	/**
	 * Translates the rendering context to the center of the bottom of the player's right leg, parameters should be passed from {@link #render(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 */
	public static void translateToRightLeg(PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch){
		if(player.isInSneakingPose() && !model.isRiding && !player.isSwimming()){
			GlStateManager.translatef(0.0F, 0.0F, 0.25F);
		}
		GlStateManager.translatef(-0.125F, 0.75F, 0.0F);
		GlStateManager.rotatef(model.rightLeg.roll * 57.5F, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotatef(model.rightLeg.yaw * 57.5F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(model.rightLeg.pitch * 57.5F, 1.0F, 0.0F, 0.0F);
		GlStateManager.translatef(0.0F, 0.75F, 0.0F);
	}
	/**
	 * Translates the rendering context to the center of the bottom of the player's left leg, parameters should be passed from {@link #render(PlayerEntityModel, AbstractClientPlayerEntity, float, float)}
	 */
	public static void translateToLeftLeg(PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch){
		if(player.isInSneakingPose() && !model.isRiding && !player.isSwimming()){
			GlStateManager.translatef(0.0F, 0.0F, 0.25F);
		}
		GlStateManager.translatef(0.125F, 0.75F, 0.0F);
		GlStateManager.rotatef(model.leftLeg.roll * 57.5F, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotatef(model.leftLeg.yaw * 57.5F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(model.leftLeg.pitch * 57.5F, 1.0F, 0.0F, 0.0F);
		GlStateManager.translatef(0.0F, 0.75F, 0.0F);
	}
}