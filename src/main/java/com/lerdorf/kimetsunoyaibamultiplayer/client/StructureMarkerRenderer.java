package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Renders only the marker ID; the marker icon itself is a particle. */
public class StructureMarkerRenderer extends EntityRenderer<StructureMarkerEntity> {
    public StructureMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StructureMarkerEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Rendered explicitly by StructureMarkerParticleHandler after entities.
    }

    public void renderMarkerId(StructureMarkerEntity entity, PoseStack poseStack,
                               MultiBufferSource buffer, int packedLight) {
        if (!entity.getMarkerId().isEmpty()) {
            renderNameTag(entity, Component.literal(entity.getMarkerId()), poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StructureMarkerEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
