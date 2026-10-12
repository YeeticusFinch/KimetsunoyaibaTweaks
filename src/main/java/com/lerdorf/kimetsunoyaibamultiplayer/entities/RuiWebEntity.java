package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRank;
import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRankManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/** Server-authoritative persistent web shape used by Rui's large techniques. */
public class RuiWebEntity extends Entity {
    public enum Pattern {
        DICING_WALL,
        CUTTING_CAGE,
        CONTORTING,
        EYE_BASKET,
        ROTATION,
        EYE_BASKET_PULSE,
        DECAPITATION_CIRCLE
    }

    private static final EntityDataAccessor<Integer> PATTERN =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VISUAL_SCALE =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RED_THREADS =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFETIME =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> TARGET =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> EYE_RADIUS =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_PROGRESS =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_PITCH =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_ROLL =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_YAW =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_ANGLE =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CAGE_WIDTH =
        SynchedEntityData.defineId(RuiWebEntity.class, EntityDataSerializers.FLOAT);

    private static final double ROTATION_SPEED = 1.0D;
    private static final float ROTATION_STEP = 0.31415927F;
    private static final double EDGE_LAUNCH_SPEED = 3.0D;

    private UUID ownerUuid;
    private Vec3 direction = Vec3.ZERO;
    private float webHealth = 30.0F;
    private int lastDamageTick = -20;
    private final java.util.Set<UUID> eyeBasketHitTargets = new java.util.HashSet<>();
    private final java.util.Set<UUID> cageHitTargets = new java.util.HashSet<>();

    public RuiWebEntity(EntityType<? extends RuiWebEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setSilent(true);
        this.noCulling = true;
    }

    public static RuiWebEntity create(Level level, LivingEntity owner, LivingEntity target,
                                      Pattern pattern, boolean redThreads, float damage,
                                      int lifetimeTicks) {
        RuiWebEntity web = new RuiWebEntity(ModEntities.RUI_WEB.get(), level);
        web.ownerUuid = owner.getUUID();
        web.setOwnerUuid(owner.getUUID());
        web.setPattern(pattern);
        web.setRedThreads(redThreads);
        web.setLifetime(lifetimeTicks);
        web.webHealth = pattern == Pattern.DICING_WALL
            ? 40.0F
            : Math.max(12.0F, damage * 5.0F);
        web.setTarget(target);

        Vec3 origin = pattern == Pattern.ROTATION
            ? owner.getEyePosition()
            : target != null && pattern != Pattern.DICING_WALL
                ? target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                : owner.position().add(0.0D, owner.getEyeHeight() * 0.7D, 0.0D);
        web.setPos(origin.x, origin.y, origin.z);
        if (pattern == Pattern.ROTATION) {
            web.direction = owner.getLookAngle().normalize();
            web.setRotationFromDirection(web.direction);
        } else {
            web.direction = new Vec3(-Math.sin(Math.toRadians(owner.getYRot())), 0.0D,
                Math.cos(Math.toRadians(owner.getYRot()))).normalize();
            web.setYRot(owner.getYRot());
            web.yRotO = owner.getYRot();
        }
        web.setVisualScale(pattern == Pattern.DICING_WALL || pattern == Pattern.ROTATION ? 1.0F : 0.15F);
        return web;
    }

    public static RuiWebEntity createEyeBasket(Level level, LivingEntity owner, Vec3 center,
                                               boolean redThreads, float damage) {
        RuiWebEntity web = new RuiWebEntity(ModEntities.RUI_WEB.get(), level);
        web.ownerUuid = owner.getUUID();
        web.setOwnerUuid(owner.getUUID());
        web.setPattern(Pattern.EYE_BASKET);
        web.setRedThreads(redThreads);
        web.setLifetime(70);
        web.webHealth = Math.max(12.0F, damage * 5.0F);
        web.setPos(center.x, center.y, center.z);
        web.setEyeRadius(15.0F);
        web.setEyeProgress(0.0F);
        return web;
    }

