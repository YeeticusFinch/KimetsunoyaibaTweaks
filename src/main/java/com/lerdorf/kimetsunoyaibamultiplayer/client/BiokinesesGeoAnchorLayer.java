package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;
import org.joml.Vector3f;

/** Captures the animated GeckoLib torso bone for the world-space arm renderer. */
public class BiokinesesGeoAnchorLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {
    public BiokinesesGeoAnchorLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.getCapability(KimetsunoyaibaMultiplayer.BIOKINESES_ARMS).map(data -> data.isActive()).orElse(false)) {
            return;
        }
        GeoBone body = bakedModel.getBone("body").orElse(null);
        if (body == null) {
            return;
        }

        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, body);
        Vector3f position = new Vector3f(0.0F, 0.0F, 0.0F);
        poseStack.last().pose().transformPosition(position);
        net.minecraft.world.phys.Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BiokinesesArmRenderer.captureGeoTorsoAnchor(animatable.getId(),
            new net.minecraft.world.phys.Vec3(position.x + camera.x, position.y + camera.y, position.z + camera.z));
        poseStack.popPose();
    }
}
