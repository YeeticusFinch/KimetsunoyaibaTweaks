package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.MugenPortalShaders;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.MugenDoorModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MugenDoorEntity;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Mugen Door entity.
 *
 * The mugen door is a decorative entity that spawns before kizuki demons in raids.
 * It plays an "open" animation and then disappears after a short time.
 */
public class MugenDoorRenderer extends GeoEntityRenderer<MugenDoorEntity> {
    private static final String PORTAL_BONE = "doorinside";
    private static final float PORTAL_HALF_WIDTH = 36.0F / 16.0F;
    private static final float PORTAL_HALF_HEIGHT = 36.0F / 16.0F;
    private static final float PORTAL_LOCAL_Y = (0.25F / 16.0F) + 0.002F;

    private static final ResourceLocation[][] PORTAL_SKYBOXES = {
        skybox("abyssal_towers"),
        skybox("shattered_graveyard"),
        skybox("day"),
        skybox("night")
    };

    private Matrix4f capturedPortalMatrix;

    public MugenDoorRenderer(EntityRendererProvider.Context context) {
        super(context, new MugenDoorModel());
    }

    @Override
    public int getPackedOverlay(MugenDoorEntity animatable, float u, float partialTick) {
        // No overlay effects
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }

    @Override
    public void render(MugenDoorEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        this.capturedPortalMatrix = null;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        if (PORTAL_BONE.equals(bone.getName())) {
            this.capturedPortalMatrix = new Matrix4f(poseStack.last().pose());
            return;
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderFinal(PoseStack poseStack, MugenDoorEntity animatable, BakedGeoModel model,
                            MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                            int packedOverlay, float red, float green, float blue, float alpha) {
        if (bufferSource instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch();
        }

        renderPortal(animatable);
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void renderPortal(MugenDoorEntity entity) {
        ShaderInstance shader = MugenPortalShaders.getPortalSkyShader();
        if (shader == null || this.capturedPortalMatrix == null || !entity.isPortalActive()) {
            return;
        }

        bindSkyboxSamplers(shader, entity);
        setPortalUniforms(shader);

        RenderSystem.setShader(() -> shader);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(this.capturedPortalMatrix, -PORTAL_HALF_WIDTH, PORTAL_LOCAL_Y, -PORTAL_HALF_HEIGHT).endVertex();
        builder.vertex(this.capturedPortalMatrix,  PORTAL_HALF_WIDTH, PORTAL_LOCAL_Y, -PORTAL_HALF_HEIGHT).endVertex();
        builder.vertex(this.capturedPortalMatrix,  PORTAL_HALF_WIDTH, PORTAL_LOCAL_Y,  PORTAL_HALF_HEIGHT).endVertex();
        builder.vertex(this.capturedPortalMatrix, -PORTAL_HALF_WIDTH, PORTAL_LOCAL_Y,  PORTAL_HALF_HEIGHT).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void bindSkyboxSamplers(ShaderInstance shader, MugenDoorEntity entity) {
        ResourceLocation[] skybox = PORTAL_SKYBOXES[entity.getPortalSkyboxVariant()];
        TextureManager textures = Minecraft.getInstance().getTextureManager();

        shader.setSampler("SkyNorth", textures.getTexture(skybox[0]));
        shader.setSampler("SkyEast", textures.getTexture(skybox[1]));
        shader.setSampler("SkySouth", textures.getTexture(skybox[2]));
        shader.setSampler("SkyWest", textures.getTexture(skybox[3]));
        shader.setSampler("SkyUp", textures.getTexture(skybox[4]));
        shader.setSampler("SkyDown", textures.getTexture(skybox[5]));
    }

    private static void setPortalUniforms(ShaderInstance shader) {
        Uniform portalAlpha = shader.getUniform("PortalAlpha");
        if (portalAlpha != null) {
            portalAlpha.set(1.0F);
        }
    }

    private static ResourceLocation[] skybox(String folder) {
        ResourceLocation[] textures = new ResourceLocation[6];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = ResourceLocation.fromNamespaceAndPath(
                KimetsunoyaibaMultiplayer.MODID,
                "textures/environment/" + folder + "/sky_" + i + ".png"
            );
        }
        return textures;
    }
}