    public static RuiWebEntity createCage(Level level, LivingEntity owner, Vec3 center,
                                          boolean redThreads, float damage) {
        RuiWebEntity web = new RuiWebEntity(ModEntities.RUI_WEB.get(), level);
        web.ownerUuid = owner.getUUID();
        web.setOwnerUuid(owner.getUUID());
        web.setPattern(Pattern.CUTTING_CAGE);
        web.setRedThreads(redThreads);
        web.setLifetime(30);
        web.webHealth = 40.0F;
        web.setPos(center.x, center.y, center.z);
        web.setYRot(owner.getYRot());
        web.yRotO = owner.getYRot();
        web.setCageWidth(15.0F);
        web.updateCageBoundingBox();
        return web;
    }

    private static RuiWebEntity createEyeBasketPulse(Level level, LivingEntity owner, Vec3 center,
                                                       float radius, boolean redThreads, float damage) {
        RuiWebEntity pulse = new RuiWebEntity(ModEntities.RUI_WEB.get(), level);
        pulse.ownerUuid = owner.getUUID();
        pulse.setOwnerUuid(owner.getUUID());
        pulse.setPattern(Pattern.EYE_BASKET_PULSE);
        pulse.setRedThreads(redThreads);
        pulse.setLifetime(8);
        pulse.webHealth = Math.max(12.0F, damage * 5.0F);
        pulse.setPos(center.x, center.y, center.z);
        pulse.setEyeRadius(radius);
        pulse.setEyeProgress(0.0F);
        pulse.setEyeRotation(level.random.nextFloat() * 360.0F,
            level.random.nextFloat() * 360.0F, level.random.nextFloat() * 360.0F);
        return pulse;
    }

