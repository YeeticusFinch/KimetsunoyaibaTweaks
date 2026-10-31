package com.lerdorf.kimetsunoyaibamultiplayer.client.events;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.LoveTornadoRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Renders Love Tornadoes after translucent world geometry so water cannot cover them. */
@Mod.EventBusSubscriber(
        modid = "kimetsunoyaibamultiplayer",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class LoveTornadoRenderEvents {
    private static final double RENDER_DISTANCE = 128.0D;

    private LoveTornadoRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        Frustum frustum = minecraft.levelRenderer.getFrustum();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        double cameraX = cameraPosition.x;
        double cameraY = cameraPosition.y;
        double cameraZ = cameraPosition.z;
        boolean rendered = false;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LoveTornadoEntity tornado) || tornado.isRemoved()
                    || tornado.distanceToSqr(cameraX, cameraY, cameraZ) > RENDER_DISTANCE * RENDER_DISTANCE
                    || !minecraft.getEntityRenderDispatcher().shouldRender(tornado, frustum, cameraX, cameraY, cameraZ)) {
                continue;
            }

            LoveTornadoRenderer.renderAfterTranslucent(tornado, event.getPartialTick(), poseStack,
                    bufferSource, cameraPosition);
            rendered = true;
        }

        if (rendered) {
            bufferSource.endBatch();
        }
    }
}
