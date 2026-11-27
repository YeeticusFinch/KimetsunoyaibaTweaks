package com.lerdorf.kimetsunoyaibamultiplayer.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/** Adds the head-rotation MoLang contribution to the late arm poses. */
public final class PlayerHeadFacingArmModifier extends AbstractModifier {
    private static final float BRAMBLES_FOLLOW_START_SECONDS = 0.9167F;
    private static final float BRAMBLES_LENGTH_SECONDS = 3.0F;

    private final AbstractClientPlayer player;
    private final String animationName;

    public PlayerHeadFacingArmModifier(AbstractClientPlayer player, String animationName) {
        this.player = player;
        this.animationName = normalize(animationName);
    }

    public static boolean supports(String animationName) {
        String normalized = normalize(animationName);
        return "right_arm_forward".equals(normalized)
            || "right_arm_up".equals(normalized)
            || "brambles".equals(normalized)
            || "left_brambles".equals(normalized);
    }

    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float partialTick, Vec3f current) {
        Vec3f transformed = super.get3DTransform(modelName, type, partialTick, current);
        if (type != TransformType.ROTATION || !isTargetBone(modelName) || !isFollowWindow(partialTick)) {
            return transformed;
        }

        float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        float headYaw = Mth.wrapDegrees(
            Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot)
                - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot));

        // The exported pose is [-90, 0, 0]. The GeckoLib expressions add
        // head_x_rotation and head_y_rotation to that base pose, so this
        // modifier contributes the same delta after Player Animator evaluates
        // the ordinary keyframes.
        return new Vec3f(
            transformed.getX().floatValue() + headPitch,
            transformed.getY().floatValue() + headYaw,
            transformed.getZ().floatValue());
    }

    private boolean isFollowWindow(float partialTick) {
        if (!(getAnim() instanceof KeyframeAnimationPlayer animationPlayer)) {
            return false;
        }
        if (isRightArmPose()) {
            return true;
        }

        KeyframeAnimation data = animationPlayer.getData();
        if (data == null) {
            return false;
        }
        float startTick = data.getLength() * (BRAMBLES_FOLLOW_START_SECONDS / BRAMBLES_LENGTH_SECONDS);
        return animationPlayer.getTick() + partialTick >= startTick;
    }

    private boolean isTargetBone(String modelName) {
        return isRightArmPose() ? "right_arm".equals(modelName) : "left_arm".equals(modelName);
    }

    private boolean isRightArmPose() {
        return "right_arm_forward".equals(animationName) || "right_arm_up".equals(animationName);
    }

    private static String normalize(String animationName) {
        if (animationName == null) {
            return "";
        }
        int separator = animationName.lastIndexOf(':');
        return separator >= 0 ? animationName.substring(separator + 1) : animationName;
    }
}
