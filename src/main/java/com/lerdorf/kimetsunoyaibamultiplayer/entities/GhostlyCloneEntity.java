package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import java.util.UUID;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.GhostlyCloneRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
 * A ghostly clone entity that appears during Mist Breathing 7th Form.
 * These clones are purely visual - they cannot be attacked, take damage, or interact.
 * They fade in and out (including full invisibility) and move in circular patterns around the mist cloud center.
 */
public class GhostlyCloneEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Float> DATA_OPACITY =
        SynchedEntityData.defineId(GhostlyCloneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_PLAYER_NAME =
        SynchedEntityData.defineId(GhostlyCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEXTURE_PATH =
        SynchedEntityData.defineId(GhostlyCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_IS_PLAYER =
        SynchedEntityData.defineId(GhostlyCloneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_SWINGING =
        SynchedEntityData.defineId(GhostlyCloneEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifetime = 0;
    private int maxLifetime = 200; // 10 seconds
    private float fadeSpeed = 0.01f; // Very slow fade speed for smooth, gradual transitions (0->0.4 takes 40 ticks = 2 seconds)
    private boolean fadingIn = false; // Start invisible
    private int swingAnimationTicks = 0; // Track swing animation duration
    private float opacityPhaseOffset = 0.0f; // Random offset for opacity cycling (0.0 to 1.0)
    private int invisibleTicks = 0; // Ticks to stay invisible before fading in

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

    public GhostlyCloneEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Can pass through blocks
        this.setInvulnerable(true);
        this.setNoAi(true); // Disable AI since it's just visual
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public GhostlyCloneEntity(Level level, LivingEntity original, int maxLifetime, Vec3 centerPos) {
        this(ModEntities.GHOSTLY_CLONE.get(), level);
        this.maxLifetime = maxLifetime;
        this.centerPos = centerPos;
        this.ownerUUID = original.getUUID();

        // Random circular motion parameters
        this.circleRadius = 3.0 + level.random.nextDouble() * 12.0; // 3-15 blocks from center
        this.circleAngle = level.random.nextDouble() * Math.PI * 2; // Random starting angle
        this.angularSpeed = (level.random.nextDouble() * 0.03 + 0.02) * (level.random.nextBoolean() ? 1 : -1); // FASTER: 0.02-0.05 radians/tick for more visible walking
        this.verticalOffset = level.random.nextDouble() * 2.0; // 0 to +2 blocks above ground level

        // Random opacity offset (0.0 to 1.0) so clones don't fade in sync
        this.opacityPhaseOffset = (float) level.random.nextDouble();

        // Random initial invisible duration (40-80 ticks = 2-4 seconds)
        this.invisibleTicks = 40 + level.random.nextInt(40);

        // Start invisible (fadingIn will be triggered after invisibleTicks)
        this.fadingIn = false;

        // Calculate initial position on the circular path (prevents zooming on spawn)
        double spawnX = centerPos.x + Math.cos(this.circleAngle) * this.circleRadius;
        double spawnZ = centerPos.z + Math.sin(this.circleAngle) * this.circleRadius;

        // Find ground level at spawn position
        double groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)spawnX, (int)spawnZ);
        double spawnY = groundY + this.verticalOffset;

        // Spawn at calculated position on the circle
        this.setPos(spawnX, spawnY, spawnZ);
        this.setYRot(original.getYRot());
        this.setXRot(0); // FIXED: Set pitch to 0 to prevent upside-down spawning

        // Setup texture and player info
        if (original instanceof Player player) {
            this.entityData.set(DATA_PLAYER_NAME, player.getName().getString());
            this.entityData.set(DATA_IS_PLAYER, true);
            this.entityData.set(DATA_TEXTURE_PATH, ""); // Will use player skin from name
        } else {
            this.entityData.set(DATA_PLAYER_NAME, "");
            this.entityData.set(DATA_IS_PLAYER, false);
            // Use Muichiro's texture for non-player entities
            this.entityData.set(DATA_TEXTURE_PATH, "kimetsunoyaibamultiplayer:textures/entity/tokito.png");
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

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            lifetime++;

            // Update opacity (fade in/out effect) with phase offset for desynchronization
            float currentOpacity = this.entityData.get(DATA_OPACITY);

            // Handle initial invisible period
            if (invisibleTicks > 0) {
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

            // Circular motion around center position
            if (centerPos != null) {
                // Update angle
                circleAngle += angularSpeed;

                // Calculate new position in circular pattern
                double targetX = centerPos.x + Math.cos(circleAngle) * circleRadius;
                double targetZ = centerPos.z + Math.sin(circleAngle) * circleRadius;

                // Find ground level at target position
                double groundY = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)targetX, (int)targetZ);
                double targetY = groundY + verticalOffset;

                // Smoothly move towards target position with interpolation for smooth motion
                Vec3 currentPos = this.position();
                Vec3 targetPos = new Vec3(targetX, targetY, targetZ);

                // Use faster interpolation (0.2 = 20% towards target each tick) for more visible movement
                Vec3 movement = targetPos.subtract(currentPos).scale(0.2);

                this.setDeltaMovement(movement);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

                // Track movement speed for animation system
                double movementSpeed = movement.length();
                lastMovementSpeed = movementSpeed;

                if (movementSpeed > 0.005) { // Lower threshold since movement is more visible now
                    ticksSinceLastMovement = 0;
                    // Rotate to face movement direction
                    this.setYRot((float) (Math.toDegrees(Math.atan2(movement.z, movement.x)) - 90));
                } else {
                    ticksSinceLastMovement++;
                }

                // Occasionally vary the radius for more organic movement
                if (this.random.nextInt(40) == 0) {
                    this.circleRadius += (this.random.nextDouble() - 0.5) * 2.0;
                    this.circleRadius = Math.max(2.0, Math.min(15.0, this.circleRadius)); // Clamp between 2-15 blocks
                }
            }

            // Remove after lifetime expires
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
        tag.putDouble("CircleRadius", this.circleRadius);
        tag.putDouble("CircleAngle", this.circleAngle);
        tag.putDouble("AngularSpeed", this.angularSpeed);
        tag.putDouble("VerticalOffset", this.verticalOffset);
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
        }
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // Use 5 tick transition for smooth but responsive animations
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "controller", 5, state -> {
            // Check if swinging - priority animation
            if (isSwinging()) {
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("attack"));
            }

            // Use improved movement detection with hysteresis to prevent jittering
            // Only switch to idle if we've been stopped for at least 3 ticks
            boolean shouldWalk;

            if (ticksSinceLastMovement < 3) {
                // Recently moved - use walk animation
                shouldWalk = true;
            } else if (ticksSinceLastMovement >= 3) {
                // Been idle for a while - use idle animation
                shouldWalk = false;
            } else {
                // Fallback to velocity check
                shouldWalk = state.isMoving();
            }

            if (shouldWalk) {
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("walk"));
            } else {
                return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("idle"));
            }
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
