package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.PlayerRole;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class EyeFamiliarEntity extends Wolf implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean awaitingEggOwner;

    public EyeFamiliarEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 50.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.ATTACK_DAMAGE, 1.0D)
            .add(Attributes.ARMOR, 20.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.05D, 3.0F, 1.5F, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.setPersistenceRequired();
        this.setHealth(this.getMaxHealth());
        this.awaitingEggOwner = reason == MobSpawnType.SPAWN_EGG && !this.isTame();
        return data;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.awaitingEggOwner) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 8.0D);
            if (nearestPlayer != null) {
                this.tame(nearestPlayer);
                this.setOrderedToSit(false);
                this.awaitingEggOwner = false;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (EntityConfig.eyeFamiliarImmuneToDamage) {
            return false;
        }
        if (this.isMountedOnOwnerPlayer() || source.is(DamageTypes.DROWN) || this.isImmuneToDemonPlayerDamage(source)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    private boolean isMountedOnOwnerPlayer() {
        return this.getVehicle() instanceof Player player && this.isOwnedBy(player);
    }

    private boolean isImmuneToDemonPlayerDamage(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && Damager.isDemon(player)) {
            return true;
        }

        Entity direct = source.getDirectEntity();
        return direct instanceof Player player && Damager.isDemon(player);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown() && this.isOwnedBy(player)) {
            return this.openMugenDoor(serverPlayer) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer && this.isOwnedBy(player)) {
            PlayerRole role = MeditationMenuService.resolveRoleForProgression(serverPlayer);
            if (QuestProgressionManager.handleQuestEntityInteract(serverPlayer, role, "Eye Familiar")) {
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    public boolean openMugenDoor(ServerPlayer owner) {
        if (this.level().isClientSide || !this.isOwnedBy(owner)) {
            return false;
        }

        MugenDoorEntity door = MugenDoorEntity.createForcedTeleportation(this.level(), this.blockPosition());
        this.level().addFreshEntity(door);
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public EyeFamiliarEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        EyeFamiliarEntity baby = ModEntities.EYE_FAMILIAR.get().create(serverLevel);
        if (baby != null && ageableMob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            baby.setOwnerUUID(ownable.getOwnerUUID());
            baby.setTame(true);
        }
        return baby;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("AwaitingEggOwner", this.awaitingEggOwner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.awaitingEggOwner = tag.getBoolean("AwaitingEggOwner");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMITE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENDERMITE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMITE_DEATH;
    }
}
