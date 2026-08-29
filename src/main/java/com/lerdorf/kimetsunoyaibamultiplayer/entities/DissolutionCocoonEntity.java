package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Dissolution Cocoon - traps a victim inside a silk cocoon for up to 5 minutes.
 *
 * - Victim inside cannot attack, use abilities, or break out by struggling.
 *   Each attempted attack/ability use shortens the remaining duration by 10 seconds.
 * - Entities OUTSIDE the cocoon can break it open by attacking it (it has HP).
 * - The trapped victim takes periodic poison ticks while cocooned.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class DissolutionCocoonEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<java.util.Optional<UUID>> VICTIM =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DURATION_TICKS =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BREAKING =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPAWNING =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_TETHER =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TETHER_X =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TETHER_Y =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TETHER_Z =
        SynchedEntityData.defineId(DissolutionCocoonEntity.class, EntityDataSerializers.FLOAT);

    public static final int BASE_DURATION_TICKS = 20 * 60 * 5;  // 5 minutes
    private static final int STRUGGLE_PENALTY_TICKS = 20 * 10;  // -10 seconds
    private static final int POISON_INTERVAL_TICKS = 40;
    private static final int COCOON_DAMAGE_INTERVAL_TICKS = 20;
    private static final float COCOON_DAMAGE = 3.0F;
    private static final float COCOON_MAX_HEALTH = 30.0F;
    private static final double COCOON_HALF_HEIGHT = 1.05D;
    private static final int ANCHOR_SCAN_BLOCKS = 30;
    private static final double MIN_ANCHOR_LIFT = 0.25D;
    private static final double MAX_ANCHOR_LIFT = 0.75D;
    private static final int SPAWN_ANIMATION_TICKS = 10;
    private static final int BREAK_ANIMATION_TICKS = 20;

    /** Persistent-data tag set on victims so ability handlers can refuse actions. */
    public static final String VICTIM_TAG = "DissolutionCocoonVictim";
    public static final String VICTIM_COCCON_ID_TAG = "DissolutionCocoonId";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int poisonTickCounter = 0;
    private int spawnAnimationTicks = SPAWN_ANIMATION_TICKS;
    private int breakAnimationTicks = 0;
    private final List<ItemStack> storedDrops = new ArrayList<>();
    private boolean storedDropsDropped = false;
    @Nullable
    private UUID victimUuid = null;

    public DissolutionCocoonEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setSilent(true);
        this.setInvulnerable(false);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, COCOON_MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /**
     * Attempt to capture a living entity in a cocoon. No-op if they are already cocooned.
     */
    public static boolean tryCapture(LivingEntity victim, @Nullable LivingEntity caster) {
        if (victim == null || !victim.isAlive() || victim.level().isClientSide()) {
            return false;
        }
        if (isCocooned(victim)) {
            return false;
        }

        Level level = victim.level();
        DissolutionCocoonEntity cocoon = new DissolutionCocoonEntity(ModEntities.DISSOLUTION_COCOON.get(), level);
        // Center the cocoon vertically on the victim's body center rather than
        // their feet, so the shell visually wraps them.
        double centerY = victim.getY() + victim.getBbHeight() / 2.0D - COCOON_HALF_HEIGHT;
        BlockPos anchor = findAnchorBlockAbove(level, victim);
        if (anchor != null) {
            double anchorY = anchor.getY();
            double liftHeight = Math.max(0.0D, anchorY - centerY);
            if (liftHeight > 0.0D) {
                double liftScale = MIN_ANCHOR_LIFT + level.random.nextDouble() * (MAX_ANCHOR_LIFT - MIN_ANCHOR_LIFT);
                centerY += liftHeight * liftScale;
                cocoon.entityData.set(HAS_TETHER, true);
                cocoon.entityData.set(TETHER_X, anchor.getX() + 0.5F);
                cocoon.entityData.set(TETHER_Y, (float) anchorY);
                cocoon.entityData.set(TETHER_Z, anchor.getZ() + 0.5F);
            }
        }
        cocoon.setPos(victim.getX(), centerY, victim.getZ());
        cocoon.victimUuid = victim.getUUID();
        cocoon.entityData.set(VICTIM, java.util.Optional.of(victim.getUUID()));
        cocoon.entityData.set(DURATION_TICKS, BASE_DURATION_TICKS);

        if (!level.addFreshEntity(cocoon)) {
            return false;
        }

        victim.getPersistentData().putBoolean(VICTIM_TAG, true);
        victim.getPersistentData().putUUID(VICTIM_COCCON_ID_TAG, cocoon.getUUID());

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                victim.getX(), victim.getY(0.5D), victim.getZ(), 20, 0.4D, 0.8D, 0.4D, 0.02D);
        }
        return true;
    }

    @Nullable
    private static BlockPos findAnchorBlockAbove(Level level, LivingEntity victim) {
        BlockPos.MutableBlockPos scanPos = BlockPos.containing(
            victim.getX(),
            victim.getY() + victim.getBbHeight(),
            victim.getZ()
        ).mutable();
        int maxY = Math.min(level.getMaxBuildHeight() - 1, scanPos.getY() + ANCHOR_SCAN_BLOCKS);

        for (int y = scanPos.getY() + 1; y <= maxY; y++) {
            scanPos.setY(y);
            var state = level.getBlockState(scanPos);
            if (!state.isAir() && (!state.getCollisionShape(level, scanPos).isEmpty() || state.blocksMotion())) {
                return scanPos.immutable();
            }
        }
        return null;
    }

    public static boolean isCocooned(LivingEntity entity) {
        return entity != null && entity.isAlive() && entity.getPersistentData().getBoolean(VICTIM_TAG);
    }

    /**
     * Called when a cocooned entity tries to attack or use an ability.
     * Shortens the remaining duration as a struggle penalty.
     */
    public static void onVictimActionAttempt(LivingEntity actor) {
        if (actor == null || actor.level().isClientSide() || !isCocooned(actor)) {
            return;
        }
        java.util.UUID cocoonId = actor.getPersistentData().hasUUID(VICTIM_COCCON_ID_TAG)
            ? actor.getPersistentData().getUUID(VICTIM_COCCON_ID_TAG) : null;
        if (cocoonId == null || !(actor.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        Entity entity = serverLevel.getEntity(cocoonId);
        if (entity instanceof DissolutionCocoonEntity cocoon) {
            cocoon.applyStrugglePenalty();
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VICTIM, java.util.Optional.empty());
        this.entityData.define(DURATION_TICKS, BASE_DURATION_TICKS);
        this.entityData.define(BREAKING, false);
        this.entityData.define(SPAWNING, true);
        this.entityData.define(HAS_TETHER, false);
        this.entityData.define(TETHER_X, 0.0F);
        this.entityData.define(TETHER_Y, 0.0F);
        this.entityData.define(TETHER_Z, 0.0F);
    }

    public int getDurationTicks() {
        return this.entityData.get(DURATION_TICKS);
    }

    public boolean isBreaking() {
        return this.entityData.get(BREAKING);
    }

    public boolean isSpawning() {
        return this.entityData.get(SPAWNING);
    }

    public boolean hasTether() {
        return this.entityData.get(HAS_TETHER);
    }

    public float getTetherX() {
        return this.entityData.get(TETHER_X);
    }

    public float getTetherY() {
        return this.entityData.get(TETHER_Y);
    }

    public float getTetherZ() {
        return this.entityData.get(TETHER_Z);
    }

    public void applyStrugglePenalty() {
        int newDuration = Math.max(0, getDurationTicks() - STRUGGLE_PENALTY_TICKS);
        this.entityData.set(DURATION_TICKS, newDuration);
        if (newDuration <= 0) {
            release(true);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (isBreaking()) {
            if (breakAnimationTicks > 0) {
                breakAnimationTicks--;
            }
            if (breakAnimationTicks <= 0) {
                this.discard();
            }
            return;
        }

        if (spawnAnimationTicks > 0) {
            spawnAnimationTicks--;
            if (spawnAnimationTicks <= 0) {
                this.entityData.set(SPAWNING, false);
            }
        } else if (isSpawning()) {
            this.entityData.set(SPAWNING, false);
        }

        LivingEntity victim = getVictim();
        if (victim == null || !victim.isAlive()) {
            clearVictimState(victim);
        } else {
            // Keep the victim anchored inside the cocoon (at the cocoon's visual center).
            victim.setPos(this.getX(), this.getY(0.0D) - (victim.getBbHeight() / 2.0D - 1.05D), this.getZ());
            victim.fallDistance = 0.0F;
            victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            victim.hurtMarked = true;

            if (this.tickCount % COCOON_DAMAGE_INTERVAL_TICKS == 0) {
                Damager.hurt(this, victim, COCOON_DAMAGE, true, true);
            }

            // Periodic poison from the dissolving silk.
            poisonTickCounter++;
            if (poisonTickCounter % POISON_INTERVAL_TICKS == 0) {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_INTERVAL_TICKS + 10, 1));
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                        getX(), getY(0.5D), getZ(), 4, 0.3D, 0.5D, 0.3D, 0.01D);
                }
            }
        }

        int duration = getDurationTicks() - 1;
        this.entityData.set(DURATION_TICKS, duration);
        if (duration <= 0) {
            release(true);
        }
    }

    @Nullable
    private LivingEntity getVictim() {
        java.util.Optional<UUID> id = this.entityData.get(VICTIM);
        if (id.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(id.get());
        return entity instanceof LivingEntity living ? living : null;
    }

    private void release(boolean dissolveEffect) {
        LivingEntity victim = getVictim();
        clearVictimState(victim);

        if (dissolveEffect && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY(0.5D), getZ(),
                16, 0.5D, 0.7D, 0.5D, 0.02D);
        }
        if (dissolveEffect) {
            startBreakingAnimation();
        } else {
            this.discard();
        }
    }

    private void startBreakingAnimation() {
        if (isBreaking()) {
            return;
        }
        this.entityData.set(BREAKING, true);
        this.entityData.set(SPAWNING, false);
        this.breakAnimationTicks = BREAK_ANIMATION_TICKS;
        this.setInvulnerable(true);
        this.setHealth(Math.max(1.0F, this.getHealth()));
        this.getNavigation().stop();
    }

    private void clearVictimState(@Nullable LivingEntity victim) {
        if (victim != null) {
            victim.getPersistentData().remove(VICTIM_TAG);
            victim.getPersistentData().remove(VICTIM_COCCON_ID_TAG);
        }
        this.victimUuid = null;
        this.entityData.set(VICTIM, java.util.Optional.empty());
    }

    public void absorbDrops(Collection<ItemEntity> drops) {
        if (drops == null || drops.isEmpty()) {
            return;
        }
        for (ItemEntity drop : drops) {
            if (drop == null || drop.getItem().isEmpty()) {
                continue;
            }
            this.storedDrops.add(drop.getItem().copy());
        }
    }

    private void dropStoredDrops() {
        if (storedDropsDropped || level().isClientSide()) {
            return;
        }
        storedDropsDropped = true;
        for (ItemStack stack : storedDrops) {
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack.copy());
            }
        }
        storedDrops.clear();
    }

    @Override
    public void die(DamageSource source) {
        // Always free the victim if the cocoon is destroyed by any means.
        if (!level().isClientSide && !isBreaking()) {
            release(true);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity victim = getVictim();
        if (victim != null) {
            victim.getPersistentData().remove(VICTIM_TAG);
            victim.getPersistentData().remove(VICTIM_COCCON_ID_TAG);
        }
        if (!level().isClientSide() && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            dropStoredDrops();
        }
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide()
            || !victim.getPersistentData().getBoolean(VICTIM_TAG)
            || !victim.getPersistentData().hasUUID(VICTIM_COCCON_ID_TAG)
            || !(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity entity = serverLevel.getEntity(victim.getPersistentData().getUUID(VICTIM_COCCON_ID_TAG));
        if (entity instanceof DissolutionCocoonEntity cocoon && cocoon.isAlive()) {
            cocoon.absorbDrops(event.getDrops());
            event.getDrops().clear();
            cocoon.clearVictimState(victim);
        }
    }

    // ==================== Breaking from outside ====================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBreaking()) {
            return false;
        }

        LivingEntity victim = getVictim();

        // The trapped victim cannot break their own cocoon by attacking it.
        if (victim != null && source.getEntity() == victim) {
            applyStrugglePenalty(); // struggling costs time too
            return false;
        }

        boolean result = super.hurt(source, amount);
        if (result && !this.isInvulnerable() && this.getHealth() <= 0.0F) {
            release(true);
        } else if (result && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY(0.6D), getZ(),
                5, 0.3D, 0.3D, 0.3D, 0.01D);
        }
        return result;
    }

    @Override
    public boolean isPickable() {
        return !isBreaking(); // hittable by outside attackers
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // no-op
    }

    // ==================== NBT ====================

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.poisonTickCounter = tag.getInt("PoisonTickCounter");
        this.storedDrops.clear();
        ListTag dropsTag = tag.getList("StoredDrops", Tag.TAG_COMPOUND);
        for (int i = 0; i < dropsTag.size(); i++) {
            ItemStack stack = ItemStack.of(dropsTag.getCompound(i));
            if (!stack.isEmpty()) {
                this.storedDrops.add(stack);
            }
        }
        this.storedDropsDropped = tag.getBoolean("StoredDropsDropped");
        this.spawnAnimationTicks = tag.contains("SpawnAnimationTicks")
            ? tag.getInt("SpawnAnimationTicks")
            : (tag.getBoolean("Spawning") ? SPAWN_ANIMATION_TICKS : 0);
        this.breakAnimationTicks = tag.getInt("BreakAnimationTicks");
        if (tag.hasUUID("Victim")) {
            this.victimUuid = tag.getUUID("Victim");
            this.entityData.set(VICTIM, java.util.Optional.of(this.victimUuid));
        }
        this.entityData.set(BREAKING, tag.getBoolean("Breaking"));
        this.entityData.set(SPAWNING, tag.getBoolean("Spawning"));
        this.entityData.set(HAS_TETHER, tag.getBoolean("HasTether"));
        this.entityData.set(TETHER_X, tag.getFloat("TetherX"));
        this.entityData.set(TETHER_Y, tag.getFloat("TetherY"));
        this.entityData.set(TETHER_Z, tag.getFloat("TetherZ"));
        this.entityData.set(DURATION_TICKS, tag.getInt("DurationTicks"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PoisonTickCounter", poisonTickCounter);
        ListTag dropsTag = new ListTag();
        for (ItemStack stack : storedDrops) {
            if (!stack.isEmpty()) {
                dropsTag.add(stack.save(new CompoundTag()));
            }
        }
        tag.put("StoredDrops", dropsTag);
        tag.putBoolean("StoredDropsDropped", storedDropsDropped);
        tag.putInt("DurationTicks", getDurationTicks());
        tag.putBoolean("Spawning", isSpawning());
        tag.putInt("SpawnAnimationTicks", spawnAnimationTicks);
        tag.putBoolean("Breaking", isBreaking());
        tag.putInt("BreakAnimationTicks", breakAnimationTicks);
        tag.putBoolean("HasTether", hasTether());
        tag.putFloat("TetherX", getTetherX());
        tag.putFloat("TetherY", getTetherY());
        tag.putFloat("TetherZ", getTetherZ());
        java.util.Optional<UUID> id = this.entityData.get(VICTIM);
        if (id.isPresent()) {
            tag.putUUID("Victim", id.get());
        }
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isBreaking()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("break"));
            }
            if (isSpawning()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("spawn"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
