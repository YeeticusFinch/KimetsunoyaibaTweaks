package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.RuiBloodDemonArt;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A white (or acid-tinted) silk ribbon that travels from the caster's hand
 * toward a target along an erratic but smooth bezier path.
 *
 * The ribbon itself is rendered client-side as a smooth ribbon mesh connecting
 * the recorded trail points (see {@link com.lerdorf.kimetsunoyaibamultiplayer.entities.client.SilkRibbonRenderer}).
 *
 * Behavior flags:
 * - ACID: applies poison on hit and renders dark green segments interleaved with white.
 * - COCOON: thick ribbon; on entity hit spawns a DissolutionCocoonEntity around the victim.
 */
public class SilkRibbonEntity extends Mob {
    public enum RibbonKind {
        SILK(0),   // pure white, damage + dust particles, up to 40 blocks
        ACID(1);   // half white / half dark green, poison, 8 blocks

        private final int id;

        RibbonKind(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static RibbonKind fromId(int id) {
            for (RibbonKind kind : values()) {
                if (kind.id == id) {
                    return kind;
                }
            }
            return SILK;
        }
    }

    private static final EntityDataAccessor<Integer> KIND =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.INT);

    /** Acid variant tint: false = white, true = dark green. */
    private static final EntityDataAccessor<Boolean> ACID_DARK_GREEN =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> RED_THREAD =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> WHIP_MODE =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WHIP_VERTICAL =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RUI_THREAD =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> WHIP_OFFSET =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHIP_ANGLE =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHIP_LENGTH_SCALE =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SNATCH_MODE =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CONTORTING_MODE =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.BOOLEAN);

    // Synced trail so late-loading clients can still draw the ribbon.
    // Stored as packed doubles: [x0,y0,z0, x1,y1,z1, ...]
    private static final EntityDataAccessor<String> TRAIL =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.STRING);

    /** Trail point spacing in blocks. */
    private static final double TRAIL_SPACING = 0.35D;
    private static final int MAX_TRAIL_POINTS = 160; // ~56 blocks of ribbon

    private float damage = 6.0F;
    private boolean cocoonOnHit = false;
    private boolean liftOnHit = false;
    private boolean snareOnDetach = false;
    @Nullable
    private LivingEntity snatchTarget = null;
    private int snatchTicksRemaining = 0;
    private Vec3 snatchOffset = Vec3.ZERO;
    @Nullable
    private java.util.UUID casterUuid = null;
    @Nullable
    private UUID targetEntityUuid = null;
    @Nullable
    private LivingEntity targetEntity = null;
    @Nullable
    private Vec3 targetPoint = null;

    private boolean targetLocked = false;

    private int ticksAlive = 0;
    private int maxAgeTicks = 100; // safety kill switch
    private double maxTravelBlocks = 40.0D;
    private double distanceTraveled = 0.0D;

    /**
     * When true the ribbon has impacted something; it stops moving but lingers
     * so its trail can fade out instead of vanishing on impact.
     */
    private boolean fadingOut = false;
    private int fadeOutTicksRemaining = 0;
    private static final int FADE_OUT_TICKS = 10; // half second linger
    private static final double TARGET_LOCK_DISTANCE_SQR = 9.0D;
    private static final int SNATCH_ATTACH_TICKS = 12;
    private static final String SNATCH_LAUNCH_TICK_KEY = "KnYRuiSnatchLaunchTick";

    /** Erratic-but-smooth lateral wobble seeds. */
    private double wobbleAmpXZ = 0.9D;
    private double wobbleFreq = 0.55D;
    private double wobblePhaseA = 0.0D;
    private double wobblePhaseB = 0.0D;

    /** Low-pass filtered aim direction; prevents heading snaps (visible shake). */
    private Vec3 smoothedAim = null;

    /** Fixed bend for Rui's attacks: the ribbon leaves the caster, bows outward, then returns to the target. */
    private Vec3 curveSide = Vec3.ZERO;
    private double curveLength = 1.0D;

    private Vec3 lastTrailPoint = null;
    private Vec3 contortingLaunchDirection = Vec3.ZERO;
    private Vec3 contortingTargetPoint = null;
    private Vec3 contortingOrigin = null;
    private boolean contortingDamageDealer = false;

    private static final int CONTORTING_LAUNCH_TICKS = 15;
    private static final double CONTORTING_LAUNCH_SPEED = 2.6D;
    private static final double CONTORTING_CLOSE_SPEED = 4.5D;

    public SilkRibbonEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setSilent(true);
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /**
     * Spawn a ribbon at the caster's hand position heading toward targetPoint.
     */
    public static SilkRibbonEntity spawn(Level level, LivingEntity caster, Vec3 start, Vec3 targetPoint,
                                         RibbonKind kind, float damage, boolean cocoonOnHit,
                                         @Nullable LivingEntity targetEntity) {
        SilkRibbonEntity ribbon = new SilkRibbonEntity(ModEntities.SILK_RIBBON.get(), level);
        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.inheritVisual(ribbon);
        start = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(start);
        targetPoint = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(targetPoint);
        ribbon.setPos(start.x, start.y, start.z);
        ribbon.casterUuid = caster.getUUID();
        ribbon.targetPoint = targetPoint;
        ribbon.targetEntity = targetEntity;
        ribbon.targetEntityUuid = targetEntity != null ? targetEntity.getUUID() : null;
        Vec3 forward = targetPoint.subtract(start);
        ribbon.curveLength = Math.max(1.0D, forward.length());
        ribbon.curveSide = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (ribbon.curveSide.lengthSqr() < 1.0E-4D) {
            ribbon.curveSide = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            ribbon.curveSide = ribbon.curveSide.normalize();
        }
        ribbon.damage = damage;
        ribbon.cocoonOnHit = cocoonOnHit;
        ribbon.entityData.set(KIND, kind.id());

        if (kind == RibbonKind.ACID) {
            ribbon.maxTravelBlocks = 8.0D;
            ribbon.maxAgeTicks = 60;
            ribbon.wobbleAmpXZ = 1.4D;
            ribbon.wobbleFreq = 0.8D;
        } else if (cocoonOnHit) {
            ribbon.maxTravelBlocks = 20.0D;
            ribbon.wobbleAmpXZ = 0.5D;
            ribbon.wobbleFreq = 0.35D;
        }
        ribbon.wobblePhaseA = level.random.nextDouble() * Math.PI * 2.0D;
        ribbon.wobblePhaseB = level.random.nextDouble() * Math.PI * 2.0D;
        ribbon.lastTrailPoint = start;
        ribbon.entityData.set(TRAIL, (float) start.x + "," + (float) start.y + "," + (float) start.z);
        return ribbon;
    }

    public void setWhipMode(boolean enabled, boolean vertical) {
        setWhipMode(enabled, vertical, 0.0F,
            vertical ? (float) (Math.PI * 0.5D) : 0.0F);
    }

    public void setWhipMode(boolean enabled, boolean vertical, float offset) {
        setWhipMode(enabled, vertical, offset,
            vertical ? (float) (Math.PI * 0.5D) : 0.0F);
    }

    public void setWhipMode(boolean enabled, boolean vertical, float offset, float angle) {
        this.entityData.set(WHIP_MODE, enabled);
        this.entityData.set(WHIP_VERTICAL, vertical);
        this.entityData.set(WHIP_OFFSET, offset);
        this.entityData.set(WHIP_ANGLE, angle);
    }

    public void setWhipLengthScale(float scale) {
        this.entityData.set(WHIP_LENGTH_SCALE, Math.max(0.1F, scale));
    }

    public void setSnatchMode(boolean snare, Vec3 offset) {
        this.snareOnDetach = snare;
        this.snatchOffset = offset == null ? Vec3.ZERO : offset;
        this.entityData.set(SNATCH_MODE, true);
    }

    public void setContortingMode(Vec3 launchDirection, Vec3 targetPoint, boolean damageDealer) {
        this.contortingLaunchDirection = launchDirection == null || launchDirection.lengthSqr() < 1.0E-6D
            ? new Vec3(0.0D, 0.0D, 1.0D) : launchDirection.normalize();
        this.contortingTargetPoint = targetPoint;
        this.contortingOrigin = position();
        this.contortingDamageDealer = damageDealer;
        this.maxTravelBlocks = 40.0D;
        this.maxAgeTicks = 60;
        this.entityData.set(CONTORTING_MODE, true);
    }

    public void setRuiThread(boolean ruiThread) {
        this.entityData.set(RUI_THREAD, ruiThread);
    }

    public boolean isRuiThread() {
        return this.entityData.get(RUI_THREAD);
    }

    private boolean isWhipMode() {
        return this.entityData.get(WHIP_MODE);
    }

    private boolean isSnatchMode() {
        return this.entityData.get(SNATCH_MODE);
    }

    private boolean isContortingMode() {
        return this.entityData.get(CONTORTING_MODE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(KIND, RibbonKind.SILK.id());
        this.entityData.define(ACID_DARK_GREEN, false);
        this.entityData.define(RED_THREAD, false);
        this.entityData.define(WHIP_MODE, false);
        this.entityData.define(WHIP_VERTICAL, false);
        this.entityData.define(RUI_THREAD, false);
        this.entityData.define(WHIP_OFFSET, 0.0F);
        this.entityData.define(WHIP_ANGLE, 0.0F);
        this.entityData.define(WHIP_LENGTH_SCALE, 1.0F);
        this.entityData.define(SNATCH_MODE, false);
        this.entityData.define(CONTORTING_MODE, false);
        this.entityData.define(TRAIL, "");
    }

    public boolean isAcidDarkGreen() {
        return this.entityData.get(ACID_DARK_GREEN);
    }

    public void setAcidDarkGreen(boolean darkGreen) {
        this.entityData.set(ACID_DARK_GREEN, darkGreen);
    }

    public boolean isRedThread() {
        return this.entityData.get(RED_THREAD);
    }

    public void setRedThread(boolean redThread) {
        this.entityData.set(RED_THREAD, redThread);
    }

    public RibbonKind getKind() {
        return RibbonKind.fromId(this.entityData.get(KIND));
    }

    public boolean isCocoonShot() {
        return cocoonOnHit;
    }

    public void setLiftOnHit(boolean liftOnHit) {
        this.liftOnHit = liftOnHit;
    }

    public float[] getTrail() {
        String packed = this.entityData.get(TRAIL);
        if (packed == null || packed.isEmpty()) {
            return new float[0];
        }
        String[] parts = packed.split(";");
        float[] result = new float[parts.length * 3];
        for (int i = 0; i < parts.length; i++) {
            String[] xyz = parts[i].split(",");
            for (int c = 0; c < 3 && c < xyz.length; c++) {
                try {
                    result[i * 3 + c] = Float.parseFloat(xyz[c]);
                } catch (NumberFormatException ignored) {
                    result[i * 3 + c] = 0.0F;
                }
            }
        }
        return result;
    }

    public double getWobblePhaseA() {
        return wobblePhaseA;
    }

    public double getWobblePhaseB() {
        return wobblePhaseB;
    }

    public double getWobbleAmpXZ() {
        return wobbleAmpXZ;
    }

    public double getWobbleFreq() {
        return wobbleFreq;
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        if (level().isClientSide) {
            return;
        }

        if (snatchTarget != null) {
            if (snatchTarget.isAlive() && !snatchTarget.isRemoved() && snatchTicksRemaining > 0) {
                setDeltaMovement(Vec3.ZERO);
                recordSnatchTrail(snatchTarget);
                snatchTicksRemaining--;
                if (snatchTicksRemaining <= 0) {
                    releaseSnatchTarget();
                }
                return;
            }
            releaseSnatchTarget();
        }

        // After impact: linger briefly so the trail fades out, then discard.
        if (fadingOut) {
            setDeltaMovement(Vec3.ZERO);
            fadeOutTicksRemaining--;
            if (fadeOutTicksRemaining <= 0) {
                discard();
            }
            return;
        }

        if (isContortingMode()) {
            tickContorting();
            return;
        }

        // Rui's basic and Silk Strike threads are straight projectiles. Other
        // silk ribbons retain their existing curved, target-seeking movement.
        Vec3 pos = position();
        double speed = 1.84D; // 60% faster so the ribbon reaches its target quickly
        Vec3 velocity;
        if (isWhipMode() || isSnatchMode()) {
            Vec3 directAim = targetPoint != null && !hasPassedTarget(pos)
                ? targetPoint.subtract(pos).normalize()
                : (getDeltaMovement().lengthSqr() > 1.0E-4D
                    ? getDeltaMovement().normalize() : new Vec3(0.0D, 0.0D, 1.0D));
            smoothedAim = directAim;
            velocity = directAim.scale(speed);
        } else {
            LivingEntity liveTarget = resolveTargetEntity();
            if (liveTarget != null && !targetLocked
                && pos.distanceToSqr(entityCenter(liveTarget)) <= TARGET_LOCK_DISTANCE_SQR) {
                targetLocked = true;
            }

            if (liveTarget != null && targetLocked) {
                Vec3 toTarget = entityCenter(liveTarget).subtract(pos);
                Vec3 directAim = toTarget.lengthSqr() > 1.0E-4D
                    ? toTarget.normalize()
                    : (getDeltaMovement().lengthSqr() > 1.0E-4D
                        ? getDeltaMovement().normalize() : Vec3.ZERO);
                smoothedAim = directAim;
                velocity = directAim.scale(speed);
            } else {
                Vec3 rawAim = targetPoint != null && !hasPassedTarget(pos)
                    ? targetPoint.subtract(pos).normalize()
                    : (getDeltaMovement().lengthSqr() > 1.0E-4D
                        ? getDeltaMovement().normalize() : new Vec3(0, 0, 1));
                if (smoothedAim == null) {
                    smoothedAim = rawAim;
                } else {
                    smoothedAim = smoothedAim.scale(0.6D).add(rawAim.scale(0.4D)).normalize();
                }

                int age = ticksAlive;
                Vec3 perpendicularA = new Vec3(-smoothedAim.z, 0.0D, smoothedAim.x).normalize();
                Vec3 perpendicularB = smoothedAim.cross(perpendicularA).normalize();

                double amp = wobbleAmpXZ * Math.min(1.0D, age / 6.0D);
                Vec3 wobble = perpendicularA.scale(Math.sin(age * wobbleFreq + wobblePhaseA) * amp)
                    .add(perpendicularB.scale(Math.sin(age * wobbleFreq * 0.7D + wobblePhaseB) * amp * 0.6D));
                double curveProgress = Math.min(1.0D, distanceTraveled / curveLength);
                double curveAmount = Math.sin(curveProgress * Math.PI) * 0.72D;
                Vec3 curve = curveSide.scale(curveAmount);
                velocity = smoothedAim.add(wobble.normalize().scale(Math.min(amp, 0.45D)))
                    .add(curve).normalize().scale(speed);
            }
        }
        setDeltaMovement(velocity);

        Vec3 newPos = pos.add(velocity);
        distanceTraveled += newPos.distanceTo(pos);
        setPos(newPos.x, newPos.y, newPos.z);

        if (isWhipMode() || isSnatchMode()) {
            recordWhipTrail(newPos);
        } else {
            recordTrail(newPos);
        }

        // Entity hit detection along this tick's segment.
        HitResult hit = findHit(newPos, pos);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            onImpact(hit, newPos);
            // AOE splash where it lands, then linger for the fade-out.
            applySplashDamage(newPos);
            if (snatchTarget == null) {
                beginFadeOut();
            }
            return;
        }

        if (distanceTraveled >= maxTravelBlocks || ticksAlive >= maxAgeTicks) {
            spawnEndParticles(newPos);
            applySplashDamage(newPos);
            beginFadeOut();
        }
    }

    private void tickContorting() {
        if (contortingTargetPoint == null) {
            beginFadeOut();
            return;
        }

        Vec3 pos = position();
        Vec3 velocity;
        if (ticksAlive < CONTORTING_LAUNCH_TICKS) {
            velocity = contortingLaunchDirection.scale(CONTORTING_LAUNCH_SPEED);
        } else {
            Vec3 toTarget = contortingTargetPoint.subtract(pos);
            if (toTarget.lengthSqr() <= CONTORTING_CLOSE_SPEED * CONTORTING_CLOSE_SPEED) {
                setPos(contortingTargetPoint.x, contortingTargetPoint.y, contortingTargetPoint.z);
                recordContortingTrail(position());
                if (contortingDamageDealer) {
                    applyContortingDamage(contortingTargetPoint);
                }
                beginFadeOut();
                return;
            }
            velocity = toTarget.normalize().scale(CONTORTING_CLOSE_SPEED);
        }

        setDeltaMovement(velocity);
        Vec3 newPos = pos.add(velocity);
        distanceTraveled += newPos.distanceTo(pos);
        setPos(newPos.x, newPos.y, newPos.z);
        recordContortingTrail(newPos);

        if (ticksAlive >= maxAgeTicks) {
            if (contortingDamageDealer) {
                applyContortingDamage(contortingTargetPoint);
            }
            beginFadeOut();
        }
    }

    private void recordContortingTrail(Vec3 ribbonPosition) {
        LivingEntity caster = resolveCaster();
        if (caster == null) {
            recordTrail(ribbonPosition);
            return;
        }

        Vec3 anchor = RuiBloodDemonArt.getThreadAnchor(caster);
        Vec3 direction = ribbonPosition.subtract(anchor);
        if (direction.lengthSqr() < 1.0E-6D) {
            recordTrail(ribbonPosition);
            return;
        }

        Vec3 control = anchor.add(direction.scale(0.55D))
            .add(0.0D, Math.min(2.0D, Math.max(0.35D, direction.length() * 0.08D)), 0.0D);
        StringBuilder packed = new StringBuilder();
        for (int index = 0; index <= 12; index++) {
            double progress = index / 12.0D;
            appendPoint(packed, quadraticBezier(anchor, control, ribbonPosition, progress));
        }
        this.entityData.set(TRAIL, packed.toString());
        this.lastTrailPoint = ribbonPosition;
    }

    /** Stop moving and linger so the rendered trail can fade out. */
    private void beginFadeOut() {
        this.fadingOut = true;
        this.fadeOutTicksRemaining = FADE_OUT_TICKS;
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean isFadingOut() {
        return fadingOut;
    }

    public float getFadeOutProgress(float partialTicks) {
        if (!fadingOut) {
            return 0.0F;
        }
        return 1.0F - Math.max(0.0F, (fadeOutTicksRemaining - partialTicks) / (float) FADE_OUT_TICKS);
    }

    /**
     * Small AOE splash where the ribbon lands: damages every living entity
     * near the impact point (except the caster) through {@link Damager#hurt}
     * so damage scales with the caster's strength/attributes.
     */
    private void applySplashDamage(Vec3 impactPos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity caster = resolveCaster();

        double splashRadius = 2.5D;
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(impactPos, impactPos).inflate(splashRadius),
            e -> e.isAlive() && e != caster && !(e instanceof SilkRibbonEntity)
                && !(e instanceof DissolutionCocoonEntity));

        float splashDamage = damage * 0.5F; // splash hits at half strength
        for (LivingEntity victim : victims) {
            if (cocoonOnHit) {
                DissolutionCocoonEntity.tryCapture(victim, caster);
            }

            double dist = victim.position().add(0, victim.getBbHeight() * 0.5, 0).distanceTo(impactPos);
            // Full damage at the center, falling off to 40% at the edge.
            float falloff = (float) (1.0D - 0.6D * Math.min(1.0D, dist / splashRadius));
            float amount = splashDamage * falloff;
            if (amount > 0.05F && caster != null) {
                Damager.hurt(caster, victim, amount, true);
            }
        }

        serverLevel.sendParticles(ParticleTypes.POOF, impactPos.x, impactPos.y, impactPos.z,
            14, splashRadius * 0.4D, splashRadius * 0.3D, splashRadius * 0.4D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, impactPos.x, impactPos.y, impactPos.z,
            8, splashRadius * 0.3D, splashRadius * 0.2D, splashRadius * 0.3D, 0.01D);
    }

    private void applyContortingDamage(Vec3 targetPoint) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity caster = resolveCaster();
        if (caster == null || contortingOrigin == null) {
            return;
        }

        Vec3 lineStart = contortingOrigin;
        Vec3 line = targetPoint.subtract(lineStart);
        double lineLengthSqr = line.lengthSqr();
        AABB searchBox = new AABB(lineStart, targetPoint).inflate(3.0D);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
            entity -> entity.isAlive() && entity != caster
                && !(entity instanceof SilkRibbonEntity)
                && !(entity instanceof DissolutionCocoonEntity))) {
            Vec3 victimCenter = entityCenter(victim);
            double progress = lineLengthSqr < 1.0E-6D
                ? 0.0D : Math.max(0.0D, Math.min(1.0D,
                    victimCenter.subtract(lineStart).dot(line) / lineLengthSqr));
            Vec3 closest = lineStart.add(line.scale(progress));
            if (victimCenter.distanceTo(closest) <= 3.0D) {
                Damager.hurt(caster, victim, damage, true);
            }
        }

        serverLevel.sendParticles(ParticleTypes.POOF, targetPoint.x, targetPoint.y, targetPoint.z,
            16, 1.0D, 1.0D, 1.0D, 0.02D);
    }

    private boolean hasPassedTarget(Vec3 pos) {
        if (targetPoint == null) {
            return true;
        }
        Vec3 toTarget = targetPoint.subtract(pos);
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-4D) {
            return false;
        }
        return toTarget.dot(motion) < 0.0D || toTarget.length() < 0.8D;
    }

    @Nullable
    private LivingEntity resolveTargetEntity() {
        if (targetEntity != null && targetEntity.isAlive() && !targetEntity.isRemoved()) {
            return targetEntity;
        }
        if (targetEntityUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(targetEntityUuid);
        if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            targetEntity = living;
            return living;
        }
        return null;
    }

    private static Vec3 entityCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    @Nullable
    private HitResult findHit(Vec3 end, Vec3 start) {
        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 1.0E-6D) {
            return null;
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level(), this, start, end,
            new AABB(start, end).inflate(0.75D),
            this::canHitEntity);
        if (entityHit != null) {
            return entityHit;
        }

        // Ribbons stick into whatever surface they touch.
        BlockHitResult blockHit = level().clip(new net.minecraft.world.level.ClipContext(
            start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            return blockHit;
        }
        return null;
    }

    private boolean canHitEntity(Entity entity) {
        if (entity == this || entity instanceof SilkRibbonEntity
            || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (casterUuid != null && casterUuid.equals(entity.getUUID())) {
            return false;
        }
        return entity instanceof LivingEntity;
    }

    private void onImpact(HitResult hit, Vec3 impactPos) {
        RibbonKind kind = getKind();

        if (!isRuiThread() && !isWhipMode() && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, impactPos.x, impactPos.y, impactPos.z,
                12, 0.25D, 0.25D, 0.25D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.CLOUD, impactPos.x, impactPos.y, impactPos.z,
                8, 0.2D, 0.2D, 0.2D, 0.01D);
            if (kind == RibbonKind.ACID) {
                serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, impactPos.x, impactPos.y, impactPos.z,
                    10, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            LivingEntity caster = resolveCaster();
            if (cocoonOnHit) {
                DissolutionCocoonEntity.tryCapture(living, caster);
            }

            DamageSource source = damageSources().mobAttack(caster != null ? caster : this);

            living.hurt(source, damage);
            living.knockback(kind == RibbonKind.ACID ? 0.25D : 0.4D,
                getX() - living.getX(), getZ() - living.getZ());

            if (liftOnHit) {
                living.fallDistance = 0.0F;
                Vec3 motion = living.getDeltaMovement();
                living.setDeltaMovement(motion.x, Math.max(0.9D, motion.y + 0.9D), motion.z);
                living.hasImpulse = true;
            }

            if (isSnatchMode()) {
                snatchTarget = living;
                snatchTicksRemaining = SNATCH_ATTACH_TICKS;
                long gameTime = level().getGameTime();
                if (living.getPersistentData().getLong(SNATCH_LAUNCH_TICK_KEY) != gameTime) {
                    living.getPersistentData().putLong(SNATCH_LAUNCH_TICK_KEY, gameTime);
                    MovementHelper.setVelocity(living, 0.0D, 2.7D, 0.0D);
                }
                recordSnatchTrail(living);
                return;
            }

            if (kind == RibbonKind.ACID) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 6, 1));
            }
        }
    }

    @Nullable
    private LivingEntity resolveCaster() {
        if (casterUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(casterUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void recordTrail(Vec3 point) {
        if (lastTrailPoint == null) {
            lastTrailPoint = point;
            return;
        }
        if (point.distanceTo(lastTrailPoint) < TRAIL_SPACING) {
            return;
        }
        lastTrailPoint = point;

        StringBuilder sb = new StringBuilder(this.entityData.get(TRAIL));
        int existingPoints = sb.length() == 0 ? 0 : sb.toString().split(";").length;
        while (existingPoints >= MAX_TRAIL_POINTS) {
            int firstSemicolon = sb.indexOf(";");
            sb.delete(0, firstSemicolon + 1);
            existingPoints--;
        }
        if (sb.length() > 0) {
            sb.append(';');
        }
        sb.append((float) point.x).append(',')
          .append((float) point.y).append(',')
          .append((float) point.z);
        this.entityData.set(TRAIL, sb.toString());
    }

    /**
     * Rebuild the left-click ribbon as a whip: a forward horizontal/vertical
     * line with its near end bent back to the caster's torso.
     */
    private void recordWhipTrail(Vec3 leadingEnd) {
        LivingEntity caster = resolveCaster();
        if (caster == null) {
            recordTrail(leadingEnd);
            return;
        }

        Vec3 anchor = RuiBloodDemonArt.getThreadAnchor(caster);
        Vec3 forward = leadingEnd.subtract(anchor);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = getDeltaMovement().lengthSqr() > 1.0E-4D
                ? getDeltaMovement().normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }

        Vec3 horizontalSide = new Vec3(-forward.z, 0.0D, forward.x);
        if (horizontalSide.lengthSqr() < 1.0E-4D) {
            horizontalSide = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horizontalSide = horizontalSide.normalize();
        }
        float angle = this.entityData.get(WHIP_ANGLE);
        Vec3 lineDirection = horizontalSide.scale(Math.cos(angle))
            .add(0.0D, Math.sin(angle), 0.0D);
        if (lineDirection.lengthSqr() < 1.0E-4D) {
            lineDirection = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            lineDirection = lineDirection.normalize();
        }

        double lengthScale = this.entityData.get(WHIP_LENGTH_SCALE);
        double maxLineLength = (this.entityData.get(WHIP_VERTICAL) ? 2.8D : 3.6D) * lengthScale;
        double lineLength = Math.min(maxLineLength,
            Math.max(0.35D, anchor.distanceTo(leadingEnd) * 0.18D * lengthScale));
        Vec3 offsetDirection = horizontalSide.scale(-Math.sin(angle))
            .add(0.0D, Math.cos(angle), 0.0D);
        if (offsetDirection.lengthSqr() < 1.0E-4D) {
            offsetDirection = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            offsetDirection = offsetDirection.normalize();
        }
        Vec3 lineOffset = offsetDirection.scale(this.entityData.get(WHIP_OFFSET));
        Vec3 nearEnd = leadingEnd.add(lineOffset).add(lineDirection.scale(lineLength));
        Vec3 control = anchor.add(forward.scale(Math.min(2.0D, forward.length() * 0.35D)))
            .add(lineOffset.scale(0.6D)).add(lineDirection.scale(lineLength * 1.15D));

        StringBuilder packed = new StringBuilder();
        for (int i = 0; i <= 6; i++) {
            double t = i / 6.0D;
            Vec3 point = quadraticBezier(anchor, control, nearEnd, t);
            appendPoint(packed, point);
        }
        for (int i = 1; i < 6; i++) {
            appendPoint(packed, nearEnd.add(lineDirection.scale(lineLength * i / 5.0D)));
        }
        this.entityData.set(TRAIL, packed.toString());
        this.lastTrailPoint = leadingEnd;
    }

    private static Vec3 quadraticBezier(Vec3 start, Vec3 control, Vec3 end, double progress) {
        double inverse = 1.0D - progress;
        return start.scale(inverse * inverse)
            .add(control.scale(2.0D * inverse * progress))
            .add(end.scale(progress * progress));
    }

    private static void appendPoint(StringBuilder packed, Vec3 point) {
        if (packed.length() > 0) {
            packed.append(';');
        }
        packed.append((float) point.x).append(',')
            .append((float) point.y).append(',')
            .append((float) point.z);
    }

    private void recordSnatchTrail(LivingEntity target) {
        LivingEntity caster = resolveCaster();
        if (caster == null) {
            return;
        }

        Vec3 anchor = RuiBloodDemonArt.getThreadAnchor(caster);
        Vec3 targetPoint = entityCenter(target).add(snatchOffset);
        Vec3 direction = targetPoint.subtract(anchor);
        double distance = direction.length();
        if (distance < 1.0E-4D) {
            return;
        }

        Vec3 control = anchor.add(direction.scale(0.52D))
            .add(0.0D, Math.min(5.0D, Math.max(1.8D, distance * 0.28D)), 0.0D);
        StringBuilder packed = new StringBuilder();
        for (int i = 0; i <= 12; i++) {
            appendPoint(packed, quadraticBezier(anchor, control, targetPoint, i / 12.0D));
        }
        this.entityData.set(TRAIL, packed.toString());
    }

    private void releaseSnatchTarget() {
        if (snareOnDetach && snatchTarget != null && snatchTarget.isAlive()) {
            snatchTarget.addEffect(new MobEffectInstance(
                ModEffects.SPIDER_SNARE.get(), 20 * 10, 0, false, false, true));
        }
        snatchTarget = null;
        snatchTicksRemaining = 0;
        beginFadeOut();
    }

    private void spawnEndParticles(Vec3 pos) {
        if (!isRuiThread() && !isWhipMode() && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 6, 0.2D, 0.2D, 0.2D, 0.01D);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // no-op: ribbons don't push entities
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksAlive = tag.getInt("TicksAlive");
        this.damage = tag.getFloat("Damage");
        this.cocoonOnHit = tag.getBoolean("CocoonOnHit");
        this.maxAgeTicks = tag.getInt("MaxAgeTicks");
        this.distanceTraveled = tag.getDouble("DistanceTraveled");
        if (tag.hasUUID("Caster")) {
            this.casterUuid = tag.getUUID("Caster");
        }
        if (tag.hasUUID("TargetEntity")) {
            this.targetEntityUuid = tag.getUUID("TargetEntity");
        }
        this.targetLocked = tag.getBoolean("TargetLocked");
        if (tag.contains("Kind")) {
            this.entityData.set(KIND, tag.getInt("Kind"));
        }
        if (tag.contains("TargetX")) {
            this.targetPoint = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        }
        if (tag.contains("AimX")) {
            this.smoothedAim = new Vec3(tag.getDouble("AimX"), tag.getDouble("AimY"), tag.getDouble("AimZ"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", ticksAlive);
        tag.putFloat("Damage", damage);
        tag.putBoolean("CocoonOnHit", cocoonOnHit);
        tag.putInt("MaxAgeTicks", maxAgeTicks);
        tag.putInt("Kind", getKind().id());
        tag.putDouble("DistanceTraveled", distanceTraveled);
        if (casterUuid != null) {
            tag.putUUID("Caster", casterUuid);
        }
        if (targetEntityUuid != null) {
            tag.putUUID("TargetEntity", targetEntityUuid);
        }
        tag.putBoolean("TargetLocked", targetLocked);
        if (targetPoint != null) {
            tag.putDouble("TargetX", targetPoint.x);
            tag.putDouble("TargetY", targetPoint.y);
            tag.putDouble("TargetZ", targetPoint.z);
        }
        if (smoothedAim != null) {
            tag.putDouble("AimX", smoothedAim.x);
            tag.putDouble("AimY", smoothedAim.y);
            tag.putDouble("AimZ", smoothedAim.z);
        }
    }
}
