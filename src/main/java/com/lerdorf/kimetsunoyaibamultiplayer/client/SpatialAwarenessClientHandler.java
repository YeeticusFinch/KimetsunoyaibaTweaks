package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.OrochiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OrochiDismountPacket;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side camera and rendering behavior for Spatial Awareness.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public class SpatialAwarenessClientHandler {

    private static final ResourceLocation[] DESATURATE_SHADERS = new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "shaders/post/spatial_awareness_desaturate.json"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "shaders/post/desaturate.json")
    };
    private static final int KNEEL_LAYER_PRIORITY = 6500;

    private static Vec3 lockedPlayerPos = null;
    private static Vec3 freeCameraPos = null;
    private static Vec3 freeCameraVelocity = Vec3.ZERO;
    private static Marker cameraAnchor = null;
    private static boolean activePreviousTick = false;
    private static boolean shaderLoaded = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            if (activePreviousTick) {
                disableSpatialAwareness(mc, null);
            }
            activePreviousTick = false;
            return;
        }

        boolean active = player.hasEffect(ModEffects.SPATIAL_AWARENESS.get());
        if (active) {
            enableSpatialAwareness(mc, player);
            updateDetachedCamera(player, mc);
            enforceFrozenPlayer(player);
        } else if (activePreviousTick) {
            disableSpatialAwareness(mc, player);
        }

        activePreviousTick = active;
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ModEffects.SPATIAL_AWARENESS.get())) {
            return;
        }

        event.getInput().forwardImpulse = 0.0f;
        event.getInput().leftImpulse = 0.0f;
        event.getInput().jumping = false;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (event.isUseItem() && player.isShiftKeyDown()) {
            OrochiEntity orochi = player.getPassengers().stream()
                .filter(OrochiEntity.class::isInstance)
                .map(OrochiEntity.class::cast)
                .findFirst()
                .orElse(null);
            if (orochi != null && player.getUUID().equals(orochi.getOwnerUUID())) {
                Log.alwaysWarn("[Orochi] Client detected shift-right-click dismount attempt by {} (cooldownRemaining={} ticks)",
                    player.getName().getString(), orochi.getMountToggleCooldownRemainingTicks());
                event.setCanceled(true);
                event.setSwingHand(false);
                ModNetworking.sendToServer(new OrochiDismountPacket(orochi.getId()));
                return;
            }
        }

        if (player.hasEffect(ModEffects.SPATIAL_AWARENESS.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ModEffects.SPATIAL_AWARENESS.get())) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ModEffects.SPATIAL_AWARENESS.get())) {
            return;
        }

        event.setRed(0.0f);
        event.setGreen(0.0f);
        event.setBlue(0.0f);
    }

    @SubscribeEvent
    public static void onClientLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        disableSpatialAwareness(Minecraft.getInstance(), null);
        activePreviousTick = false;
    }

    public static boolean isSpatialAwarenessActive() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(ModEffects.SPATIAL_AWARENESS.get()) && cameraAnchor != null;
    }

    public static Vec3 getFreeCameraPosition() {
        if (freeCameraPos != null) return freeCameraPos;
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getEyePosition() : Vec3.ZERO;
    }

    private static void enableSpatialAwareness(Minecraft mc, LocalPlayer player) {
        if (lockedPlayerPos == null) {
            lockedPlayerPos = player.position();
        }
        if (freeCameraPos == null) {
            freeCameraPos = player.getEyePosition();
            freeCameraVelocity = Vec3.ZERO;
        }
        if (cameraAnchor == null || cameraAnchor.isRemoved()) {
            cameraAnchor = new Marker(EntityType.MARKER, mc.level);
            cameraAnchor.noPhysics = true;
            cameraAnchor.setNoGravity(true);
            cameraAnchor.setInvisible(true);
            cameraAnchor.setPos(freeCameraPos);
            cameraAnchor.setYRot(player.getYRot());
            cameraAnchor.setXRot(player.getXRot());
            cameraAnchor.setOldPosAndRot();
            // Add to level so camera interpolation uses stable world entity state.
            mc.level.addFreshEntity(cameraAnchor);
        }
        if (mc.getCameraEntity() != cameraAnchor) {
            mc.setCameraEntity(cameraAnchor);
        }

        if (!shaderLoaded) {
            shaderLoaded = tryLoadDesaturationShader(mc);
        }
    }

    private static void disableSpatialAwareness(Minecraft mc, LocalPlayer player) {
        if (mc.player != null && mc.getCameraEntity() != mc.player) {
            mc.setCameraEntity(mc.player);
        }

        SpatialAwarenessEntityRenderer.onEffectDisabled();

        if (shaderLoaded) {
            mc.gameRenderer.shutdownEffect();
            shaderLoaded = false;
        }

        if (player != null) {
            removeKneelLayer(player);
        }

        lockedPlayerPos = null;
        freeCameraPos = null;
        freeCameraVelocity = Vec3.ZERO;
        if (cameraAnchor != null) {
            cameraAnchor.discard();
            cameraAnchor = null;
        }
    }

    private static void updateDetachedCamera(LocalPlayer player, Minecraft mc) {
        if (freeCameraPos == null) {
            return;
        }

        if (mc.screen != null) {
            return;
        }

        Vec3 desiredVelocity = getDesiredCameraVelocity(mc, player);
        freeCameraVelocity = freeCameraVelocity.lerp(desiredVelocity, 0.35D);
        double maxSpeed = 0.9D;
        if (freeCameraVelocity.lengthSqr() > maxSpeed * maxSpeed) {
            freeCameraVelocity = freeCameraVelocity.normalize().scale(maxSpeed);
        }
        freeCameraPos = freeCameraPos.add(freeCameraVelocity);
        if (cameraAnchor != null && !cameraAnchor.isRemoved()) {
            cameraAnchor.setPos(freeCameraPos);
            cameraAnchor.setYRot(player.getYRot());
            cameraAnchor.setXRot(player.getXRot());
            cameraAnchor.setOldPosAndRot();
        }
    }

    private static Vec3 getDesiredCameraVelocity(Minecraft mc, LocalPlayer player) {
        Vec3 look = cameraAnchor != null ? cameraAnchor.getLookAngle() : player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);

        Vec3 move = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) {
            move = move.add(forward);
        }
        if (mc.options.keyDown.isDown()) {
            move = move.subtract(forward);
        }
        if (mc.options.keyLeft.isDown()) {
            move = move.subtract(right);
        }
        if (mc.options.keyRight.isDown()) {
            move = move.add(right);
        }
        if (mc.options.keyJump.isDown()) {
            move = move.add(0.0D, 1.0D, 0.0D);
        }
        if (mc.options.keyShift.isDown()) {
            move = move.add(0.0D, -1.0D, 0.0D);
        }

        if (move.lengthSqr() <= 0.0D) {
            return Vec3.ZERO;
        }

        double speed = mc.options.keySprint.isDown() ? 0.7D : 0.35D;
        return move.normalize().scale(speed);
    }

    private static void enforceFrozenPlayer(LocalPlayer player) {
        if (lockedPlayerPos != null) {
            player.setPos(lockedPlayerPos.x, lockedPlayerPos.y, lockedPlayerPos.z);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
    }

    private static void removeKneelLayer(LocalPlayer player) {
        try {
            AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (stack != null) {
                stack.removeLayer(KNEEL_LAYER_PRIORITY);
            }
        } catch (Exception ignored) {
            // If PlayerAnimator is unavailable for any reason, skip clean-up silently.
        }
    }

    private static boolean tryLoadDesaturationShader(Minecraft mc) {
        for (ResourceLocation shaderLoc : DESATURATE_SHADERS) {
            try {
                mc.gameRenderer.loadEffect(shaderLoc);
                Log.info("Spatial Awareness shader loaded: {}", shaderLoc);
                return true;
            } catch (Exception e) {
                Log.warn("Spatial Awareness shader load failed for {}: {}", shaderLoc, e.getMessage());
            }
        }
        return false;
    }
}
