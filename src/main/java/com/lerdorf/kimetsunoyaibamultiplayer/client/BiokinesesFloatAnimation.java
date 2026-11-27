package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.BiokinesesArmController;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Procedural Player Animator equivalent of the GeckoLib biped "float" clip.
 * Player Animator does not evaluate GeckoLib MoLang, so the same values are
 * calculated from the player's interpolated, head-relative velocity here.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class BiokinesesFloatAnimation implements IAnimation {
    // Keep float motion above locomotion layers but below attacks and abilities.
    private static final int FLOAT_BASE_LAYER_PRIORITY = 490;
    private static final int PROCEDURAL_LAYER_PRIORITY = 500;
    private static final float DEG_TO_RAD = Mth.DEG_TO_RAD;
    private static final float ROTATION_SMOOTHING = 0.035F;
    private static final float POSE_SMOOTHING = 0.12F;
    private static final Map<UUID, ActiveLayer> ACTIVE_LAYERS = new HashMap<>();

    private final AbstractClientPlayer player;
    private Vec3 previousVelocity;
    private Vec3 currentVelocity;
    private float poseStrength;
    private float bodyPitch;
    private float bodyRoll;
    private float armPitch;
    private float legPitch;
    private float rightArmRoll;
    private float leftArmRoll;
    private float rightLegRoll;
    private float leftLegRoll;
    private float age;
    private boolean climbing;

    private BiokinesesFloatAnimation(AbstractClientPlayer player) {
        this.player = player;
        this.currentVelocity = player.getDeltaMovement();
        this.previousVelocity = this.currentVelocity;
        this.poseStrength = 1.0F;
        this.climbing = true;
    }

    /** Keeps the procedural layer synchronized with the server-authoritative climbing flag. */
    public static void update(AbstractClientPlayer player, boolean climbing) {
        if (player == null) {
            return;
        }

        ActiveLayer active = ACTIVE_LAYERS.get(player.getUUID());
        if (active == null) {
            if (!climbing) {
                return;
            }
            AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (stack == null) {
                return;
            }

            BiokinesesFloatAnimation animation = new BiokinesesFloatAnimation(player);
            ModifierLayer<IAnimation> proceduralLayer = new ModifierLayer<>();
            proceduralLayer.setAnimation(animation);
            stack.addAnimLayer(PROCEDURAL_LAYER_PRIORITY, proceduralLayer);

            KeyframeAnimation floatAnimation = findFloatAnimation();
            ModifierLayer<IAnimation> baseLayer = null;
            if (floatAnimation != null) {
                baseLayer = new ModifierLayer<>();
                baseLayer.setAnimation(new KeyframeAnimationPlayer(floatAnimation));
                stack.addAnimLayer(FLOAT_BASE_LAYER_PRIORITY, baseLayer);
            }

            ACTIVE_LAYERS.put(player.getUUID(), new ActiveLayer(animation, baseLayer, floatAnimation));
            return;
        }

        active.setClimbing(climbing);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE_LAYERS.clear();
            return;
        }

        Set<UUID> visiblePlayers = new HashSet<>();
        for (net.minecraft.world.entity.player.Player player : minecraft.level.players()) {
            if (!(player instanceof AbstractClientPlayer clientPlayer)) {
                continue;
            }
            visiblePlayers.add(clientPlayer.getUUID());
            clientPlayer.getCapability(KimetsunoyaibaMultiplayer.BIOKINESES_ARMS)
                .ifPresent(data -> update(clientPlayer,
                    BiokinesesArmController.isTraversalActive(clientPlayer)));
        }

        // A player can leave the tracking range without sending a final state packet.
        for (Map.Entry<UUID, ActiveLayer> entry : ACTIVE_LAYERS.entrySet()) {
            if (!visiblePlayers.contains(entry.getKey())) {
                entry.getValue().setClimbing(false);
            }
        }
    }

    @Override
    public void tick() {
        previousVelocity = currentVelocity;
        currentVelocity = player.getDeltaMovement();
        age += 1.0F;

        float targetStrength = climbing ? 1.0F : 0.0F;
        poseStrength = Mth.lerp(POSE_SMOOTHING, poseStrength, targetStrength);
    }

    @Override
    public boolean isActive() {
        return climbing || poseStrength > 0.01F;
    }

    @Override
    public Vec3f get3DTransform(String modelName, TransformType type, float partialTick, Vec3f current) {
        if (type != TransformType.ROTATION) {
            return current;
        }

        float x = 0.0F;
        float y = 0.0F;
        float z = 0.0F;
        switch (modelName) {
            case "body" -> {
                x = bodyPitch;
                z = bodyRoll;
            }
            case "torso" -> {
                return current;
            }
            case "head" -> {
                // Leave the head entirely to the player's look direction.
                return current;
            }
            case "right_arm", "rightArm" -> {
                x = armPitch;
                z = rightArmRoll;
            }
            case "left_arm", "leftArm" -> {
                x = armPitch;
                z = leftArmRoll;
            }
            case "right_leg", "rightLeg" -> {
                x = legPitch;
                z = rightLegRoll;
            }
            case "left_leg", "leftLeg" -> {
                x = legPitch;
                z = leftLegRoll;
            }
            default -> {
                return current;
            }
        }

        // Player Animator expects rotation transforms in radians, while the
        // procedural pose values are kept in degrees for readable tuning.
        float rotationScale = DEG_TO_RAD * poseStrength;
        return new Vec3f(
            current.getX().floatValue() + x * rotationScale,
            current.getY().floatValue() + y * rotationScale,
            current.getZ().floatValue() + z * rotationScale);
    }

    @Override
    public void setupAnim(float partialTick) {
        Vec3 velocity = previousVelocity.lerp(currentVelocity, Mth.clamp(partialTick, 0.0F, 1.0F));
        float yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot()) * DEG_TO_RAD;

        // Convert world velocity into the player's local forward/right axes.
        float forward = (float) (-velocity.x * Math.sin(yaw) + velocity.z * Math.cos(yaw));
        float strafe = (float) (velocity.x * Math.cos(yaw) + velocity.z * Math.sin(yaw));

        // Only horizontal motion drives the whole-body tilt. Vertical speed and
        // yaw changes must not turn a fall or a look rotation into a spin.
        float targetBodyPitch = Mth.clamp(-forward * 45.0F, -20.0F, 20.0F);
        float targetBodyRoll = Mth.clamp(strafe * 45.0F, -20.0F, 20.0F);
        float targetArmPitch = Mth.clamp(forward * 78.0F, -30.0F, 30.0F);
        float targetLegPitch = Mth.clamp(forward * 52.0F, -21.0F, 21.0F);
        float sway = Mth.sin((age + partialTick) * 0.15F) * 2.0F;
        float targetRightArmRoll = 8.0F + sway - strafe * 12.0F;
        float targetLeftArmRoll = -8.0F - sway - strafe * 12.0F;
        float targetRightLegRoll = 3.0F - strafe * 8.0F;
        float targetLeftLegRoll = -3.0F - strafe * 8.0F;

        bodyPitch = smooth(bodyPitch, targetBodyPitch);
        bodyRoll = smooth(bodyRoll, targetBodyRoll);
        armPitch = smooth(armPitch, targetArmPitch);
        legPitch = smooth(legPitch, targetLegPitch);
        rightArmRoll = smooth(rightArmRoll, targetRightArmRoll);
        leftArmRoll = smooth(leftArmRoll, targetLeftArmRoll);
        rightLegRoll = smooth(rightLegRoll, targetRightLegRoll);
        leftLegRoll = smooth(leftLegRoll, targetLeftLegRoll);
    }

    private static float smooth(float current, float target) {
        return Mth.lerp(ROTATION_SMOOTHING, current, target);
    }

    private static KeyframeAnimation findFloatAnimation() {
        try {
            return PlayerAnimationRegistry.getAnimation(
                ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "float"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class ActiveLayer {
        private final BiokinesesFloatAnimation animation;
        private final ModifierLayer<IAnimation> baseLayer;
        private final KeyframeAnimation baseAnimation;

        private ActiveLayer(BiokinesesFloatAnimation animation,
                            ModifierLayer<IAnimation> baseLayer,
                            KeyframeAnimation baseAnimation) {
            this.animation = animation;
            this.baseLayer = baseLayer;
            this.baseAnimation = baseAnimation;
        }

        private void setClimbing(boolean climbing) {
            animation.climbing = climbing;
            if (baseLayer == null || baseAnimation == null) {
                return;
            }
            if (climbing) {
                if (baseLayer.getAnimation() == null || !baseLayer.getAnimation().isActive()) {
                    baseLayer.setAnimation(new KeyframeAnimationPlayer(baseAnimation));
                }
            } else {
                baseLayer.setAnimation(null);
            }
        }
    }
}
