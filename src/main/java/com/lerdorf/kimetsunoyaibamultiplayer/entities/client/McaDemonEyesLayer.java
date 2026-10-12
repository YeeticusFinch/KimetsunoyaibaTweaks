package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesResourceHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.compat.MinecraftComesAliveCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Emissive demon-eye overlay for MCA's vanilla-model zombie villagers. */
public class McaDemonEyesLayer<T extends LivingEntity> extends RenderLayer<T, HumanoidModel<T>> {
    public McaDemonEyesLayer(RenderLayerParent<T, HumanoidModel<T>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!MinecraftComesAliveCompat.isMcaDemonized(entity) || entity.isInvisible()) {
            return;
        }

        int eyesIndex = entity.getPersistentData().getInt(MinecraftComesAliveCompat.MCA_EYE_INDEX_TAG);
        int hue = entity.getPersistentData().getInt(MinecraftComesAliveCompat.MCA_EYE_HUE_TAG);
        ResourceLocation texture = DemonEyesResourceHelper.getTexture(eyesIndex);
        RenderType renderType = CustomRenderTypes.geoEntityTranslucentEmissive(texture);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        float[] tint = DemonEyesResourceHelper.getHueTint(hue);

        getParentModel().renderToBuffer(
            poseStack,
            buffer,
            0xF000F0,
            OverlayTexture.NO_OVERLAY,
            tint[0],
            tint[1],
            tint[2],
            1.0F
        );
    }
}
