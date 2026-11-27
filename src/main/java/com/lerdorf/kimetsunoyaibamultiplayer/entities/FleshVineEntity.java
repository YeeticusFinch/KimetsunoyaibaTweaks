package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-authoritative flesh vine used by Biokineses' Ultimate Authority. */
public class FleshVineEntity extends Entity {
    public static final int MODE_CHARGE = 0;
    public static final int MODE_SURGE = 1;
    public static final int MODE_TRANSITION = 2;
    public static final int REMOVAL_NONE = 0;
    public static final int REMOVAL_FADE = 1;
    public static final int REMOVAL_RETRACT = 2;

    public static final double DEFAULT_DIAMETER = 0.75D;
    public static final double SURGE_SPEED = 1.35D;
    public static final double SURGE_MAX_DISTANCE = 45.0D;
    public static final double SURGE_AMPLITUDE = 15.0D;
    public static final float SURGE_DAMAGE = 7.0F;
    public static final int SURGE_LIFETIME = 83;
    public static final int FADE_DURATION = 20;

    private static final double SURGE_WAVE_FREQUENCY = 0.44D;
    private static final double SURGE_STEM_LENGTH = 1.0D;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_MODE =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SEED =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_START_X =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Y =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Z =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_X =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Y =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Z =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP_X =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP_Y =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_UP_Z =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PROGRESS =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PHASE =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FADE_TICKS =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REMOVAL_MODE =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_HOMING_TARGET =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_HOMING_ENABLED =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> DATA_TRAJECTORY =
        SynchedEntityData.defineId(FleshVineEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private BlockPos lastImpactBlock;
    private List<Vec3> retractionPath;
    private Vec3 retractionAnchorOffset = Vec3.ZERO;
    private int retractionTick;

    public FleshVineEntity(EntityType<? extends FleshVineEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        setInvulnerable(true);
        setSilent(true);
        setNoGravity(true);
    }

    public void initializeCharge(Vec3 start, Vec3 end, LivingEntity owner, int seed,
                                 double diameter, int lifetime) {
        setMode(MODE_CHARGE);
        setOwner(owner == null ? null : owner.getUUID());
        setSeed(seed);
        setWidth((float) diameter);
        setLifetime(lifetime);
        setStart(start);
        setEnd(end);
        setProgress(0.0F);
        setPos(start.x, start.y, start.z);
        setOldPosAndRot();
    }

    public void initializeSurge(Vec3 start, Vec3 forward, Vec3 up, LivingEntity owner,
                                int seed, double diameter, double phase) {
        setMode(MODE_SURGE);
        setOwner(owner == null ? null : owner.getUUID());
        setSeed(seed);
        setWidth((float) diameter);
        setLifetime(SURGE_LIFETIME);
        setStart(start);
        setDirection(forward);
        setUp(up);
        setPhase((float) phase);
        setProgress(0.0F);
        setPos(start.x, start.y, start.z);
        setOldPosAndRot();
    }

    public void initializeTransition(Vec3 start, LivingEntity owner, int seed,
                                     double diameter, int lifetime) {
        setMode(MODE_TRANSITION);
        setOwner(owner == null ? null : owner.getUUID());
        setSeed(seed);
        setWidth((float) diameter);
        setLifetime(lifetime);
        setStart(start);
        setEnd(start);
        setTrajectory(List.of(start));
        setPos(start.x, start.y, start.z);
        setOldPosAndRot();
    }

    public void updateCharge(Vec3 start, Vec3 end) {
        if (getMode() != MODE_CHARGE) {
            return;
        }
        setStart(start);
        setEnd(end);
        setPos(start.x, start.y, start.z);
        setOldPosAndRot();
    }

    public void updateChargePath(Vec3 start, Vec3 end, List<Vec3> path) {
        if (getMode() != MODE_CHARGE) {
            return;
        }
        setStart(start);
        setEnd(end);
        setTrajectory(path);
        setPos(start.x, start.y, start.z);
        setOldPosAndRot();
    }

    public void updateTransition(Vec3 start, Vec3 end, List<Vec3> trajectory) {
        if (getMode() != MODE_TRANSITION) {
            return;
        }
        setStart(start);
        setEnd(end);
        setTrajectory(trajectory);
        setPos(start.x, start.y, start.z);
        setOldPosAndRot();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_OWNER, Optional.empty());
        entityData.define(DATA_MODE, MODE_CHARGE);
        entityData.define(DATA_SEED, 0);
        entityData.define(DATA_START_X, 0.0F);
        entityData.define(DATA_START_Y, 0.0F);
        entityData.define(DATA_START_Z, 0.0F);
        entityData.define(DATA_END_X, 0.0F);
        entityData.define(DATA_END_Y, 0.0F);
        entityData.define(DATA_END_Z, 0.0F);
        entityData.define(DATA_DIRECTION_X, 0.0F);
        entityData.define(DATA_DIRECTION_Y, 0.0F);
        entityData.define(DATA_DIRECTION_Z, 1.0F);
        entityData.define(DATA_UP_X, 0.0F);
        entityData.define(DATA_UP_Y, 1.0F);
        entityData.define(DATA_UP_Z, 0.0F);
        entityData.define(DATA_PROGRESS, 0.0F);
        entityData.define(DATA_PHASE, 0.0F);
        entityData.define(DATA_WIDTH, (float) DEFAULT_DIAMETER);
        entityData.define(DATA_LIFETIME, 1);
        entityData.define(DATA_FADE_TICKS, 0);
        entityData.define(DATA_REMOVAL_MODE, REMOVAL_NONE);
        entityData.define(DATA_HOMING_TARGET, Optional.empty());
        entityData.define(DATA_HOMING_ENABLED, false);
        entityData.define(DATA_TRAJECTORY, new CompoundTag());
    }

