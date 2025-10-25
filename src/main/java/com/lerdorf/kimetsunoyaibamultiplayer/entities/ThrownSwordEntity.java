package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Projectile entity for thrown swords (Frost Breathing Sixth Form)
 */
public class ThrownSwordEntity extends ThrowableItemProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK =
        SynchedEntityData.defineId(ThrownSwordEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_IS_GOLDEN =
        SynchedEntityData.defineId(ThrownSwordEntity.class, EntityDataSerializers.BOOLEAN);

    private int ticksInAir = 0;
    private static final int MAX_TICKS = 40; // 2 seconds

    public ThrownSwordEntity(EntityType<? extends ThrownSwordEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownSwordEntity(Level level, LivingEntity thrower, ItemStack swordStack, boolean isGolden) {
        super(ModEntities.THROWN_SWORD.get(), thrower, level);
        this.entityData.set(DATA_ITEM_STACK, swordStack.copy());
        this.entityData.set(DATA_IS_GOLDEN, isGolden);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ITEM_STACK, ItemStack.EMPTY);
        this.entityData.define(DATA_IS_GOLDEN, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD;
    }

    public ItemStack getSwordStack() {
        return this.entityData.get(DATA_ITEM_STACK);
    }

    public boolean isGolden() {
        return this.entityData.get(DATA_IS_GOLDEN);
    }

    @Override
    public void tick() {
        super.tick();

        ticksInAir++;

        // Spawn particle trail
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = this.position();
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                pos.x, pos.y, pos.z, 3, 0.15, 0.15, 0.15, 0.02);
            // Add some sparkle particles for visual effect
            if (ticksInAir % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0);
            }
        }

        // Return sword after max duration
        if (ticksInAir >= MAX_TICKS) {
            returnSwordToOwner();
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!(result.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (!(this.getOwner() instanceof LivingEntity thrower)) {
            return;
        }

        // Don't hit the thrower
        if (target == thrower) {
            return;
        }

        // Apply damage and effects
        float baseDamage = isGolden() ? 8.0F : 6.0F;
        float damage = DamageCalculator.calculateScaledDamage(thrower, baseDamage);
        Damager.hurt(thrower, target, damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2));
        target.setTicksFrozen(target.getTicksFrozen() + 400);

        if (Config.logDebug) {
            Log.debug("Thrown sword hit {} for {} damage", target.getName().getString(), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        // Return sword when hitting anything
        if (!this.level().isClientSide) {
            returnSwordToOwner();
            this.discard();
        }
    }

    private void returnSwordToOwner() {
        if (!(this.getOwner() instanceof Player player)) {
            return;
        }

        ItemStack swordStack = getSwordStack();
        if (swordStack.isEmpty()) {
            return;
        }

        // Return sword to player
        if (!player.getInventory().add(swordStack)) {
            player.drop(swordStack, false);
        }

        this.level().playSound(null, player.blockPosition(),
            SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (Config.logDebug) {
            Log.debug("Returned thrown sword to player");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("TicksInAir", ticksInAir);
        compound.putBoolean("IsGolden", isGolden());
        if (!getSwordStack().isEmpty()) {
            compound.put("SwordStack", getSwordStack().save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        ticksInAir = compound.getInt("TicksInAir");
        this.entityData.set(DATA_IS_GOLDEN, compound.getBoolean("IsGolden"));
        if (compound.contains("SwordStack")) {
            this.entityData.set(DATA_ITEM_STACK, ItemStack.of(compound.getCompound("SwordStack")));
        }
    }

    // ===== GeckoLib Animation System =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sword_controller", 0, state -> {
            // Play the spinning animation (use any of idle/walk/fly - they're all the same)
            state.getController().setAnimation(RawAnimation.begin().thenLoop("fly"));
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
