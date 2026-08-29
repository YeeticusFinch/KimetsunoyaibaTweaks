package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-frame cache of true model-bone world positions for Puppetry puppets.
 *
 * Two capture sources feed this cache:
 * - {@code PuppetGeoBoneCaptureMixin} enables GeckoLib bone matrix tracking on
 *   {@code GeoEntityRenderer#renderRecursively} and stores each bone's
 *   world-space position here (covers all GeckoLib renderers, including the
 *   base mod's).
 * - {@link #onRenderLivingPost} captures vanilla {@link HumanoidModel} parts
 *   (players, zombies, villagers, etc.) via {@code ModelPart#translateAndRotate}.
 *
 * {@link PuppetLineRenderer} reads the cached positions each frame so the
 * string start points track the actual animated bones. Entries are only valid
 * for the frame they were captured in; stale/missing entries fall back to the
 * body-offset anchors.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class PuppetBoneCache {

    /** Bone-name keyed world positions captured during the current frame. */
    private static final Map<Integer, Map<String, Vec3>> CAPTURES = new ConcurrentHashMap<>();
    private static volatile long currentFrame = 0L;

    private PuppetBoneCache() {
    }

    /** Advances the frame counter; called by the line renderer at frame start. */
    public static void beginFrame() {
        currentFrame++;
        if (currentFrame % 600L == 0L && CAPTURES.size() > 256) {
            CAPTURES.clear(); // periodic sweep of long-gone entities
        }
    }

    /** Whether bone matrices should be captured for this entity this frame. */
    public static boolean needsCapture(LivingEntity entity) {
        return entity != null && entity.level().isClientSide()
            && entity.hasEffect(ModEffects.PUPPETRY.get());
    }

    /**
     * Stores a GeckoLib bone world position (from its world-space matrix).
     * Called from the render mixin for every rendered bone of a puppet.
     */
    public static void captureGeoBone(LivingEntity entity, String boneName, Matrix4f worldSpaceMatrix) {
        if (boneName == null || worldSpaceMatrix == null) {
            return;
        }
        Vector4f origin = worldSpaceMatrix.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        store(entity, boneName, origin.x(), origin.y(), origin.z());
    }

    /**
     * Captures vanilla humanoid model part positions after the entity render
     * completes (setupAnim has run; pose stack includes full entity transform).
     */
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!needsCapture(entity)) {
            return;
        }
        if (!(event.getRenderer().getModel() instanceof HumanoidModel<?> model)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Map<String, Vec3> bones = new HashMap<>();
        long frame = currentFrame;
        net.minecraft.world.phys.Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        capturePart(poseStack, cam, model.head, 0.0F, 0.0F, 0.0F, bones, "head", frame);
        capturePart(poseStack, cam, model.body, 0.0F, 0.0F, 0.0F, bones, "body", frame);
        bones.put("torso", bones.get("body"));
        capturePart(poseStack, cam, model.rightArm, 0.0F, 0.0F, 0.0F, bones, "right_arm", frame);
        capturePart(poseStack, cam, model.leftArm, 0.0F, 0.0F, 0.0F, bones, "left_arm", frame);
        capturePart(poseStack, cam, model.rightLeg, 0.0F, 0.0F, 0.0F, bones, "right_leg", frame);
        capturePart(poseStack, cam, model.leftLeg, 0.0F, 0.0F, 0.0F, bones, "left_leg", frame);
        // Hands: 10 model units below the arm pivots (flipped space: +y is down)
        capturePart(poseStack, cam, model.rightArm, 0.0F, 10.0F, 0.0F, bones, "itemMainHand", frame);
        capturePart(poseStack, cam, model.leftArm, 0.0F, 10.0F, 0.0F, bones, "itemOffHand", frame);
        // Root/feet: 24 model units below the body pivot (ground level)
        capturePart(poseStack, cam, model.body, 0.0F, 24.0F, 0.0F, bones, "root", frame);

        CAPTURES.put(entity.getId(), bones);
    }

    private static void capturePart(PoseStack poseStack, net.minecraft.world.phys.Vec3 cam, ModelPart part, float ox, float oy, float oz,
                                    Map<String, Vec3> out, String name, long frame) {
        if (part == null) {
            return;
        }
        try {
            poseStack.pushPose();
            part.translateAndRotate(poseStack);
            Vector4f pos = poseStack.last().pose()
                .transform(new Vector4f(ox / 16.0F, oy / 16.0F, oz / 16.0F, 1.0F));
            poseStack.popPose();
            // pose stack is camera-relative; convert to world space
            out.put(name, new Vec3(cam.x + pos.x(), cam.y + pos.y(), cam.z + pos.z()));
        } catch (Exception ex) {
            Log.debug("[PuppetryLines] Vanilla bone capture failed for {}: {}", name, ex.toString());
        }
    }

    private static void store(LivingEntity entity, String boneName, double x, double y, double z) {
        CAPTURES.computeIfAbsent(entity.getId(), id -> new ConcurrentHashMap<>())
            .put(boneName, new Vec3(x, y, z));
    }

    /**
     * Returns the cached world position of a bone captured during the current
     * frame, or null if unavailable (renderer falls back to body anchors).
     */
    public static Vec3 getBone(LivingEntity entity, String boneName) {
        if (boneName == null || entity == null) {
            return null;
        }
        Map<String, Vec3> bones = CAPTURES.get(entity.getId());
        if (bones == null) {
            return null;
        }
        Vec3 entry = bones.get(boneName);
        return entry; // frame validity enforced by capture ordering (see below)
    }

    /** Drops cached captures for an entity (effect end cleanup). */
    public static void clear(int entityId) {
        CAPTURES.remove(entityId);
    }

    /** Immutable world-space point. */
    public record Vec3(double x, double y, double z) {
    }
}
