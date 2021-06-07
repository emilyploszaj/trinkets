package dev.emi.trinkets.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public class TrinketModel extends BipedEntityModel<LivingEntity> {

	public TrinketModel(ModelPart root) {
		super(root);
		this.setVisible(false);
		this.head.visible = true;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0f);
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData head = modelPartData.addChild("head", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-4f, -16f, -4f, 8f, 8f, 8f), ModelTransform.NONE);
		head.addChild("brim", ModelPartBuilder.create().uv(0, 16)
				.cuboid(-5f, -9f, -5f, 10f, 1f, 10f), ModelTransform.NONE);
		return TexturedModelData.of(modelData, 64, 32);
	}
}
