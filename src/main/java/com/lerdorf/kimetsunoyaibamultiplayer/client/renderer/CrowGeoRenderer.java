package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
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
import com.mojang.math.Axis;

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
        	if (Config.logDebug) {
	            LOGGER.info("=== CrowGeoRenderer.render() CALLED ===");
	            LOGGER.info("Entity: {}", entity.getName().getString());
	            LOGGER.info("Entity type: {}", entity.getType());
	            LOGGER.info("Entity position: {}, {}, {}", entity.getX(), entity.getY(), entity.getZ());
	            LOGGER.info("Entity yaw: {}", entityYaw);
        	}
            hasLoggedRender = true;
        }

     // Wrap the entity in our animatable wrapper
        currentAnimatable = CrowAnimatableWrapper.getOrCreate(entity);

        try {
            // CRITICAL FIX: Trigger animation update BEFORE rendering
            // This ensures the animation controllers are called and bone transformations are calculated
        	long instanceId = this.getInstanceId(currentAnimatable);
            currentAnimatable.getAnimatableInstanceCache().getManagerForId(instanceId)
                .tryTriggerAnimation("crow_controller");
            
            // Get the model first
            BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource(currentAnimatable));
            
            // Check if entity is LivingEntity
            int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            float red = 1.0f, green = 1.0f, blue = 1.0f;
            
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                // Calculate overlay for damage flash
                overlay = getOverlayCoords(livingEntity, this.getWhiteOverlayProgress(livingEntity, partialTick));
                
                // Red tint on death
                if (livingEntity.deathTime > 0) {
                    green = 0.0f;
                    blue = 0.0f;
                }
            }
            
            if (!hasLoggedRender && Config.logDebug) {
                LOGGER.info("BakedModel obtained: {}, bones: {}", 
                    bakedModel != null, bakedModel != null ? bakedModel.topLevelBones().size() : 0);
            }

            // Get render type
            RenderType renderType = RenderType.entityCutoutNoCull(model.getTextureResource(currentAnimatable));
            VertexConsumer buffer = bufferSource.getBuffer(renderType);

            poseStack.pushPose();
            
            // Apply proper rotation to face entity direction
            // Rotate 180 degrees to fix backwards facing, then apply entity yaw
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
            
            // Tilt on death
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                if (livingEntity.deathTime > 0) {
                    float deathRotation = ((float)livingEntity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
                    deathRotation = Math.min(deathRotation, 1.0F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(deathRotation * 90.0F));
                }
            }
            
            poseStack.scale(1.0F, 1.0F, 1.0F);

            // Set custom animations - this applies the animation transformations to bones
            model.setCustomAnimations(currentAnimatable, instanceId, null);

            // Render each bone
            for (software.bernie.geckolib.cache.object.GeoBone bone : bakedModel.topLevelBones()) {
                renderRecursively(poseStack, currentAnimatable, bone, renderType, bufferSource, buffer,
                    false, partialTick, packedLight, 
                    //net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 
                    overlay,
                    red, green, blue, 1.0f);
            }
            
         // CRITICAL: This applies animation transformations to bones
            // The partialTick parameter is important for smooth animations
            this.actuallyRender(poseStack, currentAnimatable, bakedModel, renderType, 
                bufferSource, buffer, false, partialTick, packedLight, 
                //net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 
                overlay,
                red, green, blue, 1.0f);

            poseStack.popPose();

        } catch (Exception e) {
            LOGGER.error("Error during render: ", e);
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
    
    // Add this helper method
    protected float getWhiteOverlayProgress(Entity entity, float partialTicks) {
        return 0.0F;
    }

    // Add this to get overlay coordinates including damage flash
    public static int getOverlayCoords(Entity entity, float whiteOverlay) {
        return net.minecraft.client.renderer.texture.OverlayTexture.pack(
            net.minecraft.client.renderer.texture.OverlayTexture.u(whiteOverlay),
            net.minecraft.client.renderer.texture.OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0)
        );
    }
}