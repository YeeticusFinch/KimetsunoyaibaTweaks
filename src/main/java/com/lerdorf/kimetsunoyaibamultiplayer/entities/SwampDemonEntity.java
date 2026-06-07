package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.SwampDemonArt;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DamageTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraftforge.common.ForgeMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class SwampDemonEntity extends AbstractDemonEntity {
    public static final String BLOOD_DEMON_ART_ID = SwampDemonArt.ART_ID;
    private static final AABB WHOLE_SWAMP_DOMAIN_SCAN = new AABB(
        -30_000_000.0D, -2048.0D, -30_000_000.0D,
        30_000_000.0D, 2048.0D, 30_000_000.0D
    );

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> TEXTURE_VARIANT =
        net.minecraft.network.syncher.SynchedEntityData.defineId(SwampDemonEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final UUID COMBAT_SPRINT_UUID = UUID.fromString("7d4a98aa-38b7-4792-84ad-0e4cb8ca1af6");
    private static final AttributeModifier COMBAT_SPRINT_MODIFIER =
        new AttributeModifier(COMBAT_SPRINT_UUID, "Swamp demon combat sprint", 0.45D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final float WATER_SPEED_MULTIPLIER = 3.0F;
    private static final String SPAWN_PUDDLE_INIT_TAG = "SwampSpawnPuddleInitialized";
    private static final double CLONE_DEATH_EJECT_RADIUS = 15.0D;

    private boolean canSplit = true;
    private boolean splitTriggered = false;
    private int meleeAnimationTicks = 0;
    private int combatSprintTicks = 0;
    private int combatSprintCooldownTicks = 50;

    public SwampDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 140.0D)
            .add(Attributes.ATTACK_DAMAGE, 7.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D)
            .add(Attributes.ARMOR, 22.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
            .add(ForgeMod.ENTITY_REACH.get(), 3.0D);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    public static void registerBloodDemonArt() {
        SwampDemonArt.register();
    }

    private boolean isInSwampDomain() {
        return this.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (level.dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)) {
            return new AmphibiousPathNavigation(this, level);
        }
        return super.createNavigation(level);
    }

    public static void retargetSwampDomainDemonsToNearestPlayer(ServerLevel level) {
        if (!level.dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)) {
            return;
        }

        for (SwampDemonEntity demon : level.getEntitiesOfClass(SwampDemonEntity.class, WHOLE_SWAMP_DOMAIN_SCAN)) {
            demon.targetNearestSwampDomainPlayer();
        }
    }

    private ServerPlayer findNearestSwampDomainPlayer() {
        if (!(this.level() instanceof ServerLevel serverLevel) || !isInSwampDomain()) {
            return null;
        }

        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isAlive() || player.isSpectator() || !player.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)) {
                continue;
            }
            double distance = this.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void targetNearestSwampDomainPlayer() {
        ServerPlayer nearest = findNearestSwampDomainPlayer();
        if (nearest != null && this.getTarget() != nearest) {
            this.setTarget(nearest);
        } else if (nearest == null) {
            this.setTarget(null);
            this.getNavigation().stop();
        }
    }

    private boolean canTargetInCurrentDimension(LivingEntity target) {
        if (target == null || !target.isAlive() || Damager.isDemon(target)) {
            return false;
        }
        if (!isInSwampDomain()) {
            return true;
        }
        return isValidSwampDomainTarget(target);
    }

    private boolean isValidSwampDomainTarget(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        return player.isAlive()
            && !player.isSpectator()
            && player.level() == this.level()
            && player.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL);
    }

    private void enforceSwampDomainPlayerTarget() {
        if (!isInSwampDomain()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && !isValidSwampDomainTarget(target)) {
            this.setTarget(null);
            this.getNavigation().stop();
            target = null;
        }

        if (target == null || this.tickCount % 20 == 0) {
            targetNearestSwampDomainPlayer();
        }

        if (this.getTarget() == null) {
            this.getNavigation().stop();
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEXTURE_VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SwampDemonMeleeGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new SwampDemonRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            this::canTargetInCurrentDimension));
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && isInSwampDomain() && !isValidSwampDomainTarget(target)) {
            super.setTarget(null);
            this.getNavigation().stop();
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (!this.getPersistentData().getBoolean(SPAWN_PUDDLE_INIT_TAG)) {
                this.getPersistentData().putBoolean(SPAWN_PUDDLE_INIT_TAG, true);
                SwampDemonArt.activateSpawnPuddle(this);
            }

            enforceSwampDomainPlayerTarget();

            tickCombatSprint();
            maybeSplit();
        }

        if (meleeAnimationTicks > 0) {
            meleeAnimationTicks--;
        }
    }

    private void tickCombatSprint() {
        if (combatSprintCooldownTicks > 0) {
            combatSprintCooldownTicks--;
        }

        boolean shouldSprint = false;
        LivingEntity target = this.getTarget();
        if (combatSprintTicks > 0) {
            combatSprintTicks--;
            shouldSprint = target != null && target.isAlive();
        } else if (target != null && target.isAlive() && this.distanceToSqr(target) <= 196.0D
            && combatSprintCooldownTicks <= 0 && this.random.nextFloat() < 0.009F) {
            combatSprintTicks = 20 * (4 + this.random.nextInt(3));
            combatSprintCooldownTicks = 20 * (6 + this.random.nextInt(5));
            shouldSprint = true;
        }

        this.setSprinting(shouldSprint);

        var movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        boolean hasModifier = movement.getModifier(COMBAT_SPRINT_UUID) != null;
        if (shouldSprint && !hasModifier) {
            movement.addTransientModifier(COMBAT_SPRINT_MODIFIER);
        } else if (!shouldSprint && hasModifier) {
            movement.removeModifier(COMBAT_SPRINT_UUID);
        }
    }

    private void maybeSplit() {
        if (!canSplit || splitTriggered || this.getHealth() > this.getMaxHealth() * 0.75F || !(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        int cloneSpawnLimit = 2;
        if (isInSwampDomain()) {
            Vec3 densityCenter = this.getTarget() != null
                ? SwampDemonArt.resolveSwampDomainEntryPosition(this.getTarget(), this.position())
                : this.position();
            int nearbyDemonCount = serverLevel.getEntitiesOfClass(
                SwampDemonEntity.class,
                new AABB(densityCenter, densityCenter).inflate(SwampDemonArt.SWAMP_DOMAIN_DEMON_DENSITY_RADIUS)
            ).size();
            if (nearbyDemonCount >= SwampDemonArt.SWAMP_DOMAIN_MAX_SWAMP_DEMONS_PER_RADIUS) {
                return;
            }
            cloneSpawnLimit = Math.min(cloneSpawnLimit, SwampDemonArt.SWAMP_DOMAIN_MAX_SWAMP_DEMONS_PER_RADIUS - nearbyDemonCount);
        }

        splitTriggered = true;
        setTextureVariant(1);
        this.playGeckoAnimation("reel", 14);

        float sharedHealth = Math.max(1.0F, this.getHealth());
        for (int i = 0; i < cloneSpawnLimit; i++) {
            SwampDemonEntity clone = ModEntities.SWAMP_DEMON.get().create(serverLevel);
            if (clone == null) {
                continue;
            }

            double angle = (Math.PI * 2.0D * i) / 2.0D + (this.random.nextDouble() * 0.4D);
            double distance = 1.4D + (this.random.nextDouble() * 0.8D);
            double cloneY = isInSwampDomain() ? SwampDemonArt.clampSwampDomainDemonY(this.getY()) : this.getY();
            clone.moveTo(this.getX() + Math.cos(angle) * distance, cloneY, this.getZ() + Math.sin(angle) * distance, this.getYRot(), this.getXRot());
            clone.setTextureVariant(i == 0 ? 2 : 3);
            clone.setSplitClone(true);
            clone.setHealth(Math.min(sharedHealth, clone.getMaxHealth()));
            clone.setTarget(this.getTarget());
            serverLevel.addFreshEntity(clone);
        }
    }

    public int getTextureVariant() {
        return this.entityData.get(TEXTURE_VARIANT);
    }

    public void setTextureVariant(int variant) {
        this.entityData.set(TEXTURE_VARIANT, Math.max(0, Math.min(3, variant)));
    }

    public void setSplitClone(boolean splitClone) {
        this.canSplit = !splitClone;
        this.splitTriggered = splitClone;
        this.getPersistentData().putBoolean("SwampDemonClone", splitClone);
    }

    public boolean isSplitClone() {
        return this.getPersistentData().getBoolean("SwampDemonClone");
    }

    @Override
    protected float getBloodDemonArtUseChance() {
        return 0.05F;
    }

    @Override
    protected double getMuzanBloodDropChance(int lootingLevel) {
        return 0.10D + (0.05D * Math.max(0, lootingLevel));
    }

    @Override
    protected void tickBloodDemonArt() {
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = getBloodDemonArt();
        LivingEntity target = getTarget();
        if (art == null || target == null || !target.isAlive() || getBloodDemonArtCooldownTicks() > 0 || isUsingLockedAnimation()) {
            return;
        }

        double distanceSq = this.distanceToSqr(target);
        if (distanceSq > getBloodDemonArtRange() * getBloodDemonArtRange()) {
            return;
        }
        if (this.random.nextFloat() > getBloodDemonArtUseChance()) {
            return;
        }
        if (SwampDemonArt.isPuddled(this) && this.random.nextFloat() < 0.65F) {
            return;
        }

        java.util.List<com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm> availableForms = new java.util.ArrayList<>();
        for (int i = 0; i < art.getTechnique().getFormCount(); i++) {
            var form = art.getTechnique().getForm(i);
            if (form == null) {
                continue;
            }
            if (this.level().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL) && form.getFormId() == SwampDemonArt.FORM_SWAMP_DOMAIN) {
                continue;
            }
            if (SwampDemonArt.isPuddled(this) && form.getFormId() == SwampDemonArt.FORM_PUDDLE) {
                continue;
            }
            availableForms.add(form);
            if (form.getFormId() == SwampDemonArt.FORM_PUDDLE) {
                //availableForms.add(form);
                availableForms.add(form);
            }
        }

        if (availableForms.isEmpty()) {
            return;
        }

        var form = availableForms.get(this.random.nextInt(availableForms.size()));
        form.execute(this, level());
        setBloodDemonArtCooldownTicks(Math.max(20, form.getCooldownSeconds() * 20));
    }

    @Override
    protected String resolveSprintAnimation() {
        if (SwampDemonArt.isPuddled(this)) {
            return resolveWalkAnimation();
        }
        return "sprint";
    }

    @Override
    protected String resolveIdleAnimation() {
        if (SwampDemonArt.isPuddled(this)) {
            return SwampDemonArt.isPuddleFullyHidden(this) ? "invisibility" : "puddle_idle";
        }
        return super.resolveIdleAnimation();
    }

    @Override
    protected String resolveWalkAnimation() {
        if (SwampDemonArt.isPuddled(this)) {
            return SwampDemonArt.isPuddleFullyHidden(this) ? "invisibility" : "puddle_walk";
        }
        return super.resolveWalkAnimation();
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity livingTarget)) {
            return false;
        }

        float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean result = Damager.hurt(this, livingTarget, damage);
        if (result) {
            this.doEnchantDamageEffects(this, livingTarget);
        }

        if (result && meleeAnimationTicks <= 0) {
            if (SwampDemonArt.isPuddled(this)) {
                SwampDemonArt.onPuddleAttack(this);
            } else {
                SwampDemonArt.playRegularMeleeCombo(this);
            }
            meleeAnimationTicks = 10;
        }
        return result;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!this.isInWaterOrBubble()) {
            super.travel(travelVector);
            return;
        }

        float originalSpeed = this.getSpeed();
        this.setSpeed(originalSpeed * WATER_SPEED_MULTIPLIER);
        super.travel(travelVector);
        this.setSpeed(originalSpeed);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);
        float artDropChance = isSplitClone() ? 0.10F : 0.30F;
        if (this.random.nextFloat() < artDropChance) {
            this.spawnAtLocation(new ItemStack(ModItems.SWAMP_DEMON_ART.get()));
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        boolean ejectOnDeath = !this.level().isClientSide
            && isSplitClone()
            && isInSwampDomain();

        super.die(damageSource);

        if (ejectOnDeath && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            boolean satokosBowQuestClone = "swamp_demon_kidnappers_bog_satoko".equals(
                this.getPersistentData().getString(
                    com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.QUEST_TARGET_ID_TAG));
            java.util.List<LivingEntity> ejectedTargets = serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(CLONE_DEATH_EJECT_RADIUS),
                target -> target.isAlive()
                    && target instanceof ServerPlayer player
                    && (DamageTracker.hasDamageHistory(this, player)
                        || player.distanceToSqr(this) <= CLONE_DEATH_EJECT_RADIUS * CLONE_DEATH_EJECT_RADIUS));
            for (LivingEntity living : ejectedTargets) {
                net.minecraft.world.phys.Vec3 returnPos = new net.minecraft.world.phys.Vec3(
                    living.getPersistentData().getDouble(SwampDemonArt.SWAMP_RETURN_X_TAG),
                    living.getPersistentData().getDouble(SwampDemonArt.SWAMP_RETURN_Y_TAG),
                    living.getPersistentData().getDouble(SwampDemonArt.SWAMP_RETURN_Z_TAG)
                );
                String storedDim = living.getPersistentData().getString(SwampDemonArt.SWAMP_RETURN_DIM_TAG);
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(storedDim);
                if (id != null) {
                    SwampDemonArt.teleportThroughPortal(living,
                        net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id),
                        returnPos);
                    if (satokosBowQuestClone && living instanceof ServerPlayer player) {
                        ensurePlayerHasSatokosBow(player);
                    }
                }
            }
        }
    }

    private static void ensurePlayerHasSatokosBow(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.SATOKOS_BOW.get())) {
                return;
            }
        }

        ItemStack bow = new ItemStack(ModItems.SATOKOS_BOW.get());
        if (player.getInventory().add(bow)) {
            return;
        }

        int forcedSlot = player.getInventory().selected;
        ItemStack displaced = player.getInventory().getItem(forcedSlot);
        player.getInventory().setItem(forcedSlot, bow);
        player.getInventory().setChanged();
        if (!displaced.isEmpty()) {
            player.drop(displaced, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && amount > 0.0F && !this.level().isClientSide) {
            DamageTracker.recordDamage(source, this);
        }
        return damaged;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TextureVariant", getTextureVariant());
        tag.putBoolean("CanSplit", canSplit);
        tag.putBoolean("SplitTriggered", splitTriggered);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTextureVariant(tag.getInt("TextureVariant"));
        this.canSplit = tag.getBoolean("CanSplit");
        this.splitTriggered = tag.getBoolean("SplitTriggered");
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, net.minecraft.nbt.CompoundTag dataTag) {
        SpawnGroupData finalized = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (level.getLevel().dimension().equals(SwampDemonArt.SWAMP_DOMAIN_LEVEL)
            && reason == MobSpawnType.NATURAL) {
            setSplitClone(true);
            setTextureVariant(1 + this.getRandom().nextInt(3));
            this.setHealth(this.getMaxHealth() * 0.5F);
        }
        return finalized;
    }

    private static class SwampDemonRandomStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final SwampDemonEntity demon;

        private SwampDemonRandomStrollGoal(SwampDemonEntity demon, double speedModifier) {
            super(demon, speedModifier);
            this.demon = demon;
        }

        @Override
        public boolean canUse() {
            return !demon.isInSwampDomain() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !demon.isInSwampDomain() && super.canContinueToUse();
        }
    }

    private static class SwampDemonMeleeGoal extends MeleeAttackGoal {
        private final SwampDemonEntity demon;

        private SwampDemonMeleeGoal(SwampDemonEntity demon, double speedModifier) {
            super(demon, speedModifier, false);
            this.demon = demon;
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            if (SwampDemonArt.isPuddleFullyHidden(demon)) {
                return 1.5D * 1.5D;
            }
            return 4.5D + target.getBbWidth();
        }

        @Override
        public void stop() {
            super.stop();
            if (demon.combatSprintTicks <= 0) {
                demon.setSprinting(false);
            }
        }
    }
}
