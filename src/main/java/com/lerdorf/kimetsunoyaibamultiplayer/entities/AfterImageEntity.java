package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * An after image entity that appears during Flower Breathing 7th Form
 * These clones are purely visual - they cannot be attacked, take damage, or interact.
 * They slowly fade out and dissapear
 */
public class AfterImageEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Float> DATA_OPACITY =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_PLAYER_NAME =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEXTURE_PATH =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_PLAYER =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_SWINGING =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_CURRENT_ANIMATION =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_TICKS =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LOCKED_YAW =
        SynchedEntityData.defineId(AfterImageEntity.class, EntityDataSerializers.FLOAT);

    private int lifetime = 0;
    private int maxLifetime = 200; // 10 seconds
    private float fadeSpeed = 0.01f; // Very slow fade speed for smooth, gradual transitions (0->0.4 takes 40 ticks = 2 seconds)
    private boolean fadingIn = false; // Start invisible
    private boolean fadeOutOnly = false; // If true, just fade out from current opacity (no cycling)
    private int swingAnimationTicks = 0; // Track swing animation duration
    private float opacityPhaseOffset = 0.0f; // Random offset for opacity cycling (0.0 to 1.0)
    private int invisibleTicks = 0; // Ticks to stay invisible before fading in

    // New blinking behavior state
    private boolean useBlinkBehavior = false; // If true, use new blink behavior instead of fade
    private int fullOpacityTicks = 0; // Ticks to stay at full opacity before blinking
    private int blinkCount = 0; // Number of blinks remaining
    private int blinkTimer = 0; // Timer for current blink phase
    private boolean blinkOn = true; // Current blink state (visible or invisible)
    private static final int BLINK_ON_TICKS = 3; // Ticks visible during blink
    private static final int BLINK_OFF_TICKS = 3; // Ticks invisible during blink

    // Circular motion parameters
    private Vec3 centerPos; // Center of the fog cloud
    private double circleRadius; // Random radius from center
    private double circleAngle; // Current angle around the circle
    private double angularSpeed; // How fast it circles (radians per tick)
    private double verticalOffset; // Random Y offset
    private UUID ownerUUID;
    private ResourceLocation sourceTexture;

    // Movement tracking for smooth animation transitions
    private double lastMovementSpeed = 0.0;
    private int ticksSinceLastMovement = 0;

    public AfterImageEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Can pass through blocks
        this.setInvulnerable(true);
        this.setNoAi(true); // Disable AI since it's just visual
        // Default to fading in so entity doesn't immediately despawn
        this.fadingIn = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /**
     * Creates an AfterImageEntity facing a specified direction.
     * @param level The level to spawn in
     * @param original The entity to copy appearance from
     * @param maxLifetime Maximum lifetime in ticks
     * @param centerPos The position to spawn at
     * @param facingYaw The yaw rotation (direction) the after image should face
     */
    public AfterImageEntity(Level level, LivingEntity original, int maxLifetime, Vec3 centerPos, float facingYaw) {
        this(ModEntities.AFTER_IMAGE.get(), level);
        this.maxLifetime = maxLifetime;
        this.centerPos = centerPos;
        this.ownerUUID = original.getUUID();


        // Random initial invisible duration (40-80 ticks = 2-4 seconds)
        this.invisibleTicks = 40 + level.random.nextInt(40);

        // Start invisible (fadingIn will be triggered after invisibleTicks)
        this.fadingIn = false;

        // Calculate initial
        double spawnX = centerPos.x;
        double spawnZ = centerPos.z;

        // Find ground level at spawn position
        double spawnY = centerPos.y;
        //double spawnY = groundY + this.verticalOffset;

        // Spawn at calculated position on the circle
        this.setPos(spawnX, spawnY, spawnZ);
        setFacingYaw(facingYaw); // Use the inputted direction instead of copying from entity
        this.setXRot(0); // Keep pitch flat
        this.setDeltaMovement(0, 0, 0); // Ensure zero velocity on spawn

        // Setup texture and player info
        if (original instanceof Player player) {
            this.entityData.set(DATA_PLAYER_NAME, player.getName().getString());
            this.entityData.set(DATA_IS_PLAYER, true);
            this.entityData.set(DATA_TEXTURE_PATH, ""); // Will use player skin from name
        } else {
            this.entityData.set(DATA_PLAYER_NAME, "");
            this.entityData.set(DATA_IS_PLAYER, false);
            // Use Kanae's texture for non-player entities (flower breathing)
            this.entityData.set(DATA_TEXTURE_PATH, "kimetsunoyaibamultiplayer:textures/entity/kanae.png");
        }

        // Copy equipment from original to this entity's equipment slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = original.getItemBySlot(slot);
            if (!item.isEmpty()) {
                this.setItemSlot(slot, item.copy());
            }
        }

        // Start transparent
        this.entityData.set(DATA_OPACITY, 0.0f);
        this.entityData.set(DATA_IS_SWINGING, false);
    }

    /**
     * Legacy constructor that copies the entity's current rotation.
     * @deprecated Use the constructor with facingYaw parameter instead.
     */
    @Deprecated
    public AfterImageEntity(Level level, LivingEntity original, int maxLifetime, Vec3 centerPos) {
        this(level, original, maxLifetime, centerPos, original.getYRot());
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setSourceTexture(ResourceLocation texture) {
        this.sourceTexture = texture;
    }

    public ResourceLocation getSourceTexture() {
        return sourceTexture;
    }

    /**
     * Makes this after image start fully visible with blink behavior.
     * Stays at full opacity for 2 seconds, blinks 3 times quickly, then despawns.
     * Use this for effects where the afterimage should be seen right away (e.g., teleport trails).
     */
    public void startVisible() {
        this.invisibleTicks = 0;
        this.fadingIn = false;
        this.fadeOutOnly = false;
        this.useBlinkBehavior = true; // Use new blink behavior
        this.fullOpacityTicks = 40; // 2 seconds at full opacity
        this.blinkCount = 3; // 3 blinks before despawn
        this.blinkTimer = 0;
        this.blinkOn = true;
        this.entityData.set(DATA_OPACITY, 1.0f); // Start at full opacity
    }

    /**
     * Legacy method for fade out behavior.
     * Makes this after image start fully visible and slowly fade out.
     */
    public void startVisibleWithFade() {
        this.invisibleTicks = 0;
        this.fadingIn = false;
        this.fadeOutOnly = true; // Just fade out, no cycling
        this.useBlinkBehavior = false;
        this.fadeSpeed = 0.0125f; // Fade from 1.0 to 0 over 80 ticks (4 seconds)
        this.entityData.set(DATA_OPACITY, 1.0f); // Start at full opacity
    }

    @Override
    public void tick() {
        super.tick();

        // Keep afterimage orientation stable (head/body/yaw all locked to spawn-facing yaw).
        applyFacingYaw(this.entityData.get(DATA_LOCKED_YAW));

        if (!level().isClientSide) {
            lifetime++;

            // Update opacity (fade in/out effect) with phase offset for desynchronization
            float currentOpacity = this.entityData.get(DATA_OPACITY);

            // New blink behavior - full opacity for 2 seconds, blink 3 times, then despawn
            if (useBlinkBehavior) {
                if (fullOpacityTicks > 0) {
                    // Stay at full opacity
                    fullOpacityTicks--;
                    currentOpacity = 1.0f;
                } else if (blinkCount > 0) {
                    // Blinking phase
                    blinkTimer++;
                    if (blinkOn) {
                        currentOpacity = 1.0f;
                        if (blinkTimer >= BLINK_ON_TICKS) {
                            blinkOn = false;
                            blinkTimer = 0;
                        }
                    } else {
                        currentOpacity = 0.0f;
                        if (blinkTimer >= BLINK_OFF_TICKS) {
                            blinkOn = true;
                            blinkTimer = 0;
                            blinkCount--; // One blink cycle completed
                        }
                    }
                } else {
                    // Done blinking - despawn
                    this.discard();
                    return;
                }
            }
            // Fade out only mode - just fade from current opacity to 0
            else if (fadeOutOnly) {
                currentOpacity -= fadeSpeed;
                if (currentOpacity <= 0.0f) {
                    currentOpacity = 0.0f;
                    this.discard(); // Remove when fully faded out
                }
            }
            // Handle initial invisible period
            else if (invisibleTicks > 0) {
                invisibleTicks--;
                currentOpacity = 0.0f;
                if (invisibleTicks == 0) {
                    fadingIn = true; // Start fading in after invisible period
                }
            } else {
                // Normal fade cycle
                if (fadingIn) {
                    currentOpacity += fadeSpeed;
                    if (currentOpacity >= 0.4f) { // Max opacity is now 0.4 (more ghostly)
                        currentOpacity = 0.4f;
                        fadingIn = false;
                        // Stay visible for 20-40 ticks (1-2 seconds) before fading out
                        invisibleTicks = 20 + level().random.nextInt(20);
                    }
                } else {
                    // Check if we should start fading out
                    if (invisibleTicks > 0) {
                        invisibleTicks--;
                        // Stay at current opacity
                    } else {
                        // Fade out
                        currentOpacity -= fadeSpeed;
                        if (currentOpacity <= 0.0f) {
                            currentOpacity = 0.0f;
                            // Stay invisible for 30-60 ticks (1.5-3 seconds) before fading in again
                            invisibleTicks = 30 + level().random.nextInt(30);
                            fadingIn = true;
                        }
                    }
                }
            }

            this.entityData.set(DATA_OPACITY, currentOpacity);

            // Remove after lifetime expires (safety net)
            if (lifetime >= maxLifetime) {
                this.discard();
            }

            // Tick down swing animation
            if (swingAnimationTicks > 0) {
                swingAnimationTicks--;
                if (swingAnimationTicks == 0) {
                    this.entityData.set(DATA_IS_SWINGING, false);
                }
            }

            // Tick down explicit Gecko animation timer.
            int animationTicks = this.entityData.get(DATA_ANIMATION_TICKS);
            if (animationTicks > 0) {
                animationTicks--;
                this.entityData.set(DATA_ANIMATION_TICKS, animationTicks);
                if (animationTicks == 0) {
                    this.entityData.set(DATA_CURRENT_ANIMATION, "idle");
                }
            }
        }
    }

    public float getOpacity() {
        return this.entityData.get(DATA_OPACITY);
    }

    public String getPlayerName() {
        return this.entityData.get(DATA_PLAYER_NAME);
    }

    @Override
    public boolean isPickable() {
        return true; // Changed to true so mobs can target it
    }

    @Override
    public boolean isPushable() {
        return false; // Cannot be pushed
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Clones cannot be hurt - they are just visual effects
        return false;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // Clones cannot deal damage - they are just visual effects
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.lifetime = tag.getInt("Lifetime");
        this.maxLifetime = tag.getInt("MaxLifetime");
        if (tag.contains("CenterX")) {
            this.centerPos = new Vec3(tag.getDouble("CenterX"), tag.getDouble("CenterY"), tag.getDouble("CenterZ"));
        }
        this.circleRadius = tag.getDouble("CircleRadius");
        this.circleAngle = tag.getDouble("CircleAngle");
        this.angularSpeed = tag.getDouble("AngularSpeed");
        this.verticalOffset = tag.getDouble("VerticalOffset");
    }

    @Override
	public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.lifetime);
        tag.putInt("MaxLifetime", this.maxLifetime);
        if (centerPos != null) {
            tag.putDouble("CenterX", centerPos.x);
            tag.putDouble("CenterY", centerPos.y);
            tag.putDouble("CenterZ", centerPos.z);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // Register entity data parameters for syncing between client and server
        this.entityData.define(DATA_OPACITY, 0.0f);
        this.entityData.define(DATA_PLAYER_NAME, "");
        this.entityData.define(DATA_TEXTURE_PATH, "");
        this.entityData.define(DATA_IS_PLAYER, false);
        this.entityData.define(DATA_IS_SWINGING, false);
        this.entityData.define(DATA_CURRENT_ANIMATION, "idle");
        this.entityData.define(DATA_ANIMATION_TICKS, 0);
        this.entityData.define(DATA_LOCKED_YAW, 0.0f);
    }

    public boolean isPlayerClone() {
        return this.entityData.get(DATA_IS_PLAYER);
    }

    public String getTexturePath() {
        return this.entityData.get(DATA_TEXTURE_PATH);
    }

    public boolean isSwinging() {
        return this.entityData.get(DATA_IS_SWINGING);
    }

    public void setSwinging(boolean swinging) {
        this.entityData.set(DATA_IS_SWINGING, swinging);
        if (swinging) {
            this.swingAnimationTicks = 10; // 0.5 second swing animation
            if (this.entityData.get(DATA_ANIMATION_TICKS) <= 0) {
                playGeckoAnimation("attack", 10);
            }
        }
    }

    public String getCurrentAnimation() {
        return this.entityData.get(DATA_CURRENT_ANIMATION);
    }

    public int getAnimationTicks() {
        return this.entityData.get(DATA_ANIMATION_TICKS);
    }

    public void playGeckoAnimation(String animationName, int durationTicks) {
        if (animationName == null || animationName.isEmpty()) {
            return;
        }

        String resolvedName = animationName;
        int namespaceSplit = resolvedName.indexOf(':');
        if (namespaceSplit >= 0 && namespaceSplit < resolvedName.length() - 1) {
            resolvedName = resolvedName.substring(namespaceSplit + 1);
        }

        this.entityData.set(DATA_CURRENT_ANIMATION, resolvedName);
        this.entityData.set(DATA_ANIMATION_TICKS, Math.max(1, durationTicks));

        // Named animation takes priority over legacy swing toggle.
        this.entityData.set(DATA_IS_SWINGING, false);
        this.swingAnimationTicks = 0;
    }

    public void setFacingYaw(float yaw) {
        float wrapped = Mth.wrapDegrees(yaw);
        this.entityData.set(DATA_LOCKED_YAW, wrapped);
        applyFacingYaw(wrapped);
    }

    private void applyFacingYaw(float yaw) {
        float wrapped = Mth.wrapDegrees(yaw);
        this.setYRot(wrapped);
        this.yRotO = wrapped;
        this.yBodyRot = wrapped;
        this.yBodyRotO = wrapped;
        this.setYHeadRot(wrapped);
        this.yHeadRotO = wrapped;
        this.setXRot(0.0f);
        this.xRotO = 0.0f;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // Zero transition so animation calls become visible immediately.
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "controller", 0, state -> {
            String currentAnimation = getCurrentAnimation();
            int animationTicks = getAnimationTicks();

            // Priority 1: explicit form animation requested via playGeckoAnimation(...)
            if (animationTicks > 0 && currentAnimation != null && !currentAnimation.isEmpty() && !"idle".equals(currentAnimation)) {
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay(currentAnimation));
            }

            // Priority 2: legacy swing fallback
            if (isSwinging()) {
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("attack"));
            }

            // Always use idle animation for after images
            return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // Ghostly - no collisions
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return false; // Ghostly - no collisions
    }
}
