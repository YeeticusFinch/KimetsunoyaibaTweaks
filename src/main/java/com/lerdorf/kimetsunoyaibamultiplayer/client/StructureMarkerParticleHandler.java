package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StructureMarkerParticleHandler {
    private static final double MARKER_RADIUS = 64.0D;

    private StructureMarkerParticleHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || !StructureMarkerEntity.isHoldingMarker(player)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(MARKER_RADIUS);
        for (StructureMarkerEntity marker : level.getEntitiesOfClass(StructureMarkerEntity.class, area,
            StructureMarkerEntity::isAlive)) {
            if (marker.distanceToSqr(player) <= MARKER_RADIUS * MARKER_RADIUS) {
                level.addParticle(ModParticles.STRUCTURE_MARKER.get(),
                    marker.getX(), marker.getY() + 0.5D, marker.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || !StructureMarkerEntity.isHoldingMarker(player)) {
            return;
        }

        Vec3 cameraPosition = event.getCamera().getPosition();
        AABB area = player.getBoundingBox().inflate(MARKER_RADIUS);
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        for (StructureMarkerEntity marker : level.getEntitiesOfClass(StructureMarkerEntity.class, area,
            StructureMarkerEntity::isAlive)) {
            if (marker.getMarkerId().isEmpty()
                || marker.distanceToSqr(cameraPosition.x, cameraPosition.y, cameraPosition.z)
                    > MARKER_RADIUS * MARKER_RADIUS) {
                continue;
            }
            EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(marker);
            if (renderer instanceof StructureMarkerRenderer markerRenderer) {
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                poseStack.translate(marker.getX() - cameraPosition.x,
                    marker.getY() - cameraPosition.y,
                    marker.getZ() - cameraPosition.z);
                markerRenderer.renderMarkerId(marker, poseStack, buffer,
                    minecraft.getEntityRenderDispatcher().getPackedLightCoords(marker, event.getPartialTick()));
                poseStack.popPose();
            }
        }
        buffer.endBatch();
    }

    /** Keep marker entities out of the normal block/entity targeting path. */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        InteractionHand hand = event.getHand();
        if (!player.getItemInHand(hand).is(com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems.STRUCTURE_MARKER.get())) {
            if (player.getMainHandItem().is(com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems.STRUCTURE_MARKER.get())) {
                hand = InteractionHand.MAIN_HAND;
            } else if (player.getOffhandItem().is(com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems.STRUCTURE_MARKER.get())) {
                hand = InteractionHand.OFF_HAND;
            } else {
                return;
            }
        }

        StructureMarkerEntity marker = findMarkerUnderCrosshair(minecraft, player);
        if (marker == null) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        minecraft.gameMode.interact(player, marker, hand);
    }

    private static StructureMarkerEntity findMarkerUnderCrosshair(Minecraft minecraft, LocalPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = start.add(view.scale(minecraft.gameMode.getPickRange()));
        AABB searchArea = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(0.5D);

        StructureMarkerEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (StructureMarkerEntity marker : minecraft.level.getEntitiesOfClass(StructureMarkerEntity.class, searchArea,
            StructureMarkerEntity::isAlive)) {
            AABB hitBox = new AABB(
                marker.getX() - 0.35D, marker.getY(), marker.getZ() - 0.35D,
                marker.getX() + 0.35D, marker.getY() + 1.0D, marker.getZ() + 0.35D);
            Optional<Vec3> hit = hitBox.clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closest = marker;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }
}
