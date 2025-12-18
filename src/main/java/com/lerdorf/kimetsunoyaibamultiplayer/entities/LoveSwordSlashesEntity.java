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
 * Love Sword Slashes entity - visual effect for Love Breathing forms.
 *
 * Features:
 * - Spawns at specific location with custom rotation (full 3D orientation)
 * - Plays specified GeckoLib animation
 * - Despawns after lifetime expires
 * - No gravity, no physics, no collision
 * - Synced across multiplayer via SpawnLoveSwordSlashesPacket
 *
 * Default animation: "base"
 * Animation file: love_sword_slashes.animation.json
 * Model file: love_sword_slashes.geo.json
 */
public class LoveSwordSlashesEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Synced data for animation name
    private static final EntityDataAccessor<String> DATA_ANIMATION =
        SynchedEntityData.defineId(LoveSwordSlashesEntity.class, EntityDataSerializers.STRING);

    private int ticksAlive = 0;
    private int lifetime = 40; // Default 2 seconds (40 ticks)

    public LoveSwordSlashesEntity(EntityType<? extends Mob> type, Level level) {
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
        this.entityData.define(DATA_ANIMATION, "base");
    }

    /**
     * Create a love sword slashes entity with full control over position, orientation, animation, and lifetime.
     *
     * @param level The world
     * @param position The spawn position (Vec3)
     * @param yaw Horizontal rotation (degrees, 0 = north, 90 = east)
     * @param pitch Vertical rotation (degrees, -90 = up, 0 = forward, 90 = down)
     * @param animationName The animation to play (from love_sword_slashes.animation.json)
     * @param lifetimeTicks How long the entity should exist (in ticks)
     * @return The created entity
     */
    public static LoveSwordSlashesEntity create(Level level, Vec3 position, float yaw, float pitch,
                                                 String animationName, int lifetimeTicks) {
        LoveSwordSlashesEntity entity = new LoveSwordSlashesEntity(ModEntities.LOVE_SWORD_SLASHES.get(), level);
        entity.setPos(position.x, position.y, position.z);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        // Lock rotation by setting old rotation values (prevents interpolation)
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.setAnimation(animationName);
        entity.lifetime = lifetimeTicks;
        return entity;
    }

    /**
     * Convenience method - create with default lifetime (40 ticks = 2 seconds).
     */
    public static LoveSwordSlashesEntity create(Level level, Vec3 position, float yaw, float pitch,
                                                 String animationName) {
        return create(level, position, yaw, pitch, animationName, 40);
    }

    /**
     * Set the animation to play.
     */
    public void setAnimation(String animationName) {
        this.entityData.set(DATA_ANIMATION, animationName);
    }

    /**
     * Get the current animation name (synced from server).
     */
    public String getAnimation() {
        return this.entityData.get(DATA_ANIMATION);
    }

    /**
     * Set the lifetime in ticks.
     */
    public void setLifetime(int ticks) {
        this.lifetime = ticks;
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
        this.lifetime = tag.getInt("Lifetime");
        if (tag.contains("Animation")) {
            this.setAnimation(tag.getString("Animation"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putInt("Lifetime", this.lifetime);
        tag.putString("Animation", this.getAnimation());
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // Controller for love sword slashes animation
        // Animation file: love_sword_slashes.animation.json
        // Default animation: "base"
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            String animationName = getAnimation();

            // Build animation resource location
            // Format: "kimetsunoyaibamultiplayer.love_sword_slashes.<animation>"
            // Example: "kimetsunoyaibamultiplayer.love_sword_slashes.love_third_form"
            String animationResourceName = "kimetsunoyaibamultiplayer.love_sword_slashes." + animationName;

            state.getController().setAnimation(RawAnimation.begin()
                .then(animationResourceName,
                      software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE));

            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
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
        // Don't call super.travel() - this prevents physics updates
    }

    @Override
    public void move(net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 pos) {
        // Prevent any movement
        // Don't call super.move() - entity stays exactly where spawned
    }

    @Override
    public void tick() {
        // Override tick to prevent entity ticking that might apply physics
        // We'll handle our own tick logic without calling super.tick()
        this.baseTick(); // Only do base entity updates (like NBT, effects)

        // Aggressively prevent any movement or gravity
        this.setNoGravity(true);
        this.setDeltaMovement(0, 0, 0); // Zero out velocity every tick
        this.setOnGround(true); // Prevent falling calculations

        // Lock rotation - prevent any changes to orientation
        // Store current rotation to prevent interpolation artifacts
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        if (!level().isClientSide) {
            ticksAlive++;

            // Check if lifetime has expired
            if (ticksAlive >= lifetime) {
                this.discard();
            }
        }
    }
}
