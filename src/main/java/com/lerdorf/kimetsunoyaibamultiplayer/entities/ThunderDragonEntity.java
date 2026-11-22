package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Temporary visual entity used by Thunder Breathing's Flaming Thunder God. */
public final class ThunderDragonEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(ThunderDragonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
        SynchedEntityData.defineId(ThunderDragonEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ThunderDragonEntity(EntityType<? extends ThunderDragonEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setInvulnerable(true);
        setNoAi(true);
        setNoGravity(true);
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public static ThunderDragonEntity create(Level level, net.minecraft.world.entity.LivingEntity owner,
                                             Vec3 position, int lifetime) {
        ThunderDragonEntity dragon = new ThunderDragonEntity(ModEntities.THUNDER_DRAGON.get(), level);
        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.inheritVisual(dragon);
        dragon.setPos(position.x, position.y, position.z);
        dragon.entityData.set(DATA_OWNER, Optional.ofNullable(owner == null ? null : owner.getUUID()));
        dragon.entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
        if (owner != null) {
            dragon.setYRot(owner.getYRot());
            dragon.setXRot(owner.getXRot());
        }
        dragon.setOldPosAndRot();
        return dragon;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_OWNER, Optional.empty());
        entityData.define(DATA_LIFETIME, 80);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= entityData.get(DATA_LIFETIME)) {
            discard();
        }
    }

    public UUID getOwnerUuid() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (getOwnerUuid() != null) {
            tag.putUUID("Owner", getOwnerUuid());
        }
        tag.putInt("Lifetime", entityData.get(DATA_LIFETIME));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
        }
        if (tag.contains("Lifetime")) {
            entityData.set(DATA_LIFETIME, Math.max(1, tag.getInt("Lifetime")));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
