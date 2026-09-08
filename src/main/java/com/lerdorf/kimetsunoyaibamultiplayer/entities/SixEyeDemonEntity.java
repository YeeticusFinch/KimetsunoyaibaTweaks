package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ImpactParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A large biped demon with a physical combo, choke, and headbutt attacks. */
public class SixEyeDemonEntity extends AbstractDemonEntity {
    private static final String[] COMBO_ANIMATIONS = {
        "punch_left", "punch_right", "punch_overhead",
        "sword_to_left", "sword_to_right", "sword_overhead"
    };
    private static final Set<String> PUNCH_ANIMATIONS = Set.of("punch_left", "punch_right", "punch_overhead");
    private static final int BITE_ANIMATION_TICKS = 15;
    private static final float BITE_CHANCE = 0.20F;
    private static final ImpactParticleOptions IMPACT_PARTICLE =
        new ImpactParticleOptions(27, 94, 32, 1.0F);

    private static final int COMBO_ANIMATION_TICKS = 12;
    private static final int COMBO_COOLDOWN_TICKS = 5;
    private static final double COMBO_RANGE = 3.8D;
    private static final double COMBO_BOX_SIZE = 5.0D;
    private static final double GUARD_POWER = 8.0D;
    private static final double CHOKE_RANGE = 3.0D;
    private static final int CHOKE_DURATION_TICKS = 40;
    private static final int CHOKE_COOLDOWN_TICKS = 160;
    private static final int CHOKE_DAMAGE_INTERVAL = 10;
    private static final float CHOKE_INTERRUPT_DAMAGE = 20.0F;
    private static final net.minecraft.resources.ResourceLocation CHOKE_SUFFOCATION_EFFECT =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "suffocation");
    private static final int HEADBUTT_DURATION_TICKS = 16;
    private static final int HEADBUTT_ANIMATION_TICKS = 18;
    private static final int HEADBUTT_COOLDOWN_TICKS = 180;
    private static final double HEADBUTT_RANGE = 16.0D;
    private static final double HEADBUTT_SPEED = 1.15D;
    private static final float HEADBUTT_STEP_HEIGHT = 3.0F;
    private static final double HEADBUTT_DAMAGE = 14.0D;
    private static final double HEADBUTT_KNOCKBACK = 1.1D;
    private static final float BACKSTEP_CHANCE = 0.30F;
    private static final int BACKSTEP_ANIMATION_TICKS = 10;
    private static final int BACKSTEP_COOLDOWN_TICKS = 70;
    private static final double ANIMATION_BROADCAST_RADIUS = 64.0D;

    private int comboIndex;
    private int comboCooldownTicks;
    private int nextBackstepTick;
    private int chokeCooldownTicks;
    private int headbuttCooldownTicks;
    private int headbuttTicks;
    private Vec3 headbuttDirection = Vec3.ZERO;
    private final Set<UUID> headbuttVictims = new HashSet<>();
    private UUID chokeTargetId;
    private boolean chokeTargetHadNoAi;
    private float chokeDamageTaken;

    public SixEyeDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 180.0D)
            .add(Attributes.ATTACK_DAMAGE, 12.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D)
            .add(Attributes.ARMOR, 16.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
            10, true, false, this::canTargetNonDemonVictim));
    }

    @Override
    public void tick() {
        Vec3 previousPosition = position();
        super.tick();
        if (level().isClientSide || !isAlive()) return;

        if (comboCooldownTicks > 0) comboCooldownTicks--;
        if (chokeCooldownTicks > 0) chokeCooldownTicks--;
        if (headbuttCooldownTicks > 0) headbuttCooldownTicks--;

        if (headbuttTicks > 0) {
            tickHeadbutt(previousPosition);
            return;
        }
        if (chokeTargetId != null) {
            tickChoke();
            return;
        }

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            if (tryStartChoke(target)) return;
            tryStartHeadbutt(target);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity target) || !canTargetNonDemonVictim(target)
            || distanceToSqr(target) > COMBO_RANGE * COMBO_RANGE || comboCooldownTicks > 0
            || getAnimationTicks() > 0 || chokeTargetId != null || headbuttTicks > 0) return false;

        if (random.nextFloat() < BITE_CHANCE) {
            return performBite(target);
        }
        String animation = COMBO_ANIMATIONS[comboIndex];
        comboIndex = (comboIndex + 1) % COMBO_ANIMATIONS.length;
        getNavigation().stop();
        faceTarget(target);
        playGeckoAnimation(animation, COMBO_ANIMATION_TICKS);
        setAttackGuardState(COMBO_ANIMATION_TICKS);
        sendAttackVisual(animation);
        damageComboTargets();
        comboCooldownTicks = COMBO_COOLDOWN_TICKS;
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.HOSTILE, 1.0F, 0.85F + random.nextFloat() * 0.2F);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide && chokeTargetId != null) {
            chokeDamageTaken += amount;
            if (chokeDamageTaken >= CHOKE_INTERRUPT_DAMAGE) {
                endChoke(getChokeTarget());
            }
        }
        if (damaged && !level().isClientSide && isAlive()
            && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            tryBackstep(attacker);
        }
        return damaged;
    }

    private void setAttackGuardState(int duration) {
        GuardStateHelper.setGuardState(this, GUARD_POWER, 0.0D, false);
        GuardStateHelper.setAttackState(this, getAttributeValue(Attributes.ATTACK_DAMAGE), false);
        AbilityScheduler.scheduleOnce(this, () -> GuardStateHelper.clearGuardState(this), duration);
    }

    private void sendAttackVisual(String animation) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (PUNCH_ANIMATIONS.contains(animation)) {
            sendImpactParticles(serverLevel);
        } else {
            ModNetworking.sendToNearby(new MobSwordSlashPacket(getUUID(), animation, 0, "claw"),
                serverLevel, getX(), getY(), getZ(), ANIMATION_BROADCAST_RADIUS);
        }
    }

    private boolean performBite(LivingEntity target) {
        getNavigation().stop();
        faceTarget(target);
        playGeckoAnimation("bite", BITE_ANIMATION_TICKS);
        setAttackGuardState(BITE_ANIMATION_TICKS);
        if (level() instanceof ServerLevel serverLevel) {
            sendImpactParticles(serverLevel);
        }
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.15F);
        if (Damager.hurt(this, target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            Vec3 knockback = horizontalForward().scale(0.3D);
            target.push(knockback.x, 0.12D, knockback.z);
        }
        comboCooldownTicks = COMBO_COOLDOWN_TICKS;
        return true;
    }

    private void sendImpactParticles(ServerLevel serverLevel) {
        Vec3 impactPos = position()
            .add(0.0D, getEyeHeight(), 0.0D)
            .add(getLookAngle().normalize().scale(2.0D));
        serverLevel.sendParticles(IMPACT_PARTICLE, impactPos.x, impactPos.y, impactPos.z,
            1, getLookAngle().x * 0.02D, getLookAngle().y * 0.02D,
            getLookAngle().z * 0.02D, 0.12D);
    }

    private void damageComboTargets() {
        Vec3 center = position().add(horizontalForward().scale(COMBO_BOX_SIZE / 1.5D));
        AABB attackBox = new AABB(center, center).inflate(COMBO_BOX_SIZE / 2.0D);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, attackBox,
            candidate -> candidate != this && candidate.isAlive() && canTargetNonDemonVictim(candidate))) {
            if (Damager.hurt(this, target, damage)) {
                Vec3 knockback = horizontalForward().scale(0.45D);
                target.push(knockback.x, 0.18D, knockback.z);
            }
        }
    }

    private void tryBackstep(LivingEntity attacker) {
        if (attacker == null || tickCount < nextBackstepTick || getAnimationTicks() > 0
            || chokeTargetId != null || headbuttTicks > 0 || !onGround()
            || random.nextFloat() >= BACKSTEP_CHANCE) return;
        Vec3 away = new Vec3(getX() - attacker.getX(), 0.0D, getZ() - attacker.getZ());
        if (away.lengthSqr() < 1.0E-4D) away = horizontalForward().scale(-1.0D);
        away = away.normalize();
        setDeltaMovement(away.x, 0.45D, away.z);
        hurtMarked = true;
        playGeckoAnimation("backstep", BACKSTEP_ANIMATION_TICKS);
        nextBackstepTick = tickCount + BACKSTEP_COOLDOWN_TICKS;
    }

    private boolean tryStartHeadbutt(LivingEntity target) {
        if (headbuttCooldownTicks > 0 || getAnimationTicks() > 0 || !onGround()
            || distanceToSqr(target) > HEADBUTT_RANGE * HEADBUTT_RANGE || random.nextFloat() >= 0.025F) return false;
        faceTarget(target);
        headbuttDirection = horizontalForward();
        headbuttTicks = HEADBUTT_DURATION_TICKS;
        headbuttVictims.clear();
        headbuttCooldownTicks = HEADBUTT_COOLDOWN_TICKS;
        getNavigation().stop();
        setNoAi(true);
        MovementHelper.setStepHeight(this, HEADBUTT_STEP_HEIGHT);
        MovementHelper.setVelocity(this, headbuttDirection.scale(HEADBUTT_SPEED));
        playGeckoAnimation("headbutt_1", HEADBUTT_ANIMATION_TICKS);
        setAttackGuardState(HEADBUTT_ANIMATION_TICKS);
        level().playSound(null, blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
            SoundSource.HOSTILE, 1.2F, 0.75F);
        return true;
    }

    private void tickHeadbutt(Vec3 previousPosition) {
        double verticalVelocity = onGround() ? 0.0D : getDeltaMovement().y - 0.08D;
        MovementHelper.setVelocity(this, headbuttDirection.scale(HEADBUTT_SPEED)
            .add(0.0D, verticalVelocity, 0.0D));
        move(MoverType.SELF, getDeltaMovement());
        spawnHeadbuttParticles();
        AABB pathBox = getBoundingBox().expandTowards(position().subtract(previousPosition))
            .inflate(0.45D, 0.25D, 0.45D);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, pathBox,
            candidate -> candidate != this && candidate.isAlive() && !headbuttVictims.contains(candidate.getUUID())
                && canTargetNonDemonVictim(candidate))) {
            headbuttVictims.add(target.getUUID());
            if (Damager.hurt(this, target, (float) HEADBUTT_DAMAGE)) {
                target.push(headbuttDirection.x * HEADBUTT_KNOCKBACK, 0.35D,
                    headbuttDirection.z * HEADBUTT_KNOCKBACK);
            }
        }
        headbuttTicks--;
        if (headbuttTicks <= 0 || onGround() && getDeltaMovement().horizontalDistanceSqr() < 0.01D) {
            headbuttTicks = 0;
            setNoAi(false);
            MovementHelper.resetStepHeight(this);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void spawnHeadbuttParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(0.5D), getZ(),
            1, 0.35D, 0.4D, 0.35D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(0.65D), getZ(),
            4, 0.45D, 0.55D, 0.45D, 0.12D);
    }

    private boolean tryStartChoke(LivingEntity target) {
        if (chokeCooldownTicks > 0 || getAnimationTicks() > 0 || headbuttTicks > 0
            || distanceToSqr(target) > CHOKE_RANGE * CHOKE_RANGE || !isBipedAnimationTarget(target)
            || random.nextFloat() >= 0.018F) return false;
        chokeTargetId = target.getUUID();
        chokeTargetHadNoAi = target instanceof Mob mob && mob.isNoAi();
        chokeDamageTaken = 0.0F;
        chokeCooldownTicks = CHOKE_COOLDOWN_TICKS;
        setNoAi(true);
        getNavigation().stop();
        playGeckoAnimation("choke", CHOKE_DURATION_TICKS);
        sendChokeAnimation(target, false);
        tickChoke();
        return true;
    }

    private void tickChoke() {
        LivingEntity target = getChokeTarget();
        if (target == null || !target.isAlive() || target.level() != level() || getAnimationTicks() <= 0) {
            endChoke(target);
            return;
        }
        freezeForChoke(this);
        freezeForChoke(target);
        applyChokeEffects(target);
        Vec3 forward = horizontalForward();
        Vec3 targetPosition = position().add(forward.scale(1.0D)).add(0.0D, 1.0D, 0.0D);
        setEntityRotation(this, yawTowards(position(), targetPosition));
        setEntityRotation(target, yawTowards(targetPosition, position()));
        target.teleportTo(targetPosition.x, targetPosition.y, targetPosition.z);
        target.setDeltaMovement(Vec3.ZERO);
        if (tickCount % CHOKE_DAMAGE_INTERVAL == 0) Damager.hurt(this, target, 2.0F);
        if (getAnimationTicks() == 1) endChoke(target);
    }

    private void endChoke(LivingEntity target) {
        if (target != null) {
            removeChokeEffects(target);
            if (target instanceof Mob mob) mob.setNoAi(chokeTargetHadNoAi);
            sendChokeAnimation(target, true);
        }
        chokeTargetId = null;
        chokeTargetHadNoAi = false;
        chokeDamageTaken = 0.0F;
        setNoAi(false);
        GuardStateHelper.clearGuardState(this);
        playGeckoAnimation("idle", 0);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && chokeTargetId != null) {
            endChoke(getChokeTarget());
        }
        if (!level().isClientSide && headbuttTicks > 0) {
            MovementHelper.resetStepHeight(this);
        }
        super.remove(reason);
    }

    private void sendChokeAnimation(LivingEntity target, boolean stop) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (target instanceof Player player) {
            AnimationSyncPacket packet = stop
                ? AnimationSyncPacket.createStopPacket(player.getUUID())
                : new AnimationSyncPacket(player.getUUID(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "choked"),
                    0, CHOKE_DURATION_TICKS, true, false);
            ModNetworking.sendToNearby(packet, serverLevel, target.getX(), target.getY(), target.getZ(), ANIMATION_BROADCAST_RADIUS);
        } else if (target instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation(stop ? "idle" : "choked", stop ? 0 : CHOKE_DURATION_TICKS);
        } else if (target instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(stop ? "idle" : "choked", stop ? 0 : CHOKE_DURATION_TICKS);
        } else {
            ModNetworking.sendToNearby(new MobAnimationSyncPacket(target.getId(), stop ? "__stop__" :
                    KimetsunoyaibaMultiplayer.MODID + ":choked"), serverLevel,
                target.getX(), target.getY(), target.getZ(), ANIMATION_BROADCAST_RADIUS);
        }
    }

    private LivingEntity getChokeTarget() {
        if (!(level() instanceof ServerLevel serverLevel) || chokeTargetId == null) return null;
        Entity entity = serverLevel.getEntity(chokeTargetId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean isBipedAnimationTarget(LivingEntity target) {
        if (target instanceof Player || target instanceof DemonSlayerEntity || target instanceof BreathingSlayerEntity
            || target instanceof NamedDemonEntity || target instanceof DaughterEntity || target instanceof MotherEntity
            || target instanceof MantisDemonEntity || target instanceof SwampDemonEntity || target instanceof NezukoEntity
            || target instanceof UbuyashikiKidEntity || target instanceof KazumiEntity) return true;
        // Mob Player Animator exposes this method on compatible biped mobs.
        try {
            target.getClass().getMethod("getAnimationStack");
            return target instanceof Mob;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private void freezeForChoke(LivingEntity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        entity.invulnerableTime = Math.max(entity.invulnerableTime, 2);
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    private void applyChokeEffects(LivingEntity target) {
        MobEffect suffocation = ForgeRegistries.MOB_EFFECTS.getValue(CHOKE_SUFFOCATION_EFFECT);
        if (suffocation != null) {
            target.addEffect(new MobEffectInstance(suffocation, CHOKE_DURATION_TICKS + 1,
                0, false, false, false));
        }
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,
            CHOKE_DURATION_TICKS + 1, 0, false, false, false));
    }

    private void removeChokeEffects(LivingEntity target) {
        MobEffect suffocation = ForgeRegistries.MOB_EFFECTS.getValue(CHOKE_SUFFOCATION_EFFECT);
        if (suffocation != null) {
            target.removeEffect(suffocation);
        }
        target.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
    }

    private void faceTarget(LivingEntity target) {
        getLookControl().setLookAt(target, 180.0F, 180.0F);
        setEntityRotation(this, yawTowards(position(), target.position()));
    }

    private Vec3 horizontalForward() {
        Vec3 look = getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static void setEntityRotation(LivingEntity entity, float yaw) {
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.yRotO = yaw;
        entity.yHeadRotO = yaw;
        entity.yBodyRotO = yaw;
    }

    private static float yawTowards(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }

    @Override
    protected String resolveWalkAnimation() {
        return "giant_walk";
    }

    @Override
    protected String resolveSprintAnimation() {
        return "giant_walk";
    }

    @Override
    protected double getSprintEnterSpeed(boolean currentlySprintingAnim) {
        return Double.MAX_VALUE;
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return false;
    }

    @Override
    protected double getMovementAnimationSpeedMultiplier() {
        return 0.5D;
    }

    @Override
    protected double getMinimumMovementAnimationSpeed() {
        return 0.45D;
    }
}
