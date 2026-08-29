package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.PuppetBoneCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

/**
 * Captures true animated bone positions for GeckoLib-rendered Puppetry
 * puppets. Runs inside {@link GeoEntityRenderer#renderRecursively} after the
 * bone's pose stack is fully transformed, converts the pose to the same
 * world-space matrix GeckoLib uses for {@code bone.getWorldPosition()}, and
 * stores the origin in {@link PuppetBoneCache} for the puppet line renderer.
 *
 * This covers every GeckoLib renderer in the game (ours and the base mod's)
 * without touching any renderer class individually.
 */
@Mixin(value = GeoEntityRenderer.class, remap = false)
public abstract class PuppetGeoBoneCaptureMixin<T extends Entity & software.bernie.geckolib.core.animatable.GeoAnimatable> {

    @Shadow
    protected Matrix4f entityRenderTranslations;

    @Inject(method = "renderRecursively*",
        at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib/util/RenderUtils;translateAwayFromPivotPoint(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/core/animatable/CoreGeoBone;)V"),
        require = 0)
    private void kimetsunoyaibamultiplayer$captureBoneMatrix(com.mojang.blaze3d.vertex.PoseStack poseStack, software.bernie.geckolib.core.animatable.GeoAnimatable animatable, GeoBone bone,
                                                             net.minecraft.client.renderer.RenderType renderType,
                                                             net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                                             com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender,
                                                             float partialTick, int packedLight, int packedOverlay,
                                                             float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (isReRender || !(animatable instanceof LivingEntity living)
            || !PuppetBoneCache.needsCapture(living)) {
            return;
        }
        try {
            // Replicate GeoEntityRenderer's world-space matrix computation:
            // pose relative to the model, offset by the entity's position.
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(
                poseState, this.entityRenderTranslations);
            Matrix4f worldMatrix = RenderUtils.translateMatrix(
                new Matrix4f(localMatrix), living.position().toVector3f());
            PuppetBoneCache.captureGeoBone(living, bone.getName(), worldMatrix);
        } catch (Exception ignored) {
            // Rendering must never crash over line capture
        }
    }
}
