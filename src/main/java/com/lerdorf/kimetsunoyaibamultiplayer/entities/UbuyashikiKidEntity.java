package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * Shared passive behavior for Ubuyashiki children (Kanata/Kiriya).
 * They never aggro on their own, but nearby demon slayers defend them when attacked.
 */
public abstract class UbuyashikiKidEntity extends PathfinderMob implements GeoEntity {
    private static final float MAX_HP = 40.0F;
    private static final double AGGRO_CALL_RANGE = 32.0D;
    private static final ResourceLocation MT_FUJIKASANE_DIM_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "mt_fujikasane");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos guardHomePos = null;

    protected UbuyashikiKidEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HP)
            .add(Attributes.MOVEMENT_SPEED, 0.18D)
            .add(Attributes.ARMOR, 0.0D)
            .add(Attributes.FOLLOW_RANGE, 16.0D)
            .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    public void setGuardHomePosition(BlockPos pos) {
        this.guardHomePos = pos;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        this.setHealth(MAX_HP);
        this.setPersistenceRequired();

        // Explicitly ensure no armor/weapon.
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.2F);
        }

        return spawnData;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);
        if (!wasHurt || this.level().isClientSide()) {
            return wasHurt;
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity livingAttacker && livingAttacker.isAlive()) {
            DemonSlayerAggroHandler.alertNearbySlayersToAttacker(this, livingAttacker, AGGRO_CALL_RANGE);
        }

        return wasHurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }

            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk_female"));
            }

            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || guardHomePos == null) {
            return;
        }
        if (!this.level().dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
            return;
        }

        double targetX = guardHomePos.getX() + 0.5D;
        double targetY = guardHomePos.getY();
        double targetZ = guardHomePos.getZ() + 0.5D;
        double distanceSq = this.distanceToSqr(targetX, targetY, targetZ);

        if (distanceSq > 4.0D) {
            this.getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
        } else if (!this.getNavigation().isDone()) {
            this.getNavigation().stop();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (guardHomePos != null) {
            tag.putBoolean("HasGuardHomePos", true);
            tag.putInt("GuardHomeX", guardHomePos.getX());
            tag.putInt("GuardHomeY", guardHomePos.getY());
            tag.putInt("GuardHomeZ", guardHomePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("HasGuardHomePos")) {
            guardHomePos = new BlockPos(tag.getInt("GuardHomeX"), tag.getInt("GuardHomeY"), tag.getInt("GuardHomeZ"));
        } else {
            guardHomePos = null;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
