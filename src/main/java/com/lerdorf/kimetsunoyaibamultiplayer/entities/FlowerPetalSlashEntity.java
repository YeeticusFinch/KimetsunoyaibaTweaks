package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Flower Petal Slash entity - visual effect for Flower Breathing forms.
 *
 * Features:
 * - Spawns at specific location with direction (yaw, pitch) and roll rotation
 * - Animated texture cycling from sword_loop_flower0.png to sword_loop_flower17.png
 * - One frame per tick, despawns after 18 ticks (one full loop)
 * - No gravity, no physics, no collision
 *
 * Model file: flower_petal_slash.geo.json
 * Textures: sword_loop_flower0.png to sword_loop_flower17.png
 */
public class FlowerPetalSlashEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Total number of texture frames
    public static final int TOTAL_FRAMES = 18;

    // Synced data for current texture frame and roll
    private static final EntityDataAccessor<Integer> DATA_FRAME =
        SynchedEntityData.defineId(FlowerPetalSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
        SynchedEntityData.defineId(FlowerPetalSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
        SynchedEntityData.defineId(FlowerPetalSlashEntity.class, EntityDataSerializers.FLOAT);

    private int ticksAlive = 0;

    public FlowerPetalSlashEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Can pass through blocks
        this.setInvulnerable(true);
        this.setNoAi(true); // Disable AI since it's just visual
        this.setSilent(true); // No sounds
        this.setNoGravity(true); // Disable gravity
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FRAME, 0);
        this.entityData.define(DATA_ROLL, 0.0f);
        this.entityData.define(DATA_SCALE, 1.0f);
    }

    /**
     * Create a flower petal slash entity with full control over position, orientation, and roll.
     *
     * @param level The world
     * @param position The spawn position (Vec3)
     * @param direction The direction vector the slash faces
     * @param rollDegrees Roll rotation around the direction axis (degrees)
     * @return The created entity
     */
    public static FlowerPetalSlashEntity create(Level level, Vec3 position, Vec3 direction, float rollDegrees) {
        return create(level, position, direction, rollDegrees, 1.0f);
    }

    /**
     * Create a flower petal slash entity with full control over position, orientation, roll, and scale.
     */
    public static FlowerPetalSlashEntity create(Level level, Vec3 position, Vec3 direction, float rollDegrees, float scale) {
        FlowerPetalSlashEntity entity = new FlowerPetalSlashEntity(ModEntities.FLOWER_PETAL_SLASH.get(), level);
        entity.setPos(position.x, position.y, position.z);

        // Calculate yaw and pitch from direction vector
        double horizontalDist = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-direction.y, horizontalDist));

        entity.setYRot(yaw);
        entity.setXRot(pitch);
        // Lock rotation by setting old rotation values (prevents interpolation)
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.setRoll(rollDegrees);
        entity.setScale(scale);

        return entity;
    }

    /**
     * Convenience method - create with yaw and pitch directly instead of direction vector.
     */
    public static FlowerPetalSlashEntity create(Level level, Vec3 position, float yaw, float pitch, float rollDegrees, float scale) {
        FlowerPetalSlashEntity entity = new FlowerPetalSlashEntity(ModEntities.FLOWER_PETAL_SLASH.get(), level);
        entity.setPos(position.x, position.y, position.z);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        // Lock rotation by setting old rotation values (prevents interpolation)
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.setRoll(rollDegrees);
        entity.setScale(scale);
        return entity;
    }

    /**
     * Set the roll rotation (rotation around the direction axis).
     */
    public void setRoll(float rollDegrees) {
        this.entityData.set(DATA_ROLL, rollDegrees);
    }

    /**
     * Get the roll rotation.
     */
    public float getRoll() {
        return this.entityData.get(DATA_ROLL);
    }

    /**
     * Set the base render scale.
     */
    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    /**
     * Get the base render scale.
     */
    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    /**
     * Get the current texture frame (0-17).
     */
    public int getCurrentFrame() {
        return this.entityData.get(DATA_FRAME);
    }

    /**
     * Set the current texture frame.
     */
    public void setCurrentFrame(int frame) {
        this.entityData.set(DATA_FRAME, Math.max(0, Math.min(TOTAL_FRAMES - 1, frame)));
    }

    @Override
    public boolean isPickable() {
        return false; // Cannot be clicked/targeted
    }

    @Override
    public boolean isPushable() {
        return false; // Cannot be pushed
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false; // Cannot be hurt
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false; // Cannot deal damage
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksAlive = tag.getInt("TicksAlive");
        if (tag.contains("Frame")) {
            this.setCurrentFrame(tag.getInt("Frame"));
        }
        if (tag.contains("Roll")) {
            this.setRoll(tag.getFloat("Roll"));
        }
        if (tag.contains("Scale")) {
            this.setScale(tag.getFloat("Scale"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putInt("Frame", this.getCurrentFrame());
        tag.putFloat("Roll", this.getRoll());
        tag.putFloat("Scale", this.getScale());
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // Simple idle controller - we don't need GeckoLib animation since we're doing texture animation
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return software.bernie.geckolib.core.object.PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // No collisions
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return false; // No collisions
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        // Prevent any travel/movement
    }

    @Override
    public void move(net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 pos) {
        // Prevent any movement
    }

    @Override
    public void tick() {
        // Override tick to prevent entity ticking that might apply physics
        this.baseTick(); // Only do base entity updates

        // Aggressively prevent any movement or gravity
        this.setNoGravity(true);
        this.setDeltaMovement(0, 0, 0);
        this.setOnGround(true);

        // Lock rotation
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        if (!level().isClientSide) {
            // Increment frame each tick
            this.setCurrentFrame(ticksAlive);

            ticksAlive++;

            // Despawn after one full animation loop (18 frames = 18 ticks)
            if (ticksAlive >= TOTAL_FRAMES) {
                this.discard();
            }
        }
    }
}
