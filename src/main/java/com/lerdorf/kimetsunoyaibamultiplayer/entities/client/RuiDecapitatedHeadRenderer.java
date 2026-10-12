package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiDecapitatedHeadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders Rui's detached head and its returning silk tether. */
public class RuiDecapitatedHeadRenderer extends GeoEntityRenderer<RuiDecapitatedHeadEntity> {
    private static final ResourceLocation RUI_MODEL = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "geo/head.geo.json");
    private static final ResourceLocation PLAYER_MODEL = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "geo/head_player.geo.json");
    private static final ResourceLocation RUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/oni_rui.png");
    private static final ResourceLocation THREAD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/silk_ribbon.png");

    public RuiDecapitatedHeadRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<RuiDecapitatedHeadEntity>() {
            @Override
            public ResourceLocation getModelResource(RuiDecapitatedHeadEntity entity) {
                return entity.isRuiHead() ? RUI_MODEL : PLAYER_MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(RuiDecapitatedHeadEntity entity) {
                if (entity.isRuiHead()) {
                    return RUI_TEXTURE;
                }
                LivingEntity owner = entity.getOwnerEntity();
                if (owner instanceof net.minecraft.client.player.AbstractClientPlayer player) {
                    return player.getSkinTextureLocation();
                }
                return ResourceLocation.withDefaultNamespace("textures/entity/steve.png");
            }

            @Override
            public ResourceLocation getAnimationResource(RuiDecapitatedHeadEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }

        });
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(RuiDecapitatedHeadEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        if (!entity.isReturning()) {
            return;
        }
        LivingEntity owner = entity.getOwnerEntity();
        if (owner == null) {
            return;
        }

        Vec3 start = Vec3.ZERO;
        Vec3 end = owner.getEyePosition().add(0.0D, -0.06D, 0.0D).subtract(entity.position());
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(THREAD_TEXTURE));
        drawLine(pose, buffer, start, end, 0.045F);
    }

    private static void drawLine(PoseStack.Pose pose, VertexConsumer buffer,
                                 Vec3 start, Vec3 end, float width) {
        Vector3f a = new Vector3f((float) start.x, (float) start.y, (float) start.z);
        Vector3f b = new Vector3f((float) end.x, (float) end.y, (float) end.z);
        Vector3f side = new Vector3f(b).sub(a).cross(new Vector3f(0.0F, 1.0F, 0.0F));
        if (side.lengthSquared() < 1.0E-6F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize().mul(width);
        vertex(buffer, pose, new Vector3f(a).add(side));
        vertex(buffer, pose, new Vector3f(b).add(side));
        vertex(buffer, pose, new Vector3f(b).sub(side));
        vertex(buffer, pose, new Vector3f(a).sub(side));
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f point) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        buffer.vertex(matrix, point.x(), point.y(), point.z()).color(0.96F, 0.96F, 1.0F, 1.0F)
            .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
