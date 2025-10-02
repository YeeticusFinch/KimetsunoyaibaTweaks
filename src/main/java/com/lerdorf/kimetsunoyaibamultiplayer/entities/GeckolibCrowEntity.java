package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
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
import net.minecraft.world.entity.LivingEntity;
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
    private static final EntityDataAccessor<Boolean> IS_LANDING =
        SynchedEntityData.defineId(GeckolibCrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<UUID>> ORIGINAL_CROW_UUID =
        SynchedEntityData.defineId(GeckolibCrowEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // Reference to the original kasugai_crow entity this mirrors
    private Entity originalCrowCache;

    // Hurt flash timer
    private int hurtFlashTimer = 0;

    // Grace period before giving up on finding original crow (100 ticks = 5 seconds)
    private int ticksAlive = 0;
    private static final int GRACE_PERIOD_TICKS = 100;

    // Death animation timer (2 seconds = 40 ticks to match animation length)
    private int deathAnimationTimer = 0;
    private static final int DEATH_ANIMATION_DURATION = 40;

    // Idle animation system
    private int ticksSinceLastIdleAction = 0;
    private static final int IDLE_ACTION_COOLDOWN = 400; // 20 seconds
    private static final double IDLE_ACTION_CHANCE = 0.002; // 0.2% chance per tick when idle (avg 10 seconds between checks)
    private String currentIdleAnimation = null;

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
        this.entityData.define(IS_LANDING, false);
        this.entityData.define(ORIGINAL_CROW_UUID, java.util.Optional.empty());
    }

    /**
     * Set the UUID of the original crow this entity should mirror
     */
    public void setOriginalCrow(UUID crowUUID) {
        this.entityData.set(ORIGINAL_CROW_UUID, java.util.Optional.ofNullable(crowUUID));
        this.originalCrowCache = null; // Clear cache
    }

    /**
     * Get the UUID of the original crow
     */
    public UUID getOriginalCrowUUID() {
        return this.entityData.get(ORIGINAL_CROW_UUID).orElse(null);
    }

    /**
     * Trigger landing animation
     */
    public void triggerLanding() {
        this.entityData.set(IS_LANDING, true);
    }

    /**
     * Get the original crow entity
     * Works on both client and server side
     */
    public Entity getOriginalCrow() {
        UUID originalCrowUUID = getOriginalCrowUUID();

        if (originalCrowUUID == null) {
            if (ticksAlive % 20 == 0 && Config.logDebug) {
                LOGGER.error("Mirror crow {} has null originalCrowUUID!", this.getUUID());
            }
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
                        if (ticksAlive % 20 == 0 && Config.logDebug) {
                            LOGGER.info("Found original crow via ServerLevel.getEntity(): {}", entity.getName().getString());
                        }
                        originalCrowCache = entity;
                        return entity;
                    } else if (ticksAlive % 20 == 0 && Config.logDebug) {
                        LOGGER.warn("ServerLevel.getEntity() returned null for UUID: {}", originalCrowUUID);
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
        } else if (ticksAlive % 20 == 0 && Config.logDebug) {
            LOGGER.warn("Mirror crow level is null!");
        }

        return null;
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        Entity originalCrow = getOriginalCrow();

        // If original crow is gone, remove this entity
        // BUT give it a grace period to find the original crow (handles entity loading delays)
        if (originalCrow == null || !originalCrow.isAlive()) {
            UUID originalCrowUUID = getOriginalCrowUUID(); // Get UUID from synced data, not from null entity
            if (ticksAlive < GRACE_PERIOD_TICKS) {
                // Still in grace period - don't remove yet
                if (Config.logDebug && ticksAlive % 20 == 0) { // Log every second
                    LOGGER.warn("Mirror crow {} can't find original crow {} yet (tick {}), waiting...",
                        this.getUUID(), originalCrowUUID, ticksAlive);
                }
                return; // Don't do anything else this tick, just wait
            } else {
                // Grace period expired
            	if (Config.logDebug)
                LOGGER.warn("Mirror crow {} giving up on finding original crow {} after {} ticks",
                    this.getUUID(), originalCrowUUID, ticksAlive);
                this.discard();
                return;
            }
        }

        // Found the original crow! Log it once
        if (Config.logDebug && (ticksAlive == 1 || (ticksAlive < GRACE_PERIOD_TICKS && ticksAlive % 20 == 0))) {
            LOGGER.info("Mirror crow {} successfully linked to original crow {} at tick {}",
                this.getUUID(), originalCrow.getUUID(), ticksAlive);
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

        // Sync health from original crow
        if (originalCrow instanceof LivingEntity livingOriginal) {
            this.setHealth(livingOriginal.getHealth());
        }

        // Update flying state
        UUID originalCrowUUID = getOriginalCrowUUID();
        boolean isFlying = originalCrowUUID != null && CrowEnhancementHandler.isCrowFlying(originalCrowUUID);
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
            if (!this.entityData.get(IS_DEAD)) {
                // Just died, trigger death animation
                this.entityData.set(IS_DEAD, true);
                deathAnimationTimer = DEATH_ANIMATION_DURATION;
                if (Config.logDebug)
                LOGGER.info("Mirror crow starting death animation (will remove in {} ticks)", DEATH_ANIMATION_DURATION);
            }
        }

        // Handle death animation timer
        if (this.entityData.get(IS_DEAD)) {
            if (deathAnimationTimer > 0) {
                deathAnimationTimer--;
                if (deathAnimationTimer == 0) {
                    // Death animation finished, remove this mirror
                	if (Config.logDebug)
                    LOGGER.info("Mirror crow death animation finished, removing entity");
                    this.discard();
                    return;
                }
            }
        }

        // Update idle action timer
        ticksSinceLastIdleAction++;
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
        // Never take fall damage
        if (source.getMsgId().equals("fall")) {
            return false;
        }

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
        UUID originalCrowUUID = getOriginalCrowUUID();
        if (originalCrowUUID != null) {
            tag.putUUID("OriginalCrowUUID", originalCrowUUID);
        }
        tag.putInt("DeathAnimationTimer", deathAnimationTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OriginalCrowUUID")) {
            setOriginalCrow(tag.getUUID("OriginalCrowUUID"));
        }
        if (tag.contains("DeathAnimationTimer")) {
            deathAnimationTimer = tag.getInt("DeathAnimationTimer");
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

        // Check landing animation
        if (this.entityData.get(IS_LANDING)) {
            if (this.onGround()) {
                // Play landing animation once, then clear flag
                event.getController().setAnimation(RawAnimation.begin()
                    .then("kimetsunoyaibamultiplayer.crow.landing", Animation.LoopType.PLAY_ONCE)
                    .thenLoop("kimetsunoyaibamultiplayer.crow.idle"));
                this.entityData.set(IS_LANDING, false);
            } else {
                // Still in air, keep flying animation
                event.getController().setAnimation(RawAnimation.begin()
                    .thenLoop("kimetsunoyaibamultiplayer.crow.flying"));
            }
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
            currentIdleAnimation = null; // Clear idle animation when not idle
            event.getController().setAnimation(RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.flying"));
        } else if (isMovingOnGround) {
            currentIdleAnimation = null; // Clear idle animation when not idle
            event.getController().setAnimation(RawAnimation.begin()
                .thenLoop("kimetsunoyaibamultiplayer.crow.walk"));
        } else {
            // Crow is idle - check if we should play an idle action animation
            if (currentIdleAnimation != null) {
                // Currently playing an idle action - check if it's finished
                if (event.getController().isPlayingTriggeredAnimation()) {
                    // Still playing, keep it
                    return PlayState.CONTINUE;
                } else {
                    // Finished, reset
                    currentIdleAnimation = null;
                    ticksSinceLastIdleAction = 0;
                }
            } else if (ticksSinceLastIdleAction >= IDLE_ACTION_COOLDOWN &&
                       this.random.nextDouble() < IDLE_ACTION_CHANCE) {
                // Cooldown passed and random chance hit - play a random idle action
                String[] idleActions = {
                    "kimetsunoyaibamultiplayer.crow.idle_action",
                    "kimetsunoyaibamultiplayer.crow.idle_action2",
                    "kimetsunoyaibamultiplayer.crow.idle_action3",
                    "kimetsunoyaibamultiplayer.crow.idle_action4"
                };
                currentIdleAnimation = idleActions[this.random.nextInt(idleActions.length)];

                event.getController().setAnimation(RawAnimation.begin()
                    .then(currentIdleAnimation, Animation.LoopType.PLAY_ONCE)
                    .thenLoop("kimetsunoyaibamultiplayer.crow.idle"));
            } else {
                // Just play regular idle
                event.getController().setAnimation(RawAnimation.begin()
                    .thenLoop("kimetsunoyaibamultiplayer.crow.idle"));
            }
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

    public boolean isLanding() {
        return this.entityData.get(IS_LANDING);
    }
}
