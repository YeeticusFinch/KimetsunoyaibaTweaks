package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.CombustibleBlood;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.minecraft.world.DifficultyInstance;

public class NezukoEntity extends AbstractDemonEntity {
    public static final String BLOOD_DEMON_ART_ID = CombustibleBlood.ART_ID;

    public enum Stage {
        SMALL(0, 0.50F),
        NORMAL(1, 0.85F),
        AWAKENED(2, 1.00F);

        private final int id;
        private final float scale;

        Stage(int id, float scale) {
            this.id = id;
            this.scale = scale;
        }

        public int id() {
            return id;
        }

        public float scale() {
            return scale;
        }

        public static Stage fromId(int id) {
            for (Stage stage : values()) {
                if (stage.id == id) {
                    return stage;
                }
            }
            return NORMAL;
        }
    }

    private static final EntityDataAccessor<Integer> STAGE =
        SynchedEntityData.defineId(NezukoEntity.class, EntityDataSerializers.INT);

    private static final String STAGE_TAG = "NezukoStage";
    private static final String LAST_COMBAT_TICK_TAG = "NezukoLastCombatTick";
    private static final int AMBIENT_STAGE_SWITCH_INTERVAL = 20 * 60;
    private static final float AMBIENT_STAGE_SWITCH_CHANCE = 0.50F;
    private static final EntityDimensions SMALL_STAGE_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.0F);

    private static final UUID SMALL_SPEED_UUID = UUID.fromString("2e83f8e0-8988-4f2f-bd2f-49b60720c10a");
    private static final UUID AWAKENED_SPEED_UUID = UUID.fromString("4790e1a3-dc4a-4123-8ca3-78f9953ec2f6");
    private static final UUID SMALL_DAMAGE_UUID = UUID.fromString("e89fcb96-4ea3-4da5-b1bf-89f390baeb78");
    private static final UUID AWAKENED_DAMAGE_UUID = UUID.fromString("401fdad3-f316-42aa-9ea0-67f3a74d9d2e");
    private static final UUID AWAKENED_ARMOR_UUID = UUID.fromString("009fce28-c43d-472f-b7f9-f2de1239e5e5");

    private static final AttributeModifier SMALL_SPEED_MODIFIER =
        new AttributeModifier(SMALL_SPEED_UUID, "Nezuko small speed", 0.50D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier AWAKENED_SPEED_MODIFIER =
        new AttributeModifier(AWAKENED_SPEED_UUID, "Nezuko awakened speed", 0.40D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier SMALL_DAMAGE_MODIFIER =
        new AttributeModifier(SMALL_DAMAGE_UUID, "Nezuko small damage suppression", -5.0D, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier AWAKENED_DAMAGE_MODIFIER =
        new AttributeModifier(AWAKENED_DAMAGE_UUID, "Nezuko awakened damage", 5.0D, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier AWAKENED_ARMOR_MODIFIER =
        new AttributeModifier(AWAKENED_ARMOR_UUID, "Nezuko awakened armor", 20.0D, AttributeModifier.Operation.ADDITION);

    private long lastCombatTick = 0L;

    public NezukoEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // Nezuko is allied with demon slayers for AI/friendly-fire systems.
        this.getPersistentData().putBoolean("oni", false);
        this.getPersistentData().putBoolean("kisatsutai", true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 200.0D)
            .add(Attributes.ATTACK_DAMAGE, 5.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ARMOR, 0.0D)
            .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    public static void registerBloodDemonArt() {
        CombustibleBlood.register();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STAGE, Stage.NORMAL.id());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new NezukoMeleeGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 12.0F, 1.30D, 1.55D,
            target -> isSmallStage() && target != null && target.isAlive() && DemonSlayerAggroHandler.isDemonTarget(target)));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            target -> !isSmallStage() && target != null && target.isAlive() && DemonSlayerAggroHandler.isDemonTarget(target)));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        long gameTime = serverLevel.getGameTime();

        boolean inCombat = isInCombat();
        if (inCombat) {
            this.lastCombatTick = gameTime;
            this.getPersistentData().putLong(LAST_COMBAT_TICK_TAG, gameTime);
        }

        boolean enteredAwakenedThisTick = false;
        if (!isAwakenedStage() && this.getHealth() <= (this.getMaxHealth() * 0.5F)) {
            enterAwakenedStage(serverLevel);
            enteredAwakenedThisTick = true;
        }

        if (!enteredAwakenedThisTick
            && this.tickCount > 0
            && this.tickCount % AMBIENT_STAGE_SWITCH_INTERVAL == 0
            && this.random.nextFloat() < AMBIENT_STAGE_SWITCH_CHANCE) {
            tryAmbientStageSwitch(inCombat);
        }

        if (isSmallStage()) {
            this.setTarget(null);
            LivingEntity nearbyDemon = findNearestDemon(12.0D);
            if (nearbyDemon != null) {
                fleeFrom(nearbyDemon, 1.55D);
            }
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        SpawnGroupData spawnGroupData, CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
        setStage(Stage.NORMAL, true);
        this.lastCombatTick = this.level().getGameTime();
        this.getPersistentData().putLong(LAST_COMBAT_TICK_TAG, this.lastCombatTick);
        return data;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (isSmallStage()) {
            return SMALL_STAGE_DIMENSIONS;
        }
        EntityDimensions base = super.getDimensions(pose);
        float scale = getStageScale();
        return EntityDimensions.scalable(base.width * scale, base.height * scale);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        if (isSmallStage()) {
            return 1.0F;
        }
        return size.height * 0.90F;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STAGE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt || this.level().isClientSide || !isSmallStage()) {
            return hurt;
        }

        Entity direct = source.getEntity();
        if (direct instanceof LivingEntity attacker) {
            this.setTarget(null);
            fleeFrom(attacker, 1.65D);
        }
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isSmallStage() || !(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        if (!DemonSlayerAggroHandler.isDemonTarget(livingTarget)) {
            return false;
        }

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean damaged = Damager.hurt(this, livingTarget, damage);
        if (damaged) {
            this.doEnchantDamageEffects(this, livingTarget);
            CombustibleBlood.playRegularMeleeCombo(this);
            this.lastCombatTick = this.level().getGameTime();
            this.getPersistentData().putLong(LAST_COMBAT_TICK_TAG, this.lastCombatTick);
        }
        return damaged;
    }

    @Override
    public boolean canFireProjectileWeapon(net.minecraft.world.item.ProjectileWeaponItem projectileWeaponItem) {
        return false;
    }

    @Override
    protected BloodDemonArtRegistry.RegisteredBloodDemonArt getBloodDemonArt() {
        return BloodDemonArtRegistry.getArt(BLOOD_DEMON_ART_ID);
    }

    @Override
    protected float getBloodDemonArtUseChance() {
        return isAwakenedStage() ? 0.07F : 0.0F;
    }

    @Override
    protected void tickBloodDemonArt() {
        if (isAwakenedStage()) {
            super.tickBloodDemonArt();
        }
    }

    @Override
    protected boolean isSunlightImmune() {
        return true;
    }

    @Override
    protected String resolveIdleAnimation() {
        return "idle";
    }

    @Override
    protected String resolveWalkAnimation() {
        return isSmallStage() ? "nezuko_small_walk" : "walk_female";
    }

    @Override
    protected String resolveSprintAnimation() {
        return isSmallStage() ? "nezuko_small_walk" : "walk_female";
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return "walk_female".equals(animation) || "nezuko_small_walk".equals(animation);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == this) {
            return true;
        }

        if (other instanceof LivingEntity living) {
            if (living instanceof NezukoEntity) {
                return true;
            }
            if (Damager.isDemonSlayer(living) || EntityTagHelper.isDemonSlayer(living) || EntityTagHelper.isCivilian(living)
                || living instanceof Villager) {
                return true;
            }
            if (living instanceof Player player && !player.getPersistentData().getBoolean("oni")) {
                return true;
            }
            return false;
        }

        return super.isAlliedTo(other);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(STAGE_TAG, this.entityData.get(STAGE));
        compound.putLong(LAST_COMBAT_TICK_TAG, this.lastCombatTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        Stage savedStage = compound.contains(STAGE_TAG) ? Stage.fromId(compound.getInt(STAGE_TAG)) : Stage.NORMAL;
        setStage(savedStage, false);
        this.lastCombatTick = compound.getLong(LAST_COMBAT_TICK_TAG);
    }

    public Stage getStage() {
        return Stage.fromId(this.entityData.get(STAGE));
    }

    public float getStageScale() {
        return getStage().scale();
    }

    public boolean isSmallStage() {
        return getStage() == Stage.SMALL;
    }

    public boolean isAwakenedStage() {
        return getStage() == Stage.AWAKENED;
    }

    private void applyAwakenedEffects(Stage stage) {
        if (this.level().isClientSide) {
            return;
        }

        if (stage == Stage.AWAKENED) {
            this.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                999999,
                1,      // Resistance II
                true,   // ambient
                false,  // no particles
                false   // no icon
            ));

            this.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                999999,
                4,      // Strength V
                true,
                false,
                false
            ));

            this.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                999999,
                0,      // Regeneration I
                true,
                false,
                false
            ));

            return;
        }
        else {
            this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            this.removeEffect(MobEffects.DAMAGE_BOOST);
            this.removeEffect(MobEffects.REGENERATION);
        }
    }

    public void setStage(Stage stage, boolean refresh) {
        Stage current = getStage();
        if (current == stage) {
            applyStageAttributes(stage);
            refreshEquipment(stage);
            applyAwakenedEffects(stage);
            if (refresh) {
                this.refreshDimensions();
            }
            return;
        }

        this.entityData.set(STAGE, stage.id());
        applyStageAttributes(stage);
        refreshEquipment(stage);
        applyAwakenedEffects(stage);

        if (refresh) {
            this.refreshDimensions();
        }
    }

    private void enterAwakenedStage(ServerLevel serverLevel) {
        spawnAwakeningSpiral(serverLevel);
        setStage(Stage.AWAKENED, true);
        this.setHealth(this.getMaxHealth());
        this.lastCombatTick = serverLevel.getGameTime();
        this.getPersistentData().putLong(LAST_COMBAT_TICK_TAG, this.lastCombatTick);
    }

    private void spawnAwakeningSpiral(ServerLevel level) {
        double radius = 1.1D;
        for (int i = 0; i < 72; i++) {
            double angle = (Math.PI * 2.0D * i) / 18.0D;
            double yOffset = 0.05D * i;
            double px = this.getX() + Math.cos(angle) * radius;
            double py = this.getY() + 0.1D + yOffset;
            double pz = this.getZ() + Math.sin(angle) * radius;
            Vec3 drift = new Vec3(Math.cos(angle), 0.25D, Math.sin(angle)).scale(0.03D + (this.random.nextDouble() * 0.02D));
            level.sendParticles(ModParticles.BLOOD_FLAME.get(), px, py, pz, 1, drift.x, drift.y, drift.z, 0.0D);
            if (i % 12 == 0) {
                radius += 0.04D;
            }
        }
    }

    private void applyStageAttributes(Stage stage) {
        AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);

        if (movement != null) {
            movement.removeModifier(SMALL_SPEED_UUID);
            movement.removeModifier(AWAKENED_SPEED_UUID);
            if (stage == Stage.SMALL) {
                movement.addTransientModifier(SMALL_SPEED_MODIFIER);
            } else if (stage == Stage.AWAKENED) {
                movement.addTransientModifier(AWAKENED_SPEED_MODIFIER);
            }
        }

        if (attack != null) {
            attack.removeModifier(SMALL_DAMAGE_UUID);
            attack.removeModifier(AWAKENED_DAMAGE_UUID);
            if (stage == Stage.SMALL) {
                attack.addTransientModifier(SMALL_DAMAGE_MODIFIER);
            } else if (stage == Stage.AWAKENED) {
                attack.addTransientModifier(AWAKENED_DAMAGE_MODIFIER);
            }
        }

        if (armor != null) {
            armor.removeModifier(AWAKENED_ARMOR_UUID);
            if (stage == Stage.AWAKENED) {
                armor.addTransientModifier(AWAKENED_ARMOR_MODIFIER);
            }
        }
    }

    private void refreshEquipment(Stage stage) {
        if (stage == Stage.AWAKENED) {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems.COMBUSTIBLE_BLOOD.get()));
            this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
            return;
        }

        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    private void tryAmbientStageSwitch(boolean inCombat) {
        Stage stage = getStage();
        if (stage == Stage.AWAKENED) {
            if (!inCombat) {
                setStage(Stage.NORMAL, true);
            }
            return;
        }

        setStage(stage == Stage.NORMAL ? Stage.SMALL : Stage.NORMAL, true);
    }

    private LivingEntity findNearestDemon(double range) {
        return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range),
                target -> target != this && target.isAlive() && DemonSlayerAggroHandler.isDemonTarget(target))
            .stream()
            .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
            .orElse(null);
    }

    private void fleeFrom(LivingEntity attacker, double speed) {
        Vec3 away = LandRandomPos.getPosAway(this, 16, 7, attacker.position());
        if (away == null) {
            away = this.position().subtract(attacker.position()).normalize().scale(8.0D).add(this.position());
        }
        this.getNavigation().moveTo(away.x, away.y, away.z, speed);
    }

    private boolean isInCombat() {
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            return true;
        }
        if (this.getLastHurtByMob() != null && (this.tickCount - this.getLastHurtByMobTimestamp()) <= 100) {
            return true;
        }
        return this.getLastHurtMob() != null && (this.tickCount - this.getLastHurtMobTimestamp()) <= 100;
    }

    private static class NezukoMeleeGoal extends MeleeAttackGoal {
        private final NezukoEntity nezuko;

        private NezukoMeleeGoal(NezukoEntity nezuko, double speedModifier) {
            super(nezuko, speedModifier, false);
            this.nezuko = nezuko;
        }

        @Override
        public boolean canUse() {
            return !this.nezuko.isSmallStage() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.nezuko.isSmallStage() && super.canContinueToUse();
        }
    }
}
