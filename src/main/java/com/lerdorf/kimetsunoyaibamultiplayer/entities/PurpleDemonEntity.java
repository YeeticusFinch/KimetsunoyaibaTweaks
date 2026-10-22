package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Generic demon using the demon 0 biped model and one of its three sprint styles. */
public class PurpleDemonEntity extends NamedDemonEntity {
    private static final String TYPE_TAG = "Type";
    private static final float BITE_CHANCE = 0.25F;
    private static final int BITE_ANIMATION_TICKS = 15;
    private static final int BITE_IMPACT_DELAY_TICKS = 5;
    private static final float BITE_DAMAGE_MULTIPLIER = 1.5F;
    private static final int REGULAR_ATTACK_ANIMATION_TICKS = 10;
    private static final int REGULAR_ATTACK_GUARD_TICKS = 10;
    private static final double REGULAR_ATTACK_GUARD_POWER = 10.0D;
    private static final String SWORD_LEFT_TAG = "KnYPurpleDemonSwordLeft";
    private static final EntityDataAccessor<Integer> DEMON_TYPE =
        SynchedEntityData.defineId(PurpleDemonEntity.class, EntityDataSerializers.INT);

    public PurpleDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide) {
            setDemonType(1 + this.getRandom().nextInt(3));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NamedDemonEntity.createAttributes();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEMON_TYPE, 1);
    }

    public int getDemonType() {
        return this.entityData.get(DEMON_TYPE);
    }

    private void setDemonType(int type) {
        this.entityData.set(DEMON_TYPE, Math.max(1, Math.min(3, type)));
    }

    @Override
    protected String resolveSprintAnimation() {
        return switch (getDemonType()) {
            case 2 -> "sprint_demon2";
            case 3 -> "sprint_demon3";
            default -> "sprint_demon";
        };
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return "sprint_demon".equals(animation)
            || "sprint_demon2".equals(animation)
            || "sprint_demon3".equals(animation);
    }

    @Override
    public ResourceLocation getSkinTexture() {
        return ResourceLocation.fromNamespaceAndPath(
            "kimetsunoyaibamultiplayer", "textures/entity/purple_demon_0.png");
    }

    @Override
    public ResourceLocation getEyesTexture() {
        return ResourceLocation.fromNamespaceAndPath(
            "kimetsunoyaibamultiplayer", "textures/entity/purple_demon_0_eyes.png");
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity livingTarget
            && livingTarget.isAlive()
            && getRandom().nextFloat() < BITE_CHANCE) {
            startBite(livingTarget);
            return true;
        }
        return performRegularAttack(target);
    }

    private boolean performRegularAttack(Entity target) {
        String animation = chooseRegularAttackAnimation();
        playGeckoAnimation(animation, REGULAR_ATTACK_ANIMATION_TICKS);
        GuardStateHelper.setGuardState(this, REGULAR_ATTACK_GUARD_POWER, 0.0D, false);
        AbilityScheduler.scheduleOnce(this,
            () -> GuardStateHelper.clearGuardState(this), REGULAR_ATTACK_GUARD_TICKS);

        if (!level().isClientSide) {
            ModNetworking.sendToAllClients(new MobSwordSlashPacket(
                getUUID(), animation, 0, "claw"));
            level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return super.doHurtTarget(target);
    }

    private String chooseRegularAttackAnimation() {
        if (getRandom().nextInt(100) < 8) {
            return "sword_overhead";
        }

        boolean lastWasLeft = getPersistentData().getBoolean(SWORD_LEFT_TAG);
        getPersistentData().putBoolean(SWORD_LEFT_TAG, !lastWasLeft);
        return lastWasLeft ? "sword_to_right" : "sword_to_left";
    }

    private void startBite(LivingEntity target) {
        getNavigation().stop();
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        playGeckoAnimation("bite2", BITE_ANIMATION_TICKS);

        AbilityScheduler.scheduleOnce(this, () -> {
            playSound(SoundEvents.PLAYER_BURP, 1.0F, 0.9F + getRandom().nextFloat() * 0.2F);
            if (target.isAlive()) {
                Damager.hurt(this, target,
                    (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * BITE_DAMAGE_MULTIPLIER));
            }
        }, BITE_IMPACT_DELAY_TICKS);
    }

    @Override
    protected boolean hasAnimationOverlay() {
        return true;
    }

    @Override
    protected String getAnimationOverlay() {
        return getAnimationTicks() > 0 && "bite2".equals(getCurrentAnimation()) ? "bite2" : null;
    }

    @Override
    protected boolean isBaseMovementAnimation(String animation) {
        return "bite2".equals(animation) || super.isBaseMovementAnimation(animation);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType reason, SpawnGroupData spawnData, CompoundTag dataTag) {
        SpawnGroupData finalized = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (dataTag != null && dataTag.contains(TYPE_TAG)) {
            setDemonType(dataTag.getInt(TYPE_TAG));
        }
        return finalized;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TYPE_TAG, getDemonType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TYPE_TAG)) {
            setDemonType(tag.getInt(TYPE_TAG));
        }
    }
}
