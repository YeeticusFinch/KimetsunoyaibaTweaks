package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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
    private static final int LAYER_PRIORITY = 2000;
    private static final float DEG_TO_RAD = Mth.DEG_TO_RAD;
    private static final Map<UUID, ActiveLayer> ACTIVE_LAYERS = new HashMap<>();

    private final AbstractClientPlayer player;
    private Vec3 previousVelocity;
    private Vec3 currentVelocity;
    private float poseStrength;
    private float bodyPitch;
    private float bodyRoll;
    private float headPitch;
    private float headRoll;
    private float armPitch;
    private float legPitch;
    private float torsoRoll;
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
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            layer.setAnimation(animation);
            stack.addAnimLayer(LAYER_PRIORITY, layer);
            ACTIVE_LAYERS.put(player.getUUID(), new ActiveLayer(animation));
            return;
        }

        active.animation.climbing = climbing;
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
                .ifPresent(data -> update(clientPlayer, data.isClimbing()));
        }

        // A player can leave the tracking range without sending a final state packet.
        for (Map.Entry<UUID, ActiveLayer> entry : ACTIVE_LAYERS.entrySet()) {
            if (!visiblePlayers.contains(entry.getKey())) {
                entry.getValue().animation.climbing = false;
            }
        }
    }

    @Override
    public void tick() {
        previousVelocity = currentVelocity;
        currentVelocity = player.getDeltaMovement();
        age += 1.0F;

        float targetStrength = climbing ? 1.0F : 0.0F;
        poseStrength = Mth.lerp(0.22F, poseStrength, targetStrength);
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
            case "head" -> {
                // Body is the parent of the head and every limb. Counter-rotate
                // it so the player's view remains stable while the body banks.
                x = headPitch;
                z = headRoll;
            }
            case "torso" -> z = torsoRoll;
            case "right_arm" -> {
                x = armPitch;
                z = rightArmRoll;
            }
            case "left_arm" -> {
                x = armPitch;
                z = leftArmRoll;
            }
            case "right_leg" -> {
                x = legPitch;
                z = rightLegRoll;
            }
            case "left_leg" -> {
                x = legPitch;
                z = leftLegRoll;
            }
            default -> {
                return current;
            }
        }

        return new Vec3f(
            current.getX().floatValue() + x * poseStrength,
            current.getY().floatValue() + y * poseStrength,
            current.getZ().floatValue() + z * poseStrength);
    }

    @Override
    public void setupAnim(float partialTick) {
        Vec3 velocity = previousVelocity.lerp(currentVelocity, Mth.clamp(partialTick, 0.0F, 1.0F));
        float yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot()) * DEG_TO_RAD;

        // Convert world velocity into the player's local forward/right axes.
        float forward = (float) (-velocity.x * Math.sin(yaw) + velocity.z * Math.cos(yaw));
        float strafe = (float) (velocity.x * Math.cos(yaw) + velocity.z * Math.sin(yaw));
        float vertical = (float) velocity.y;
        float turn = Mth.wrapDegrees(player.getYRot() - player.yRotO);

        bodyPitch = Mth.clamp(-forward * 180.0F - vertical * 45.0F, -65.0F, 65.0F);
        bodyRoll = Mth.clamp(-strafe * 100.0F - turn * 1.5F, -25.0F, 25.0F);
        headPitch = -bodyPitch;
        headRoll = -bodyRoll;

        armPitch = Mth.clamp(forward * 120.0F + vertical * 45.0F, -35.0F, 35.0F);
        legPitch = Mth.clamp(-forward * 65.0F - vertical * 25.0F, -22.0F, 22.0F);
        float sway = Mth.sin((age + partialTick) * 0.15F) * 3.0F;
        rightArmRoll = 8.0F + sway + strafe * 18.0F;
        leftArmRoll = -8.0F - sway + strafe * 18.0F;
        rightLegRoll = 3.0F;
        leftLegRoll = -3.0F;
        torsoRoll = Mth.clamp(-turn * 0.3F, -8.0F, 8.0F);
    }

    private record ActiveLayer(BiokinesesFloatAnimation animation) {
    }
}
