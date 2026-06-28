package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.FamiliarEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public class OrochiEntity extends Wolf implements GeoEntity {
    private static final EntityDataAccessor<Integer> JUMP_ANIMATION_TICKS =
        SynchedEntityData.defineId(OrochiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_ANIMATION_TICKS =
        SynchedEntityData.defineId(OrochiEntity.class, EntityDataSerializers.INT);
    private static final int JUMP_ANIMATION_MIN_TICKS = 8;
    private static final int ATTACK_ANIMATION_DURATION = 16;
    private static final float ATTACK_DAMAGE = 3.0F;
    private static final int POISON_DURATION_TICKS = 20 * 5;
    private static final int MOUNT_TOGGLE_COOLDOWN_TICKS = 20;
    private static final double OWNER_TELEPORT_DISTANCE = 50.0D;
    private static final double OWNER_TELEPORT_DISTANCE_SQR = OWNER_TELEPORT_DISTANCE * OWNER_TELEPORT_DISTANCE;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean awaitingEggOwner;
    private boolean wasOnGround = true;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;
    private long lastMountToggleGameTime = Long.MIN_VALUE;

    public OrochiEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 50.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.32D)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.ARMOR, 20.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(JUMP_ANIMATION_TICKS, 0);
        this.entityData.define(ATTACK_ANIMATION_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new OrochiMeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.05D, 3.0F, 1.5F, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
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

        if (!this.level().isClientSide) {
            if (this.awaitingEggOwner) {
                Player nearestPlayer = this.level().getNearestPlayer(this, 8.0D);
                if (nearestPlayer != null) {
                    this.tame(nearestPlayer);
                    this.setOrderedToSit(false);
                    this.awaitingEggOwner = false;
                }
            }

            if (this.isPassenger()) {
                this.setTarget(null);
                this.getNavigation().stop();
                this.setOrderedToSit(false);
                this.rememberOwnerPosition(this.getOwner());
            } else {
                this.tickOwnerTeleport();
            }

            boolean onGround = this.onGround();
            if (this.wasOnGround && !onGround && !this.isPassenger()) {
                this.entityData.set(JUMP_ANIMATION_TICKS, JUMP_ANIMATION_MIN_TICKS);
            } else if (onGround && !this.wasOnGround) {
                this.entityData.set(JUMP_ANIMATION_TICKS, 0);
            } else {
                int jumpTicks = this.entityData.get(JUMP_ANIMATION_TICKS);
                if (jumpTicks > 0 && !onGround) {
                    this.entityData.set(JUMP_ANIMATION_TICKS, jumpTicks - 1);
                }
            }
            this.wasOnGround = onGround;

            int attackTicks = this.entityData.get(ATTACK_ANIMATION_TICKS);
            if (attackTicks > 0) {
                this.entityData.set(ATTACK_ANIMATION_TICKS, attackTicks - 1);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (EntityConfig.orochiImmuneToDamage) {
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
    public boolean isPickable() {
        return !this.isPassenger() && super.isPickable();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isPassenger() && super.canBeCollidedWith();
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !this.isPassenger() && super.canCollideWith(entity);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.isPassenger()) {
            return false;
        }
        boolean result = false;
        if (target instanceof LivingEntity livingTarget) {
            float damage = DamageCalculator.calculateScaledDamage(this, ATTACK_DAMAGE);
            result = livingTarget.hurt(DamageCalculator.getDamageSource(this), damage);
            if (result) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 0), this);
            }
        }
        if (result && !this.level().isClientSide) {
            this.entityData.set(ATTACK_ANIMATION_TICKS, ATTACK_ANIMATION_DURATION);
        }
        return result;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer
            && player.isShiftKeyDown() && this.isOwnedBy(player)) {
            return this.mountOwner(serverPlayer) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer && this.isOwnedBy(player)) {
            com.lerdorf.kimetsunoyaibamultiplayer.quest.PlayerRole role =
                com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService.resolveRoleForProgression(serverPlayer);
            if (com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager.handleQuestEntityInteract(serverPlayer, role, "Orochi")) {
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    public boolean mountOwner(ServerPlayer owner) {
        if (this.level().isClientSide || !this.isOwnedBy(owner) || !this.isAlive() || owner.isRemoved()) {
            return false;
        }
        if (!this.canToggleMountState()) {
            return false;
        }
        if (this.getVehicle() == owner) {
            syncPassengerState(owner);
            this.markMountToggle();
            return true;
        }

        this.stopRiding();
        this.setOrderedToSit(false);
        this.setTarget(null);
        this.getNavigation().stop();
        this.teleportTo(owner.getX(), owner.getY() + owner.getBbHeight(), owner.getZ());
        this.setDeltaMovement(Vec3.ZERO);

        boolean mounted = this.startRiding(owner, true);
        if (mounted) {
            this.refreshDimensions();
            syncPassengerState(owner);
            this.markMountToggle();
        }
        return mounted;
    }

    private void syncPassengerState(ServerPlayer owner) {
        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(owner);
        owner.connection.send(packet);
        if (owner.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(owner, packet);
        }
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        if (vehicle instanceof Player player && this.isOwnedBy(player)) {
            return true;
        }
        return super.canRide(vehicle);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.CHICKEN)
            || stack.is(Items.RABBIT)
            || stack.is(Items.SALMON)
            || stack.is(Items.COD)
            || stack.is(Items.COOKED_CHICKEN)
            || stack.is(Items.COOKED_RABBIT)
            || stack.is(Items.COOKED_SALMON)
            || stack.is(Items.COOKED_COD);
    }

    public boolean isJumpAnimating() {
        return this.entityData.get(JUMP_ANIMATION_TICKS) > 0 || (!this.onGround() && !this.isPassenger());
    }

    public boolean isAttackAnimating() {
        return this.entityData.get(ATTACK_ANIMATION_TICKS) > 0;
    }

    public boolean dismountToSafeLocation() {
        Entity vehicle = this.getVehicle();
        if (vehicle == null) {
            return false;
        }
        if (!this.canToggleMountState()) {
            return false;
        }

        Level level = this.level();
        BlockPos origin = vehicle.blockPosition();
        this.stopRiding();
        if (vehicle instanceof ServerPlayer serverPlayer) {
            syncPassengerState(serverPlayer);
        }
        this.refreshDimensions();
        this.setTarget(null);

        BlockPos safePos = findSafeDismountPos(level, origin);
        if (safePos == null) {
            safePos = origin;
        }

        this.teleportTo(safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D);
        this.setDeltaMovement(Vec3.ZERO);
        this.markMountToggle();
        return true;
    }

    public boolean forceDismountToSafeLocation(ServerPlayer owner) {
        if (this.level().isClientSide || owner == null || !this.isOwnedBy(owner) || !this.isAlive()) {
            return false;
        }
        if (!this.canToggleMountState()) {
            return false;
        }

        Level level = this.level();
        BlockPos origin = owner.blockPosition();
        this.stopRiding();
        syncPassengerState(owner);
        this.refreshDimensions();
        this.setTarget(null);

        BlockPos safePos = findSafeDismountPos(level, origin);
        if (safePos == null) {
            safePos = origin;
        }

        this.teleportTo(safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D);
        this.setDeltaMovement(Vec3.ZERO);
        this.markMountToggle();
        return true;
    }

    @Nullable
    public static OrochiEntity findOwnedOrochi(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return null;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            for (Entity passenger : player.getPassengers()) {
                if (passenger instanceof OrochiEntity orochi
                    && orochi.isAlive()
                    && orochi.isTame()
                    && player.getUUID().equals(orochi.getOwnerUUID())) {
                    return orochi;
                }
            }

            AABB worldBounds = new AABB(
                -30000000.0D,
                serverLevel.getMinBuildHeight(),
                -30000000.0D,
                30000000.0D,
                serverLevel.getMaxBuildHeight(),
                30000000.0D
            );
            return serverLevel.getEntitiesOfClass(
                    OrochiEntity.class,
                    worldBounds,
                    candidate -> candidate.isAlive()
                        && candidate.isTame()
                        && player.getUUID().equals(candidate.getOwnerUUID()))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        }

        return null;
    }

    public static int discardOwnedOrochi(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }

        AABB worldBounds = new AABB(
            -30000000.0D,
            serverLevel.getMinBuildHeight(),
            -30000000.0D,
            30000000.0D,
            serverLevel.getMaxBuildHeight(),
            30000000.0D
        );
        List<OrochiEntity> owned = serverLevel.getEntitiesOfClass(
            OrochiEntity.class,
            worldBounds,
            candidate -> candidate.isAlive()
                && candidate.isTame()
                && player.getUUID().equals(candidate.getOwnerUUID())
        );

        int removed = 0;
        for (OrochiEntity orochi : owned) {
            if (!orochi.isRemoved()) {
                orochi.discard();
                removed++;
            }
        }
        return removed;
    }

    @Nullable
    public static OrochiEntity resolveOwnedOrochi(ServerPlayer player, int preferredEntityId) {
        if (player == null || player.level().isClientSide()) {
            return null;
        }

        if (preferredEntityId >= 0 && player.level().getEntity(preferredEntityId) instanceof OrochiEntity preferred
            && player.getUUID().equals(preferred.getOwnerUUID())) {
            return preferred;
        }

        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof OrochiEntity orochi && player.getUUID().equals(orochi.getOwnerUUID())) {
                return orochi;
            }
        }

        return player.serverLevel().getEntitiesOfClass(
                OrochiEntity.class,
                player.getBoundingBox().inflate(16.0D),
                candidate -> player.getUUID().equals(candidate.getOwnerUUID()))
            .stream()
            .min((left, right) -> Double.compare(left.distanceToSqr(player), right.distanceToSqr(player)))
            .orElse(null);
    }

    private boolean canToggleMountState() {
        if (this.level().isClientSide) {
            return false;
        }

        long currentGameTime = this.level().getGameTime();
        return this.lastMountToggleGameTime == Long.MIN_VALUE
            || currentGameTime - this.lastMountToggleGameTime >= MOUNT_TOGGLE_COOLDOWN_TICKS;
    }

    private long getMountToggleCooldownRemaining() {
        if (this.level().isClientSide || this.lastMountToggleGameTime == Long.MIN_VALUE) {
            return 0L;
        }

        long elapsed = this.level().getGameTime() - this.lastMountToggleGameTime;
        return Math.max(0L, MOUNT_TOGGLE_COOLDOWN_TICKS - elapsed);
    }

    private void markMountToggle() {
        this.lastMountToggleGameTime = this.level().getGameTime();
    }

    public long getMountToggleCooldownRemainingTicks() {
        return this.getMountToggleCooldownRemaining();
    }

    private void tickOwnerTeleport() {
        LivingEntity owner = this.getOwner();
        if (!(owner instanceof ServerPlayer serverPlayer) || owner.isSpectator()) {
            return;
        }

        boolean ownerMovedMoreThanTeleportThreshold = hasRememberedOwnerPosition()
            && distanceToRememberedOwnerPositionSqr(serverPlayer) > OWNER_TELEPORT_DISTANCE_SQR;
        boolean tooFarFromOwner = this.distanceToSqr(serverPlayer) > OWNER_TELEPORT_DISTANCE_SQR;

        if (ownerMovedMoreThanTeleportThreshold || tooFarFromOwner) {
            teleportNearOwner(serverPlayer);
        }

        rememberOwnerPosition(serverPlayer);
    }

    private boolean hasRememberedOwnerPosition() {
        return !Double.isNaN(this.lastOwnerX) && !Double.isNaN(this.lastOwnerY) && !Double.isNaN(this.lastOwnerZ);
    }

    private double distanceToRememberedOwnerPositionSqr(LivingEntity owner) {
        double dx = owner.getX() - this.lastOwnerX;
        double dy = owner.getY() - this.lastOwnerY;
        double dz = owner.getZ() - this.lastOwnerZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private void rememberOwnerPosition(@Nullable LivingEntity owner) {
        if (owner == null) {
            return;
        }
        this.lastOwnerX = owner.getX();
        this.lastOwnerY = owner.getY();
        this.lastOwnerZ = owner.getZ();
    }

    private void teleportNearOwner(ServerPlayer owner) {
        BlockPos safePos = FamiliarEntityHelper.findNearestSafeTeleportPosition(owner.level(), owner.blockPosition());
        double x = owner.getX();
        double y = owner.getY();
        double z = owner.getZ();
        if (safePos != null) {
            x = safePos.getX() + 0.5D;
            y = safePos.getY();
            z = safePos.getZ() + 0.5D;
        }

        this.stopRiding();
        this.setTarget(null);
        this.getNavigation().stop();
        this.moveTo(x, y, z, this.getYRot(), this.getXRot());
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Nullable
    private static BlockPos findSafeDismountPos(Level level, BlockPos origin) {
        for (int radius = 1; radius <= 4; radius++) {
            for (BlockPos.MutableBlockPos mutable : BlockPos.spiralAround(origin, radius, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH)) {
                BlockPos candidate = mutable.immutable();
                if (isSafeDismountPos(level, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isSafeDismountPos(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
            && head.getCollisionShape(level, pos.above()).isEmpty()
            && !floor.getCollisionShape(level, pos.below()).isEmpty()
            && !level.getFluidState(pos).isSource();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SILVERFISH_STEP, 0.08F, this.getVoicePitch() * 1.35F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isPassenger()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("shoulder"));
            }
            if (this.isJumpAnimating()) {
                return state.setAndContinue(RawAnimation.begin().then("jump", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
            if (this.isAttackAnimating()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("attack"));
            }
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
    public OrochiEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        OrochiEntity baby = ModEntities.OROCHI.get().create(serverLevel);
        if (baby != null && ageableMob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            baby.setOwnerUUID(ownable.getOwnerUUID());
            baby.setTame(true);
        }
        return baby;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);
        int count = 1 + this.random.nextInt(3);
        this.spawnAtLocation(new ItemStack(ModAlchemyItems.OROCHI_SCALES.get(), count));
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.isPassenger()) {
            return EntityDimensions.fixed(0.01F, 0.01F);
        }
        return super.getDimensions(pose);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("AwaitingEggOwner", this.awaitingEggOwner);
        tag.putBoolean("WasOnGround", this.wasOnGround);
        if (this.hasRememberedOwnerPosition()) {
            tag.putDouble("LastOwnerX", this.lastOwnerX);
            tag.putDouble("LastOwnerY", this.lastOwnerY);
            tag.putDouble("LastOwnerZ", this.lastOwnerZ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.awaitingEggOwner = tag.getBoolean("AwaitingEggOwner");
        this.wasOnGround = !tag.contains("WasOnGround") || tag.getBoolean("WasOnGround");
        if (tag.contains("LastOwnerX")) {
            this.lastOwnerX = tag.getDouble("LastOwnerX");
            this.lastOwnerY = tag.getDouble("LastOwnerY");
            this.lastOwnerZ = tag.getDouble("LastOwnerZ");
        }
    }

    private static class OrochiMeleeAttackGoal extends MeleeAttackGoal {
        private final OrochiEntity orochi;

        private OrochiMeleeAttackGoal(OrochiEntity orochi, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(orochi, speedModifier, followingTargetEvenIfNotSeen);
            this.orochi = orochi;
        }

        @Override
        public boolean canUse() {
            return !this.orochi.isPassenger() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.orochi.isPassenger() && super.canContinueToUse();
        }
    }
}
