package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.List;
import java.util.UUID;

/**
 * Swamp Hand entity - temporary attack effect for Swamp Demon Art.
 *
 * Timeline:
 * - 0 ticks: Spawn, start "attack" animation
 * - 10 ticks: Deal 5 damage to all living entities within 2 blocks
 * - 20 ticks: Animation complete, despawn
 *
 * Features:
 * - Owner-based damage scaling using Damager.calculateScaledDamage()
 * - Invulnerable, no physics, purely visual
 * - GeoLib animated entity
 */
public class SwampHandEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Synced data for animation state
    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
        SynchedEntityData.defineId(SwampHandEntity.class, EntityDataSerializers.BOOLEAN);

    // Owner tracking for damage scaling
    private UUID ownerUUID = null;
    private LivingEntity cachedOwner = null;

    // Animation timing constants (in ticks)
    private static final int ATTACK_ANIMATION_DURATION = 20; // 20 ticks = 1 second
    private static final int DAMAGE_TRIGGER_TICK = 10; // Deal damage at tick 10

    private int ticksAlive = 0;
    private boolean damageDealt = false;

    public SwampHandEntity(EntityType<? extends Mob> type, Level level) {
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
        this.entityData.define(DATA_ATTACKING, false);
    }

    /**
     * Create a swamp hand entity at a specific position with an optional owner.
     *
     * @param level The world
     * @param position The spawn position
     * @param owner The owner entity (can be null for no owner)
     * @return The created swamp hand entity
     */
    public static SwampHandEntity create(Level level, net.minecraft.world.phys.Vec3 position, LivingEntity owner) {
        SwampHandEntity hand = new SwampHandEntity(ModEntities.SWAMP_HAND.get(), level);
        hand.setPos(position.x, position.y, position.z);
        if (owner != null) {
            hand.ownerUUID = owner.getUUID();
            hand.cachedOwner = owner;
        }
        return hand;
    }

    /**
     * Create a swamp hand entity at a specific position without an owner.
     */
    public static SwampHandEntity create(Level level, net.minecraft.world.phys.Vec3 position) {
        return create(level, position, null);
    }

    /**
     * Spawn a swamp hand entity at a specific location with an owner.
     * This is a convenience method that creates and spawns the entity in one call.
     *
     * @param level The world
     * @param position The spawn position
     * @param owner The owner entity (used for damage scaling and as damage source)
     */
    public static void spawn(Level level, net.minecraft.world.phys.Vec3 position, LivingEntity owner) {
        SwampHandEntity hand = create(level, position, owner);
        level.addFreshEntity(hand);
    }

    /**
     * Spawn a swamp hand entity at a specific location without an owner.
     * This is a convenience method that creates and spawns the entity in one call.
     *
     * @param level The world
     * @param position The spawn position
     */
    public static void spawn(Level level, net.minecraft.world.phys.Vec3 position) {
        spawn(level, position, null);
    }

    /**
     * Get the owner entity if available.
     */
    public LivingEntity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        if (ownerUUID != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(ownerUUID);
            if (entity instanceof LivingEntity livingOwner) {
                cachedOwner = livingOwner;
                return livingOwner;
            }
        }
        return null;
    }

    /**
     * Check if this entity has an owner.
     */
    public boolean hasOwner() {
        return ownerUUID != null;
    }

    /**
     * Get the current attacking state (synced to client)
     */
    private boolean isAttacking() {
        return this.entityData.get(DATA_ATTACKING);
    }

    /**
     * Set the current attacking state (syncs to client)
     */
    private void setAttacking(boolean attacking) {
        this.entityData.set(DATA_ATTACKING, attacking);
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
        return false; // Cannot deal damage via melee
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksAlive = tag.getInt("TicksAlive");
        this.damageDealt = tag.getBoolean("DamageDealt");
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putBoolean("DamageDealt", this.damageDealt);
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", state -> {
            return state.setAndContinue(
                RawAnimation.begin().then(
                    "kimetsunoyaibamultiplayer.swamp_hand.attack",
                    software.bernie.geckolib.core.animation.Animation.LoopType.HOLD_ON_LAST_FRAME
                )
            );
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

    // @Override
    // public void travel(net.minecraft.world.phys.Vec3 travelVector) {
    //     // Prevent any travel/movement
    // }

    // @Override
    // public void move(net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 pos) {
    //     // Prevent any movement
    // }

    @Override
    public void tick() {
        super.tick();

        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.hurtMarked = true; // optional, helps sync motion reset cleanly

        if (!level().isClientSide) {
            ticksAlive++;

            if (ticksAlive == DAMAGE_TRIGGER_TICK && !damageDealt) {
                dealDamageToNearbyEntities();
                damageDealt = true;
            }

            if (ticksAlive >= ATTACK_ANIMATION_DURATION) {
                this.discard();
            }
        }
    }

    /**
     * Deal damage to all living entities within 2 blocks of this entity.
     * Uses owner for damage scaling if available.
     */
    private void dealDamageToNearbyEntities() {
        float baseDamage = 5.0f;
        LivingEntity owner = getOwner();

        // Get all living entities within 2 blocks
        List<LivingEntity> nearbyEntities = level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(2.0), // 2 block radius
            entity -> entity != null && entity.isAlive() && entity != this
        );

        for (LivingEntity target : nearbyEntities) {
            // Skip the owner (don't damage them)
            if (owner != null && target.equals(owner)) {
                continue;
            }

            // Calculate scaled damage if we have an owner, otherwise use base damage
            float damage = baseDamage;
            if (owner != null) {
                damage = Damager.calculateScaledDamage(owner, baseDamage);
            }

            // Apply damage using the owner as the source (if available)
            LivingEntity damageSource = owner != null ? owner : this;
            Damager.hurt(damageSource, target, damage);
        }
    }
}
