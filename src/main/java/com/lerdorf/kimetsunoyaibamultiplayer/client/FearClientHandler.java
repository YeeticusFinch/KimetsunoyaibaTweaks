package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.mixin.EntityRenderDispatcherAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.Random;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public class FearClientHandler {
    private static final ResourceLocation INVERT_SHADER =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "shaders/post/fear_invert.json");
    private static final ResourceLocation KOKUSHIBO_EYES =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/overlay/fear_effect_kokushibo_eyes.png");
    private static final int BG_FRAMES = 14;
    private static final int FG_FRAMES = 23;
    private static final int MIN_NORMAL_TICKS = 20;
    private static final int MAX_NORMAL_TICKS = 60;
    private static final int MIN_OVERLAY_PULSE_TICKS = 80;
    private static final int MAX_OVERLAY_PULSE_TICKS = 160;
    private static final int PULSE_TOTAL_TICKS = 15;
    private static final double KOKUSHIBO_RANGE = 40.0D;
    private static final double ENTITY_RERENDER_RANGE = 96.0D;
    private static final float OVERLAY_FADE_STEP = 1.0F / 6.0F;

    private static final Random RANDOM = new Random();
    private static int normalTicksRemaining = randomNormalDelay();
    private static int overlayPulsePeriodTicks = randomOverlayPulsePeriod();
    private static int overlayPulseTick = 0;
    private static int pulseTick = -1;
    private static boolean fearWasActive = false;
    private static boolean shaderLoaded = false;
    private static float overlayAlpha = 0.0F;
    private static ShaderInstance fearOverlayShader;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean fearActive = player != null && mc.level != null && player.hasEffect(ModEffects.FEAR.get());
        boolean paralysisActive = fearActive && FearEffectHandler.isParalyzed(player);
        updateOverlayAlpha(paralysisActive);
        updateOverlayPulse(paralysisActive || overlayAlpha > 0.01F);
        if (!fearActive) {
            if (fearWasActive || shaderLoaded) {
                cleanupShader(mc);
            }
            fearWasActive = false;
            pulseTick = -1;
            normalTicksRemaining = randomNormalDelay();
            resetOverlayPulseIfHidden();
            return;
        }

        if (!fearWasActive) {
            pulseTick = 0;
            normalTicksRemaining = randomNormalDelay();
        } else {
            updateHeartbeatPulse();
        }
        if (isFearInversionActive()) {
            ensureShaderLoaded(mc);
        } else if (shaderLoaded) {
            cleanupShader(mc);
        }

        if (paralysisActive) {
            clearActionKeys(mc);
        }
        fearWasActive = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !FearEffectHandler.isParalyzed(player)) {
            return;
        }

        event.getInput().forwardImpulse = 0.0F;
        event.getInput().leftImpulse = 0.0F;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !FearEffectHandler.isParalyzed(player)) {
            return;
        }
        if (event.isAttack() || event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && FearEffectHandler.isParalyzed(player)) {
            clearCycleKeys();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseInput(InputEvent.MouseButton event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && FearEffectHandler.isParalyzed(player)) {
            clearCycleKeys();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isFearInversionActive()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ModEffects.FEAR.get())) {
            return;
        }

        float levelStrength = getDisplayedFearLevel(player) / 10.0F;
        float yawPitchMax = 0.4F + 3.0F * levelStrength;
        float rollMax = 0.8F + 6.0F * levelStrength;
        float time = player.tickCount + (float) event.getPartialTick();

        float yawOffset = yawPitchMax * (0.65F * Mth.sin(time * 2.10F) + 0.35F * Mth.sin(time * 4.70F + 1.70F));
        float pitchOffset = yawPitchMax * (0.60F * Mth.sin(time * 2.60F + 0.80F) + 0.40F * Mth.sin(time * 5.10F + 2.40F));
        float rollOffset = rollMax * (0.70F * Mth.sin(time * 3.00F + 0.35F) + 0.30F * Mth.sin(time * 6.30F + 1.20F));

        event.setYaw(event.getYaw() + yawOffset);
        event.setPitch(event.getPitch() + pitchOffset);
        event.setRoll(event.getRoll() + rollOffset);
    }

    public static void renderWorldFearOverlay(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!shouldRenderOverlay(mc, player)) {
            return;
        }

        float alpha = overlayAlpha;
        renderScreenTexture(frameTexture("fear_effect_bg_", overlayFrame(BG_FRAMES, partialTick)), alpha, false);
        if (hasKokushiboNearby(player)) {
            renderScreenTexture(KOKUSHIBO_EYES, alpha, false);
        }
        rerenderVisibleLivingEntities(mc, poseStack, partialTick);
    }

    public static void renderForegroundFearOverlay(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!shouldRenderOverlay(mc, player)) {
            return;
        }

        renderScreenTexture(frameTexture("fear_effect_fg_", foregroundFrame(player)), overlayAlpha, isFearInversionActive());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        renderForegroundFearOverlay(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onClientLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        cleanupShader(Minecraft.getInstance());
        fearWasActive = false;
        pulseTick = -1;
        normalTicksRemaining = randomNormalDelay();
        overlayAlpha = 0.0F;
        resetOverlayPulse();
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "fear_overlay"),
                    DefaultVertexFormat.POSITION_TEX_COLOR
                ),
                shader -> fearOverlayShader = shader
            );
        } catch (IOException e) {
            Log.warn("Fear overlay shader registration failed: {}", e.getMessage());
        }
    }

    public static boolean isFearInversionActive() {
        return pulseTick >= 0 && pulseTick < PULSE_TOTAL_TICKS && (pulseTick < 5 || pulseTick >= 10);
    }

    private static void updateHeartbeatPulse() {
        if (pulseTick >= 0) {
            pulseTick++;
            if (pulseTick >= PULSE_TOTAL_TICKS) {
                pulseTick = -1;
                normalTicksRemaining = randomNormalDelay();
            }
            return;
        }

        normalTicksRemaining--;
        if (normalTicksRemaining <= 0) {
            pulseTick = 0;
        }
    }

    private static void updateOverlayAlpha(boolean paralysisActive) {
        float target = paralysisActive ? 1.0F : 0.0F;
        if (overlayAlpha < target) {
            overlayAlpha = Math.min(target, overlayAlpha + OVERLAY_FADE_STEP);
        } else if (overlayAlpha > target) {
            overlayAlpha = Math.max(target, overlayAlpha - OVERLAY_FADE_STEP);
        }
    }

    private static void updateOverlayPulse(boolean renderable) {
        if (!renderable) {
            return;
        }
        overlayPulseTick++;
        if (overlayPulseTick >= overlayPulsePeriodTicks) {
            overlayPulseTick = 0;
            overlayPulsePeriodTicks = randomOverlayPulsePeriod();
        }
    }

    private static void resetOverlayPulseIfHidden() {
        if (overlayAlpha <= 0.01F) {
            resetOverlayPulse();
        }
    }

    private static void resetOverlayPulse() {
        overlayPulseTick = 0;
        overlayPulsePeriodTicks = randomOverlayPulsePeriod();
    }

    private static void ensureShaderLoaded(Minecraft mc) {
        if (shaderLoaded) {
            return;
        }
        try {
            SpatialAwarenessClientHandler.onExternalShaderReplaced();
            mc.gameRenderer.loadEffect(INVERT_SHADER);
            shaderLoaded = true;
        } catch (Exception e) {
            Log.warn("Fear inversion shader load failed: {}", e.getMessage());
            shaderLoaded = false;
        }
    }

    private static void cleanupShader(Minecraft mc) {
        if (shaderLoaded) {
            mc.gameRenderer.shutdownEffect();
            shaderLoaded = false;
            SpatialAwarenessClientHandler.restoreShaderIfActive();
        }
    }

    private static void clearActionKeys(Minecraft mc) {
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
        clearCycleKeys();
    }

    private static void clearCycleKeys() {
        clearMapping(net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART);
        clearMapping(ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD);
        clearMapping(ModKeyBindings.CYCLE_FORM_VARIATION);
    }

    private static void clearMapping(KeyMapping mapping) {
        if (mapping == null) {
            return;
        }
        while (mapping.consumeClick()) {
            // Drain queued clicks while the paralysis window is active.
        }
        mapping.setDown(false);
    }

    private static boolean hasKokushiboNearby(LocalPlayer player) {
        if (player.level() == null) {
            return false;
        }
        return !player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(KOKUSHIBO_RANGE),
            entity -> entity != player && isKokushibo(entity)
        ).isEmpty();
    }

    private static boolean isKokushibo(Entity entity) {
        ResourceLocation id = EntityType.getKey(entity.getType());
        if (id == null) {
            return false;
        }
        String value = id.toString();
        return "kimetsu:kokushibo".equals(value) || "kimetsunoyaiba:kokushibo".equals(value);
    }

    private static boolean shouldRenderOverlay(Minecraft mc, LocalPlayer player) {
        return player != null && mc.level != null && overlayAlpha > 0.01F;
    }

    private static int getDisplayedFearLevel(LocalPlayer player) {
        MobEffectInstance effect = player.getEffect(ModEffects.FEAR.get());
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private static void rerenderVisibleLivingEntities(Minecraft mc, PoseStack poseStack, float partialTick) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Frustum frustum = mc.levelRenderer.getFrustum();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        boolean originalRenderShadow = ((EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).kimetsunoyaibamultiplayer$getShouldRenderShadow();

        try {
            mc.getEntityRenderDispatcher().setRenderShadow(false);
            double cameraX = cameraPos.x();
            double cameraY = cameraPos.y();
            double cameraZ = cameraPos.z();

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity livingEntity)) {
                    continue;
                }
                if (!shouldRerenderEntity(mc, camera, frustum, livingEntity, cameraX, cameraY, cameraZ)) {
                    continue;
                }

                double x = Mth.lerp((double) partialTick, livingEntity.xOld, livingEntity.getX());
                double y = Mth.lerp((double) partialTick, livingEntity.yOld, livingEntity.getY());
                double z = Mth.lerp((double) partialTick, livingEntity.zOld, livingEntity.getZ());
                float yaw = Mth.lerp(partialTick, livingEntity.yRotO, livingEntity.getYRot());
                mc.getEntityRenderDispatcher().render(
                    livingEntity,
                    x - cameraX,
                    y - cameraY,
                    z - cameraZ,
                    yaw,
                    partialTick,
                    poseStack,
                    bufferSource,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(livingEntity, partialTick)
                );
            }
        } finally {
            bufferSource.endBatch();
            mc.getEntityRenderDispatcher().setRenderShadow(originalRenderShadow);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static boolean shouldRerenderEntity(Minecraft mc, Camera camera, Frustum frustum, LivingEntity entity, double cameraX, double cameraY, double cameraZ) {
        if (entity.distanceToSqr(cameraX, cameraY, cameraZ) > ENTITY_RERENDER_RANGE * ENTITY_RERENDER_RANGE) {
            return false;
        }
        if (!mc.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraX, cameraY, cameraZ) && !entity.hasIndirectPassenger(mc.player)) {
            return false;
        }
        if (entity == camera.getEntity() && !camera.isDetached() && !entity.isSleeping()) {
            return false;
        }
        if (mc.level == null || !mc.level.isOutsideBuildHeight(entity.blockPosition().getY()) && !mc.levelRenderer.isChunkCompiled(entity.blockPosition())) {
            return false;
        }
        return !(entity instanceof LocalPlayer) || camera.getEntity() == entity || entity == mc.player && !mc.player.isSpectator();
    }

    private static ResourceLocation frameTexture(String prefix, int frame) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/overlay/" + prefix + frame + ".png"
        );
    }

    private static int overlayFrame(int frameCount, float partialTick) {
        float progress = (overlayPulseTick + partialTick) / (float) overlayPulsePeriodTicks;
        return Mth.clamp((int) (progress * frameCount), 0, frameCount - 1);
    }

    private static int foregroundFrame(LocalPlayer player) {
        return Math.floorMod(player.tickCount, FG_FRAMES);
    }

    private static void renderScreenTexture(ResourceLocation texture, float alpha, boolean invert) {
        Minecraft mc = Minecraft.getInstance();
        if (alpha <= 0.01F) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        RenderSystem.backupProjectionMatrix();
        Matrix4f projection = new Matrix4f().setOrtho(
            0.0F,
            (float) width,
            (float) height,
            0.0F,
            1000.0F,
            ForgeHooksClient.getGuiFarPlane()
        );
        RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        modelViewStack.translate(0.0D, 0.0D, 1000.0F - ForgeHooksClient.getGuiFarPlane());
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(invert && fearOverlayShader != null ? FearClientHandler::getFearOverlayShader : GameRenderer::getPositionTexColorShader);
        if (invert && fearOverlayShader != null && fearOverlayShader.getUniform("Invert") != null) {
            fearOverlayShader.getUniform("Invert").set(1.0F);
        } else if (fearOverlayShader != null && fearOverlayShader.getUniform("Invert") != null) {
            fearOverlayShader.getUniform("Invert").set(0.0F);
        }
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, 0, height, 0).uv(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.vertex(matrix, width, height, 0).uv(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.vertex(matrix, width, 0, 0).uv(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.vertex(matrix, 0, 0, 0).uv(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        poseStack.popPose();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static ShaderInstance getFearOverlayShader() {
        return fearOverlayShader;
    }

    private static int randomNormalDelay() {
        return MIN_NORMAL_TICKS + RANDOM.nextInt(MAX_NORMAL_TICKS - MIN_NORMAL_TICKS + 1);
    }

    private static int randomOverlayPulsePeriod() {
        return MIN_OVERLAY_PULSE_TICKS + RANDOM.nextInt(MAX_OVERLAY_PULSE_TICKS - MIN_OVERLAY_PULSE_TICKS + 1);
    }

    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) {
            FearClientHandler.onRegisterShaders(event);
        }
    }
}
