package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
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

public class BeastSlashesEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<String> DATA_ANIMATION =
        SynchedEntityData.defineId(BeastSlashesEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TEXTURE_VARIANT =
        SynchedEntityData.defineId(BeastSlashesEntity.class, EntityDataSerializers.INT);

    private int ticksAlive = 0;
    private int lifetime = 15;

    public BeastSlashesEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setSilent(true);
        this.setNoGravity(true);
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
        this.entityData.define(DATA_TEXTURE_VARIANT, 0);
    }

    public static BeastSlashesEntity create(Level level, Vec3 position, float yaw, float pitch, String animationName, int lifetimeTicks) {
        BeastSlashesEntity entity = new BeastSlashesEntity(ModEntities.BEAST_SLASHES.get(), level);
        entity.setPos(position.x, position.y, position.z);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.setAnimation(animationName);
        entity.lifetime = lifetimeTicks;
        entity.randomizeTextureVariant(level.random);
        return entity;
    }

    public void setAnimation(String animationName) {
        if ("beasst".equalsIgnoreCase(animationName)) {
            this.entityData.set(DATA_ANIMATION, "base");
            return;
        }
        this.entityData.set(DATA_ANIMATION, animationName);
    }

    public String getAnimation() {
        return this.entityData.get(DATA_ANIMATION);
    }

    public int getTextureVariant() {
        return this.entityData.get(DATA_TEXTURE_VARIANT);
    }

    public void randomizeTextureVariant(RandomSource random) {
        this.entityData.set(DATA_TEXTURE_VARIANT, random.nextInt(3));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksAlive = tag.getInt("TicksAlive");
        this.lifetime = tag.getInt("Lifetime");
        if (tag.contains("Animation")) {
            this.setAnimation(tag.getString("Animation"));
        }
        if (tag.contains("TextureVariant")) {
            this.entityData.set(DATA_TEXTURE_VARIANT, tag.getInt("TextureVariant"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putInt("Lifetime", this.lifetime);
        tag.putString("Animation", this.getAnimation());
        tag.putInt("TextureVariant", this.getTextureVariant());
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            String animationResourceName = getAnimation();
            state.getController().setAnimation(RawAnimation.begin()
                .then(animationResourceName, software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE));
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
    }

    @Override
    public void move(net.minecraft.world.entity.MoverType type, Vec3 pos) {
    }

    @Override
    public void tick() {
        this.baseTick();
        this.setNoGravity(true);
        this.setDeltaMovement(0, 0, 0);
        this.setOnGround(true);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        if (!level().isClientSide) {
            ticksAlive++;
            if (ticksAlive >= lifetime) {
                this.discard();
            }
        }
    }
}
