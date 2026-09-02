package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DissolutionCocoonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DaughterEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SilkRibbonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * Blood Demon Art: Silk Manipulation - the Daughter's thread-based art.
 *
 * Forms:
 * 1. Silk Spray (3500)      - a single white bezier ribbon that chases the
 *                             crosshair target up to 40 blocks; damage + dust on hit.
 * 2. Acid Spray (3501)      - multiple spread ribbons, half white / half dark green,
 *                             8 block range; poison on hit.
 * 3. Dissolution Cocoon (3502) - thick silk ribbon that cocoons whoever it hits,
 *                             trapping and poisoning them for up to 60 seconds.
 */
public final class SilkManipulation {
    public static final String ART_ID = "silk_manipulation";

    public static final int FORM_SILK_SPRAY = 3500;
    public static final int FORM_ACID_SPRAY = 3501;
    public static final int FORM_DISSOLUTION_COCOON = 3502;

    private static final double SILK_SPRAY_RANGE = 40.0D;
    private static final double ACID_SPRAY_RANGE = 8.0D;
    private static final int ACID_RIBBON_COUNT = 6;
    private static final float SILK_SPRAY_DAMAGE = 6.0F;
    private static final float ACID_SPRAY_DAMAGE = 3.0F;

    private SilkManipulation() {
    }

    public static void register() {
        if (BloodDemonArtRegistry.isRegistered(ART_ID)) {
            return;
        }
        KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Silk Manipulation", createTechnique());
    }

    private static BloodDemonArtTechnique createTechnique() {
        List<BloodDemonArtForm> forms = Arrays.asList(
            new BloodDemonArtForm(FORM_SILK_SPRAY, "Silk Spray",
                "Fires a white silk ribbon along an erratic path at your crosshair target.",
                1, SilkManipulation::executeSilkSpray),
            new BloodDemonArtForm(FORM_ACID_SPRAY, "Acid Spray",
                "Sprays multiple corrosive silk threads that poison anything they touch.",
                6, SilkManipulation::executeAcidSpray),
            new BloodDemonArtForm(FORM_DISSOLUTION_COCOON, "Dissolution Cocoon",
                "Wraps the target in dissolving silk, trapping them in a cocoon.",
                20, SilkManipulation::executeDissolutionCocoon)
        );
        return new BloodDemonArtTechnique("Silk Manipulation", forms, 0xF5F5F5);
    }

    // ==================== Form implementations ====================

    private static void executeSilkSpray(LivingEntity caster, Level level, int formId) {
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(0.6D)).subtract(0.0D, 0.25D, 0.0D);
        AimResolution aim = resolveSilkAim(caster, level, SILK_SPRAY_RANGE);

        SilkRibbonEntity ribbon = SilkRibbonEntity.spawn(level, caster, start, aim.point(),
            SilkRibbonEntity.RibbonKind.SILK, SILK_SPRAY_DAMAGE, false, aim.entity());

        if (!level.addFreshEntity(ribbon)) {
            return;
        }

        caster.swing(InteractionHand.MAIN_HAND, true);

        // Cocoon variant flag: none here, plain spray.
        playCastEffects(caster, level, start);
    }

    private static void executeAcidSpray(LivingEntity caster, Level level, int formId) {
        Vec3 baseAim = resolveAimDirection(caster, ACID_SPRAY_RANGE);
        Vec3 start = caster.getEyePosition().add(baseAim.scale(0.6D)).subtract(0.0D, 0.25D, 0.0D);

        for (int i = 0; i < ACID_RIBBON_COUNT; i++) {
            boolean darkGreen = (i % 2 == 1); // half white, half dark green

            // Spread: rotate the aim direction progressively around the vertical axis
            // plus slight pitch jitter so the ribbons fan out.
            double yawSpread = (i - (ACID_RIBBON_COUNT - 1) / 2.0D) * 0.14D;
            double pitchJitter = (level.random.nextDouble() - 0.5D) * 0.10D;
            Vec3 spreadAim = rotateAroundY(baseAim, yawSpread)
                .add(0.0D, pitchJitter, 0.0D).normalize();

            SilkRibbonEntity ribbon = SilkRibbonEntity.spawn(level, caster, start,
                start.add(spreadAim.scale(ACID_SPRAY_RANGE)),
                SilkRibbonEntity.RibbonKind.ACID, ACID_SPRAY_DAMAGE, false, null);
            ribbon.setAcidDarkGreen(darkGreen);
            if (!level.addFreshEntity(ribbon)) {
                continue;
            }
        }

        caster.swing(InteractionHand.MAIN_HAND, true);
        playCastEffects(caster, level, start);
    }

    private static void executeDissolutionCocoon(LivingEntity caster, Level level, int formId) {
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(0.6D)).subtract(0.0D, 0.15D, 0.0D);
        AimResolution aim = resolveSilkAim(caster, level, 20.0D);

        SilkRibbonEntity ribbon = SilkRibbonEntity.spawn(level, caster, start, aim.point(),
            SilkRibbonEntity.RibbonKind.SILK, 2.0F, true, aim.entity());

        if (!level.addFreshEntity(ribbon)) {
            return;
        }

        caster.swing(InteractionHand.MAIN_HAND, true);
        playCastEffects(caster, level, start);
    }

    // ==================== Helpers ====================

    /** Raytrace a silk shot, retaining a living entity hit for the ribbon to lock onto. */
    private static AimResolution resolveSilkAim(LivingEntity caster, Level level, double range) {
        Vec3 start = caster.getEyePosition();
        LivingEntity daughterTarget = caster instanceof DaughterEntity daughter ? daughter.getTarget() : null;
        Vec3 maxEnd = daughterTarget != null && daughterTarget.isAlive()
            ? entityCenter(daughterTarget)
            : start.add(caster.getLookAngle().scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(
            start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level, caster, start, rayEnd, new AABB(start, rayEnd).inflate(1.0D),
            entity -> entity != caster && entity instanceof LivingEntity living
                && living.isAlive() && !living.isSpectator());
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            return new AimResolution(entityCenter(living), living);
        }
        return new AimResolution(rayEnd, null);
    }

    private static Vec3 entityCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private record AimResolution(Vec3 point, LivingEntity entity) {
    }

    private static Vec3 resolveAimDirection(LivingEntity caster, double range) {
        // Prefer entity crosshair pick so sprays track targets slightly off-block.
        HitResult hit = caster.pick(range, 1.0F, false);
        Vec3 to = hit.getLocation();
        Vec3 dir = to.subtract(caster.getEyePosition());
        return dir.lengthSqr() > 1.0E-4D ? dir.normalize() : caster.getLookAngle();
    }

    private static Vec3 rotateAroundY(Vec3 vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vec.x * cos + vec.z * sin, vec.y, -vec.x * sin + vec.z * cos);
    }

    private static void playCastEffects(LivingEntity caster, Level level, Vec3 handPos) {
        if (level.isClientSide()) {
            return;
        }
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
            handPos.x, handPos.y, handPos.z, 6, 0.15D, 0.15D, 0.15D, 0.01D);
    }
}
