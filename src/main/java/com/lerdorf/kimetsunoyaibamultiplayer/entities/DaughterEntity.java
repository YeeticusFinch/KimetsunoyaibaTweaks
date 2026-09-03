package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.SilkManipulation;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.BloodDemonArtM1AttackHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.DaughterBackstepGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The Daughter (Rui's "sister") spider demon.
 *
 * Two synced forms:
 * - DEMON form (daughter skin): hostile to humans, civilians, non-demon players and
 *   demon slayers; targeted by demon slayers.
 * - HUMAN form (ryoko disguise skin): fully neutral. Does not attack anyone and is
 *   not targeted by demon slayers or demons, unless she has attacked them first
 *   (HurtByTargetGoal / last-hurt memory still applies).
 */
public class DaughterEntity extends AbstractDemonEntity {
    public static final String BLOOD_DEMON_ART_ID = SilkManipulation.ART_ID;

    private static final int SILK_SPRAY_COOLDOWN_TICKS = 20;
    private static final double MIN_RANGED_DISTANCE = 15.0D;
    private static final double MIN_RANGED_DISTANCE_SQ = MIN_RANGED_DISTANCE * MIN_RANGED_DISTANCE;
    private static final double IDEAL_RANGED_DISTANCE = 24.0D;
    private static final double IDEAL_RANGED_DISTANCE_SQ = IDEAL_RANGED_DISTANCE * IDEAL_RANGED_DISTANCE;
    private static final double MAX_RANGED_DISTANCE = 40.0D;
    private static final double MAX_RANGED_DISTANCE_SQ = MAX_RANGED_DISTANCE * MAX_RANGED_DISTANCE;
    private static final double WEB_MELEE_RANGE = 5.0D;
    private static final int WEB_MELEE_COOLDOWN_TICKS = 10;

    private static final EntityDataAccessor<Boolean> HUMAN_FORM =
        SynchedEntityData.defineId(DaughterEntity.class, EntityDataSerializers.BOOLEAN);

    /** How long the human disguise lasts once engaged (-1 = indefinite). */
    private static final int DEFAULT_DISGUISE_TICKS = 20 * 120;

    private int webMeleeCooldownTicks;
    private int backstepFollowupTicks;
    private java.util.UUID backstepTargetId;

    public DaughterEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 80.0D)
            .add(Attributes.ATTACK_DAMAGE, 7.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D)
            .add(Attributes.ARMOR, 6.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public static void registerBloodDemonArt() {
        SilkManipulation.register();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HUMAN_FORM, false);
    }

    // ==================== Form switching ====================

    public boolean isInHumanForm() {
        return this.entityData.get(HUMAN_FORM);
    }

    public void setHumanForm(boolean humanForm) {
        this.entityData.set(HUMAN_FORM, humanForm);
    }

    @Override
    public boolean isSunlightImmune() {
        // The human disguise also shields her from sunlight burn.
        return super.isSunlightImmune() || isInHumanForm();
    }

    /**
     * Switch into her ryoko human disguise. While disguised she is neutral.
     */
    public void enterHumanDisguise() {
        if (!level().isClientSide) {
            setHumanForm(true);
            this.setTarget(null);
            this.setAggressive(false);
            this.getNavigation().stop();
            this.getPersistentData().putInt("DaughterDisguiseTicks", DEFAULT_DISGUISE_TICKS);
            Log.debug("[Daughter] entered human disguise");
        }
    }

    /**
     * Reveal her true spider demon form. Anyone she was hurt by recently is
     * immediately re-aggroed via HurtByTargetGoal memory.
     */
    public void revealDemonForm() {
        if (!level().isClientSide) {
            setHumanForm(false);
            this.getPersistentData().remove("DaughterDisguiseTicks");
            Log.debug("[Daughter] revealed demon form");
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && webMeleeCooldownTicks > 0) {
            webMeleeCooldownTicks--;
        }

        if (!level().isClientSide && isInHumanForm()) {
            CompoundTag data = this.getPersistentData();
            if (data.contains("DaughterDisguiseTicks")) {
                int ticks = data.getInt("DaughterDisguiseTicks") - 1;
                if (ticks <= 0) {
                    revealDemonForm();
                } else {
                    data.putInt("DaughterDisguiseTicks", ticks);
                }
            }

        }

        if (!level().isClientSide && !isInHumanForm()) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()
                && distanceToSqr(target) <= WEB_MELEE_RANGE * WEB_MELEE_RANGE) {
                doHurtTarget(target);
            }
        }
    }

    // ==================== Faction behavior ====================

    /**
     * While in her human disguise she behaves as a neutral civilian:
     * no demon tags apply for targeting systems.
     */
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        if (isInHumanForm()) {
            return other instanceof LivingEntity living
                ? !Damager.isDemonSlayer(living) || !wasRecentlyProvoked()
                : true;
        }
        return super.isAlliedTo(other);
    }

    private boolean wasRecentlyProvoked() {
        LivingEntity lastHurtBy = this.getLastHurtByMob();
        if (lastHurtBy == null) {
            return false;
        }
        int ticksSince = this.tickCount - this.getLastHurtByMobTimestamp();
        return ticksSince >= 0 && ticksSince <= 400; // 20 seconds of grudge
    }

    /**
     * Demon slayers should not auto-target her while disguised.
     */
    public boolean isTargetableBySlayers() {
        return !isInHumanForm() || wasRecentlyProvoked();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DaughterBackstepGoal(this));
        this.goalSelector.addGoal(2, new DaughterKeepDistanceGoal(this));
        this.goalSelector.addGoal(3, new DaughterRangedCombatGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new DaughterFallbackMeleeGoal(this, 1.05D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // Only actively hunt humans/civilians/slayers while in demon form.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
            10, true, false, this::isValidDemonFormTarget));
    }

    private boolean isValidDemonFormTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (isInHumanForm()) {
            return false;
        }
        if (Damager.isDemon(target)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator()
                && !player.getPersistentData().getBoolean("kisatsutai_neutral_guard");
        }
        // Villagers, civilians, demon slayers, etc.
        return target instanceof Villager || !(target instanceof Monster);
    }

    @Override
    public void setTarget(LivingEntity target) {
        // Never acquire targets while disguised unless provoked.
        if (target != null && isInHumanForm() && !wasRecentlyProvoked()) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isInHumanForm()) {
            return this.getLastHurtByMob() == target;
        }
        return super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (!(target instanceof LivingEntity livingTarget)
            || isInHumanForm()
            || distanceToSqr(livingTarget) > WEB_MELEE_RANGE * WEB_MELEE_RANGE
            || isUsingLockedAnimation()
            || webMeleeCooldownTicks > 0) {
            return false;
        }

        if (BloodDemonArtM1AttackHandler.performWebSlashAttack(this, livingTarget.getUUID())) {
            webMeleeCooldownTicks = WEB_MELEE_COOLDOWN_TICKS;
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        // Taking damage while disguised blows her cover.
        if (result && isInHumanForm() && source.getEntity() != null) {
            revealDemonForm();
            this.setLastHurtByMob(source.getEntity() instanceof LivingEntity living ? living : null);
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!level().isClientSide && !isInHumanForm()) {
            // Ambient poison on melee handled by BDA; keep base attack simple here.
        }
    }

    @Override
    protected BloodDemonArtRegistry.RegisteredBloodDemonArt getBloodDemonArt() {
        return BloodDemonArtRegistry.getArt(BLOOD_DEMON_ART_ID);
    }

    @Override
    protected double getBloodDemonArtRange() {
        return MAX_RANGED_DISTANCE;
    }

    @Override
    protected String resolveWalkAnimation() {
        return "walk_female";
    }

    @Override
    protected double getSprintEnterSpeed(boolean currentlySprintingAnim) {
        // Daughter only uses sprinting when a combat goal explicitly requests it.
        return Double.MAX_VALUE;
    }

    @Override
    protected float getBloodDemonArtUseChance() {
        return 1.0F;
    }

    @Override
    protected void tickBloodDemonArt() {
        if (isInHumanForm()
            || com.lerdorf.kimetsunoyaibamultiplayer.effects.PuppetryHandler.isAbilityUseBlocked(this)) {
            return;
        }

        if (backstepFollowupTicks > 0) {
            LivingEntity target = getBackstepTarget();
            if (target != null) {
                faceCombatTarget(target);
            }
            backstepFollowupTicks--;
            if (backstepFollowupTicks == 0) {
                executeBackstepFollowup(target);
            }
            return;
        }

        BloodDemonArtRegistry.RegisteredBloodDemonArt art = getBloodDemonArt();
        LivingEntity target = getTarget();
        if (art == null || target == null || !target.isAlive() || getBloodDemonArtCooldownTicks() > 0 || isUsingLockedAnimation()) {
            return;
        }

        double distanceSq = this.distanceToSqr(target);
        if (distanceSq > MAX_RANGED_DISTANCE_SQ) {
            return;
        }

        faceCombatTarget(target);
        BloodDemonArtForm form = pickDaughterBloodDemonArtForm(art, distanceSq);
        if (form == null) {
            return;
        }

        form.execute(this, level());
        if (form.getFormId() == SilkManipulation.FORM_SILK_SPRAY) {
            setBloodDemonArtCooldownTicks(SILK_SPRAY_COOLDOWN_TICKS);
        } else {
            setBloodDemonArtCooldownTicks(Math.max(SILK_SPRAY_COOLDOWN_TICKS, form.getCooldownSeconds() * 20));
        }
    }

    public LivingEntity getNearbyCombatEnemy() {
        LivingEntity[] candidates = {getTarget(), getLastHurtByMob(), getLastHurtMob()};
        LivingEntity closest = null;
        double closestDistanceSq = 15.0D * 15.0D;
        for (LivingEntity candidate : candidates) {
            if (!isRecentCombatCandidate(candidate) || candidate == this) {
                continue;
            }
            double distanceSq = distanceToSqr(candidate);
            if (distanceSq <= closestDistanceSq) {
                closest = candidate;
                closestDistanceSq = distanceSq;
            }
        }
        return closest;
    }

    public void startBackstep(LivingEntity target) {
        if (target == null) {
            return;
        }

        faceCombatTarget(target);
        Vec3 away = position().subtract(target.position());
        away = new Vec3(away.x, 0.0D, away.z).normalize();
        if (away.lengthSqr() < 1.0E-4D) {
            Vec3 look = getLookAngle();
            away = new Vec3(-look.x, 0.0D, -look.z).normalize();
        }

        setDeltaMovement(new Vec3(away.x * 1.0D, 0.45D, away.z * 1.0D));
        getNavigation().stop();
        hurtMarked = true;
        playGeckoAnimation("backstep", 8);
        backstepTargetId = target.getUUID();
        backstepFollowupTicks = 8;
    }

    private boolean isRecentCombatCandidate(LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive()) {
            return false;
        }
        if (candidate == getTarget()) {
            return true;
        }
        if (candidate == getLastHurtByMob()) {
            return tickCount - getLastHurtByMobTimestamp() <= 200;
        }
        return candidate == getLastHurtMob() && tickCount - getLastHurtMobTimestamp() <= 200;
    }

    private LivingEntity getBackstepTarget() {
        if (backstepTargetId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(backstepTargetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void executeBackstepFollowup(LivingEntity target) {
        backstepTargetId = null;
        if (target == null || distanceToSqr(target) > MAX_RANGED_DISTANCE_SQ) {
            return;
        }

        faceCombatTarget(target);
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = getBloodDemonArt();
        if (art == null) {
            return;
        }

        int formId = random.nextBoolean()
            ? SilkManipulation.FORM_SILK_SPRAY : SilkManipulation.FORM_DISSOLUTION_COCOON;
        BloodDemonArtForm form = art.getTechnique().getForm(formId);
        if (form != null) {
            form.execute(this, level());
            setBloodDemonArtCooldownTicks(Math.max(SILK_SPRAY_COOLDOWN_TICKS, form.getCooldownSeconds() * 20));
        }
    }

    private BloodDemonArtForm pickDaughterBloodDemonArtForm(BloodDemonArtRegistry.RegisteredBloodDemonArt art, double distanceSq) {
        List<BloodDemonArtForm> preferred = new ArrayList<>();
        List<BloodDemonArtForm> fallback = new ArrayList<>();

        for (BloodDemonArtForm form : art.getTechnique().getForms()) {
            if (form == null) {
                continue;
            }
            int formId = form.getFormId();
            if (distanceSq <= 64.0D && formId == SilkManipulation.FORM_ACID_SPRAY) {
                preferred.add(form);
                preferred.add(form);
                continue;
            }
            if (distanceSq <= 400.0D && formId == SilkManipulation.FORM_DISSOLUTION_COCOON) {
                preferred.add(form);
                continue;
            }
            if (formId == SilkManipulation.FORM_SILK_SPRAY) {
                preferred.add(form);
                preferred.add(form);
                preferred.add(form);
            } else {
                fallback.add(form);
            }
        }

        List<BloodDemonArtForm> pool = preferred.isEmpty() ? fallback : preferred;
        return pool.isEmpty() ? null : pool.get(this.random.nextInt(pool.size()));
    }

    private void faceCombatTarget(LivingEntity target) {
        this.getLookControl().setLookAt(target, 180.0F, 180.0F);

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dy = target.getEyeY() - this.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float)(Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float)(-(Math.atan2(dy, horizontal) * (180.0D / Math.PI)));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    // ==================== NBT ====================

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HumanForm")) {
            setHumanForm(tag.getBoolean("HumanForm"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HumanForm", isInHumanForm());
    }

    private static class DaughterKeepDistanceGoal extends Goal {
        private final DaughterEntity daughter;
        private Vec3 awayPos;
        private int repathTicks;

        private DaughterKeepDistanceGoal(DaughterEntity daughter) {
            this.daughter = daughter;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = daughter.getNearbyCombatEnemy();
            if (target == null || !target.isAlive() || daughter.isInHumanForm()) {
                return false;
            }
            if (daughter.distanceToSqr(target) >= MIN_RANGED_DISTANCE_SQ) {
                return false;
            }

            awayPos = LandRandomPos.getPosAway(daughter, 18, 7, target.position());
            return awayPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = daughter.getNearbyCombatEnemy();
            return target != null
                && target.isAlive()
                && !daughter.isInHumanForm()
                && daughter.distanceToSqr(target) < IDEAL_RANGED_DISTANCE_SQ
                && !daughter.getNavigation().isDone();
        }

        @Override
        public void start() {
            repathTicks = 0;
            moveAway();
        }

        @Override
        public void stop() {
            awayPos = null;
            repathTicks = 0;
            daughter.setSprinting(false);
        }

        @Override
        public void tick() {
            LivingEntity target = daughter.getNearbyCombatEnemy();
            if (target == null) {
                return;
            }

            daughter.faceCombatTarget(target);
            if (repathTicks > 0) {
                repathTicks--;
                return;
            }

            if (daughter.distanceToSqr(target) < MIN_RANGED_DISTANCE_SQ || daughter.getNavigation().isDone()) {
                awayPos = LandRandomPos.getPosAway(daughter, 18, 7, target.position());
                moveAway();
            }
        }

        private void moveAway() {
            if (awayPos != null) {
                daughter.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, 1.25D);
                daughter.setSprinting(true);
                repathTicks = 8;
            }
        }
    }

    private static class DaughterRangedCombatGoal extends Goal {
        private final DaughterEntity daughter;
        private final double speedModifier;

        private DaughterRangedCombatGoal(DaughterEntity daughter, double speedModifier) {
            this.daughter = daughter;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = daughter.getTarget();
            return target != null
                && target.isAlive()
                && !daughter.isInHumanForm()
                && daughter.distanceToSqr(target) >= MIN_RANGED_DISTANCE_SQ;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            daughter.getNavigation().stop();
            daughter.setSprinting(false);
        }

        @Override
        public void tick() {
            LivingEntity target = daughter.getTarget();
            if (target == null) {
                return;
            }

            daughter.faceCombatTarget(target);
            if (daughter.isUsingLockedAnimation()) {
                daughter.getNavigation().stop();
                daughter.setSprinting(false);
                return;
            }

            double distanceSq = daughter.distanceToSqr(target);
            if (distanceSq > MAX_RANGED_DISTANCE_SQ) {
                daughter.getNavigation().moveTo(target, 1.15D);
                daughter.setSprinting(true);
            } else if (distanceSq > IDEAL_RANGED_DISTANCE_SQ && !daughter.getSensing().hasLineOfSight(target)) {
                daughter.getNavigation().moveTo(target, speedModifier);
                daughter.setSprinting(false);
            } else {
                daughter.getNavigation().stop();
                daughter.setSprinting(false);
            }
        }
    }

    private static class DaughterFallbackMeleeGoal extends MeleeAttackGoal {
        private final DaughterEntity daughter;

        private DaughterFallbackMeleeGoal(DaughterEntity daughter, double speedModifier) {
            super(daughter, speedModifier, false);
            this.daughter = daughter;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = daughter.getTarget();
            return target != null
                && daughter.distanceToSqr(target) < MIN_RANGED_DISTANCE_SQ
                && !daughter.isInHumanForm()
                && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = daughter.getTarget();
            return target != null
                && daughter.distanceToSqr(target) < MIN_RANGED_DISTANCE_SQ
                && !daughter.isInHumanForm()
                && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = this.mob.getTarget();
            if (target != null && this.mob.distanceToSqr(target) <= this.getAttackReachSqr(target)) {
                this.mob.getNavigation().stop();
            }
        }
    }
}