    @Override
    public void tick() {
        super.tick();
        if (getFadeTicks() > 0) {
            if (getRemovalMode() == REMOVAL_RETRACT) {
                if (retractionPath == null) {
                    retractionPath = captureRetractionPath();
                }
                tickRetraction();
            }
            if (getRemovalMode() != REMOVAL_RETRACT
                && !level().isClientSide() && level() instanceof ServerLevel serverLevel) {
                spawnFadeParticles(serverLevel);
            }
            int remaining = getFadeTicks() - 1;
            setFadeTicks(remaining);
            if (remaining <= 0) {
                discard();
            }
            return;
        }
        if (tickCount >= getLifetime()) {
            beginFade();
            return;
        }
        if (level().isClientSide() || getMode() != MODE_SURGE) {
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            beginFade();
            return;
        }
        LivingEntity owner = getOwner(serverLevel);
        if (owner == null || !owner.isAlive()) {
            beginFade();
            return;
        }

        Vec3 previous = isHoming() ? position() : getSurgeEndpoint();
        double nextProgress = getProgress() + SURGE_SPEED;
        setProgress((float) Math.min(SURGE_MAX_DISTANCE, nextProgress));
        Vec3 trajectoryEndpoint = getSurgeEndpoint();
        LivingEntity target = null;
        if (isHomingEnabled()) {
            target = getHomingTarget(serverLevel);
            if (target == null) {
                target = findHomingTarget(serverLevel, owner, trajectoryEndpoint);
                if (target != null) {
                    setHomingTarget(target);
                    previous = trajectoryEndpoint;
                }
            }
        }

        Vec3 next = trajectoryEndpoint;
        if (target != null) {
            if (previous.distanceToSqr(target.position()) > 225.0D) {
                clearHomingTarget();
            } else {
                Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                Vec3 toTarget = targetPoint.subtract(previous);
                double distance = toTarget.length();
                next = distance <= SURGE_SPEED ? targetPoint
                    : previous.add(toTarget.scale(SURGE_SPEED / distance));
            }
        }
        setPos(next.x, next.y, next.z);
        setOldPosAndRot();

        boolean hitTarget = false;
        if (tickCount % 2 == 0) {
            hitTarget = damageEndpoint(serverLevel, owner, next);
        }
        checkBlockImpact(serverLevel, owner, previous, next);
        if (hitTarget && isHoming()) {
            clearHomingTarget();
        }
    }

