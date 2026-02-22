package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.VermilionEyeEffect;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FlowerPetalSlashEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Client-side overlay for the Vermilion Eye effect.
 * Renders a red tint over the screen when the effect is active.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer", value = Dist.CLIENT)
public class VermilionEyeOverlay {

    private static final Minecraft mc = Minecraft.getInstance();

    // Vermilion red color components: #E34234
    private static final float RED = 0.89f;   // 227/255
    private static final float GREEN = 0.26f; // 66/255
    private static final float BLUE = 0.20f;  // 52/255
    private static final float BASE_ALPHA = 0.15f; // Base transparency
    private static final float PULSE_ALPHA = 0.35f; // Peak transparency during pulse

    // Heartbeat timing: two rapid pulses every 25 ticks
    private static final int HEARTBEAT_CYCLE = 25; // Total cycle length in ticks
    private static final int FIRST_PULSE_START = 0;
    private static final int FIRST_PULSE_PEAK = 2;
    private static final int FIRST_PULSE_END = 4;
    private static final int SECOND_PULSE_START = 6;
    private static final int SECOND_PULSE_PEAK = 8;
    private static final int SECOND_PULSE_END = 10;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        // Only render on the main layer
        if (!event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        // Check if player has Vermilion Eye effect
        if (!player.hasEffect(ModEffects.VERMILION_EYE.get())) {
            return;
        }

        // Render the red tint overlay
        renderRedTint(event.getGuiGraphics());
    }

    /**
     * Calculates the pulsing alpha value based on the heartbeat timing.
     * Creates a "lub-dub" heartbeat effect with two rapid pulses.
     */
    private static float calculatePulsingAlpha() {
        LocalPlayer player = mc.player;
        if (player == null) return BASE_ALPHA;

        // Use player tick count for timing
        int tick = player.tickCount % HEARTBEAT_CYCLE;

        // Calculate alpha based on which part of the heartbeat cycle we're in
        float pulseIntensity = 0.0f;

        // First pulse (lub)
        if (tick >= FIRST_PULSE_START && tick <= FIRST_PULSE_END) {
            if (tick <= FIRST_PULSE_PEAK) {
                // Rising edge of first pulse
                pulseIntensity = (float)(tick - FIRST_PULSE_START) / (FIRST_PULSE_PEAK - FIRST_PULSE_START);
            } else {
                // Falling edge of first pulse
                pulseIntensity = 1.0f - (float)(tick - FIRST_PULSE_PEAK) / (FIRST_PULSE_END - FIRST_PULSE_PEAK);
            }
        }
        // Second pulse (dub)
        else if (tick >= SECOND_PULSE_START && tick <= SECOND_PULSE_END) {
            if (tick <= SECOND_PULSE_PEAK) {
                // Rising edge of second pulse
                pulseIntensity = (float)(tick - SECOND_PULSE_START) / (SECOND_PULSE_PEAK - SECOND_PULSE_START);
            } else {
                // Falling edge of second pulse
                pulseIntensity = 1.0f - (float)(tick - SECOND_PULSE_PEAK) / (SECOND_PULSE_END - SECOND_PULSE_PEAK);
            }
        }

        // Interpolate between base and pulse alpha
        return BASE_ALPHA + (PULSE_ALPHA - BASE_ALPHA) * pulseIntensity;
    }

    /**
     * Renders a red vermilion tint over the entire screen with pulsing heartbeat effect.
     */
    private static void renderRedTint(GuiGraphics guiGraphics) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Calculate the pulsing alpha
        float alpha = calculatePulsingAlpha();

        // Use RenderSystem for proper blending
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Get the pose stack and matrix
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Draw a full-screen quad with vermilion color and pulsing alpha
        bufferBuilder.vertex(matrix, 0, screenHeight, 0).color(RED, GREEN, BLUE, alpha).endVertex();
        bufferBuilder.vertex(matrix, screenWidth, screenHeight, 0).color(RED, GREEN, BLUE, alpha).endVertex();
        bufferBuilder.vertex(matrix, screenWidth, 0, 0).color(RED, GREEN, BLUE, alpha).endVertex();
        bufferBuilder.vertex(matrix, 0, 0, 0).color(RED, GREEN, BLUE, alpha).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    /**
     * Determines the glow color for an entity based on its threat level.
     *
     * Color coding:
     * - RED (0xFF0000): Hostile/aggressive entities, players who are targeting you
     * - BLUE (0x0088FF): Non-hostile players
     * - YELLOW (0xFFFF00): Neutral mobs (monsters not currently aggro)
     * - GREEN (0x00FF00): Passive animals
     *
     * @param entity The entity to check
     * @param viewer The player viewing the entity
     * @return The glow color as an RGB integer (0xRRGGBB)
     */
    public static int getEntityGlowColor(Entity entity, Player viewer) {
        if (entity == null || viewer == null) {
            return 0xFFFF00; // Yellow as default
        }
        if (entity instanceof FlowerPetalSlashEntity) {
            return 0xFFFF00;
        }

        // Handle players separately
        if (entity instanceof Player targetPlayer) {
            // Check if this player is aggro towards the viewer (PvP)
            if (Damager.isAngry(targetPlayer, viewer)) {
                return 0xFF0000; // Red for hostile players
            }
            return 0x0088FF; // Blue for friendly/neutral players
        }

        // Handle living entities (mobs)
        if (entity instanceof LivingEntity living) {
            // First priority: Check if mob is actively targeting the viewer
            if (entity instanceof Mob mob) {
                LivingEntity target = mob.getTarget();
                if (target != null && target.equals(viewer)) {
                    return 0xFF0000; // Red - directly targeting the player
                }
                // Also red if targeting anyone (aggressive state)
                if (target != null) {
                    return 0xFF0000; // Red - aggressive towards something
                }
            }

            // Check using Damager utility for combat history
            if (Damager.isAngry(living, viewer)) {
                return 0xFF0000; // Red - has attacked or been attacked by player
            }

            // Check if it's a passive animal (cows, pigs, sheep, chickens, etc.)
            if (entity instanceof Animal) {
                return 0x00FF00; // Green for passive animals
            }

            // Check if it's a monster type (zombies, skeletons, creepers, etc.)
            if (entity instanceof Monster) {
                // Monster but not currently targeting anyone - yellow (neutral/dormant)
                return 0xFFFF00; // Yellow for neutral monsters
            }
        }

        // Default: yellow for neutral/unknown entities
        return 0xFFFF00;
    }

    /**
     * Checks if the local player has the Vermilion Eye effect active.
     */
    public static boolean isVermilionEyeActive() {
        LocalPlayer player = mc.player;
        if (player == null) return false;
        return player.hasEffect(ModEffects.VERMILION_EYE.get());
    }

    /**
     * Gets the visibility range for the Vermilion Eye effect.
     */
    public static double getVisibilityRange() {
        return VermilionEyeEffect.VISIBILITY_RANGE;
    }
}
