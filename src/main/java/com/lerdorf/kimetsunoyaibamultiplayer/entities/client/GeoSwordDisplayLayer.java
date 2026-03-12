package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SheathModelRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordSheathRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

/**
 * Render layer for displaying nichirin swords + sheaths on GeckoLib entities
 * when they are out of combat (mirrors the player sword display logic).
 */
public class GeoSwordDisplayLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {
    private static final String ANCHOR_BONE_NAME = "body";

    public GeoSwordDisplayLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (!SwordDisplayConfig.enabled) {
            return;
        }

        if (isInvisibilityAnimationActive(animatable)) {
            return;
        }

        ItemStack mainHand = animatable.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!SwordParticleMapping.isKimetsunoyaibaSword(mainHand) || SwordParticleMapping.isSheathExempt(mainHand)) {
            return;
        }

        GeoBone anchorBone = bakedModel.getBone(ANCHOR_BONE_NAME).orElse(null);
        if (anchorBone == null) {
            return;
        }

        EntityCombatStateTracker.updateCombatState(animatable);
        boolean inCombat = EntityCombatStateTracker.isInCombat(animatable);
        boolean sheathingTransition = EntityCombatStateTracker.isInSheathingTransition(animatable);

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem());
        boolean hasPerSwordOverride = SwordDisplayConfig.swordPositionOverrides.containsKey(itemId.toString());
        SwordDisplayConfig.SwordDisplayPosition position =
            SwordDisplayConfig.getPositionForSword(itemId.toString());
        if (animatable instanceof DemonSlayerEntity slayer) {
            // Respect explicit per-sword config placement for demon slayers first.
            // Entity behavior only provides fallback placement when no override exists.
            if (!hasPerSwordOverride) {
                int level = slayer.getPowerLevel();
                if (level >= 1 && level <= 4) {
                    position = slayer.isSheatheOnBack()
                        ? SwordDisplayConfig.SwordDisplayPosition.BACK
                        : SwordDisplayConfig.SwordDisplayPosition.HIP;
                } else if (level >= 5) {
                    position = SwordDisplayConfig.SwordDisplayPosition.HIP;
                }
            }
        }
        // Primary entity sheath slot is always left; secondary (when present) is right.
        boolean isLeft = true;
        if (animatable instanceof DemonSlayerEntity && animatable.tickCount % 20 == 0) {
            Log.debug("[GeoSwordDisplayLayer] {} render mode: inCombat={}, sheathingTransition={}, position={}, side={}",
                animatable.getType().getDescriptionId(), inCombat, sheathingTransition, position, isLeft ? "left" : "right");
        }

        poseStack.pushPose();
        applyEntityYawRotation(poseStack, animatable, partialTick);
        poseStack.translate(0, 0.01f, 0);
        RenderUtils.prepMatrixForBone(poseStack, anchorBone);

        if (inCombat || sheathingTransition) {
            renderSheathOnly(poseStack, bufferSource, packedLight, animatable, mainHand, position, isLeft);
        } else {
            renderSwordWithSheath(poseStack, bufferSource, packedLight, animatable, mainHand, position, isLeft);
        }

        renderSuperSeniorExtraSheaths(poseStack, bufferSource, packedLight, animatable);

        poseStack.popPose();
    }

    private static void renderSuperSeniorExtraSheaths(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                      LivingEntity entity) {
        if (!SwordDisplayConfig.renderSheaths || !(entity instanceof DemonSlayerEntity slayer) || slayer.getPowerLevel() < 5) {
            return;
        }

        String alt1 = slayer.getAltSwordId1();
        String alt2 = slayer.getAltSwordId2();
        if (alt1.isEmpty() && alt2.isEmpty()) {
            return;
        }

        // Super senior layout: primary sheath on left hip (main path), plus one right hip and one back.
        renderSheathForSwordId(poseStack, buffer, packedLight, entity, slayer, alt1, false, false);
        renderSheathForSwordId(poseStack, buffer, packedLight, entity, slayer, alt2, true, true);
    }

    private static void renderSheathForSwordId(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                               LivingEntity entity, DemonSlayerEntity slayer,
                                               String swordId, boolean isLeft, boolean fallbackBackPosition) {
        if (swordId == null || swordId.isEmpty()) {
            return;
        }

        ItemStack swordStack = slayer.getSwordStackById(swordId);
        if (swordStack.isEmpty()) {
            return;
        }

        Item sheathItem = SwordSheathRegistry.getSheathItem(swordStack);
        if (sheathItem == null) {
            return;
        }

        ResourceLocation swordItemId = BuiltInRegistries.ITEM.getKey(swordStack.getItem());
        boolean hasPerSwordOverride = SwordDisplayConfig.swordPositionOverrides.containsKey(swordItemId.toString());
        boolean backPosition = hasPerSwordOverride
            ? SwordDisplayConfig.getPositionForSword(swordItemId.toString()) == SwordDisplayConfig.SwordDisplayPosition.BACK
            : fallbackBackPosition;

        poseStack.pushPose();

        if (backPosition) {
            // Gecko entity body-space is mirrored relative to player layer-space.
            // Invert side at the final transform so requested logical side renders correctly.
            applyBackPosition(poseStack, !isLeft, swordStack);
            poseStack.translate(0.10D, -0.05D, 0.04D);
        } else {
            applyHipPosition(poseStack, !isLeft, swordStack);
            poseStack.translate(0.06D, -0.03D, 0.02D);
        }

        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);
        SheathModelRenderer.renderSheath(sheathItem, poseStack, buffer, packedLight, entity.getId());
        poseStack.popPose();
    }

    private static boolean isInvisibilityAnimationActive(LivingEntity entity) {
        if (entity instanceof BreathingSlayerEntity slayer) {
            String currentAnimation = slayer.getCurrentAnimation();
            return currentAnimation != null
                && "invisibility".equals(currentAnimation)
                && slayer.getAnimationTicks() > 0;
        }
        return false;
    }

    private static void applyEntityYawRotation(PoseStack poseStack, LivingEntity entity, float partialTick) {
        float bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - bodyRot));
    }

    private static void renderSwordWithSheath(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                              LivingEntity entity, ItemStack sword,
                                              SwordDisplayConfig.SwordDisplayPosition position, boolean isLeft) {
        poseStack.pushPose();

        if (position == SwordDisplayConfig.SwordDisplayPosition.HIP) {
            // Gecko entity body-space is mirrored relative to player layer-space.
            // Invert side at the final transform so requested logical side renders correctly.
            applyHipPosition(poseStack, !isLeft, sword);
        } else {
            applyBackPosition(poseStack, !isLeft, sword);
        }

        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);

        if (SwordDisplayConfig.renderSheaths) {
            Item sheathItem = SwordSheathRegistry.getSheathItem(sword);
            if (sheathItem != null) {
                SheathModelRenderer.renderSheath(sheathItem, poseStack, buffer, packedLight, entity.getId());
            }
        }

        // Ensure GeoItem sword renderers (e.g. love/kanroji) can resolve non-player entity context.
        // Without this, displayed sheathed swords may fall back to stack NBT idle animation.
        LivingEntity previousEntity = EntityRenderContext.getCurrentEntity();
        boolean shouldRestoreContext = previousEntity != entity;
        if (shouldRestoreContext) {
            EntityRenderContext.setCurrentEntity(entity);
        }
        try {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                sword,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
            );
        } finally {
            if (shouldRestoreContext) {
                if (previousEntity != null) {
                    EntityRenderContext.setCurrentEntity(previousEntity);
                } else {
                    EntityRenderContext.clearCurrentEntity();
                }
            }
        }

        poseStack.popPose();
    }

    private static void renderSheathOnly(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                         LivingEntity entity, ItemStack sword,
                                         SwordDisplayConfig.SwordDisplayPosition position, boolean isLeft) {
        if (!SwordDisplayConfig.renderSheaths) {
            return;
        }

        SwordSheathRegistry.SheathInfo sheathInfo = SwordSheathRegistry.getSheathInfo(sword);
        if (sheathInfo == null || !sheathInfo.persistsWhenDrawn()) {
            return;
        }

        Item sheathItem = sheathInfo.getSheathItem();
        if (sheathItem == null) {
            return;
        }

        poseStack.pushPose();

        if (position == SwordDisplayConfig.SwordDisplayPosition.HIP) {
            // Keep sheath-only render aligned with the same mirrored side mapping.
            applyHipPosition(poseStack, !isLeft, sword);
        } else {
            applyBackPosition(poseStack, !isLeft, sword);
        }

        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);

        SheathModelRenderer.renderSheath(sheathItem, poseStack, buffer, packedLight, entity.getId());

        poseStack.popPose();
    }


    private static double resolveEntityTranslation(double base, double offset, boolean flip) {
        return (flip ? -base : base) + offset;
    }

    private static double resolveEntityRotation(double base, double offset, boolean flip) {
        return (flip ? -base : base) + offset;
    }

    private static void applyHipPosition(PoseStack poseStack, boolean isLeft, ItemStack sword) {
        SwordDisplayConfig.SwordOffsets customOffsets = resolveSwordOffsets(
            sword,
            isLeft ? SwordDisplayConfig.SwordDisplaySlot.HIP_LEFT : SwordDisplayConfig.SwordDisplaySlot.HIP_RIGHT
        );
        double translateX = isLeft ? SwordDisplayConfig.hipLeftTranslateX : SwordDisplayConfig.hipRightTranslateX;
        double translateY = isLeft ? SwordDisplayConfig.hipLeftTranslateY : SwordDisplayConfig.hipRightTranslateY;
        double translateZ = isLeft ? SwordDisplayConfig.hipLeftTranslateZ : SwordDisplayConfig.hipRightTranslateZ;

        translateX += customOffsets != null ? customOffsets.translateX : 0.0D;
        translateY += customOffsets != null ? customOffsets.translateY : 0.0D;
        translateZ += customOffsets != null ? customOffsets.translateZ : 0.0D;

        translateX = resolveEntityTranslation(translateX, SwordDisplayConfig.entityHipTranslateOffsetX,
            SwordDisplayConfig.entityHipTranslateFlipX);
        translateY = resolveEntityTranslation(translateY, SwordDisplayConfig.entityHipTranslateOffsetY,
            SwordDisplayConfig.entityHipTranslateFlipY);
        translateZ = resolveEntityTranslation(translateZ, SwordDisplayConfig.entityHipTranslateOffsetZ,
            SwordDisplayConfig.entityHipTranslateFlipZ);

        double rotateZ = isLeft ? SwordDisplayConfig.hipLeftRotateZ : SwordDisplayConfig.hipRightRotateZ;
        double rotateY = isLeft ? SwordDisplayConfig.hipLeftRotateY : SwordDisplayConfig.hipRightRotateY;
        double rotateX = isLeft ? SwordDisplayConfig.hipLeftRotateX : SwordDisplayConfig.hipRightRotateX;

        rotateZ += customOffsets != null ? customOffsets.rotateZ : 0.0D;
        rotateY += customOffsets != null ? customOffsets.rotateY : 0.0D;
        rotateX += customOffsets != null ? customOffsets.rotateX : 0.0D;

        rotateZ = resolveEntityRotation(rotateZ, SwordDisplayConfig.entityHipRotateOffsetZ,
            SwordDisplayConfig.entityHipRotateFlipZ);
        rotateY = resolveEntityRotation(rotateY, SwordDisplayConfig.entityHipRotateOffsetY,
            SwordDisplayConfig.entityHipRotateFlipY);
        rotateX = resolveEntityRotation(rotateX, SwordDisplayConfig.entityHipRotateOffsetX,
            SwordDisplayConfig.entityHipRotateFlipX);

        poseStack.translate(translateX, translateY, translateZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotateZ));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) rotateY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rotateX));
    }

    private static void applyBackPosition(PoseStack poseStack, boolean isLeft, ItemStack sword) {
        SwordDisplayConfig.SwordOffsets customOffsets = resolveSwordOffsets(
            sword,
            isLeft ? SwordDisplayConfig.SwordDisplaySlot.BACK_LEFT : SwordDisplayConfig.SwordDisplaySlot.BACK_RIGHT
        );
        double translateX = isLeft ? SwordDisplayConfig.backLeftTranslateX : SwordDisplayConfig.backRightTranslateX;
        double translateY = isLeft ? SwordDisplayConfig.backLeftTranslateY : SwordDisplayConfig.backRightTranslateY;
        double translateZ = isLeft ? SwordDisplayConfig.backLeftTranslateZ : SwordDisplayConfig.backRightTranslateZ;

        translateX += customOffsets != null ? customOffsets.translateX : 0.0D;
        translateY += customOffsets != null ? customOffsets.translateY : 0.0D;
        translateZ += customOffsets != null ? customOffsets.translateZ : 0.0D;

        translateX = resolveEntityTranslation(translateX, SwordDisplayConfig.entityBackTranslateOffsetX,
            SwordDisplayConfig.entityBackTranslateFlipX);
        translateY = resolveEntityTranslation(translateY, SwordDisplayConfig.entityBackTranslateOffsetY,
            SwordDisplayConfig.entityBackTranslateFlipY);
        translateZ = resolveEntityTranslation(translateZ, SwordDisplayConfig.entityBackTranslateOffsetZ,
            SwordDisplayConfig.entityBackTranslateFlipZ);

        double rotateZ = isLeft ? SwordDisplayConfig.backLeftRotateZ : SwordDisplayConfig.backRightRotateZ;
        double rotateY = isLeft ? SwordDisplayConfig.backLeftRotateY : SwordDisplayConfig.backRightRotateY;
        double rotateX = isLeft ? SwordDisplayConfig.backLeftRotateX : SwordDisplayConfig.backRightRotateX;

        rotateZ += customOffsets != null ? customOffsets.rotateZ : 0.0D;
        rotateY += customOffsets != null ? customOffsets.rotateY : 0.0D;
        rotateX += customOffsets != null ? customOffsets.rotateX : 0.0D;

        rotateZ = resolveEntityRotation(rotateZ, SwordDisplayConfig.entityBackRotateOffsetZ,
            SwordDisplayConfig.entityBackRotateFlipZ);
        rotateY = resolveEntityRotation(rotateY, SwordDisplayConfig.entityBackRotateOffsetY,
            SwordDisplayConfig.entityBackRotateFlipY);
        rotateX = resolveEntityRotation(rotateX, SwordDisplayConfig.entityBackRotateOffsetX,
            SwordDisplayConfig.entityBackRotateFlipX);

        poseStack.translate(translateX, translateY, translateZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotateZ));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) rotateY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rotateX));
    }

    private static SwordDisplayConfig.SwordOffsets resolveSwordOffsets(ItemStack sword, SwordDisplayConfig.SwordDisplaySlot slot) {
        if (sword == null || sword.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(sword.getItem());
        if (itemId == null) {
            return null;
        }
        return SwordDisplayConfig.getSwordOffsets(itemId.toString(), slot);
    }

}
