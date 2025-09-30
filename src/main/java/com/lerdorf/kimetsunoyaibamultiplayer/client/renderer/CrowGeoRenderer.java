package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CrowAnimatableWrapper;
import com.lerdorf.kimetsunoyaibamultiplayer.client.model.CrowGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * GeckoLib renderer for kasugai_crow entities
 * Accepts Entity and wraps it in CrowAnimatableWrapper for GeckoLib rendering
 */
public class CrowGeoRenderer extends EntityRenderer<Entity> implements GeoRenderer<CrowAnimatableWrapper> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean hasLoggedRender = false;

    private final CrowGeoModel model;
    private CrowAnimatableWrapper currentAnimatable;

    public CrowGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.3f;
        this.model = new CrowGeoModel();
        LOGGER.info("CrowGeoRenderer initialized with GeckoLib model");
    }

    @Override
    public void render(Entity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!hasLoggedRender) {
            LOGGER.info("=== CrowGeoRenderer.render() CALLED ===");
            LOGGER.info("Entity: {}", entity.getName().getString());
            LOGGER.info("Entity type: {}", entity.getType());
            LOGGER.info("Entity position: {}, {}, {}", entity.getX(), entity.getY(), entity.getZ());
            hasLoggedRender = true;
        }

        // Wrap the entity in our animatable wrapper
        currentAnimatable = CrowAnimatableWrapper.getOrCreate(entity);

        try {
            LOGGER.info("Attempting to render crow model...");

            // Get the model
            BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource(currentAnimatable));
            LOGGER.info("BakedModel obtained: {}, bones: {}", bakedModel != null, bakedModel != null ? bakedModel.topLevelBones().size() : 0);

            // Get render type
            RenderType renderType = RenderType.entityCutoutNoCull(model.getTextureResource(currentAnimatable));
            VertexConsumer buffer = bufferSource.getBuffer(renderType);

            // Log position
            LOGGER.info("Rendering at position: {}, {}, {} (yaw: {})", entity.getX(), entity.getY(), entity.getZ(), entityYaw);
            LOGGER.info("PoseStack last pose translation: {}", poseStack.last().pose());

            // Scale up the crow for visibility
            poseStack.pushPose();
            poseStack.scale(2.0F, 2.0F, 2.0F);  // Make it 2x bigger for debugging
            LOGGER.info("Applied 2x scale");

            // Render using defaultRender
            defaultRender(poseStack, currentAnimatable, bufferSource, renderType, buffer,
                         entityYaw, partialTick, packedLight);

            poseStack.popPose();

            LOGGER.info("Render completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error during render: ", e);
            e.printStackTrace();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return ResourceLocation.tryBuild(KimetsunoyaibaMultiplayer.MODID, "textures/entity/crow.png");
    }

    @Override
    public CrowAnimatableWrapper getAnimatable() {
        return currentAnimatable;
    }

    @Override
    public GeoModel<CrowAnimatableWrapper> getGeoModel() {
        return model;
    }

    @Override
    public void updateAnimatedTextureFrame(CrowAnimatableWrapper animatable) {
        // No animated texture
    }

    @Override
    public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource,
                                    float partialTick, int packedLight) {
        // No post render event needed
    }

    @Override
    public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource,
                                      float partialTick, int packedLight) {
        return false;
    }

    @Override
    public void fireCompileRenderLayersEvent() {
        // No compile render layers needed
    }
}