    public static RuiWebEntity createDecapitationCircle(Level level, LivingEntity owner) {
        RuiWebEntity circle = new RuiWebEntity(ModEntities.RUI_WEB.get(), level);
        circle.ownerUuid = owner.getUUID();
        circle.setOwnerUuid(owner.getUUID());
        circle.setTarget(owner);
        circle.setPattern(Pattern.DECAPITATION_CIRCLE);
        circle.setRedThreads(false);
        circle.setLifetime(5);
        circle.webHealth = 0.0F;
        circle.setEyeRadius(1.0F);
        circle.setEyeProgress(1.0F);
        circle.setPos(owner.getX(), owner.getEyeY() - 0.06D, owner.getZ());
        return circle;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PATTERN, Pattern.DICING_WALL.ordinal());
        this.entityData.define(VISUAL_SCALE, 1.0F);
        this.entityData.define(RED_THREADS, false);
        this.entityData.define(LIFETIME, 60);
        this.entityData.define(TARGET, Optional.empty());
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(EYE_RADIUS, 0.0F);
        this.entityData.define(EYE_PROGRESS, 1.0F);
        this.entityData.define(EYE_PITCH, 0.0F);
        this.entityData.define(EYE_ROLL, 0.0F);
        this.entityData.define(EYE_YAW, 0.0F);
        this.entityData.define(ROTATION_ANGLE, 0.0F);
        this.entityData.define(CAGE_WIDTH, 0.0F);
    }

    public Pattern getPattern() {
        int index = Math.max(0, Math.min(Pattern.values().length - 1, this.entityData.get(PATTERN)));
        return Pattern.values()[index];
    }

    public void setPattern(Pattern pattern) {
        this.entityData.set(PATTERN, pattern.ordinal());
    }

    public float getVisualScale() {
        return this.entityData.get(VISUAL_SCALE);
    }

    public void setVisualScale(float scale) {
        this.entityData.set(VISUAL_SCALE, Math.max(0.0F, scale));
    }

    public boolean isRedThreads() {
        return this.entityData.get(RED_THREADS);
    }

    public void setRedThreads(boolean redThreads) {
        this.entityData.set(RED_THREADS, redThreads);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, Math.max(1, lifetime));
    }

    public float getEyeRadius() {
        return this.entityData.get(EYE_RADIUS);
    }

    private void setEyeRadius(float radius) {
        this.entityData.set(EYE_RADIUS, Math.max(0.0F, radius));
    }

    public float getEyeProgress() {
        return this.entityData.get(EYE_PROGRESS);
    }

    private void setEyeProgress(float progress) {
        this.entityData.set(EYE_PROGRESS, Math.max(0.0F, Math.min(1.0F, progress)));
    }

    public float getEyePitch() {
        return this.entityData.get(EYE_PITCH);
    }

    public float getEyeRoll() {
        return this.entityData.get(EYE_ROLL);
    }

    public float getEyeYaw() {
        return this.entityData.get(EYE_YAW);
    }

    private void setEyeRotation(float pitch, float roll, float yaw) {
        this.entityData.set(EYE_PITCH, pitch);
        this.entityData.set(EYE_ROLL, roll);
        this.entityData.set(EYE_YAW, yaw);
    }

    public float getRotationAngle() {
        return this.entityData.get(ROTATION_ANGLE);
    }

    private void setRotationAngle(float angle) {
        this.entityData.set(ROTATION_ANGLE, angle);
    }

    public float getCageWidth() {
        return this.entityData.get(CAGE_WIDTH);
    }

    private void setCageWidth(float width) {
        this.entityData.set(CAGE_WIDTH, Math.max(0.0F, width));
    }

    public double getRotationZOffset(int circleIndex) {
        java.util.Random random = new java.util.Random(
            0x6A09E667L ^ (long) getId() * 0x9E3779B9L ^ (long) circleIndex * 0xC2B2AE3DL);
        return (random.nextDouble() * 2.0D - 1.0D) * 0.1D;
    }

    private void setRotationFromDirection(Vec3 look) {
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(Math.asin(-Math.max(-1.0D, Math.min(1.0D, look.y))));
        setYRot(yaw);
        yRotO = yaw;
        setXRot(pitch);
        xRotO = pitch;
    }

    private UUID getTargetUuid() {
        return this.entityData.get(TARGET).orElse(null);
    }

    public void setTarget(LivingEntity target) {
        this.entityData.set(TARGET, Optional.ofNullable(target == null ? null : target.getUUID()));
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.entityData.set(OWNER, Optional.ofNullable(ownerUuid));
    }

    public UUID getOwnerUuid() {
        return this.entityData.get(OWNER).orElse(ownerUuid);
    }

    private boolean isMovingWall() {
        return getPattern() == Pattern.DICING_WALL || getPattern() == Pattern.ROTATION;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        Pattern pattern = getPattern();
        if (pattern == Pattern.DECAPITATION_CIRCLE) {
            tickDecapitationCircle();
            return;
        }
        if (pattern == Pattern.EYE_BASKET || pattern == Pattern.EYE_BASKET_PULSE) {
            tickEyeBasket(pattern);
            return;
        }
        if (pattern == Pattern.CUTTING_CAGE) {
            tickCage();
            return;
        }

        LivingEntity target = resolveTarget();
        if (!isMovingWall()) {
            if (target == null || !target.isAlive()) {
                discard();
                return;
            }
            setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        } else {
            double speed = pattern == Pattern.DICING_WALL ? 0.9D
                : pattern == Pattern.ROTATION ? ROTATION_SPEED : 0.45D;
            if (target != null && target.isAlive()) {
                Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                    .subtract(position());
                if (toTarget.lengthSqr() > 1.0D) {
                    direction = toTarget.normalize();
                    setYRot((float) (Math.toDegrees(Math.atan2(-direction.x, direction.z))));
                } else {
                    speed = 0.0D;
                }
            }
            if (pattern == Pattern.ROTATION) {
                setRotationAngle(getRotationAngle() + ROTATION_STEP);
            }
            setPos(getX() + direction.x * speed, getY() + direction.y * speed,
                getZ() + direction.z * speed);
        }

        float progress = Math.min(1.0F, tickCount / 12.0F);
        if (!isMovingWall()) {
            setVisualScale(pattern == Pattern.CONTORTING
                || pattern == Pattern.CUTTING_CAGE || pattern == Pattern.EYE_BASKET
                ? Math.max(0.05F, 1.0F - progress)
                : Math.min(1.0F, progress));
        }

        if (tickCount - lastDamageTick >= 8) {
            if (isMovingWall()) {
                if (pattern == Pattern.ROTATION) {
                    damageRotationCircles();
                } else {
                    damageNearby();
                }
            } else if (target != null && target.isAlive() && intersectsTarget(target)
                && Damager.hurt(resolveOwner(), target, webHealth / 5.0F, true)) {
                lastDamageTick = tickCount;
            }
        }

        if (tickCount >= getLifetime()) {
            discard();
        }
    }

    private void tickDecapitationCircle() {
        LivingEntity owner = resolveOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        setPos(owner.getX(), owner.getEyeY() - 0.06D, owner.getZ());
        setEyeRadius(Math.max(0.0F, 1.0F * (1.0F - tickCount / 5.0F)));
        if (tickCount >= 5) {
            discard();
        }
    }

    private void tickEyeBasket(Pattern pattern) {
        if (pattern == Pattern.EYE_BASKET_PULSE) {
            if (tickCount >= getLifetime()) {
                discard();
            }
            return;
        }

        if (tickCount < 10) {
            setEyeRadius(15.0F);
            setEyeProgress((tickCount + 1) / 10.0F);
        } else {
            int shrinkTick = tickCount - 9;
            float radius = Math.max(0.0F, 15.0F * (1.0F - shrinkTick / 60.0F));
            setEyeRadius(radius);
            setEyeProgress(1.0F);

            if (tickCount < 70) {
                LivingEntity owner = resolveOwner();
                if (owner != null && owner.isAlive()) {
                    for (int pulseIndex = 0; pulseIndex < 2; pulseIndex++) {
                        RuiWebEntity pulse = createEyeBasketPulse(level(), owner, position(), radius,
                            isRedThreads(), webHealth / 5.0F);
                        level().addFreshEntity(pulse);
                    }
                }
            }
        }

        containEyeBasketEntities(getEyeRadius());
        damageEyeBasketSphere(getEyeRadius());
        if (tickCount >= 70) {
            discard();
        }
    }

    private void tickCage() {
        float width = Math.max(0.0F, 15.0F * (1.0F - tickCount / 30.0F));
        setCageWidth(width);
        updateCageBoundingBox();

        if (tickCount < 30) {
            containCageEntities(width);
            damageCageWalls(width);
        } else {
            damageCageCenter();
            discard();
        }
    }

    private void damageCageWalls(float width) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = resolveOwner();
        if (owner == null) {
            return;
        }

        double half = width * 0.5D;
        double searchRadius = Math.max(2.0D, half + 1.5D);
        Vec3 right = cageRight();
        Vec3 forward = cageForward();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(position(), position()).inflate(searchRadius),
            entity -> entity.isAlive() && entity != owner && !cageHitTargets.contains(entity.getUUID()))) {
            Vec3 relative = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D)
                .subtract(position());
            double localX = relative.dot(right);
            double localY = relative.y;
            double localZ = relative.dot(forward);
            double tolerance = Math.max(0.75D, victim.getBbWidth() * 0.5D + 0.25D);
            boolean insideOtherAxes = Math.abs(localX) <= half + tolerance
                && Math.abs(localY) <= half + tolerance
                && Math.abs(localZ) <= half + tolerance;
            boolean touchesWall = Math.abs(Math.abs(localX) - half) <= tolerance
                || Math.abs(Math.abs(localY) - half) <= tolerance
                || Math.abs(Math.abs(localZ) - half) <= tolerance;
            if (insideOtherAxes && touchesWall) {
                launchTowardCenter(victim);
                if (Damager.hurt(owner, victim, webHealth / 5.0F, true)) {
                    cageHitTargets.add(victim.getUUID());
                }
            }
        }
    }

    private void containCageEntities(float width) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = resolveOwner();
        if (owner == null) {
            return;
        }

        double half = Math.max(0.1D, width * 0.5D);
        double searchRadius = Math.max(2.0D, half + 2.0D);
        Vec3 right = cageRight();
        Vec3 forward = cageForward();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(position(), position()).inflate(searchRadius),
            entity -> entity.isAlive() && entity != owner)) {
            Vec3 relative = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D)
                .subtract(position());
            double localX = relative.dot(right);
            double localY = relative.y;
            double localZ = relative.dot(forward);
            double tolerance = Math.max(0.75D, victim.getBbWidth() * 0.5D + 0.25D);
            if (Math.max(Math.abs(localX), Math.max(Math.abs(localY), Math.abs(localZ)))
                < half - tolerance) {
                continue;
            }

            launchTowardCenter(victim);
            double allowed = Math.max(0.0D, half - tolerance);
            double clampedX = Math.max(-allowed, Math.min(allowed, localX));
            double clampedY = Math.max(-allowed, Math.min(allowed, localY));
            double clampedZ = Math.max(-allowed, Math.min(allowed, localZ));
            Vec3 clampedCenter = position().add(right.scale(clampedX))
                .add(0.0D, clampedY, 0.0D).add(forward.scale(clampedZ));
            victim.setPos(clampedCenter.x, clampedCenter.y - victim.getBbHeight() * 0.5D,
                clampedCenter.z);
            victim.fallDistance = 0.0F;
        }
    }

    private void damageCageCenter() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = resolveOwner();
        if (owner == null) {
            return;
        }
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(position(), position()).inflate(2.0D),
            entity -> entity.isAlive() && entity != owner)) {
            if (victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D)
                .distanceTo(position()) <= 2.0D) {
                launchTowardCenter(victim);
                Damager.hurt(owner, victim, webHealth / 5.0F, true);
            }
        }
    }

    private void launchTowardCenter(LivingEntity victim) {
        Vec3 towardCenter = position().subtract(victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D));
        if (towardCenter.lengthSqr() > 1.0E-6D) {
            victim.setDeltaMovement(towardCenter.normalize().scale(EDGE_LAUNCH_SPEED));
            victim.hurtMarked = true;
        }
    }

    private Vec3 cageForward() {
        double yaw = Math.toRadians(getYRot());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    private Vec3 cageRight() {
        double yaw = Math.toRadians(getYRot());
        return new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
    }

    private void updateCageBoundingBox() {
        double half = 1.5D;
        setBoundingBox(new AABB(
            getX() - half, getY() - half, getZ() - half,
            getX() + half, getY() + half, getZ() + half));
    }

    private void damageEyeBasketSphere(float radius) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = resolveOwner();
        if (owner == null) {
            return;
        }

        double searchRadius = Math.max(1.0D, radius + 1.5D);
        AABB searchBox = new AABB(position(), position()).inflate(searchRadius);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
            entity -> entity.isAlive() && entity != owner && !eyeBasketHitTargets.contains(entity.getUUID()))) {
            Vec3 victimCenter = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
            double distance = victimCenter.distanceTo(position());
            double tolerance = radius <= 0.01D
                ? 1.0D
                : Math.max(0.75D, victim.getBbWidth() * 0.5D + 0.25D);
            boolean crossed = radius <= 0.01D
                ? distance <= 1.0D
                : Math.abs(distance - radius) <= tolerance;
            if (!crossed) {
                continue;
            }

            Vec3 towardCenter = position().subtract(victimCenter);
            if (towardCenter.lengthSqr() > 1.0E-6D) {
                victim.setDeltaMovement(towardCenter.normalize().scale(EDGE_LAUNCH_SPEED));
                victim.hurtMarked = true;
            }
            if (Damager.hurt(owner, victim, webHealth / 5.0F, true)) {
                eyeBasketHitTargets.add(victim.getUUID());
            }
        }
    }

    private void containEyeBasketEntities(float radius) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = resolveOwner();
        if (owner == null) {
            return;
        }

        double boundary = Math.max(0.5D, radius);
        double searchRadius = boundary + 2.0D;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(position(), position()).inflate(searchRadius),
            entity -> entity.isAlive() && entity != owner)) {
            Vec3 victimCenter = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
            Vec3 fromCenter = victimCenter.subtract(position());
            double distance = fromCenter.length();
            double tolerance = Math.max(0.75D, victim.getBbWidth() * 0.5D + 0.25D);
            if (distance < boundary - tolerance) {
                continue;
            }

            launchEyeTowardCenter(victim, fromCenter);
            double allowed = Math.max(0.0D, boundary - tolerance);
            Vec3 clampedCenter = distance > 1.0E-6D
                ? position().add(fromCenter.scale(Math.min(allowed / distance, 1.0D)))
                : position();
            victim.setPos(clampedCenter.x, clampedCenter.y - victim.getBbHeight() * 0.5D,
                clampedCenter.z);
            victim.fallDistance = 0.0F;
        }
    }

    private void launchEyeTowardCenter(LivingEntity victim, Vec3 fromCenter) {
        if (fromCenter.lengthSqr() > 1.0E-6D) {
            victim.setDeltaMovement(fromCenter.normalize().scale(-EDGE_LAUNCH_SPEED));
            victim.hurtMarked = true;
        }
    }

    private boolean intersectsTarget(LivingEntity target) {
        double radius = switch (getPattern()) {
            case DICING_WALL -> 3.5D;
            case ROTATION -> 3.8D;
            case CUTTING_CAGE, CONTORTING -> Math.max(0.4D, 3.0D * getVisualScale());
            case EYE_BASKET -> Math.max(0.4D, 2.6D * getVisualScale());
            case EYE_BASKET_PULSE, DECAPITATION_CIRCLE -> 0.0D;
        };
        return getBoundingBox().inflate(radius).intersects(target.getBoundingBox());
    }

    private void damageNearby() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = resolveOwner();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(1.0D), entity -> entity.isAlive() && entity != owner)) {
            if (Damager.hurt(owner, victim, webHealth / 5.0F, true)) {
                lastDamageTick = tickCount;
            }
        }
    }

    private void damageRotationCircles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = resolveOwner();
        if (owner == null) {
            return;
        }

        Vec3 forward = direction.normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        float angle = getRotationAngle();

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(4.5D), entity -> entity.isAlive() && entity != owner)) {
            Vec3 relative = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D)
                .subtract(position());
            double localX = relative.dot(right);
            double localY = relative.dot(up);
            double localZ = relative.dot(forward);
            double tolerance = Math.max(0.6D, victim.getBbWidth() * 0.5D + 0.25D);
            boolean hit = false;

            for (int index = 0; index < 8; index++) {
                double baseAngle = Math.PI * 0.5D + index * Math.PI * 0.25D;
                boolean clockwise = ((index + 1) & 1) == 0;
                double circleAngle = baseAngle + (clockwise ? -angle : angle);
                double centerX = Math.cos(circleAngle);
                double centerY = Math.sin(circleAngle);
                double radialDistance = Math.hypot(localX - centerX, localY - centerY);
                if (Math.abs(radialDistance - 2.0D) <= tolerance
                    && Math.abs(localZ - getRotationZOffset(index)) <= tolerance) {
                    hit = true;
                    break;
                }
            }

            if (hit && Damager.hurt(owner, victim, webHealth / 5.0F, true)) {
                lastDamageTick = tickCount;
            }
        }
    }

    private LivingEntity resolveTarget() {
        UUID targetId = getTargetUuid();
        if (targetId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(targetId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private LivingEntity resolveOwner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (level().isClientSide || tickCount - lastDamageTick < 2) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity living)) {
            return false;
        }

        boolean strong = living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) >= 12.0D;
        if (living instanceof ServerPlayer player) {
            DemonSlayerRank rank = DemonSlayerRankManager.getRank(player);
            strong |= rank != null && rank.level() >= DemonSlayerRank.KINOE.level();
        }
        webHealth -= strong ? amount * 3.0F : amount;
        lastDamageTick = tickCount;
        if (webHealth <= 0.0F) {
            discard();
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Pattern", getPattern().ordinal());
        tag.putFloat("VisualScale", getVisualScale());
        tag.putBoolean("RedThreads", isRedThreads());
        tag.putInt("Lifetime", getLifetime());
        tag.putFloat("WebHealth", webHealth);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        if (getTargetUuid() != null) {
            tag.putUUID("Target", getTargetUuid());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setPattern(Pattern.values()[Math.max(0, Math.min(Pattern.values().length - 1, tag.getInt("Pattern")))]);
        setVisualScale(tag.getFloat("VisualScale"));
        setRedThreads(tag.getBoolean("RedThreads"));
        setLifetime(tag.getInt("Lifetime"));
        webHealth = tag.getFloat("WebHealth");
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.entityData.set(TARGET, Optional.of(tag.getUUID("Target")));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return getPattern() != Pattern.CUTTING_CAGE
            && getPattern() != Pattern.EYE_BASKET
            && getPattern() != Pattern.EYE_BASKET_PULSE;
    }

    @Override
    public boolean canBeCollidedWith() {
        return getPattern() != Pattern.CUTTING_CAGE
            && getPattern() != Pattern.EYE_BASKET
            && getPattern() != Pattern.EYE_BASKET_PULSE;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25600.0D;
    }
}
