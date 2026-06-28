package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.DemonTargetingHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared base for addon demons with blood demon art access and sunlight death behavior.
 */
public abstract class AbstractDemonEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<String> CURRENT_ANIMATION =
        SynchedEntityData.defineId(AbstractDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ANIMATION_TICKS =
        SynchedEntityData.defineId(AbstractDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SUNLIGHT_BURN_TICKS =
        SynchedEntityData.defineId(AbstractDemonEntity.class, EntityDataSerializers.INT);

    private static final ResourceLocation[] MUZAN_BLOOD_ITEM_IDS = new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "muzan_blood"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "blood_of_muzan"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "bloodmuzan")
    };

    private static final java.util.Map<String, RawAnimation> LOOPING_MOVEMENT_ANIMATIONS = new java.util.HashMap<>();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int bloodDemonArtCooldownTicks = 0;
    private boolean suppressLootDrops = false;
    private int movementAnimationGraceTicks = 0;
    private boolean movementAnimationLatched = false;
    private int sprintAnimationGraceTicks = 0;
    private int sprintStateHoldTicks = 0;
    private int movementStateHoldTicks = 0;
    private int movementAnimationSwitchCooldownTicks = 0;
    private double smoothedHorizontalSpeed = 0.0D;
    private double smoothedAnimationSpeed = 1.0D;
    private String latchedMovementAnimation = "idle";

    protected AbstractDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.getPersistentData().putBoolean("oni", true);
    }

    public static AttributeSupplier.Builder createDemonAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0D)
            .add(Attributes.ATTACK_DAMAGE, 6.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_ANIMATION, "idle");
        this.entityData.define(ANIMATION_TICKS, 0);
        this.entityData.define(SUNLIGHT_BURN_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (bloodDemonArtCooldownTicks > 0) {
            bloodDemonArtCooldownTicks--;
        }

        if (!level().isClientSide) {
            tickSunlightBurn();
            DemonTargetingHelper.retargetToCloserNonDemonPlayer(this, this::canTargetNonDemonVictim);
            tickBloodDemonArt();
        }

        int animTicks = getAnimationTicks();
        if (animTicks > 0) {
            this.entityData.set(ANIMATION_TICKS, animTicks - 1);
        }
    }

    protected void tickBloodDemonArt() {
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = getBloodDemonArt();
        LivingEntity target = getTarget();
        if (art == null || target == null || !target.isAlive() || bloodDemonArtCooldownTicks > 0 || isUsingLockedAnimation()) {
            return;
        }

        double distanceSq = this.distanceToSqr(target);
        if (distanceSq > getBloodDemonArtRange() * getBloodDemonArtRange()) {
            return;
        }
        if (this.random.nextFloat() > getBloodDemonArtUseChance()) {
            return;
        }

        int index = this.random.nextInt(art.getTechnique().getFormCount());
        var form = art.getTechnique().getForm(index);
        if (form == null) {
            return;
        }

        form.execute(this, level());
        bloodDemonArtCooldownTicks = Math.max(20, form.getCooldownSeconds() * 20);
    }

    protected double getBloodDemonArtRange() {
        return 12.0D;
    }

    protected float getBloodDemonArtUseChance() {
        return 0.04F;
    }

    protected boolean canTargetNonDemonVictim(LivingEntity target) {
        return target != null && target.isAlive() && !Damager.isDemon(target);
    }

    protected boolean isUsingLockedAnimation() {
        String currentAnimation = getCurrentAnimation();
        return getAnimationTicks() > 0 && currentAnimation != null && !isBaseMovementAnimation(currentAnimation);
    }

    protected void tickSunlightBurn() {
        if (isSunlightImmune()) {
            return;
        }

        if (isInBurningSunlight()) {
            int burnTicks = this.entityData.get(SUNLIGHT_BURN_TICKS) + 1;
            this.entityData.set(SUNLIGHT_BURN_TICKS, burnTicks);
            this.setSecondsOnFire(2);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(0.5D), getZ(), 4, 0.3D, 0.4D, 0.3D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.LAVA, getX(), getY(0.2D), getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
            }

            if (burnTicks % 10 == 0 && burnTicks <= 40) {
                this.hurt(this.damageSources().onFire(), 10.0F);
            }

            if (burnTicks >= 40) {
                suppressLootDrops = true;
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(0.6D), getZ(), 12, 0.3D, 0.4D, 0.3D, 0.02D);
                }
                this.playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.1F);
                this.discard();
            }
        } else if (this.entityData.get(SUNLIGHT_BURN_TICKS) != 0) {
            this.entityData.set(SUNLIGHT_BURN_TICKS, 0);
        }
    }

    protected boolean isInBurningSunlight() {
        if (!(this.level() instanceof ServerLevel serverLevel) || !serverLevel.isDay()) {
            return false;
        }
        if (this.isInWaterRainOrBubble() || this.isUnderWater()) {
            return false;
        }
        BlockPos pos = this.blockPosition();
        return serverLevel.canSeeSky(pos) && !serverLevel.isRainingAt(pos);
    }

    protected boolean isSunlightImmune() {
        ResourceLocation entityId = EntityType.getKey(this.getType());
        return entityId != null && DemonRegistry.isSunlightImmune(entityId);
    }

    protected BloodDemonArtRegistry.RegisteredBloodDemonArt getBloodDemonArt() {
        ResourceLocation entityId = EntityType.getKey(this.getType());
        if (entityId == null) {
            return null;
        }

        DemonRegistry.RegisteredDemon demon = DemonRegistry.get(entityId);
        if (demon == null || demon.getBloodDemonArtId() == null || demon.getBloodDemonArtId().isEmpty()) {
            return null;
        }
        return BloodDemonArtRegistry.getArt(demon.getBloodDemonArtId());
    }

    public void playGeckoAnimation(String animationName, int durationTicks) {
        this.entityData.set(CURRENT_ANIMATION, animationName == null || animationName.isEmpty() ? "idle" : animationName);
        this.entityData.set(ANIMATION_TICKS, Math.max(0, durationTicks));
    }

    public String getCurrentAnimation() {
        return this.entityData.get(CURRENT_ANIMATION);
    }

    public int getAnimationTicks() {
        return this.entityData.get(ANIMATION_TICKS);
    }

    protected void setBloodDemonArtCooldownTicks(int cooldownTicks) {
        this.bloodDemonArtCooldownTicks = Math.max(0, cooldownTicks);
    }

    protected int getBloodDemonArtCooldownTicks() {
        return bloodDemonArtCooldownTicks;
    }

    public void setExternalBloodDemonArtCooldownTicks(int cooldownTicks) {
        setBloodDemonArtCooldownTicks(cooldownTicks);
    }

    public int getExternalBloodDemonArtCooldownTicks() {
        return getBloodDemonArtCooldownTicks();
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        if (other == this) {
            return true;
        }
        if (other instanceof LivingEntity living && !Damager.isDemonSlayer(living)) {
            if (Damager.isDemon(living) || living instanceof Monster) {
                return true;
            }
        }
        return super.isAlliedTo(other);
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        if (suppressLootDrops) {
            return;
        }
        super.dropAllDeathLoot(source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);

        if (suppressLootDrops) {
            return;
        }

        if (CustomProgressionConfig.customDemonInitiation != null
            && CustomProgressionConfig.customDemonInitiation.get()
            && !EntityTagHelper.isTwelveKizuki(this)) {
            return;
        }

        Item muzanBlood = resolveMuzanBloodItem();
        if (muzanBlood == null) {
            return;
        }

        double chance = Math.min(1.0D, getMuzanBloodDropChance(lootingLevel));
        if (this.random.nextDouble() < chance) {
            this.spawnAtLocation(new ItemStack(muzanBlood));
        }
    }

    protected double getMuzanBloodDropChance(int lootingLevel) {
        return 0.30D + (0.05D * Math.max(0, lootingLevel));
    }

    private static Item resolveMuzanBloodItem() {
        if (CustomProgressionConfig.customDemonInitiation != null && CustomProgressionConfig.customDemonInitiation.get()) {
            Item customItem = ModItems.BLOOD_OF_MUZAN.get();
            if (customItem != null && customItem != net.minecraft.world.item.Items.AIR) {
                return customItem;
            }
        }
        for (ResourceLocation id : MUZAN_BLOOD_ITEM_IDS) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }

            String anim = getCurrentAnimation();
            int animTicks = getAnimationTicks();
            if (animTicks > 0 && !isBaseMovementAnimation(anim)) {
                state.getController().setAnimationSpeed(1.0D);
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }

            boolean hasActivePath = !this.getNavigation().isDone();
            double travelSpeed = getHorizontalTravelSpeed();
            double velocitySq = this.getDeltaMovement().horizontalDistanceSqr();
            this.smoothedHorizontalSpeed = (this.smoothedHorizontalSpeed * 0.65D) + (travelSpeed * 0.35D);
            double speedNow = this.smoothedHorizontalSpeed;

            boolean movementEnter = speedNow > 0.014D || velocitySq > 0.00002D || hasActivePath || state.isMoving();
            boolean movementExit = speedNow < 0.0025D && velocitySq < 0.000002D && !hasActivePath;

            if (movementEnter) {
                this.movementAnimationLatched = true;
                this.movementAnimationGraceTicks = 28;
            } else if (this.movementAnimationGraceTicks > 0) {
                this.movementAnimationGraceTicks--;
            } else if (movementExit) {
                this.movementAnimationLatched = false;
            }

            boolean isMoving = this.movementAnimationLatched || this.movementAnimationGraceTicks > 0;
            if (!isMoving && (hasActivePath || speedNow > 0.003D || velocitySq > 0.00001D)) {
                isMoving = true;
                this.movementAnimationLatched = true;
                this.movementAnimationGraceTicks = Math.max(this.movementAnimationGraceTicks, 16);
            }

            String desiredMovementAnim;
            double desiredAnimSpeed;
            if (!isMoving) {
                if (this.movementStateHoldTicks > 0) {
                    this.movementStateHoldTicks--;
                    desiredMovementAnim = resolveWalkAnimation();
                    desiredAnimSpeed = clamp(speedNow / 0.085D, 1.05D, 2.1D);
                } else {
                    desiredMovementAnim = resolveIdleAnimation();
                    desiredAnimSpeed = 1.0D;
                }
            } else {
                this.movementStateHoldTicks = 28;
                boolean currentlySprintingAnim = isSprintAnimation(this.latchedMovementAnimation);
                boolean speedWantsSprint = speedNow >= getSprintEnterSpeed(currentlySprintingAnim);
                if (this.isSprinting() || speedWantsSprint) {
                    this.sprintAnimationGraceTicks = 20;
                    this.sprintStateHoldTicks = 28;
                } else if (this.sprintAnimationGraceTicks > 0) {
                    this.sprintAnimationGraceTicks--;
                }
                if (this.sprintStateHoldTicks > 0) {
                    this.sprintStateHoldTicks--;
                }

                boolean shouldSprint = this.sprintAnimationGraceTicks > 0 || this.sprintStateHoldTicks > 0;
                if (shouldSprint) {
                    desiredMovementAnim = resolveSprintAnimation();
                    desiredAnimSpeed = clamp(speedNow / 0.11D, 1.0D, 2.8D);
                } else {
                    desiredMovementAnim = resolveWalkAnimation();
                    desiredAnimSpeed = clamp(speedNow / 0.085D, 1.05D, 2.2D);
                }
            }

            if (!desiredMovementAnim.equals(this.latchedMovementAnimation)) {
                int classChanged = movementAnimClass(desiredMovementAnim);
                int oldClass = movementAnimClass(this.latchedMovementAnimation);
                if (this.movementAnimationSwitchCooldownTicks <= 0 || classChanged != oldClass) {
                    this.latchedMovementAnimation = desiredMovementAnim;
                    this.movementAnimationSwitchCooldownTicks = 6;
                } else {
                    this.movementAnimationSwitchCooldownTicks--;
                }
            } else if (this.movementAnimationSwitchCooldownTicks > 0) {
                this.movementAnimationSwitchCooldownTicks--;
            }

            this.smoothedAnimationSpeed = (this.smoothedAnimationSpeed * 0.70D) + (desiredAnimSpeed * 0.30D);
            state.getController().setAnimationSpeed(clamp(this.smoothedAnimationSpeed, 0.9D, 2.8D));
            String currentLoop = state.getController().getCurrentAnimation() == null
                ? null
                : state.getController().getCurrentAnimation().animation().name();
            if (this.latchedMovementAnimation.equals(currentLoop)) {
                return PlayState.CONTINUE;
            }

            RawAnimation loop = LOOPING_MOVEMENT_ANIMATIONS.computeIfAbsent(this.latchedMovementAnimation,
                key -> RawAnimation.begin().thenLoop(key));
            return state.setAndContinue(loop);
        }));
    }

    protected String resolveIdleAnimation() {
        return "idle";
    }

    protected String resolveWalkAnimation() {
        return "walk";
    }

    protected String resolveSprintAnimation() {
        return "sprint";
    }

    protected double getSprintEnterSpeed(boolean currentlySprintingAnim) {
        return currentlySprintingAnim ? 0.105D : 0.155D;
    }

    protected boolean isSprintAnimation(String animation) {
        return "sprint".equals(animation);
    }

    protected boolean isBaseMovementAnimation(String animation) {
        return resolveIdleAnimation().equals(animation)
            || resolveWalkAnimation().equals(animation)
            || resolveSprintAnimation().equals(animation);
    }

    private double getHorizontalTravelSpeed() {
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        return Math.sqrt((dx * dx) + (dz * dz));
    }

    private int movementAnimClass(String animation) {
        if (animation == null || animation.isEmpty() || resolveIdleAnimation().equals(animation)) {
            return 0;
        }
        if (resolveWalkAnimation().equals(animation)) {
            return 1;
        }
        if (isSprintAnimation(animation)) {
            return 2;
        }
        return 3;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
