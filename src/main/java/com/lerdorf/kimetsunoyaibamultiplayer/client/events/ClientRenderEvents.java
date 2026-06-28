package com.lerdorf.kimetsunoyaibamultiplayer.client.events;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonPropositionClientController;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DualLayerSlashRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordSlashRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker.SlashRenderRequest;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(
	    modid = "kimetsunoyaibamultiplayer",
	    bus = Mod.EventBusSubscriber.Bus.FORGE,
	    value = Dist.CLIENT
	)
public class ClientRenderEvents {
    private static boolean demonPropositionRenderOverrideActive;
    private static float originalYRot;
    private static float originalYRotO;
    private static float originalYHeadRot;
    private static float originalYHeadRotO;
    private static float originalYBodyRot;
    private static float originalYBodyRotO;

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || !DemonPropositionClientController.shouldOverrideLocalPlayer(player)) {
            return;
        }

        float facingYaw = DemonPropositionClientController.getFacingYaw(player);
        originalYRot = player.getYRot();
        originalYRotO = player.yRotO;
        originalYHeadRot = player.getYHeadRot();
        originalYHeadRotO = player.yHeadRotO;
        originalYBodyRot = player.yBodyRot;
        originalYBodyRotO = player.yBodyRotO;
        demonPropositionRenderOverrideActive = true;

        player.setYRot(facingYaw);
        player.yRotO = facingYaw;
        player.setYHeadRot(facingYaw);
        player.yHeadRotO = facingYaw;
        player.setYBodyRot(facingYaw);
        player.yBodyRotO = facingYaw;
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (!demonPropositionRenderOverrideActive || event.getEntity() != mc.player) {
            return;
        }

        LocalPlayer player = (LocalPlayer) event.getEntity();
        player.setYRot(originalYRot);
        player.yRotO = originalYRotO;
        player.setYHeadRot(originalYHeadRot);
        player.yHeadRotO = originalYHeadRotO;
        player.setYBodyRot(originalYBodyRot);
        player.yBodyRotO = originalYBodyRotO;
        demonPropositionRenderOverrideActive = false;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        // Get common resources
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();

        // Render slash models if any exist
        List<SlashRenderRequest> queue = BonePositionTracker.getRenderQueue();
        if (queue.isEmpty())
            return;

        int packedLight = 0xF000F0;

        // Render all active slash models
        for (SlashRenderRequest req : queue) {
            // Calculate current progress based on elapsed time (smooth animation)
            float progress = req.getCurrentProgress();

            // Calculate position and rotation dynamically based on animation type
            Vec3 worldPos;
            float[] rotation;

            if (req.isRawSlash) {
                // Raw slash with custom angle control
                worldPos = BonePositionTracker.calculateRawSlashPosition(req.entity, progress, req);
                rotation = BonePositionTracker.calculateRawSlashRotation(req.entity, progress, req);
            } else if (req.isRawHorizontal) {
                // Raw horizontal slash with custom vert parameter
                worldPos = BonePositionTracker.calculateRawHorizontalPosition(req.entity, progress, req);
                rotation = BonePositionTracker.calculateRawHorizontalRotation(req.entity, progress, req);
            } else if (req.isRawVertical) {
                // Raw vertical slash with custom hor parameter
                worldPos = BonePositionTracker.calculateRawVerticalPosition(req.entity, progress, req);
                rotation = BonePositionTracker.calculateRawVerticalRotation(req.entity, progress, req);
            } else if (req.isHorizontal) {
                worldPos = BonePositionTracker.calculateHorizontalPosition(req.entity, progress, req.leftToRight, req.leftHand);
                rotation = BonePositionTracker.calculateHorizontalRotation(req.entity, progress, req.leftToRight, req.leftHand);
            } else if (req.isVertical) {
                worldPos = BonePositionTracker.calculateVerticalPosition(req.entity, progress, req.upward, req.leftHand);
                rotation = BonePositionTracker.calculateVerticalRotation(req.entity, progress, req.upward, req.leftHand);
            } else if (req.isSpin) {
                worldPos = BonePositionTracker.calculateSpinPosition(req.entity, progress);
                rotation = BonePositionTracker.calculateSpinRotation(req.entity, progress);
            } else {
                continue; // Unknown animation type
            }

            // Convert world coordinates to camera-relative coordinates
            Vec3 cameraRelative = worldPos.subtract(camera);

            // Calculate scale (use sizeScaler for raw slashes, default 2.5f for standard slashes)
            float scale = (req.isRawSlash || req.isRawHorizontal || req.isRawVertical) ? (2.5f * req.sizeScaler) : 2.5f;

            // Determine if we should flip the texture horizontally based on animation direction
            // For animations that spin in the opposite direction, we flip the texture using the "reverse" animation
            boolean flipHorizontal = false;
            if (req.isHorizontal) {
                // For horizontal slashes (sword_to_left vs sword_to_right):
                // sword_to_right has leftToRight=false → flip texture
                // sword_to_left has leftToRight=true → don't flip
                flipHorizontal = !req.leftToRight;
                com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Horizontal slash: leftToRight=" + req.leftToRight + ", flipHorizontal=" + flipHorizontal + ", anim=" + req.animationName);
            } else if (req.isVertical) {
                // For vertical slashes (sword_overhead vs sword_to_upper):
                // sword_overhead has upward=false → flip texture
                // sword_to_upper has upward=true → don't flip
                flipHorizontal = !req.upward;
                com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Vertical slash: upward=" + req.upward + ", flipHorizontal=" + flipHorizontal + ", anim=" + req.animationName);
            } else if (req.isSpin) {
                // For spin attacks (sword_rotate):
                // Use reverse flag to determine flip
                flipHorizontal = !req.reverse;
                com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Spin slash: reverse=" + req.reverse + ", flipHorizontal=" + flipHorizontal + ", anim=" + req.animationName);
            } else if (req.isRawHorizontal) {
                // For raw horizontal slashes:
                // reverse=false → flip texture
                // reverse=true → don't flip
                flipHorizontal = !req.reverse;
                com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Raw horizontal slash: reverse=" + req.reverse + ", flipHorizontal=" + flipHorizontal + ", anim=" + req.animationName);
            } else if (req.isRawVertical) {
                // For raw vertical slashes:
                // reverse=false → flip texture
                // reverse=true → don't flip
                flipHorizontal = !req.reverse;
                com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Raw vertical slash: reverse=" + req.reverse + ", flipHorizontal=" + flipHorizontal + ", anim=" + req.animationName);
            } else if (req.isRawSlash) {
                // For raw slashes with custom angle:
                // reverse=false → flip texture
                // reverse=true → don't flip
                flipHorizontal = !req.reverse;
                com.lerdorf.kimetsunoyaibamultiplayer.Log.info("Raw slash: reverse=" + req.reverse + ", flipHorizontal=" + flipHorizontal + ", anim=" + req.animationName);
            }

            // Render model with dual-layer system (base + emissive)
            DualLayerSlashRenderer.renderDualLayer(
                poseStack,
                bufferSource,
                cameraRelative,
                rotation[0],  // yaw
                rotation[1],  // pitch
                rotation[2],  // roll
                scale,        // Scale (adjusted by sizeScaler for raw slashes)
                progress,
                req.modelKey,
                packedLight,
                flipHorizontal,  // Pass flip flag to use "base" or "reverse" animation
                req.startTime,   // Start time for animated texture frame calculation
                req.duration,    // Duration for animated texture frame calculation
                req.tintColor
            );
        }

        // flush draw calls
        bufferSource.endBatch();

        // Remove old models (instead of clearing everything)
        queue.removeIf(req -> req.shouldRemove());
    }

    /** 
     * First-person sword animation rendering
     * Uses keyframe-based animations that match the third-person player animations
     */
    /*
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) { // THIS IS THE OLD ONE, DO NOT USE!!!
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        // Check if custom first-person swing is enabled
        if (!Config.customFirstPersonSwingEnabled) return;

        // Only apply to main hand
        //if (event.getHand() != InteractionHand.MAIN_HAND) return;
        
        float side = event.getHand() != InteractionHand.MAIN_HAND ? 1.0F : -1.0F;

        ItemStack stack = event.getItemStack();

        // Check if holding a nichirin sword (from this mod, base mod, or any addon)
        boolean holdingNichirinSword = isNichirinSword(stack);

        if (!holdingNichirinSword) return;

        // Update animation tracking for the local player
        com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker.updateCurrentAnimation(player);

        // Get current animation
        String currentAnimation = com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker.getCurrentAnimation();

        // Only apply custom rendering if we have keyframes for this animation
        if (currentAnimation == null || !com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes.hasKeyframes(currentAnimation)) {
            return; // Let vanilla handle it
        }

        // Get animation progress
        float animProgress = com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker.getCurrentAnimationProgress();

        // Get keyframes for this animation
        var keyframes = com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes.getKeyframes(currentAnimation);
        if (keyframes == null) return;

        // Find the keyframe pair to interpolate between
        var framePair = com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes.findFramePair(keyframes, animProgress);
        if (framePair == null) return;

        // Interpolate between keyframes
        Vec3[] transforms = com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes.interpolateKeyframes(framePair, animProgress);
        Vec3 translation = transforms[0];
        Vec3 rotation = transforms[1];

        // DON'T cancel the event - let vanilla apply everything (hand position, item transforms)
        // We'll just add transforms to counter vanilla swing and apply our custom animation
        PoseStack poseStack = event.getPoseStack();
        float swingProgress = event.getSwingProgress();

        // Apply counter-swing transform to cancel out vanilla's swing animation
        // This makes our keyframes the ONLY animation
        applyCounterSwing(poseStack, swingProgress, side);

        // Apply our custom keyframe-based transforms
        // These are now applied on top of: vanilla base + item model transforms + (canceled vanilla swing)
        poseStack.translate(translation.x * Config.translateScale, translation.y * Config.translateScale, translation.z * Config.translateScale);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rotation.x));  // Pitch
        poseStack.mulPose(Axis.YP.rotationDegrees((float) rotation.y));  // Yaw
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotation.z));  // Roll

        // Let vanilla continue - it will apply item model JSON transforms and render
        // Final hierarchy: vanilla base → vanilla swing → counter-swing → our keyframes → item model JSON → render
    }*/
    
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!Config.customFirstPersonSwingEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = event.getItemStack();

        // Check if holding a nichirin sword (from this mod, base mod, or any addon)
        boolean holdingNichirinSword = isNichirinSword(stack);

        if (!holdingNichirinSword) return;
        
        // Update animation tracking
        com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker
                .updateCurrentAnimation(player);

        // Get current animation
        String currentAnimation =
                com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker
                        .getCurrentAnimation();

        // No tracked animation -> let vanilla render.
        if (currentAnimation == null) {
            return;
        }

        // Route left_sword_* animations to offhand render.
        // double_sword_overhead renders custom animation on BOTH hands.
        boolean shouldRenderOffhandOnly = com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker
                .isOffhandAnimation(currentAnimation);
        boolean isDoubleOverhead = currentAnimation.equals("double_sword_overhead");

        // Determine the animation used for this hand.
        // During offhand-only swings, main hand plays sword_none to stay visible and stable.
        String animationForThisHand = currentAnimation;
        if (shouldRenderOffhandOnly && event.getHand() == InteractionHand.MAIN_HAND) {
            animationForThisHand = "sword_none";
        }

        // No keyframes for this hand's animation -> fall back to vanilla.
        if (animationForThisHand == null ||
            !com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes
                    .hasKeyframes(animationForThisHand)) {
            return;
        }

        if (!isDoubleOverhead) {
            if (shouldRenderOffhandOnly) {
                if (event.getHand() != InteractionHand.OFF_HAND && event.getHand() != InteractionHand.MAIN_HAND) return;
            } else if (event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
        }

        event.setCanceled(true);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();

        poseStack.pushPose();

        ItemInHandRenderer renderer = mc.gameRenderer.itemInHandRenderer;

        // ✔ VANILLA BASE OFFSET
        HumanoidArm renderArm = event.getHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        applyVanillaFirstPersonBaseTransform(
            poseStack,
            renderArm,
            event.getEquipProgress()
        );

        // ❌ DO NOT call applyItemArmAttackTransform

        // ✔ CUSTOM ANIMATION
        boolean applyOffhandZOffset = event.getHand() == InteractionHand.OFF_HAND
                && (shouldRenderOffhandOnly || isDoubleOverhead);
        applyCustomSwordAnimation(poseStack, player, event.getSwingProgress(), animationForThisHand, applyOffhandZOffset);

        // ✔ RENDER
        mc.getItemRenderer().renderStatic(
        	    stack,
        	    event.getHand() == InteractionHand.MAIN_HAND
        	            ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
        	            : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
        	    event.getPackedLight(),
        	    OverlayTexture.NO_OVERLAY,
        	    poseStack,
        	    buffers,
        	    mc.level,
        	    player.getId()
        	);

        poseStack.popPose();
    }
    
    private static boolean handSwing = false;
    
    private static void applyCustomSwordAnimation(
            PoseStack poseStack,
            LocalPlayer player,
            float swingProgress,
            String currentAnimation,
            boolean applyOffhandZOffset
    ) {
    	/*
        // Update animation tracking
        com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker
                .updateCurrentAnimation(player);

        // Get current animation
        String currentAnimation =
                com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker
                        .getCurrentAnimation();

        // No animation → do nothing
        if (currentAnimation == null ||
            !com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes
                    .hasKeyframes(currentAnimation)) {
            return;
        }*/
        
        if (swingProgress > 0.01f) handSwing = true; // this is a hand swing, use hand swing progress
        
     // Get animation progress
        float animProgress = com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonAnimationTracker.getCurrentAnimationProgress();

        if (!handSwing && swingProgress < 0.01 && animProgress > 0.01) swingProgress = animProgress; // not a hand swing, use animation progress
        
        if (handSwing && swingProgress < 0.01 && animProgress < 0.01) handSwing = false; // reset
        
        // Fetch keyframes
        var keyframes =
                com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes
                        .getKeyframes(currentAnimation);
        if (keyframes == null) return;

        // Find interpolation pair using actual animation progress
        var framePair =
                com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes
                        .findFramePair(keyframes, animProgress);
        if (framePair == null) return;

        // 🔹 Interpolate (translation + quaternion rotation)
        Object[] transforms =
                com.lerdorf.kimetsunoyaibamultiplayer.client.FirstPersonSwordKeyframes
                        .interpolateKeyframes(framePair, animProgress);

        Vec3 translation = (Vec3) transforms[0];
        Quaternionf rotation = (Quaternionf) transforms[1];

        // 🔹 Apply translation (local-space offset from rest pose)
        poseStack.translate(
                translation.x * Config.translateScale,
                translation.y * Config.translateScale,
                translation.z * Config.translateScale
        );

        // 🔹 Apply rotation (single quaternion, correct order)
        poseStack.mulPose(rotation);

        // Offhand first-person fix:
        // Blender exporter uses mc_rot=(w, x, z, -y), so a +180deg on source rot_delta.z maps to MC +Y axis.
        if (applyOffhandZOffset) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        }
    }


	private static void applyVanillaFirstPersonBaseTransform(
            PoseStack poseStack,
            HumanoidArm arm,
            float equipProgress
    ) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;

        // Move hand to side
        poseStack.translate(
            side * 0.56F,
            -0.52F + equipProgress * -0.6F,
            -0.72F
        );
    }


    /**
     * Apply counter-swing transform to cancel out vanilla's swing animation
     * Vanilla applies swing animation, we reverse it so our keyframes are the only animation
     */
    private static void applyCounterSwing(PoseStack poseStack, float swingProgress, float side) {
        if (swingProgress <= 0.0f) return; // No swing, no counter needed

        // Vanilla's swing animation uses these calculations:
        // swingAngle = sin(swingProgress^2 * PI)
        // swingAngle2 = sin(sqrt(swingProgress) * PI)

        float swingAngle = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingAngle2 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

        // Reverse vanilla's swing transforms
        // Vanilla applies these (for right hand), so we apply the opposite
        // Note: We need to apply in REVERSE order of vanilla's application

     // Reverse vanilla's translation
        poseStack.translate(
        		// 0 - side * 1.2 * swingSqrt
        	    side * Config.counterSwingTranslateX * swingAngle + side * Config.counterSwingTranslateX2 * swingAngle2,
        	    // 0 - 1.05 * swingSqrt
        	    Config.counterSwingTranslateY * swingAngle + Config.counterSwingTranslateY2 * swingAngle2,
        	    // 0.5 * swing
        	    Config.counterSwingTranslateZ * swingAngle + Config.counterSwingTranslateZ2 * swingAngle2
        	);

        // Reverse vanilla's Y rotation (yaw swing)
        // swingSqrt * side * 85
        poseStack.mulPose(Axis.YP.rotationDegrees(swingAngle * (float)Config.counterSwingRotateY * side + swingAngle2 * (float)Config.counterSwingRotateY2 * side));
        
        // Reverse vanilla's Z rotation (roll swing)
        // swingSqrt * side * (-23)
        poseStack.mulPose(Axis.ZP.rotationDegrees(swingAngle * (float)Config.counterSwingRotateZ * side + swingAngle2 * (float)Config.counterSwingRotateZ2 * side));

        // Reverse vanilla's X rotation (pitch swing)
        // swingSqrt * 25
        poseStack.mulPose(Axis.XP.rotationDegrees(swingAngle * (float)Config.counterSwingRotateX + swingAngle2 * (float)Config.counterSwingRotateX2));
        
        
    }

    /**
     * Check if an item is a nichirin sword (from this mod, base mod, or any addon).
     * Delegates to the centralized check in BreathingInfoDetector.
     */
    private static boolean isNichirinSword(ItemStack stack) {
        return com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(stack);
    }

}
