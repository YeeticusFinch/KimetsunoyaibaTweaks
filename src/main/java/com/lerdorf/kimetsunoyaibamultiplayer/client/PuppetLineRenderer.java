package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Renders Puppetry effect strings as thin white fading ribbon meshes.
 *
 * Each string stores one fixed world-space endpoint per puppet UUID and line
 * index. The visible string is always the first 20 blocks from the puppet's
 * body anchor toward that stored endpoint, fading to full transparency at the
 * far end.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class PuppetLineRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/silk_ribbon.png");

    private static final int LINES_PER_PUPPET = 10;
    private static final int LINE_SEGMENTS = 16;
    private static final double MIN_ENDPOINT_RADIUS = 20.0D;
    private static final double MAX_ENDPOINT_RADIUS = 30.0D;
    private static final double LINE_LENGTH = 20.0D;
    private static final float LINE_HALF_WIDTH = 0.002F; // puppet line thickness

    private static final Vec3[] BODY_ANCHORS = {
        new Vec3(0.0D, 0.96D, 0.0D),   // head
        new Vec3(0.0D, 0.76D, 0.0D),   // chest
        new Vec3(0.0D, 0.55D, 0.0D),   // waist
        new Vec3(0.42D, 0.72D, 0.0D),  // right upper arm
        new Vec3(-0.42D, 0.72D, 0.0D), // left upper arm
        new Vec3(0.46D, 0.45D, 0.0D),  // right hand
        new Vec3(-0.46D, 0.45D, 0.0D), // left hand
        new Vec3(0.18D, 0.28D, 0.0D),  // right leg
        new Vec3(-0.18D, 0.28D, 0.0D), // left leg
        new Vec3(0.0D, 0.08D, 0.0D)    // feet/root
    };

    /**
     * Bone names (in GeckoLib / humanoid capture order) matching the semantic
     * role of each BODY_ANCHORS entry; used to look up true model-bone world
     * positions from {@link PuppetBoneCache} so the string start points track
     * the actual animated bones.
     */
    private static final String[] ANCHOR_BONES = {
        "head",           // head
        "body",           // chest
        "torso",          // waist
        "right_arm",      // right upper arm
        "left_arm",       // left upper arm
        "itemMainHand",   // right hand
        "itemOffHand",    // left hand
        "right_leg",      // right leg
        "left_leg",       // left leg
        "root"            // feet/root
    };

    /**
     * Stable global endpoints keyed by entity UUID. These are deliberately not
     * regenerated while the same puppet remains affected, so strings do not
     * flicker or re-aim every frame.
     */
    private static final Map<UUID, Vec3[]> LINE_ENDPOINTS = new HashMap<>();

    private PuppetLineRenderer() {
    }

    public static synchronized void setSyncedEndpoints(UUID puppetUuid, int entityId, Vec3[] endpoints) {
        if (puppetUuid == null || endpoints == null || endpoints.length == 0) {
            return;
        }
        LINE_ENDPOINTS.put(puppetUuid, endpoints);
        Log.debugVisible("[PuppetryLines] Client received {} line endpoints for puppet uuid={} entityId={}",
            endpoints.length, puppetUuid, entityId);
    }

    public static synchronized void clearSyncedEndpoints(UUID puppetUuid, int entityId) {
        if (puppetUuid != null) {
            LINE_ENDPOINTS.remove(puppetUuid);
        }
        Log.debugVisible("[PuppetryLines] Client cleared line endpoints for puppet uuid={} entityId={}",
            puppetUuid, entityId);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Camera cameraInfo = event.getCamera();
        Vec3 camera = cameraInfo.getPosition();
        PuppetBoneCache.beginFrame();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucent(TEXTURE);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int renderedPuppets = 0;
        int renderedLines = 0;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity puppet)) {
                continue;
            }
            Vec3[] endpoints = getEndpoints(puppet);
            if (endpoints == null && !puppet.hasEffect(ModEffects.PUPPETRY.get())
                && !puppet.hasEffect(ModEffects.WEB_TRAVERSAL.get())) {
                continue;
            }

            int lines = renderPuppetStrings(puppet, matrix, normal, camera, buffer, endpoints);
            if (lines > 0) {
                renderedPuppets++;
                renderedLines += lines;
            }
        }

        if (renderedLines > 0) {
            bufferSource.endBatch(renderType);
            Log.debugVisibleEvery("puppetry-lines-render", 2000L,
                "[PuppetryLines] Rendered {} lines for {} puppets", renderedLines, renderedPuppets);
        }
    }

    private static Vec3[] getEndpoints(LivingEntity puppet) {
        synchronized (PuppetLineRenderer.class) {
            return LINE_ENDPOINTS.get(puppet.getUUID());
        }
    }

    private static int renderPuppetStrings(LivingEntity puppet, Matrix4f matrix, Matrix3f normal,
                                           Vec3 camera, VertexConsumer buffer, Vec3[] syncedEndpoints) {
        Vec3[] endpoints = syncedEndpoints != null ? syncedEndpoints : getOrCreateEndpoints(puppet);
        Vec3 puppetPos = puppet.getPosition(Minecraft.getInstance().getFrameTime());
        float yRotRad = puppet.getYRot() * ((float) Math.PI / 180F);
        double width = Math.max(0.35D, puppet.getBbWidth());
        double height = Math.max(0.4D, puppet.getBbHeight());
        int rendered = 0;

        for (int i = 0; i < Math.min(LINES_PER_PUPPET, endpoints.length); i++) {
            Vec3 boneWorld = resolveBoneAnchor(puppet, i, puppetPos, yRotRad, width, height);
            Vec3 toEndpoint = endpoints[i].subtract(boneWorld);
            if (toEndpoint.lengthSqr() < 1.0E-4D) {
                continue;
            }
            double lineLength = Math.min(LINE_LENGTH, toEndpoint.length());
            Vec3 endWorld = boneWorld.add(toEndpoint.normalize().scale(lineLength));
            drawBezierRibbon(matrix, normal, buffer, camera, boneWorld, endWorld, i);
            rendered++;
        }

        if (rendered == 0) {
            Log.debugVisibleEvery("puppetry-lines-empty-" + puppet.getId(), 2000L,
                "[PuppetryLines] Puppet {} had endpoints but rendered no lines", puppet.getId());
        }
        return rendered;
    }

    /**
     * Resolves the line's start point: the cached true model-bone position for
     * this frame when available (moves with the animated bones), otherwise the
     * classic body-offset anchor.
     */
    private static Vec3 resolveBoneAnchor(LivingEntity puppet, int lineIndex, Vec3 puppetPos,
                                          float yRotRad, double width, double height) {
        String boneName = ANCHOR_BONES[lineIndex % ANCHOR_BONES.length];
        // The local player is not rendered in first person, and player model
        // capture matrices are camera-relative in third person. Use the live
        // entity position so the line origin remains in global world space.
        PuppetBoneCache.Vec3 bone = puppet instanceof Player
            ? null : PuppetBoneCache.getBone(puppet, boneName);
        if (bone != null) {
            return new Vec3(bone.x(), bone.y(), bone.z());
        }
        // Bone capture unavailable this frame (offscreen, non-humanoid vanilla
        // model, etc.) - fall back to the body-offset anchor.
        Vec3 anchor = BODY_ANCHORS[lineIndex % BODY_ANCHORS.length];
        return anchorToWorld(puppetPos, anchor, yRotRad, width, height);
    }

    private static Vec3 anchorToWorld(Vec3 puppetPos, Vec3 anchor, float yRotRad, double width, double height) {
        double localX = anchor.x * width;
        double localY = anchor.y * height;
        double cos = Math.cos(yRotRad);
        double sin = Math.sin(yRotRad);
        double x = puppetPos.x + localX * cos;
        double z = puppetPos.z + localX * sin;
        return new Vec3(x, puppetPos.y + localY, z);
    }

    private static void drawBezierRibbon(Matrix4f matrix, Matrix3f normal, VertexConsumer buffer,
                                         Vec3 camera, Vec3 startWorld, Vec3 endWorld, int lineIndex) {
        double distance = startWorld.distanceTo(endWorld);
        Vec3 direction = endWorld.subtract(startWorld);
        if (distance < 1.0E-4D) {
            return;
        }

        Vec3 dirNorm = direction.normalize();
        Vec3 side = dirNorm.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-4D) {
            side = dirNorm.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize().scale(((lineIndex % 2) == 0 ? 1.0D : -1.0D) * Math.min(1.4D, distance * 0.08D));
        double lift = Math.min(2.2D, distance * 0.16D);

        Vec3 controlA = startWorld.add(direction.scale(0.32D)).add(0.0D, lift, 0.0D).add(side);
        Vec3 controlB = startWorld.add(direction.scale(0.72D)).add(0.0D, lift * 0.55D, 0.0D).subtract(side.scale(0.35D));

        for (int s = 0; s < LINE_SEGMENTS; s++) {
            float t0 = (float) s / LINE_SEGMENTS;
            float t1 = (float) (s + 1) / LINE_SEGMENTS;
            Vec3 worldA = cubicBezier(startWorld, controlA, controlB, endWorld, t0);
            Vec3 worldB = cubicBezier(startWorld, controlA, controlB, endWorld, t1);
            Vector3f a = toVector(worldA.subtract(camera));
            Vector3f b = toVector(worldB.subtract(camera));

            Vector3f sideA = ribbonSide(worldA, a, b, camera);
            Vector3f sideB = new Vector3f(sideA);
            Vector3f leftA = new Vector3f(a).add(sideA);
            Vector3f rightA = new Vector3f(a).sub(sideA);
            Vector3f leftB = new Vector3f(b).add(sideB);
            Vector3f rightB = new Vector3f(b).sub(sideB);

            float alpha0 = 0.95F * (1.0F - t0);
            float alpha1 = 0.95F * (1.0F - t1);
            quad(buffer, matrix, normal, leftA, leftB, rightB, rightA, alpha0, alpha1);
        }
    }

    private static Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inv = 1.0D - t;
        return p0.scale(inv * inv * inv)
            .add(p1.scale(3.0D * inv * inv * t))
            .add(p2.scale(3.0D * inv * t * t))
            .add(p3.scale(t * t * t));
    }

    private static Vector3f ribbonSide(Vec3 worldPoint, Vector3f point, Vector3f next, Vec3 camera) {
        Vector3f dir = new Vector3f(next).sub(point);
        if (dir.lengthSquared() < 1.0E-8F) {
            dir.set(0.0F, 1.0F, 0.0F);
        }
        Vector3f toCamera = new Vector3f(
            (float) (camera.x - worldPoint.x),
            (float) (camera.y - worldPoint.y),
            (float) (camera.z - worldPoint.z));
        Vector3f side = dir.cross(toCamera);
        if (side.lengthSquared() < 1.0E-8F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        return side.normalize().mul(LINE_HALF_WIDTH);
    }

    private static Vector3f toVector(Vec3 vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                             Vector3f leftA, Vector3f leftB, Vector3f rightB, Vector3f rightA,
                             float alphaA, float alphaB) {
        buffer.vertex(matrix, leftA.x(), leftA.y(), leftA.z()).color(1.0F, 1.0F, 1.0F, alphaA).uv(0.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, leftB.x(), leftB.y(), leftB.z()).color(1.0F, 1.0F, 1.0F, alphaB).uv(0.0F, 1.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, rightB.x(), rightB.y(), rightB.z()).color(1.0F, 1.0F, 1.0F, alphaB).uv(1.0F, 1.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, rightA.x(), rightA.y(), rightA.z()).color(1.0F, 1.0F, 1.0F, alphaA).uv(1.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static synchronized Vec3[] getOrCreateEndpoints(LivingEntity puppet) {
        return LINE_ENDPOINTS.computeIfAbsent(puppet.getUUID(), uuid -> {
            Random random = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
            Vec3[] endpoints = new Vec3[LINES_PER_PUPPET];
            Vec3 origin = puppet.position().add(0.0D, puppet.getBbHeight() * 0.5D, 0.0D);
            for (int i = 0; i < LINES_PER_PUPPET; i++) {
                Vec3 dir = new Vec3(random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 1.4D - 0.2D,
                    random.nextDouble() * 2.0D - 1.0D);
                if (dir.lengthSqr() < 1.0E-4D) {
                    dir = new Vec3(0.0D, 1.0D, 0.0D);
                }
                double dist = MIN_ENDPOINT_RADIUS
                    + random.nextDouble() * (MAX_ENDPOINT_RADIUS - MIN_ENDPOINT_RADIUS);
                endpoints[i] = origin.add(dir.normalize().scale(dist));
            }
            return endpoints;
        });
    }
}
