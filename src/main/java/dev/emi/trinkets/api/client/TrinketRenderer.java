package dev.emi.trinkets.api.client;

import dev.emi.trinkets.api.Trinket;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;

public interface TrinketRenderer {

	void render(ItemStack stack, Trinket.SlotReference slot, MatrixStack matrixStack, VertexConsumerProvider vertexConsumer, int light,
			PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw, float headPitch);

	// todo: Review these translation methods before release
	/**
	 * Translates the rendering context to the center of the player's face
	 */
	static void translateToFace(MatrixStack matrixStack, PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player, float headYaw,
			float headPitch) {

		if (player.isInSwimmingPose() || player.isFallFlying()) {
			matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(model.head.roll));
			matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(headYaw));
			matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-45.0F));
		} else {

			if (player.isInSneakingPose() && !model.riding) {
				matrixStack.translate(0.0F, 0.25F, 0.0F);
			}
			matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(headYaw));
			matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(headPitch));
		}
		matrixStack.translate(0.0F, -0.25F, -0.3F);
	}

	/**
	 * Translates the rendering context to the center of the player's chest/torso segment
	 */
	static void translateToChest(MatrixStack matrixStack, PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrixStack.translate(0.0F, 0.2F, 0.0F);
			matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(model.torso.pitch * 57.5F));
		}
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(model.torso.yaw * 57.5F));
		matrixStack.translate(0.0F, 0.4F, -0.16F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right arm
	 */
	static void translateToRightArm(MatrixStack matrixStack, PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrixStack.translate(0.0F, 0.2F, 0.0F);
		}
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(model.torso.yaw * 57.5F));
		matrixStack.translate(-0.3125F, 0.15625F, 0.0F);
		matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(model.rightArm.roll * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(model.rightArm.yaw * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(model.rightArm.pitch * 57.5F));
		matrixStack.translate(-0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left arm
	 */
	static void translateToLeftArm(MatrixStack matrixStack, PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrixStack.translate(0.0F, 0.2F, 0.0F);
		}
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(model.torso.yaw * 57.5F));
		matrixStack.translate(0.3125F, 0.15625F, 0.0F);
		matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(model.leftArm.roll * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(model.leftArm.yaw * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(model.leftArm.pitch * 57.5F));
		matrixStack.translate(0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right leg
	 */
	static void translateToRightLeg(MatrixStack matrixStack, PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrixStack.translate(0.0F, 0.0F, 0.25F);
		}
		matrixStack.translate(-0.125F, 0.75F, 0.0F);
		matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(model.rightLeg.roll * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(model.rightLeg.yaw * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(model.rightLeg.pitch * 57.5F));
		matrixStack.translate(0.0F, 0.75F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left leg
	 */
	static void translateToLeftLeg(MatrixStack matrixStack, PlayerEntityModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrixStack.translate(0.0F, 0.0F, 0.25F);
		}
		matrixStack.translate(0.125F, 0.75F, 0.0F);
		matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(model.leftLeg.roll * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(model.leftLeg.yaw * 57.5F));
		matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(model.leftLeg.pitch * 57.5F));
		matrixStack.translate(0.0F, 0.75F, 0.0F);
	}
}
