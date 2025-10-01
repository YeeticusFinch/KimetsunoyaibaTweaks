package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.UUID;

/**
 * A GeckoLib-powered crow entity that mirrors the behavior and position of a kasugai_crow entity.
 * This entity is purely visual and non-interactive - all interactions are forwarded to the original crow.
 */
public class GeckolibCrowEntity extends PathfinderMob implements GeoEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Data serializers for syncing data across network
    private static final EntityDataAccessor<Boolean> IS_FLYING =
        SynchedEntityData.defineId(GeckolibCrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_HURT =
        SynchedEntityData.defineId(GeckolibCrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_DEAD =
        SynchedEntityData.defineId(GeckolibCrowEntity.class, EntityDataSerializers.BOOLEAN);

    // Reference to the original kasugai_crow entity this mirrors
    private UUID originalCrowUUID;
    private Entity originalCrowCache;

    // Hurt flash timer
    private int hurtFlashTimer = 0;

    public GeckolibCrowEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true; // Always render
        this.setNoGravity(false); // Will be controlled by sync
    }

    /**
     * Create default attributes for the GeckoLib crow
     */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 10.0D) // Same as kasugai_crow
            .add(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED, 0.4D)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.2D)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_FLYING, false);
        this.entityData.define(IS_HURT, false);
        this.entityData.define(IS_DEAD, false);
    }

    /**
     * Set the UUID of the original crow this entity should mirror
     */
    public void setOriginalCrow(UUID crowUUID) {
        this.originalCrowUUID = crowUUID;
        this.originalCrowCache = null; // Clear cache
    }

    /**
     * Get the original crow entity
     * Works on both client and server side
     */
    public Entity getOriginalCrow() {
        if (originalCrowUUID == null) {
            return null;
        }

        // Use cached reference if still valid
        if (originalCrowCache != null && originalCrowCache.isAlive() &&
            originalCrowCache.getUUID().equals(originalCrowUUID)) {
            return originalCrowCache;
        }

        // Search for the entity in the current level
        // This works on both client and server side
        if (this.level() != null) {
            // Use server-side entity lookup if on server
            if (!this.level().isClientSide()) {
                // Server side - use ServerLevel's entity manager
                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    Entity entity = serverLevel.getEntity(originalCrowUUID);
                    if (entity != null) {
                        originalCrowCache = entity;
                        return entity;
                    }
                }
            } else {
                // Client side - search nearby entities
                AABB searchBox = this.getBoundingBox().inflate(64.0); // 64 block radius
                for (Entity entity : this.level().getEntities(this, searchBox)) {
                    if (entity.getUUID().equals(originalCrowUUID)) {
                        originalCrowCache = entity;
                        return entity;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void tick() {
        super.tick();

        Entity originalCrow = getOriginalCrow();

        // If original crow is gone, remove this entity
        if (originalCrow == null || !originalCrow.isAlive()) {
            this.discard();
            return;
        }

        // Sync position, rotation, and velocity from original crow
        this.setPos(originalCrow.getX(), originalCrow.getY(), originalCrow.getZ());
        this.setYRot(originalCrow.getYRot());
        this.setXRot(originalCrow.getXRot());
        this.yHeadRot = originalCrow instanceof PathfinderMob mob ? mob.yHeadRot : originalCrow.getYRot();
        this.yBodyRot = originalCrow instanceof PathfinderMob mob ? mob.yBodyRot : originalCrow.getYRot();
        this.setDeltaMovement(originalCrow.getDeltaMovement());
        this.setNoGravity(originalCrow.isNoGravity());
        this.setOnGround(originalCrow.onGround());

        // Update flying state
        boolean isFlying = CrowEnhancementHandler.isCrowFlying(originalCrowUUID);
        this.entityData.set(IS_FLYING, isFlying);

        // Update hurt flash timer
        if (hurtFlashTimer > 0) {
            hurtFlashTimer--;
            if (hurtFlashTimer == 0) {
                this.entityData.set(IS_HURT, false);
            }
        }

        // Check if dead
        if (originalCrow.isRemoved() || !originalCrow.isAlive()) {
            this.entityData.set(IS_DEAD, true);
        }
    }

    /**
     * Trigger hurt animation
     */
    public void triggerHurt() {
        this.entityData.set(IS_HURT, true);
        this.hurtFlashTimer = 10; // Red flash for 10 ticks (0.5 seconds)
    }

    /**
     * Trigger death animation
     */
    public void triggerDeath() {
        this.entityData.set(IS_DEAD, true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Forward damage to original crow
        Entity originalCrow = getOriginalCrow();
        if (originalCrow != null) {
            return originalCrow.hurt(source, amount);
        }
        return false;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitPos, InteractionHand hand) {
    	 Entity originalCrow = getOriginalCrow();
         if (originalCrow instanceof PathfinderMob mob) {
             return mob.interact(player, hand);
         }
        return originalCrow.interactAt(player, hitPos, hand);
    }
    
    /*
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        // Forward interaction to original crow
        Entity originalCrow = getOriginalCrow();
        if (originalCrow instanceof PathfinderMob mob) {
            return mob.interact(player, hand);
        }
        return InteractionResult.PASS;
    }*/

    @Override
    public boolean isPickable() {
        // Make this entity pickable so it can receive interactions
        return true;
    }

    @Override
    public boolean isPushable() {
        // Don't allow pushing - position is controlled by original crow
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Don't push other entities
    }

    @Override
    protected void pushEntities() {
        // Don't push entities
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (originalCrowUUID != null) {
            tag.putUUID("OriginalCrowUUID", originalCrowUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OriginalCrowUUID")) {
            this.originalCrowUUID = tag.getUUID("OriginalCrowUUID");
        }
    }

    // ===== GeckoLib Animation System =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "crow_controller", 0, this::animationPredicate));
    }

    private PlayState animationPredicate(software.bernie.geckolib.core.animation.AnimationState<GeckolibCrowEntity> event) {
        // Check death first
        if (this.entityData.get(IS_DEAD)) {
            event.getController().setAnimation(RawAnimation.begin()
                .then("kimetsunoyaibamultiplayer.crow.death", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }

        // Check if hurt
        if (this.entityData.get(IS_HURT)) {
            // Don't change animation, just apply red tint in renderer
        }

        // Check flying state
        boolean isFlying = this.entityData.get(IS_FLYING);
        Vec3 deltaMovement = this.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z);
        boolean isMovingOnGround = horizontalSpeed > 0.01 && this.onGround();

        if (isFlying) {
            event.getController().setAnimation(RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.flying"));
        } else if (isMovingOnGround) {
            event.getController().setAnimation(RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.walk"));
        } else {
            event.getController().setAnimation(RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.idle"));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // Getters for renderer
    public boolean isFlying() {
        return this.entityData.get(IS_FLYING);
    }

    public boolean isHurt() {
        return this.entityData.get(IS_HURT);
    }

    public boolean isDead() {
        return this.entityData.get(IS_DEAD);
    }
}
