package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.RuiBloodDemonArt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** A launched, damageable head used by Rui's Decapitate Self form. */
public class RuiDecapitatedHeadEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
        SynchedEntityData.defineId(RuiDecapitatedHeadEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> RUI_HEAD =
        SynchedEntityData.defineId(RuiDecapitatedHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETURNING =
        SynchedEntityData.defineId(RuiDecapitatedHeadEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int FLIGHT_TICKS = 40;
    public static final int RETURN_TICKS = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUuid;
    private boolean returning;
    private int returnTick;
    private Vec3 returnStart = Vec3.ZERO;

    public RuiDecapitatedHeadEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(false);
        setSilent(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public static RuiDecapitatedHeadEntity create(Level level, LivingEntity owner) {
        RuiDecapitatedHeadEntity head = new RuiDecapitatedHeadEntity(ModEntities.RUI_DECAPITATED_HEAD.get(), level);
        head.ownerUuid = owner.getUUID();
        head.entityData.set(OWNER, Optional.of(owner.getUUID()));
        head.entityData.set(RUI_HEAD, owner instanceof RuiEntity);
        Vec3 anchor = RuiBloodDemonArt.getHeadAnchor(owner);
        head.setPos(anchor.x, anchor.y, anchor.z);
        Vec3 sideways = new Vec3(level.random.nextDouble() * 2.0D - 1.0D, 0.0D,
            level.random.nextDouble() * 2.0D - 1.0D);
        if (sideways.lengthSqr() < 1.0E-4D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D);
        }
        sideways = sideways.normalize().scale(0.28D);
        head.setDeltaMovement(sideways.x, 0.62D, sideways.z);
        return head;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, Optional.empty());
        entityData.define(RUI_HEAD, false);
        entityData.define(RETURNING, false);
    }

    @Nullable
    public LivingEntity getOwnerEntity() {
        UUID uuid = getOwnerUuid();
        if (uuid == null) {
            return null;
        }
        if (level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(uuid) instanceof LivingEntity living ? living : null;
        }
        Player player = level().getPlayerByUUID(uuid);
        if (player != null) {
            return player;
        }
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(128.0D),
            living -> uuid.equals(living.getUUID())).stream().findFirst().orElse(null);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return entityData.get(OWNER).orElse(ownerUuid);
    }

    public boolean isRuiHead() {
        return entityData.get(RUI_HEAD);
    }

    public boolean isReturning() {
        return entityData.get(RETURNING);
    }

    public float getReturnProgress(float partialTick) {
        return Math.min(1.0F, (returnTick + partialTick) / (float) RETURN_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwnerEntity();
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        if (level().isClientSide) {
            return;
        }

        if (!returning && tickCount >= FLIGHT_TICKS) {
            returning = true;
            returnTick = 0;
            returnStart = position();
            entityData.set(RETURNING, true);
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        }

        if (!returning) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            move(MoverType.SELF, getDeltaMovement());
            Vec3 motion = getDeltaMovement();
            if (onGround()) {
                setDeltaMovement(motion.x * 0.62D, 0.0D, motion.z * 0.62D);
            } else {
                setDeltaMovement(motion.x * 0.99D, motion.y, motion.z * 0.99D);
            }
            return;
        }

        Vec3 anchor = RuiBloodDemonArt.getHeadAnchor(owner);
        returnTick++;
        setPos(returnStart.lerp(anchor, Math.min(1.0D, returnTick / (double) RETURN_TICKS)));
        setDeltaMovement(Vec3.ZERO);
        if (returnTick >= RETURN_TICKS) {
            RuiBloodDemonArt.completeDecapitation(owner, this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        LivingEntity owner = getOwnerEntity();
        return owner != null && owner.isAlive()
            && RuiBloodDemonArt.damageThroughDecapitatedHead(owner, this, source, amount);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getOwnerUuid() != null) {
            tag.putUUID("Owner", getOwnerUuid());
        }
        tag.putBoolean("RuiHead", isRuiHead());
        tag.putBoolean("Returning", returning);
        tag.putInt("ReturnTick", returnTick);
        tag.putDouble("ReturnStartX", returnStart.x);
        tag.putDouble("ReturnStartY", returnStart.y);
        tag.putDouble("ReturnStartZ", returnStart.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
            entityData.set(OWNER, Optional.of(ownerUuid));
        }
        entityData.set(RUI_HEAD, tag.getBoolean("RuiHead"));
        returning = tag.getBoolean("Returning");
        entityData.set(RETURNING, returning);
        returnTick = tag.getInt("ReturnTick");
        returnStart = new Vec3(tag.getDouble("ReturnStartX"), tag.getDouble("ReturnStartY"), tag.getDouble("ReturnStartZ"));
    }
}