    private boolean damageEndpoint(ServerLevel level, LivingEntity owner, Vec3 endpoint) {
        double radius = getWidth();
        AABB damageBox = new AABB(endpoint.x - radius, endpoint.y - radius, endpoint.z - radius,
            endpoint.x + radius, endpoint.y + radius, endpoint.z + radius);
        boolean damaged = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, damageBox,
            candidate -> candidate.isAlive() && !candidate.isSpectator() && candidate != owner)) {
            if (Damager.hurt(owner, target, SURGE_DAMAGE, true)) {
                damaged = true;
                Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                level.sendParticles(ModParticles.BLACKRED_IMPACT.get(), impact.x, impact.y, impact.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        return damaged;
    }

    private LivingEntity findHomingTarget(ServerLevel level, LivingEntity owner, Vec3 endpoint) {
        AABB searchBox = new AABB(endpoint, endpoint).inflate(15.0D);
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchBox,
            entity -> entity != owner && entity.isAlive() && !entity.isSpectator()
                && endpoint.distanceToSqr(entity.position()) <= 225.0D
                && EnhancedLoveForms.isTargetable(owner, entity))) {
            double distance = endpoint.distanceToSqr(candidate.position());
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private void checkBlockImpact(ServerLevel level, LivingEntity owner, Vec3 previous, Vec3 next) {
        BlockHitResult hit = level.clip(new ClipContext(previous, next,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        BlockPos block = hit.getType() == HitResult.Type.BLOCK
            ? hit.getBlockPos() : BlockPos.containing(next);
        if (!level.getBlockState(block).isSolidRender(level, block)) {
            return;
        }
        if (!block.equals(lastImpactBlock)) {
            lastImpactBlock = block;
            level.sendParticles(ParticleTypes.EXPLOSION, next.x, next.y, next.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, next.x, next.y, next.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 1.0F, 0.75F);
        }
    }

    private void spawnFadeParticles(ServerLevel level) {
        int samples = getMode() == MODE_SURGE ? 20 : 8;
        for (int index = 0; index <= samples; index++) {
            double progress = index / (double) samples;
            Vec3 point = getMode() == MODE_SURGE
                ? getSurgePoint(getProgress() * progress)
                : getStart().lerp(getEnd(), progress);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, point.x, point.y, point.z,
                1, 0.06D, 0.06D, 0.06D, 0.0D);
        }
    }

    public Vec3 getStart() {
        return new Vec3(entityData.get(DATA_START_X), entityData.get(DATA_START_Y), entityData.get(DATA_START_Z));
    }

    public Vec3 getEnd() {
        return new Vec3(entityData.get(DATA_END_X), entityData.get(DATA_END_Y), entityData.get(DATA_END_Z));
    }

    public List<Vec3> getTransitionPath() {
        List<Vec3> path = new ArrayList<>();
        ListTag points = entityData.get(DATA_TRAJECTORY).getList("Points", 10);
        for (int index = 0; index < points.size(); index++) {
            CompoundTag point = points.getCompound(index);
            path.add(new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z")));
        }
        return path;
    }

    public List<Vec3> getChargePath() {
        return getTransitionPath();
    }

    public Vec3 getVineDirection() {
        return new Vec3(entityData.get(DATA_DIRECTION_X), entityData.get(DATA_DIRECTION_Y),
            entityData.get(DATA_DIRECTION_Z)).normalize();
    }

    public Vec3 getUp() {
        return new Vec3(entityData.get(DATA_UP_X), entityData.get(DATA_UP_Y),
            entityData.get(DATA_UP_Z)).normalize();
    }

    public Vec3 getSurgePoint(double distance) {
        Vec3 start = getStart();
        Vec3 forward = getVineDirection();
        Vec3 up = getUp();
        double stem = Math.min(SURGE_STEM_LENGTH, distance);
        double waveDistance = Math.max(0.0D, distance - SURGE_STEM_LENGTH);
        double phase = getPhase();
        double wave = waveDistance <= 0.0D ? 0.0D
            : SURGE_AMPLITUDE * 0.5D * (Math.cos(Math.PI * 0.5D + waveDistance * SURGE_WAVE_FREQUENCY + phase)
                - Math.cos(Math.PI * 0.5D + phase));
        return distance <= SURGE_STEM_LENGTH
            ? start.add(up.scale(distance))
            : start.add(up.scale(stem + wave)).add(forward.scale(waveDistance));
    }

    public Vec3 getSurgeEndpoint() {
        return getSurgePoint(getProgress());
    }

    public int getMode() {
        return entityData.get(DATA_MODE);
    }

    public int getSeed() {
        return entityData.get(DATA_SEED);
    }

    public float getProgress() {
        return entityData.get(DATA_PROGRESS);
    }

    public float getPhase() {
        return entityData.get(DATA_PHASE);
    }

    public float getWidth() {
        return entityData.get(DATA_WIDTH);
    }

    public int getLifetime() {
        return entityData.get(DATA_LIFETIME);
    }

    public int getFadeTicks() {
        return entityData.get(DATA_FADE_TICKS);
    }

    public int getRemovalMode() {
        return entityData.get(DATA_REMOVAL_MODE);
    }

    public float getFadeAlpha() {
        return getFadeTicks() <= 0 ? 1.0F : getFadeTicks() / (float) FADE_DURATION;
    }

    public boolean isHoming() {
        return entityData.get(DATA_HOMING_TARGET).isPresent();
    }

    public boolean isHomingEnabled() {
        return entityData.get(DATA_HOMING_ENABLED);
    }

    public void beginFade() {
        if (getFadeTicks() > 0) {
            return;
        }
        setRemovalMode(REMOVAL_FADE);
        setFadeTicks(FADE_DURATION);
    }

    /** Fades while shrinking the current path back to its owner-side anchor. */
    public void beginRetract() {
        if (getRemovalMode() == REMOVAL_RETRACT) {
            return;
        }
        retractionPath = captureRetractionPath();
        if (retractionPath.size() < 2) {
            beginFade();
            return;
        }

        Vec3 anchor = retractionPath.get(0);
        LivingEntity owner = getOwnerIfPresent();
        if (owner != null) {
            retractionAnchorOffset = anchor.subtract(owner.position());
        }
        retractionTick = 0;
        setRemovalMode(REMOVAL_RETRACT);
        setTrajectory(retractionPath);
        setEnd(retractionPath.get(retractionPath.size() - 1));
        setFadeTicks(FADE_DURATION);
    }

    public void setHomingEnabled(boolean enabled) {
        entityData.set(DATA_HOMING_ENABLED, enabled);
        if (!enabled) {
            clearHomingTarget();
        }
    }

    private void setHomingTarget(LivingEntity target) {
        entityData.set(DATA_HOMING_TARGET, Optional.of(target.getUUID()));
    }

    private void clearHomingTarget() {
        entityData.set(DATA_HOMING_TARGET, Optional.empty());
    }

    private LivingEntity getHomingTarget(ServerLevel level) {
        UUID targetId = entityData.get(DATA_HOMING_TARGET).orElse(null);
        if (targetId == null || !(level.getEntity(targetId) instanceof LivingEntity target)
            || !target.isAlive() || target.isSpectator()) {
            if (targetId != null) {
                clearHomingTarget();
            }
            return null;
        }
        return target;
    }

    public void setMode(int mode) {
        entityData.set(DATA_MODE, mode);
    }

    public void setOwner(UUID owner) {
        entityData.set(DATA_OWNER, Optional.ofNullable(owner));
    }

    public void setSeed(int seed) {
        entityData.set(DATA_SEED, seed);
    }

    public void setStart(Vec3 start) {
        entityData.set(DATA_START_X, (float) start.x);
        entityData.set(DATA_START_Y, (float) start.y);
        entityData.set(DATA_START_Z, (float) start.z);
    }

    public void setEnd(Vec3 end) {
        entityData.set(DATA_END_X, (float) end.x);
        entityData.set(DATA_END_Y, (float) end.y);
        entityData.set(DATA_END_Z, (float) end.z);
    }

    private void setTrajectory(List<Vec3> trajectory) {
        CompoundTag data = new CompoundTag();
        ListTag points = new ListTag();
        for (Vec3 point : trajectory) {
            CompoundTag encoded = new CompoundTag();
            encoded.putDouble("X", point.x);
            encoded.putDouble("Y", point.y);
            encoded.putDouble("Z", point.z);
            points.add(encoded);
        }
        data.put("Points", points);
        entityData.set(DATA_TRAJECTORY, data);
    }

    public void setDirection(Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        entityData.set(DATA_DIRECTION_X, (float) normalized.x);
        entityData.set(DATA_DIRECTION_Y, (float) normalized.y);
        entityData.set(DATA_DIRECTION_Z, (float) normalized.z);
    }

    public void setUp(Vec3 up) {
        Vec3 normalized = up.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 1.0D, 0.0D) : up.normalize();
        entityData.set(DATA_UP_X, (float) normalized.x);
        entityData.set(DATA_UP_Y, (float) normalized.y);
        entityData.set(DATA_UP_Z, (float) normalized.z);
    }

    public void setProgress(float progress) {
        entityData.set(DATA_PROGRESS, Math.max(0.0F, progress));
    }

    public void setPhase(float phase) {
        entityData.set(DATA_PHASE, phase);
    }

    public void setWidth(float width) {
        entityData.set(DATA_WIDTH, Math.max(0.05F, width));
    }

    public void setLifetime(int lifetime) {
        entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
    }

    private void setRemovalMode(int mode) {
        entityData.set(DATA_REMOVAL_MODE, mode);
    }

    private void setFadeTicks(int ticks) {
        entityData.set(DATA_FADE_TICKS, Math.max(0, ticks));
    }

    private List<Vec3> captureRetractionPath() {
        List<Vec3> path;
        if (getMode() == MODE_SURGE) {
            int count = Mth.clamp((int) Math.ceil(Math.max(1.0D, getProgress()) * 2.0D), 4, 32);
            path = new ArrayList<>(count + 1);
            for (int index = 0; index <= count; index++) {
                path.add(getSurgePoint(getProgress() * index / count));
            }
        } else {
            path = getTransitionPath();
        }
        if (path.size() < 2) {
            path = new ArrayList<>(List.of(getStart(), getEnd()));
        } else {
            path = new ArrayList<>(path);
            path.set(0, getStart());
        }
        return path;
    }

    private void tickRetraction() {
        if (retractionPath == null || retractionPath.size() < 2) {
            return;
        }

        double progress = (FADE_DURATION - getFadeTicks() + 1.0D) / FADE_DURATION;
        double remaining = Math.max(0.0D, 1.0D - progress);
        Vec3 originalAnchor = retractionPath.get(0);
        Vec3 anchor = originalAnchor;
        LivingEntity owner = getOwnerIfPresent();
        if (owner != null) {
            anchor = owner.position().add(retractionAnchorOffset);
        }

        List<Vec3> path = new ArrayList<>(retractionPath.size());
        path.add(anchor);
        for (int index = 1; index < retractionPath.size(); index++) {
            Vec3 offset = retractionPath.get(index).subtract(originalAnchor);
            Vec3 point = anchor.add(offset.scale(remaining));
            if (offset.lengthSqr() > 1.0E-8D && remaining > 0.0D) {
                Vec3 direction = offset.normalize();
                Vec3 reference = Math.abs(direction.y) < 0.9D
                    ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
                Vec3 side = direction.cross(reference);
                if (side.lengthSqr() > 1.0E-8D) {
                    double ratio = index / (double) (retractionPath.size() - 1);
                    double amplitude = Math.min(0.45D, offset.length() * 0.08D)
                        * Math.sin(ratio * Math.PI) * remaining;
                    double phase = getSeed() * 0.071D + retractionTick * 0.85D + index * 1.37D;
                    point = point.add(side.normalize().scale(Math.sin(phase) * amplitude));
                }
            }
            path.add(point);
        }
        setStart(anchor);
        setEnd(path.get(path.size() - 1));
        setTrajectory(path);
        setPos(anchor.x, anchor.y, anchor.z);
        setOldPosAndRot();
        retractionTick++;
    }

    private LivingEntity getOwnerIfPresent() {
        UUID ownerId = entityData.get(DATA_OWNER).orElse(null);
        return ownerId == null || !(level().getEntity(ownerId) instanceof LivingEntity owner)
            ? null : owner;
    }

    private LivingEntity getOwner(ServerLevel level) {
        UUID ownerId = entityData.get(DATA_OWNER).orElse(null);
        return ownerId == null ? null : level.getEntity(ownerId) instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", getMode());
        tag.putInt("Seed", getSeed());
        tag.putFloat("Progress", getProgress());
        tag.putFloat("Phase", getPhase());
        tag.putFloat("Width", getWidth());
        tag.putInt("Lifetime", getLifetime());
        UUID owner = entityData.get(DATA_OWNER).orElse(null);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        Vec3 start = getStart();
        Vec3 end = getEnd();
        Vec3 direction = getVineDirection();
        Vec3 up = getUp();
        tag.putDouble("StartX", start.x);
        tag.putDouble("StartY", start.y);
        tag.putDouble("StartZ", start.z);
        tag.putDouble("EndX", end.x);
        tag.putDouble("EndY", end.y);
        tag.putDouble("EndZ", end.z);
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionY", direction.y);
        tag.putDouble("DirectionZ", direction.z);
        tag.putDouble("UpX", up.x);
        tag.putDouble("UpY", up.y);
        tag.putDouble("UpZ", up.z);
        tag.put("Trajectory", entityData.get(DATA_TRAJECTORY).copy());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setMode(tag.getInt("Mode"));
        setSeed(tag.getInt("Seed"));
        setProgress(tag.getFloat("Progress"));
        setPhase(tag.getFloat("Phase"));
        setWidth(tag.getFloat("Width"));
        setLifetime(tag.getInt("Lifetime"));
        if (tag.hasUUID("Owner")) {
            setOwner(tag.getUUID("Owner"));
        }
        setStart(new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ")));
        setEnd(new Vec3(tag.getDouble("EndX"), tag.getDouble("EndY"), tag.getDouble("EndZ")));
        setDirection(new Vec3(tag.getDouble("DirectionX"), tag.getDouble("DirectionY"), tag.getDouble("DirectionZ")));
        setUp(new Vec3(tag.getDouble("UpX"), tag.getDouble("UpY"), tag.getDouble("UpZ")));
        if (tag.contains("Trajectory", 10)) {
            entityData.set(DATA_TRAJECTORY, tag.getCompound("Trajectory"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D;
    }
}
