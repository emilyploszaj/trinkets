package dev.emi.trinkets.api.client;

import dev.emi.trinkets.api.SlotReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public interface TrinketRenderer {

	/**
	 * Renders the Trinket
	 *
	 * @param stack The {@link ItemStack} for the Trinket being rendered
	 * @param slotReference The exact slot for the item being rendered
	 * @param contextModel The model this Trinket is being rendered on
	 */
	void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
				MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity,
				float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw,
				float headPitch);

	/**
	 * Rotates the rendering for the models based on the entity's poses and movements. This will do
	 * nothing if the entity render object does not implement {@link LivingEntityRenderer} or if the
	 * model does not implement {@link BipedEntityModel}).
	 *
	 * @param entity The wearer of the trinket
	 * @param model The model to align to the body movement
	 */
	@SuppressWarnings("unchecked")
	static void followBodyRotations(final LivingEntity entity, final BipedEntityModel<LivingEntity> model) {

		EntityRenderer<? super LivingEntity> render = MinecraftClient.getInstance()
				.getEntityRenderDispatcher().getRenderer(entity);

		if (render instanceof LivingEntityRenderer) {
			//noinspection unchecked
			LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> livingRenderer =
					(LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) render;
			EntityModel<LivingEntity> entityModel = livingRenderer.getModel();

			if (entityModel instanceof BipedEntityModel) {
				BipedEntityModel<LivingEntity> bipedModel = (BipedEntityModel<LivingEntity>) entityModel;
				bipedModel.copyBipedStateTo(model);
			}
		}
	}

	/**
	 * Translates the rendering context to the center of the player's face
	 */
	static void translateToFace(MatrixStack matrices, PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity player, float headYaw, float headPitch) {

		if (player.isInSwimmingPose() || player.isFallFlying()) {
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(model.head.roll));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(headYaw));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-45.0F));
		} else {

			if (player.isInSneakingPose() && !model.riding) {
				matrices.translate(0.0F, 0.25F, 0.0F);
			}
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(headYaw));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));
		}
		matrices.translate(0.0F, -0.25F, -0.3F);
	}

	/**
	 * Translates the rendering context to the center of the player's chest/torso segment
	 */
	static void translateToChest(MatrixStack matrices, PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrices.translate(0.0F, 0.2F, 0.0F);
			matrices.multiply(RotationAxis.POSITIVE_X.rotation(model.body.pitch));
		}
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(model.body.yaw));
		matrices.translate(0.0F, 0.4F, -0.16F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right arm
	 */
	static void translateToRightArm(MatrixStack matrices, PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrices.translate(0.0F, 0.2F, 0.0F);
		}
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(model.body.yaw));
		matrices.translate(-0.3125F, 0.15625F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation(model.rightArm.roll));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(model.rightArm.yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(model.rightArm.pitch));
		matrices.translate(-0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left arm
	 */
	static void translateToLeftArm(MatrixStack matrices, PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrices.translate(0.0F, 0.2F, 0.0F);
		}
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(model.body.yaw));
		matrices.translate(0.3125F, 0.15625F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation(model.leftArm.roll));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(model.leftArm.yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(model.leftArm.pitch));
		matrices.translate(0.0625F, 0.625F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's right leg
	 */
	static void translateToRightLeg(MatrixStack matrices, PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrices.translate(0.0F, 0.0F, 0.25F);
		}
		matrices.translate(-0.125F, 0.75F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation(model.rightLeg.roll));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(model.rightLeg.yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(model.rightLeg.pitch));
		matrices.translate(0.0F, 0.75F, 0.0F);
	}

	/**
	 * Translates the rendering context to the center of the bottom of the player's left leg
	 */
	static void translateToLeftLeg(MatrixStack matrices, PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity player) {

		if (player.isInSneakingPose() && !model.riding && !player.isSwimming()) {
			matrices.translate(0.0F, 0.0F, 0.25F);
		}
		matrices.translate(0.125F, 0.75F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation(model.leftLeg.roll));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(model.leftLeg.yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(model.leftLeg.pitch));
		matrices.translate(0.0F, 0.75F, 0.0F);
	}
}
