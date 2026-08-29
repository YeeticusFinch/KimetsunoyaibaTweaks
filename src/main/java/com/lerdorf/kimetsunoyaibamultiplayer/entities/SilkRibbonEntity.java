package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
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

    // Synced trail so late-loading clients can still draw the ribbon.
    // Stored as packed doubles: [x0,y0,z0, x1,y1,z1, ...]
    private static final EntityDataAccessor<String> TRAIL =
        SynchedEntityData.defineId(SilkRibbonEntity.class, EntityDataSerializers.STRING);

    /** Trail point spacing in blocks. */
    private static final double TRAIL_SPACING = 0.35D;
    private static final int MAX_TRAIL_POINTS = 160; // ~56 blocks of ribbon

    private float damage = 6.0F;
    private boolean cocoonOnHit = false;
    @Nullable
    private java.util.UUID casterUuid = null;
    @Nullable
    private Vec3 targetPoint = null;

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

    /** Erratic-but-smooth lateral wobble seeds. */
    private double wobbleAmpXZ = 0.9D;
    private double wobbleFreq = 0.55D;
    private double wobblePhaseA = 0.0D;
    private double wobblePhaseB = 0.0D;

    /** Low-pass filtered aim direction; prevents heading snaps (visible shake). */
    private Vec3 smoothedAim = null;

    private Vec3 lastTrailPoint = null;

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
                                         RibbonKind kind, float damage, boolean cocoonOnHit) {
        SilkRibbonEntity ribbon = new SilkRibbonEntity(ModEntities.SILK_RIBBON.get(), level);
        ribbon.setPos(start.x, start.y, start.z);
        ribbon.casterUuid = caster.getUUID();
        ribbon.targetPoint = targetPoint;
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
        return ribbon;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(KIND, RibbonKind.SILK.id());
        this.entityData.define(ACID_DARK_GREEN, false);
        this.entityData.define(TRAIL, "");
    }

    public boolean isAcidDarkGreen() {
        return this.entityData.get(ACID_DARK_GREEN);
    }

    public void setAcidDarkGreen(boolean darkGreen) {
        this.entityData.set(ACID_DARK_GREEN, darkGreen);
    }

    public RibbonKind getKind() {
        return RibbonKind.fromId(this.entityData.get(KIND));
    }

    public boolean isCocoonShot() {
        return cocoonOnHit;
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

        // After impact: linger briefly so the trail fades out, then discard.
        if (fadingOut) {
            setDeltaMovement(Vec3.ZERO);
            fadeOutTicksRemaining--;
            if (fadeOutTicksRemaining <= 0) {
                discard();
            }
            return;
        }

        // Desired direction: toward the target point (re-aimed each tick so it
        // tracks crosshair targets), plus a smooth sinusoidal wobble for the
        // erratic silk feel. The aim direction is low-pass filtered so the
        // handoff between "homing" and "coasting" (once past the target) does
        // NOT snap the heading - that snap was the source of the visible shake.
        Vec3 pos = position();
        Vec3 rawAim = targetPoint != null && !hasPassedTarget(pos)
            ? targetPoint.subtract(pos).normalize()
            : (getDeltaMovement().lengthSqr() > 1.0E-4D ? getDeltaMovement().normalize() : new Vec3(0, 0, 1));
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

        double speed = 1.84D; // 60% faster so the ribbon reaches its target quickly
        Vec3 velocity = smoothedAim.add(wobble.normalize().scale(Math.min(amp, 0.45D))).normalize().scale(speed);
        setDeltaMovement(velocity);

        Vec3 newPos = pos.add(velocity);
        distanceTraveled += newPos.distanceTo(pos);
        setPos(newPos.x, newPos.y, newPos.z);

        recordTrail(newPos);

        // Entity hit detection along this tick's segment.
        HitResult hit = findHit(newPos, pos);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            onImpact(hit, newPos);
            // AOE splash where it lands, then linger for the fade-out.
            applySplashDamage(newPos);
            beginFadeOut();
            return;
        }

        if (distanceTraveled >= maxTravelBlocks || ticksAlive >= maxAgeTicks) {
            spawnEndParticles(newPos);
            applySplashDamage(newPos);
            beginFadeOut();
        }
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
        if (entity == this || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (casterUuid != null && casterUuid.equals(entity.getUUID())) {
            return false;
        }
        return entity instanceof LivingEntity;
    }

    private void onImpact(HitResult hit, Vec3 impactPos) {
        RibbonKind kind = getKind();

        if (level() instanceof ServerLevel serverLevel) {
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

    private void spawnEndParticles(Vec3 pos) {
        if (level() instanceof ServerLevel serverLevel) {
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
