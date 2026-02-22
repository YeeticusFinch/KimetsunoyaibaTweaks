package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Enhanced renderer for after images that appear during Flower Breathing 7th Form.
 * Renders as a semi-transparent copy of the player with idle animation and equipment.
 * Uses GeckoLib for proper armor/item rendering.
 */
public class AfterImageRenderer extends GeoEntityRenderer<AfterImageEntity> {

    public AfterImageRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<AfterImageEntity>() {
            @Override
            public ResourceLocation getModelResource(AfterImageEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(AfterImageEntity entity) {
                // Check if this is a player clone
                if (entity.isPlayerClone()) {
                    // Get player name and find their skin
                    String playerName = entity.getPlayerName();
                    if (!playerName.isEmpty()) {
                        for (Player player : entity.level().players()) {
                            if (player.getName().getString().equals(playerName)) {
                                if (player instanceof AbstractClientPlayer clientPlayer) {
                                    return clientPlayer.getSkinTextureLocation();
                                }
                            }
                        }
                    }
                    // Fallback to Steve skin if player not found
                    return ResourceLocation.withDefaultNamespace("textures/entity/steve.png");
                } else {
                    // Non-player clone - use stored texture path
                    String texturePath = entity.getTexturePath();
                    if (!texturePath.isEmpty()) {
                        try {
                            return ResourceLocation.parse(texturePath);
                        } catch (Exception e) {
                            // Fall through to default
                        }
                    }
                    // Fallback to Kanae's texture (flower breathing)
                    return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/kanae.png");
                }
            }

            @Override
            public ResourceLocation getAnimationResource(AfterImageEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });

        // Add armor rendering layer
        this.addRenderLayer(new GeoArmorLayer<>(this));

        // Add equipment rendering layer
        this.addRenderLayer(new GeoEquipmentLayer<>(this));

        // Sword display layer intentionally omitted for ghostly clones
    }

    @Override
    public void render(AfterImageEntity entity, float entityYaw, float partialTicks,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        // Get opacity for fading effect
        float opacity = entity.getOpacity();
        float lockedYaw = entity.getYRot();

        // Skip rendering if completely transparent
        if (opacity < 0.01f) {
            return;
        }

        // Scale down non-player clones (75%)
        if (!entity.isPlayerClone()) {
            poseStack.pushPose();
            poseStack.scale(0.75f, 0.75f, 0.75f);
            super.render(entity, lockedYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
        } else {
            super.render(entity, lockedYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    /**
     * CRITICAL: This is THE method that determines the actual RGBA color applied to vertices
     * GeckoLib calls this when rendering each part of the model
     */
    @Override
    public int getPackedOverlay(AfterImageEntity animatable, float u, float partialTick) {
        // No red overlay when hurt
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }

    /**
     * This is called BEFORE rendering to set up colors
     * We override the alpha here with the entity's opacity
     */
    @Override
    public void actuallyRender(PoseStack poseStack, AfterImageEntity animatable, BakedGeoModel model,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha) {

        // Get the entity's opacity and use it as alpha
        float opacity = animatable.getOpacity();

        // Call super with our modified alpha value
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                           isReRender, partialTick, packedLight, packedOverlay,
                           red, green, blue, opacity);  // Use opacity as alpha
    }

    /**
     * Override to use translucent render type
     */
    @Override
    public RenderType getRenderType(AfterImageEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        // Use translucent rendering for the ghostly effect
        return RenderType.entityTranslucent(texture);
    }

    /**
     * Override to control the alpha (transparency) of the entity
     */
    @Override
    protected float getDeathMaxRotation(AfterImageEntity animatable) {
        return 0.0f; // No death rotation
    }

    @Override
    public long getInstanceId(AfterImageEntity animatable) {
        // Use entity ID for caching with transparency
        return animatable.getId();
    }

    /**
     * Method to get the entity texture (for non-player entities)
     */
    public static ResourceLocation getEntityTexture(LivingEntity entity) {
        // For non-player living entities, use a fallback texture
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/kanae.png");
    }
}
