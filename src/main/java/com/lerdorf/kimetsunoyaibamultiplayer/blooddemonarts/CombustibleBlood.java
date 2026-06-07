package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticleHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateLoveSwordSlashesPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CombustibleBlood {
    public static final String ART_ID = "combustible_blood";

    public static final int FORM_EXPLODING_BLOOD = 3400;
    public static final int FORM_EXPLODING_BLOOD_RUPTURE = 3401;
    public static final int FORM_EXPLODING_BLOOD_HELLFIRE = 3402;
    public static final int FORM_HEMOKINESIS = 3403;
    public static final int FORM_CRAZY_SCRATCHING = 3404;
    public static final int FORM_HEEL_BASH = 3405;
    public static final int FORM_EXPLODING_BLOOD_HEEL_BASH = 3406;
    public static final int FORM_FLYING_KICK = 3407;
    public static final int FORM_NAILS_OF_FURY = 3408;
    public static final int FORM_FRENZIED_KICKS = 3409;
    public static final int FORM_EXPLODING_BLOOD_STRIKE = 3410;
    public static final int FORM_DROP_KICK = 3411;
    public static final int FORM_FIERY_SLASH = 3412;
    public static final int FORM_SPIN_KICK = 3413;

    private static final String[] COMBO_ANIMATIONS = {"punch_right", "punch_left", "kick_right", "kick_left"};
    private static final String COMBO_INDEX_TAG = "CombustibleBloodComboIndex";
    private static final String LAST_LEFT_CLICK_TICK_TAG = "CombustibleBloodLastLeftClickTick";
    private static final String LAST_ABILITY_USE_TICK_TAG = "CombustibleBloodLastAbilityUseTick";
    private static final DustParticleOptions EXPLODING_BLOOD_DUST =
        new DustParticleOptions(new Vector3f(0.45F, 0.03F, 0.09F), 0.65F);
    private static final int EXPLODING_BLOOD_PROJECTILE_COUNT = 3;
    private static final int EXPLODING_BLOOD_LAUNCH_INTERVAL_TICKS = 3;
    private static final int EXPLODING_BLOOD_PROJECTILE_LIFETIME_TICKS = 20;
    private static final double EXPLODING_BLOOD_PROJECTILE_SPEED = 1.1D;
    private static final double EXPLODING_BLOOD_PROJECTILE_HIT_RADIUS = 0.38D;
    private static final double EXPLODING_BLOOD_VORTEX_RADIUS = 3.0D;
    private static final double EXPLODING_BLOOD_VORTEX_HEIGHT = 7.0D;
    private static final int EXPLODING_BLOOD_VORTEX_DURATION_TICKS = 34;
    private static final int EXPLODING_BLOOD_VORTEX_EFFECT_INTERVAL = 6;
    private static final float EXPLODING_BLOOD_VORTEX_DEMON_DAMAGE = 4.0F;
    private static final int HELLFIRE_KICK_TICKS = 10;
    private static final int HELLFIRE_BACKFLIP_TICKS = 16;
    private static final int HELLFIRE_TORNADO_TICKS = 15;
    private static final double HELLFIRE_KICK_FORWARD_RANGE = 2.6D;
    private static final double HELLFIRE_KICK_HALF_WIDTH = 1.4D;
    private static final double HELLFIRE_KICK_HEIGHT = 1.8D;
    private static final double HELLFIRE_KICK_KNOCKBACK = 1.15D;
    private static final double HELLFIRE_HEEL_LAUNCH_SPEED = 0.85D;
    private static final double HELLFIRE_HEEL_LAUNCH_UPWARD = 0.32D;
    private static final double HELLFIRE_TORNADO_SPEED = 0.95D;
    private static final double HELLFIRE_TORNADO_HIT_RADIUS = 2.1D;
    private static final double HELLFIRE_TORNADO_EXPLOSION_RADIUS = 5.0D;
    private static final double HELLFIRE_TORNADO_EXPLOSION_HEIGHT = 3.2D;
    private static final double HEMOKINESIS_RANGE = 20.0D;
    private static final int HEMOKINESIS_DURATION_TICKS = 240;
    private static final double HEMOKINESIS_BEAM_START_CONTROL = 4.0D;
    private static final double HEMOKINESIS_BEAM_END_CONTROL = 3.0D;
    private static final double HEMOKINESIS_BEAM_POINT_STEP = 0.06D;
    private static final double HEMOKINESIS_TARGET_PULL_POINT_OFFSET = 8.0D;
    private static final double HEMOKINESIS_TARGET_VELOCITY_SCALE = 0.5D;
    private static final double HEMOKINESIS_ENTITY_SEARCH_INFLATE = 3.0D;
    private static final double HEMOKINESIS_ENTITY_HIT_INFLATE = 0.35D;
    private static final double HEMOKINESIS_FALLING_BLOCK_MAX_SPEED = 0.9D;
    private static final double HEMOKINESIS_FALLING_BLOCK_GROUND_CHECK = 0.2D;
    private static final double HEMOKINESIS_FALLING_BLOCK_LIFT_STEP = 0.25D;
    private static final int HEMOKINESIS_FALLING_BLOCK_LIFT_ATTEMPTS = 6;
    private static final double HEMOKINESIS_FALLING_BLOCK_ESCAPE_SPEED = 0.35D;
    private static final int RUPTURE_KICK_PHASE_TICKS = 10;
    private static final double RUPTURE_KICK_SPEED = 1.5D;
    private static final double RUPTURE_KICK_HIT_RADIUS = 2.0D;
    private static final int RUPTURE_ATTACK_COUNT = 4;
    private static final int RUPTURE_ATTACK_INTERVAL_TICKS = 6;
    private static final int RUPTURE_HEEL_DROP_TICKS = 20;
    private static final int RUPTURE_HEEL_IMPACT_DELAY_TICKS = 8;
    private static final int RUPTURE_RIFT_DELAY_TICKS = 6;
    private static final int RUPTURE_RIFT_POINT_COUNT = 6;
    private static final double RUPTURE_RIFT_RADIUS = 6.0D;
    private static final double RUPTURE_RIFT_BEAM_HEIGHT = 15.0D;
    private static final int RUPTURE_RIFT_BEAM_DURATION_TICKS = 5;
    private static final double RUPTURE_RIFT_BEAM_HIT_RADIUS = 2.0D;
    private static final String RUPTURE_SLASH_MODEL_KEY = "claw_nezuko";
    private static final int RUPTURE_SLASH_DURATION_MS = 150;
    private static final float RUPTURE_SLASH_ARC_RANGE = 160.0F;
    private static final int CRAZY_SCRATCHING_ATTACK_COUNT = 8;
    private static final int CRAZY_SCRATCHING_ATTACK_INTERVAL_TICKS = 6;
    private static final double CLAW_ATTACK_FORWARD_DISTANCE = 3.0D;
    private static final double CLAW_ATTACK_RADIUS = 3.0D;
    private static final int HEEL_BASH_ASCENT_TICKS = 15;
    private static final int HEEL_BASH_DROP_TICKS = 30;
    private static final int HEEL_BASH_TOTAL_TICKS = HEEL_BASH_ASCENT_TICKS + HEEL_BASH_DROP_TICKS;
    private static final double HEEL_BASH_BACKFLIP_SPEED = 0.28D;
    private static final double HEEL_BASH_BACKFLIP_UPWARD = 0.72D;
    private static final double HEEL_BASH_DOWNWARD_SPEED = -1.85D;
    private static final float HEEL_BASH_EXPLOSION_POWER = 2.6F;
    private static final double EXPLODING_HEEL_BASH_TELEPORT_HEIGHT = 20.0D;
    private static final int FLYING_KICK_ATTACK_TICKS = 10;
    private static final int FLYING_KICK_BACKFLIP_TICKS = 14;
    private static final double FLYING_KICK_SPEED = 1.35D;
    private static final double FLYING_KICK_HIT_RADIUS = 1.8D;
    private static final double FLYING_KICK_KNOCKBACK = 1.0D;
    private static final int NAILS_OF_FURY_TICKS = 10;
    private static final int FRENZIED_KICK_PHASE_TICKS = 5;
    private static final double FRENZIED_KICK_RADIUS = 3.0D;
    private static final double FRENZIED_SPIN_RADIUS = 4.0D;
    private static final double FRENZIED_UPWARD_VELOCITY = 0.85D;
    private static final double FRENZIED_SPIN_KNOCKBACK = 1.45D;
    private static final int EXPLODING_BLOOD_STRIKE_TICKS = 20;
    private static final double EXPLODING_BLOOD_STRIKE_SPEED = 1.15D;
    private static final double EXPLODING_BLOOD_STRIKE_HIT_RADIUS = 3.0D;
    private static final double EXPLODING_BLOOD_STRIKE_TRAIL_LENGTH = 4.5D;
    private static final int EXPLODING_BLOOD_STRIKE_TRAIL_POINTS = 12;
    private static final int DROP_KICK_TICKS = 12;
    private static final double DROP_KICK_RANGE = 18.0D;
    private static final double DROP_KICK_HIT_RADIUS = 3.0D;
    private static final int DROP_KICK_TRAIL_PARTICLES = 60;
    private static final int FIERY_SLASH_TICKS = 10;
    private static final double FIERY_SLASH_HIT_RADIUS = 4.0D;
    private static final int SPIN_KICK_TICKS = 16;
    private static final double SPIN_KICK_VORTEX_DISTANCE = 3.0D;

    private CombustibleBlood() {
    }

    public static void register() {
        if (BloodDemonArtRegistry.isRegistered(ART_ID)) {
            return;
        }

        KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Combustible Blood", createTechnique());
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Combustible Blood",
            getOrderedForms(),
            0xFF6B88
        );
    }

    public static List<BloodDemonArtForm> getOrderedForms() {
        return List.of(
            explodingBloodForm(),
            hemokinesisForm(),
            crazyScratchingForm(),
            heelBashForm(),
            explodingBloodHeelBashForm(),
            flyingKickForm(),
            nailsOfFuryForm(),
            frenziedKicksForm(),
            explodingBloodStrikeForm(),
            dropKickForm(),
            fierySlashForm(),
            spinKickForm(),
            explodingBloodRuptureForm(),
            explodingBloodHellfire()
        );
    }

    private static boolean isValidTarget(LivingEntity source, LivingEntity target) {
        if (source == null || target == null) {
			return false;
		}
        return source != target && target.isAlive() && !target.isSpectator();
    }

    private static BloodDemonArtForm explodingBloodForm() {
        return new BloodDemonArtForm(
            FORM_EXPLODING_BLOOD,
            "Exploding Blood",
            "Release three blood bursts that erupt into blood flame vortexes.",
            7,
            CombustibleBlood::executeExplodingBlood
        );
    }

    private static BloodDemonArtForm explodingBloodRuptureForm() {
        return new BloodDemonArtForm(
                FORM_EXPLODING_BLOOD_RUPTURE,
                "Exploding Blood, Rupture",
                "Release a flurry of attacks followed by a kick powered by flaming blood.",
                9,
                CombustibleBlood::executeExplodingBloodRupture);
    }
    
    private static BloodDemonArtForm explodingBloodHellfire() {
        return new BloodDemonArtForm(
            FORM_EXPLODING_BLOOD_HELLFIRE,
            "Exploding Blood, Hellfire",
            "Release a powerful kick followe by a flaming blood tornado.",
            9,
            CombustibleBlood::executeExplodingBloodHellfire
        );
    }

    private static BloodDemonArtForm crazyScratchingForm() {
        return new BloodDemonArtForm(
            FORM_CRAZY_SCRATCHING,
            "Crazy Scratching",
            "Unleash a flurry of claw attacks.",
            4,
            CombustibleBlood::executeCrazyScratching
        );
    }
    private static BloodDemonArtForm heelBashForm() {
        return new BloodDemonArtForm(
            FORM_HEEL_BASH,
            "Heel Bash",
            "Backflip upwards and drop down with an explosive heel bash.",
            6,
            CombustibleBlood::executeHeelBash
        );
    }
    private static BloodDemonArtForm explodingBloodHeelBashForm() {
        return new BloodDemonArtForm(
            FORM_EXPLODING_BLOOD_HEEL_BASH,
            "Exploding Blood, Heel Bash",
            "Teleport upwards into the air, and drop down with a flaming heel bash.",
            8,
            CombustibleBlood::executeExplodingBloodHeelBash
        );
    }
    private static BloodDemonArtForm flyingKickForm() {
        return new BloodDemonArtForm(
            FORM_FLYING_KICK,
            "Flying Kick",
            "Perform a dropkick and retreat with a backflip.",
            5,
            CombustibleBlood::executeFlyingKick
        );
    }
    private static BloodDemonArtForm nailsOfFuryForm() {
        return new BloodDemonArtForm(
            FORM_NAILS_OF_FURY,
            "Nails of Fury",
            "Unleash a powerful uppercut claw attack.",
            6,
            CombustibleBlood::executeNailsOfFury
        );
    }
    private static BloodDemonArtForm frenziedKicksForm() {
        return new BloodDemonArtForm(
            FORM_FRENZIED_KICKS,
            "Frenzied Kicks",
            "Release two upward kicks followed by a horizontal spin kick.",
            2,
            CombustibleBlood::executeFrenziedKicks
        );
    }
    private static BloodDemonArtForm explodingBloodStrikeForm() {
        return new BloodDemonArtForm(
            FORM_EXPLODING_BLOOD_STRIKE,
            "Exploding Blood, Strike",
            "Lunge forward and do a flaming jab.",
            7,
            CombustibleBlood::executeExplodingBloodStrike
        );
    }
    private static BloodDemonArtForm dropKickForm() {
        return new BloodDemonArtForm(
            FORM_DROP_KICK,
            "Drop Kick",
            "Teleport forward with a powerful dropkick.",
            7,
            CombustibleBlood::executeDropKick
        );
    }
    private static BloodDemonArtForm fierySlashForm() {
        return new BloodDemonArtForm(
            FORM_FIERY_SLASH,
            "Fiery Sash",
            "Unleash an X-shaped slash that engulfs the target in blood flames.",
            6,
            CombustibleBlood::executeFierySlash
        );
    }
    private static BloodDemonArtForm spinKickForm() {
        return new BloodDemonArtForm(
            FORM_SPIN_KICK,
            "Spin Kick",
            "Unleash a vertical spinning kick, unleashing a flaming blood vortex.",
            6,
            CombustibleBlood::executeSpinKick
        );
    }

    private static void executeCrazyScratching(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 6F);
        float halfDamage = damage * 0.5F;
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);

        int duration = CRAZY_SCRATCHING_ATTACK_COUNT * CRAZY_SCRATCHING_ATTACK_INTERVAL_TICKS;
        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int localTick = 0;

            @Override
            public void run() {
                if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                    return;
                }

                MovementHelper.lookAtTarget(entity);
                if (localTick % CRAZY_SCRATCHING_ATTACK_INTERVAL_TICKS == 0) {
                    int attackIndex = localTick / CRAZY_SCRATCHING_ATTACK_INTERVAL_TICKS;
                    boolean useLeftAttack = (attackIndex & 1) == 0;
                    playAnimation(entity, useLeftAttack ? "sword_to_left" : "left_sword_to_right",
                        CRAZY_SCRATCHING_ATTACK_INTERVAL_TICKS);
                    spawnRuptureSlashEntity(currentLevel, entity, useLeftAttack, attackIndex);
                    damageTargetsNearPoint(entity, currentLevel, getForwardHitCenter(entity), CLAW_ATTACK_RADIUS, damage, true);
                }

                localTick++;
            }
        }, 1, duration);

        AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), duration);
    }

    private static void executeHeelBash(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 12F);
        float halfDamage = damage * 0.5F;
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);

        playAnimation(entity, "backflip", HEEL_BASH_ASCENT_TICKS);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                return;
            }

            Vec3 backflipVelocity = getSafeLookVector(entity)
                .scale(-HEEL_BASH_BACKFLIP_SPEED)
                .add(0.0D, HEEL_BASH_BACKFLIP_UPWARD, 0.0D);
            MovementHelper.setVelocity(entity, backflipVelocity);
        }, 1, HEEL_BASH_ASCENT_TICKS);

        scheduleHeelBashDrop(entity, damage, true, 3, HEEL_BASH_ASCENT_TICKS);
    }

    private static void executeExplodingBloodHeelBash(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 11F);
        float halfDamage = damage * 0.5F;
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);

        entity.teleportTo(entity.getX(), entity.getY() + EXPLODING_HEEL_BASH_TELEPORT_HEIGHT, entity.getZ());
        MovementHelper.setVelocity(entity, Vec3.ZERO);

        scheduleHeelBashDrop(entity, damage, true, 4, HEEL_BASH_ASCENT_TICKS);
    }

    private static void executeFlyingKick(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 9F);
        float halfDamage = damage * 0.5F;
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);

        playAnimation(entity, "kick_flying", FLYING_KICK_ATTACK_TICKS);
        final Vec3 launchDirection = getSafeHorizontalLookVector(entity);
        final Set<java.util.UUID> hitTargets = new HashSet<>();

        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                return;
            }

            MovementHelper.lookAtTarget(entity);
            MovementHelper.setVelocity(entity, launchDirection.scale(FLYING_KICK_SPEED));
            for (LivingEntity target : getValidTargetsNear(activeLevel, entity,
                entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), FLYING_KICK_HIT_RADIUS)) {
                if (!hitTargets.add(target.getUUID())) {
                    continue;
                }

                Damager.hurt(entity, target, damage, true);
                MovementHelper.addVelocity(target, launchDirection.scale(FLYING_KICK_KNOCKBACK).add(0.0D, 0.25D, 0.0D));
            }
        }, 1, FLYING_KICK_ATTACK_TICKS);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                cleanupForm(entity);
                return;
            }

            playAnimation(entity, "backflip", FLYING_KICK_BACKFLIP_TICKS);
            AbilityScheduler.scheduleRepeating(entity, () -> {
                if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                    return;
                }

                MovementHelper.setVelocity(entity, launchDirection.scale(-0.32D).add(0.0D, 0.58D, 0.0D));
            }, 1, FLYING_KICK_BACKFLIP_TICKS);

            AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), FLYING_KICK_BACKFLIP_TICKS);
        }, FLYING_KICK_ATTACK_TICKS);
    }

    private static void executeNailsOfFury(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 10F);
        float halfDamage = damage * 0.5F;
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);

        playAnimation(entity, "sword_to_upper", NAILS_OF_FURY_TICKS);
        spawnNailsOfFurySlash((ServerLevel) level, entity);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                cleanupForm(entity);
                return;
            }

            for (LivingEntity target : getValidTargetsNear(currentLevel, entity, getForwardHitCenter(entity), CLAW_ATTACK_RADIUS)) {
                Damager.hurt(entity, target, damage, true);
                MovementHelper.setVelocity(target, target.getDeltaMovement().x, Math.max(target.getDeltaMovement().y, 1.25D),
                    target.getDeltaMovement().z);
            }
        }, 2);

        AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), NAILS_OF_FURY_TICKS);
    }

    private static void executeFrenziedKicks(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 8F);
        float halfDamage = damage * 0.5F;
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);

        playAnimation(entity, "kick_left", FRENZIED_KICK_PHASE_TICKS);
        applyFrenziedUpwardKick(entity, FRENZIED_KICK_PHASE_TICKS / 2, halfDamage);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                cleanupForm(entity);
                return;
            }

            playAnimation(entity, "kick_right", FRENZIED_KICK_PHASE_TICKS);
            applyFrenziedUpwardKick(entity, FRENZIED_KICK_PHASE_TICKS / 2, halfDamage);
        }, FRENZIED_KICK_PHASE_TICKS);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                cleanupForm(entity);
                return;
            }

            playAnimation(entity, "kick_rotate2", FRENZIED_KICK_PHASE_TICKS);
            AbilityScheduler.scheduleOnce(entity, () -> {
                if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                    cleanupForm(entity);
                    return;
                }

                Vec3 forward = getSafeHorizontalLookVector(entity);
                for (LivingEntity target : getValidTargetsNear(currentLevel, entity, getForwardHitCenter(entity), FRENZIED_SPIN_RADIUS)) {
                    Damager.hurt(entity, target, damage, true);
                    Vec3 knockback = target.position().subtract(entity.position());
                    if (knockback.horizontalDistanceSqr() < 1.0E-4D) {
                        knockback = forward;
                    } else {
                        knockback = new Vec3(knockback.x, 0.0D, knockback.z).normalize();
                    }
                    MovementHelper.addVelocity(target, knockback.scale(FRENZIED_SPIN_KNOCKBACK).add(0.0D, 0.35D, 0.0D));
                }
            }, FRENZIED_KICK_PHASE_TICKS / 2);
        }, FRENZIED_KICK_PHASE_TICKS * 2);

        AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), FRENZIED_KICK_PHASE_TICKS * 3);
    }

    private static void executeExplodingBloodStrike(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 8F);
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);
        playAnimation(entity, "speed_attack_punch", EXPLODING_BLOOD_STRIKE_TICKS);

        final Vec3 strikeDirection = getSafeHorizontalLookVector(entity);
        final Set<java.util.UUID> hitTargets = new HashSet<>();

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            @Override
            public void run() {
                if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                    return;
                }

                MovementHelper.lookAtTarget(entity);
                MovementHelper.setVelocity(entity, strikeDirection.scale(EXPLODING_BLOOD_STRIKE_SPEED));
                spawnExplodingBloodStrikeTrail(currentLevel, entity, strikeDirection);

                for (LivingEntity target : getValidTargetsNear(currentLevel, entity,
                    entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D),
                    EXPLODING_BLOOD_STRIKE_HIT_RADIUS)) {
                    if (!hitTargets.add(target.getUUID())) {
                        continue;
                    }

                    bloodFlameHit(entity, target, damage);
                }
            }
        }, 1, EXPLODING_BLOOD_STRIKE_TICKS);

        AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), EXPLODING_BLOOD_STRIKE_TICKS);
    }

    private static void executeDropKick(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity);
        float damage = DamageCalculator.calculateScaledDamage(entity, 9F);
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);

        Vec3 startPos = entity.position();
        Vec3 targetPos = findDropKickTeleportPosition(entity, serverLevel, DROP_KICK_RANGE);
        ParticleHelper.spawnParticleLine(serverLevel,
            startPos.add(0.0D, entity.getBbHeight() * 0.55D, 0.0D),
            targetPos.add(0.0D, entity.getBbHeight() * 0.55D, 0.0D),
            ModParticles.BLOOD_FLAME.get(),
            DROP_KICK_TRAIL_PARTICLES);

        entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        playAnimation(entity, "kick_rotate2", DROP_KICK_TICKS);
        spawnDropKickBloodFlameBurst(serverLevel, entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));

        for (LivingEntity target : getValidTargetsNear(serverLevel, entity,
            entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), DROP_KICK_HIT_RADIUS)) {
            bloodFlameHit(entity, target, damage);
        }

        AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), DROP_KICK_TICKS);
    }

    private static void executeFierySlash(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity);
        float damage = DamageCalculator.calculateScaledDamage(entity, 9F);
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);
        playAnimation(entity, "beast2", FIERY_SLASH_TICKS);
        spawnFierySlashX(serverLevel, entity);

        Vec3 hitCenter = getForwardHitCenter(entity);
        for (LivingEntity target : getValidTargetsNear(serverLevel, entity, hitCenter, FIERY_SLASH_HIT_RADIUS)) {
            bloodFlameHit(entity, target, damage);
        }

        AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), FIERY_SLASH_TICKS);
    }

    private static void executeSpinKick(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity);
        float damage = DamageCalculator.calculateScaledDamage(entity, 6F);
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);
        playAnimation(entity, "kick_rotate5", SPIN_KICK_TICKS);

        Vec3 vortexPos = entity.position().add(getSafeHorizontalLookVector(entity).scale(SPIN_KICK_VORTEX_DISTANCE));
        spawnExplodingBloodVortex(entity, serverLevel, vortexPos);

        AbilityScheduler.scheduleOnce(entity, () -> cleanupForm(entity), SPIN_KICK_TICKS);
    }

    private static void executeExplodingBloodRupture(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 6F);
        float halfDamage = damage * 0.5F;
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);
        playAnimation(entity, "kick_flying", RUPTURE_KICK_PHASE_TICKS);

        final Vec3 launchDirection = getSafeLookVector(entity);
        final LivingEntity[] primaryTarget = {null};
        final Set<java.util.UUID> kickHitTargets = new HashSet<>();

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                    return;
                }

                MovementHelper.lookAtTarget(entity);
                MovementHelper.setVelocity(entity, launchDirection.scale(RUPTURE_KICK_SPEED));

                Vec3 currentVelocity = entity.getDeltaMovement();
                Vec3 knockbackDirection = currentVelocity.lengthSqr() < 1.0E-4D ? launchDirection : currentVelocity.normalize();

                AABB kickArea = entity.getBoundingBox().inflate(RUPTURE_KICK_HIT_RADIUS);
                List<LivingEntity> kickTargets = activeLevel.getEntitiesOfClass(LivingEntity.class, kickArea,
                    living -> living.isAlive() && living != entity && !living.isSpectator());

                for (LivingEntity target : kickTargets) {
                    if (!isValidTarget(entity, target) || !kickHitTargets.add(target.getUUID())) {
                        continue;
                    }

                    Damager.hurt(entity, target, damage, true);
                    MovementHelper.addVelocity(target, knockbackDirection.scale(0.9D));

                    if (primaryTarget[0] == null) {
                        primaryTarget[0] = target;
                    }
                }

                tick++;
            }
        }, 1, RUPTURE_KICK_PHASE_TICKS);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                return;
            }

            LivingEntity lockedTarget = primaryTarget[0];
            final boolean hasLockedTarget = lockedTarget != null && lockedTarget.isAlive() && !lockedTarget.isSpectator();
            if (hasLockedTarget) {
                lockedTarget.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    100,
                    4,
                    true,
                    false,
                    false
                ));
            }

            final Vec3[] heelImpactPos = {null};
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int localTick = 0;

                @Override
                public void run() {
                    if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                        return;
                    }

                    if (hasLockedTarget && lockedTarget.isAlive() && !lockedTarget.isSpectator()) {
                        if (!(entity instanceof Player)) {
                            MovementHelper.lookAt(entity, lockedTarget.position().add(0.0D, lockedTarget.getEyeHeight() * 0.5D, 0.0D));
                        }

                        Vec3 desiredPosition = lockedTarget.position().add(entity.getLookAngle().scale(-1.5D));
                        MovementHelper.setVelocity(entity, desiredPosition.subtract(entity.position()).scale(0.45D));
                    }

                    if (localTick < RUPTURE_ATTACK_COUNT * RUPTURE_ATTACK_INTERVAL_TICKS) {
                        if (localTick % RUPTURE_ATTACK_INTERVAL_TICKS == 0) {
                            int attackIndex = localTick / RUPTURE_ATTACK_INTERVAL_TICKS;
                            boolean useLeftAttack = (attackIndex & 1) == 0;
                            playAnimation(entity, useLeftAttack ? "sword_to_left" : "left_sword_to_right", RUPTURE_ATTACK_INTERVAL_TICKS);
                            spawnRuptureSlashEntity(currentLevel, entity, useLeftAttack, attackIndex);

                            if (hasLockedTarget && lockedTarget.isAlive() && !lockedTarget.isSpectator()) {
                                Vec3 beforeHitVelocity = lockedTarget.getDeltaMovement();
                                Damager.hurt(entity, lockedTarget, halfDamage, true);
                                MovementHelper.setVelocity(lockedTarget, beforeHitVelocity);
                            }
                        }
                    } else if (localTick < RUPTURE_ATTACK_COUNT * RUPTURE_ATTACK_INTERVAL_TICKS + RUPTURE_HEEL_DROP_TICKS) {
                        if (localTick == RUPTURE_ATTACK_COUNT * RUPTURE_ATTACK_INTERVAL_TICKS) {
                            playAnimation(entity, "heel_drop", RUPTURE_HEEL_DROP_TICKS);
                        }

                        if (localTick == RUPTURE_ATTACK_COUNT * RUPTURE_ATTACK_INTERVAL_TICKS + RUPTURE_HEEL_IMPACT_DELAY_TICKS) {
                            heelImpactPos[0] = getHeelImpactPosition(entity);
                            triggerRuptureHeelImpact(currentLevel, heelImpactPos[0]);
                        }

                        if (localTick == RUPTURE_ATTACK_COUNT * RUPTURE_ATTACK_INTERVAL_TICKS
                            + RUPTURE_HEEL_IMPACT_DELAY_TICKS
                            + RUPTURE_RIFT_DELAY_TICKS) {
                            Vec3 impactPos = heelImpactPos[0] != null ? heelImpactPos[0] : getHeelImpactPosition(entity);
                            triggerRuptureBloodFlameBurst(entity, currentLevel, impactPos, damage);
                        }
                    }

                    localTick++;
                }
            }, 1, RUPTURE_ATTACK_COUNT * RUPTURE_ATTACK_INTERVAL_TICKS + RUPTURE_HEEL_DROP_TICKS);

            AbilityScheduler.scheduleOnce(entity, () -> {
                GuardStateHelper.clearGuardState(entity);
                GuardStateHelper.clearAttackFlag(entity);
                playAnimation(entity, "cancel", 1);
            }, RUPTURE_ATTACK_COUNT * RUPTURE_ATTACK_INTERVAL_TICKS + RUPTURE_HEEL_DROP_TICKS);
        }, RUPTURE_KICK_PHASE_TICKS);
    }

    private static void spawnRuptureSlashEntity(ServerLevel level, LivingEntity entity, boolean useLeftAttack, int attackIndex) {
        boolean largerSlash = attackIndex >= 2;
        float vert = useLeftAttack ? -2.0F : 2.0F;
        float rollOffset = useLeftAttack ? -20.0F : 20.0F;
        float angleOffset = useLeftAttack ? -15.0F : 15.0F;
        float radiusScaler = largerSlash ? 0.9F : 0.4F;
        float sizeScaler = largerSlash ? 1.6F : 1.1F;
        String animationName = (useLeftAttack ? "sword_to_left" : "left_sword_to_right") + "_rupture_" + attackIndex;

        BonePositionTracker.sendRawHorizontalSlashToClients(
            level,
            new Vec3(0.0D, 0.5D, 0.0D),
            RUPTURE_SLASH_MODEL_KEY,
            vert,
            false,
            RUPTURE_SLASH_ARC_RANGE,
            RUPTURE_SLASH_DURATION_MS,
            0.0F,
            0.0F,
            rollOffset,
            radiusScaler,
            sizeScaler,
            angleOffset,
            entity.getUUID(),
            animationName
        );
    }
    
    private static void executeExplodingBloodHellfire(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        float damage = DamageCalculator.calculateScaledDamage(entity, 6F);
        GuardStateHelper.setGuardState(entity, damage, formId);
        GuardStateHelper.setAttackState(entity, damage);
        playAnimation(entity, "kick_right", HELLFIRE_KICK_TICKS);

        final Set<java.util.UUID> kickHitTargets = new HashSet<>();
        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            @Override
            public void run() {
                if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                    return;
                }

                MovementHelper.lookAtTarget(entity);
                applyHellfireKickHits(entity, activeLevel, damage, kickHitTargets);
            }
        }, 1, HELLFIRE_KICK_TICKS);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                return;
            }

            MovementHelper.lookAtTarget(entity);
            playAnimation(entity, "heel_drop", RUPTURE_HEEL_DROP_TICKS);
            Vec3 heelLaunch = getSafeLookVector(entity)
                .scale(HELLFIRE_HEEL_LAUNCH_SPEED)
                .add(0.0D, HELLFIRE_HEEL_LAUNCH_UPWARD, 0.0D);
            MovementHelper.setVelocity(entity, heelLaunch);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int localTick = 0;

                @Override
                public void run() {
                    if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                        return;
                    }

                    if (localTick == RUPTURE_HEEL_IMPACT_DELAY_TICKS) {
                        Vec3 heelImpactPos = getHeelImpactPosition(entity);
                        triggerRuptureHeelImpact(currentLevel, heelImpactPos);
                    }

                    localTick++;
                }
            }, 1, RUPTURE_HEEL_DROP_TICKS);
        }, HELLFIRE_KICK_TICKS);

        int backflipStartTick = HELLFIRE_KICK_TICKS + RUPTURE_HEEL_DROP_TICKS;
        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                return;
            }

            playAnimation(entity, "backflip", HELLFIRE_BACKFLIP_TICKS);
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                @Override
                public void run() {
                    if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                        return;
                    }
                    MovementHelper.setVelocity(entity, getSafeLookVector(entity).scale(-0.2D).add(0.0D, 0.5D, 0.0D));
                }
            }, 1, HELLFIRE_BACKFLIP_TICKS);
        }, backflipStartTick);

        int tornadoStartTick = backflipStartTick + HELLFIRE_BACKFLIP_TICKS;
        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                return;
            }

            Vec3 tornadoDirection = getHellfireTornadoDirection(entity);
            playAnimation(entity, "tilted_spin", HELLFIRE_TORNADO_TICKS);
            LoveSwordSlashesEntity tornadoSlash = LoveSwordSlashesSpawner.spawnNezukoTornado(
                activeLevel,
                getHellfireTornadoSlashPosition(entity),
                tornadoDirection,
                HELLFIRE_TORNADO_TICKS
            );

            final Set<java.util.UUID> tornadoHitTargets = new HashSet<>();
            final boolean[] exploded = {false};
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int localTick = 0;

                @Override
                public void run() {
                    if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                        discardHellfireTornadoSlash(tornadoSlash);
                        return;
                    }

                    MovementHelper.setVelocity(entity, tornadoDirection.scale(HELLFIRE_TORNADO_SPEED));
                    updateHellfireTornadoSlash(currentLevel, tornadoSlash, entity, tornadoDirection);
                    applyHellfireTornadoHits(entity, currentLevel, damage, tornadoHitTargets);

                    if (!exploded[0] && localTick > 0 && isHellfireTornadoGroundImpact(entity, currentLevel)) {
                        exploded[0] = true;
                        triggerHellfireTornadoExplosion(entity, currentLevel, entity.position(), damage);
                    }

                    localTick++;
                }
            }, 1, HELLFIRE_TORNADO_TICKS);

            AbilityScheduler.scheduleOnce(entity, () -> {
                if (!exploded[0] && entity.level() instanceof ServerLevel currentLevel && entity.isAlive()) {
                    exploded[0] = true;
                    triggerHellfireTornadoExplosion(entity, currentLevel, entity.position(), damage);
                }
                discardHellfireTornadoSlash(tornadoSlash);
                GuardStateHelper.clearGuardState(entity);
                GuardStateHelper.clearAttackFlag(entity);
                playAnimation(entity, "cancel", 1);
                
            }, HELLFIRE_TORNADO_TICKS);
        }, tornadoStartTick);
    }

    private static void applyHellfireKickHits(
        LivingEntity entity,
        ServerLevel level,
        float damage,
        Set<java.util.UUID> hitTargets
    ) {
        Vec3 forward = getSafeLookVector(entity);
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-4D) {
            horizontalForward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontalForward = horizontalForward.normalize();

        Vec3 center = entity.position()
            .add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)
            .add(horizontalForward.scale(HELLFIRE_KICK_FORWARD_RANGE * 0.5D));
        AABB searchArea = new AABB(
            center.x - HELLFIRE_KICK_FORWARD_RANGE,
            center.y - HELLFIRE_KICK_HEIGHT,
            center.z - HELLFIRE_KICK_FORWARD_RANGE,
            center.x + HELLFIRE_KICK_FORWARD_RANGE,
            center.y + HELLFIRE_KICK_HEIGHT,
            center.z + HELLFIRE_KICK_FORWARD_RANGE
        );

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchArea,
            living -> living.isAlive() && living != entity && !living.isSpectator())) {
            if (!isValidTarget(entity, target) || !hitTargets.add(target.getUUID())) {
                continue;
            }

            Vec3 offset = target.position().subtract(entity.position());
            double forwardDistance = offset.dot(horizontalForward);
            Vec3 sideOffset = offset.subtract(horizontalForward.scale(forwardDistance));
            if (forwardDistance < 0.0D
                || forwardDistance > HELLFIRE_KICK_FORWARD_RANGE
                || sideOffset.horizontalDistance() > HELLFIRE_KICK_HALF_WIDTH) {
                continue;
            }

            Damager.hurt(entity, target, damage, true);
            MovementHelper.addVelocity(target, horizontalForward.scale(HELLFIRE_KICK_KNOCKBACK).add(0.0D, 0.25D, 0.0D));
        }
    }

    private static Vec3 getHellfireTornadoDirection(LivingEntity entity) {
        Vec3 look = getSafeLookVector(entity);
        Vec3 tornadoDirection = new Vec3(look.x, Math.min(0.0D, look.y), look.z);
        if (tornadoDirection.lengthSqr() < 1.0E-4D) {
            tornadoDirection = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return tornadoDirection.normalize();
    }

    private static Vec3 getHellfireTornadoSlashPosition(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private static void updateHellfireTornadoSlash(
        ServerLevel level,
        LoveSwordSlashesEntity tornadoSlash,
        LivingEntity entity,
        Vec3 tornadoDirection
    ) {
        if (tornadoSlash == null || tornadoSlash.isRemoved()) {
            return;
        }

        Vec3 slashPos = getHellfireTornadoSlashPosition(entity);
        float yaw = (float) Math.toDegrees(Math.atan2(-tornadoDirection.x, tornadoDirection.z));
        yaw += 180.0F;
        float pitch = (float) Math.toDegrees(-Math.asin(tornadoDirection.y));
        tornadoSlash.absMoveTo(slashPos.x, slashPos.y, slashPos.z, yaw, pitch);
        tornadoSlash.setPos(slashPos.x, slashPos.y, slashPos.z);
        tornadoSlash.yRotO = yaw;
        tornadoSlash.xRotO = pitch;
        tornadoSlash.setDeltaMovement(Vec3.ZERO);
        tornadoSlash.hasImpulse = true;
        tornadoSlash.hurtMarked = true;

        ModNetworking.sendToNearby(
            new UpdateLoveSwordSlashesPacket(tornadoSlash.getId(), slashPos.x, slashPos.y, slashPos.z, yaw, pitch),
            level,
            slashPos.x,
            slashPos.y,
            slashPos.z,
            96.0D
        );
    }

    private static void applyHellfireTornadoHits(
        LivingEntity entity,
        ServerLevel level,
        float damage,
        Set<java.util.UUID> hitTargets
    ) {
        AABB hitArea = entity.getBoundingBox().inflate(HELLFIRE_TORNADO_HIT_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitArea,
            living -> living.isAlive() && living != entity && !living.isSpectator())) {
            if (!isValidTarget(entity, target) || !hitTargets.add(target.getUUID())) {
                continue;
            }
            bloodFlameHit(entity, target, damage);
        }
    }

    private static boolean isHellfireTornadoGroundImpact(LivingEntity entity, ServerLevel level) {
        if (entity.onGround()) {
            return true;
        }

        BlockPos below = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY - 0.08D, entity.getZ());
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    private static void triggerHellfireTornadoExplosion(
        LivingEntity entity,
        ServerLevel level,
        Vec3 center,
        float damage
    ) {
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.1F, 0.75F);
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 1.0F, 0.85F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.2D, center.z,
            1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ModParticles.BLOOD_FLAME.get(), center.x, center.y + 0.7D, center.z,
            120, 1.1D, 0.7D, 1.1D, 0.08D);
        level.sendParticles(EXPLODING_BLOOD_DUST, center.x, center.y + 0.4D, center.z,
            90, 0.9D, 0.35D, 0.9D, 0.03D);

        AABB explosionArea = new AABB(
            center.x - HELLFIRE_TORNADO_EXPLOSION_RADIUS,
            center.y - 0.75D,
            center.z - HELLFIRE_TORNADO_EXPLOSION_RADIUS,
            center.x + HELLFIRE_TORNADO_EXPLOSION_RADIUS,
            center.y + HELLFIRE_TORNADO_EXPLOSION_HEIGHT,
            center.z + HELLFIRE_TORNADO_EXPLOSION_RADIUS
        );
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, explosionArea,
            living -> living.isAlive() && living != entity && !living.isSpectator())) {
            if (isValidTarget(entity, target)
                && target.position().distanceToSqr(center) <= HELLFIRE_TORNADO_EXPLOSION_RADIUS * HELLFIRE_TORNADO_EXPLOSION_RADIUS) {
                bloodFlameHit(entity, target, damage);
            }
        }
    }

    private static void discardHellfireTornadoSlash(LoveSwordSlashesEntity tornadoSlash) {
        if (tornadoSlash != null && !tornadoSlash.isRemoved()) {
            tornadoSlash.discard();
        }
    }

    private static void executeExplodingBlood(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
        playAnimation(entity, "punch_right", 20);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F, 1.2F);

        Vec3 look = entity.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        final Vec3 forward = look.normalize();

        for (int i = 0; i < EXPLODING_BLOOD_PROJECTILE_COUNT; i++) {
            int index = i;
            AbilityScheduler.scheduleOnce(entity, () -> {
                if (!entity.isAlive() || !(entity.level() instanceof ServerLevel activeLevel)) {
                    return;
                }
                MovementHelper.lookAtTarget(entity); // IMPORTANT: THIS IS SO THAT NON-PLAYER ENTITIES LOOK AT THEIR TARGET BEFORE USING THEIR FORM
                double yawOffset = (index - 1) * 6.0D;
                Vec3 direction = rotateYaw(forward, yawOffset).add(0.0D, 0.02D, 0.0D).normalize();
                activeLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.DISPENSER_DISPENSE, SoundSource.HOSTILE, 0.9F, 1.5F);
                launchExplodingBloodProjectile(entity, direction);
            }, i * EXPLODING_BLOOD_LAUNCH_INTERVAL_TICKS);
        }
    }

    private static void launchExplodingBloodProjectile(LivingEntity caster, Vec3 direction) {
        final Vec3[] currentPos = {caster.getEyePosition().add(direction.scale(0.55D))};
        final boolean[] landed = {false};

        AbilityScheduler.scheduleRepeating(caster, new Runnable() {
            @Override
            public void run() {
                if (!caster.isAlive() || !(caster.level() instanceof ServerLevel activeLevel)) {
                    return;
                }

                if (!landed[0]) {
                    Vec3 nextPos = currentPos[0].add(direction.scale(EXPLODING_BLOOD_PROJECTILE_SPEED));
                    BlockHitResult blockHit = activeLevel.clip(new ClipContext(
                        currentPos[0], nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));

                    if (blockHit.getType() == HitResult.Type.BLOCK) {
                        landed[0] = true;
                        currentPos[0] = blockHit.getLocation();
                    } else {
                        LivingEntity hitTarget = findProjectileHitTarget(caster, activeLevel, currentPos[0], nextPos);
                        if (hitTarget != null) {
                            landed[0] = true;
                            currentPos[0] = new Vec3(hitTarget.getX(), hitTarget.getY(0.5D), hitTarget.getZ());
                        } else {
                            currentPos[0] = nextPos;
                        }
                    }
                }

                spawnExplodingBloodTrail(activeLevel, currentPos[0], direction);
            }
        }, 1, EXPLODING_BLOOD_PROJECTILE_LIFETIME_TICKS);

        AbilityScheduler.scheduleOnce(caster, () -> {
            if (!(caster.level() instanceof ServerLevel activeLevel)) {
                return;
            }
            spawnExplodingBloodVortex(caster, activeLevel, currentPos[0]);
        }, EXPLODING_BLOOD_PROJECTILE_LIFETIME_TICKS);
    }

    private static LivingEntity findProjectileHitTarget(LivingEntity caster, ServerLevel level, Vec3 start, Vec3 end) {
        AABB travelBox = new AABB(start, end).inflate(EXPLODING_BLOOD_PROJECTILE_HIT_RADIUS);
        LivingEntity closest = null;
        double closestSqr = Double.MAX_VALUE;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, travelBox,
            living -> living.isAlive() && living != caster && !living.isSpectator())) {
            Optional<Vec3> clipped = target.getBoundingBox().inflate(0.2D).clip(start, end);
            if (clipped.isEmpty()) {
                continue;
            }
            double distSqr = start.distanceToSqr(clipped.get());
            if (distSqr < closestSqr) {
                closestSqr = distSqr;
                closest = target;
            }
        }

        return closest;
    }

    private static void spawnExplodingBloodTrail(ServerLevel level, Vec3 pos, Vec3 direction) {
        for (int i = 0; i < 3; i++) {
            Vec3 trail = pos.subtract(direction.scale(i * 0.2D));
            level.sendParticles(EXPLODING_BLOOD_DUST, trail.x, trail.y, trail.z, 2, 0.015D, 0.015D, 0.015D, 0.0D);
        }
    }

    private static void spawnExplodingBloodVortex(LivingEntity caster, ServerLevel level, Vec3 center) {
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.8F, 0.75F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.8F, 1.0F);
        level.sendParticles(ModParticles.BLOOD_FLAME.get(), center.x, center.y + 0.35D, center.z,
            85, 0.7D, 0.7D, 0.7D, 0.05D);

        AbilityScheduler.scheduleRepeating(caster, new Runnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!(caster.level() instanceof ServerLevel activeLevel) || !caster.isAlive()) {
                    return;
                }

                spawnVortexParticles(activeLevel, center, tick);
                applyVortexEffects(caster, activeLevel, center, tick);
                tick++;
            }
        }, 1, EXPLODING_BLOOD_VORTEX_DURATION_TICKS);
    }

    private static void spawnVortexParticles(ServerLevel level, Vec3 center, int tick) {
        double phase = tick * 0.45D;
        for (int layer = 0; layer < 14; layer++) {
            double y = center.y + ((layer / 13.0D) * EXPLODING_BLOOD_VORTEX_HEIGHT);
            double radius = EXPLODING_BLOOD_VORTEX_RADIUS * (0.68D + (0.28D * Math.sin((tick + layer) * 0.23D)));
            double heightFactor = layer / 13.0D;

            for (int arm = 0; arm < 3; arm++) {
                double angle = phase + (layer * 0.55D) + (arm * (Math.PI * 2.0D / 3.0D));
                double x = center.x + (Math.cos(angle) * radius);
                double z = center.z + (Math.sin(angle) * radius);
                Vec3 tangent = new Vec3(-Math.sin(angle), 0.28D + (heightFactor * 0.42D), Math.cos(angle)).normalize();
                level.sendParticles(ModParticles.BLOOD_FLAME.get(), x, y, z, 1, tangent.x, tangent.y, tangent.z, 0.0D);
                if ((layer & 1) == 0) {
                    level.sendParticles(EXPLODING_BLOOD_DUST, x, y, z, 2, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }

        for (int i = 0; i < 18; i++) {
            double angle = (tick * 0.35D) + (i * (Math.PI * 2.0D / 18.0D));
            double y = center.y + (EXPLODING_BLOOD_VORTEX_HEIGHT * (i / 18.0D));
            double radius = EXPLODING_BLOOD_VORTEX_RADIUS * 0.55D;
            double x = center.x + (Math.cos(angle) * radius);
            double z = center.z + (Math.sin(angle) * radius);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.34D, Math.cos(angle)).normalize();
            level.sendParticles(ModParticles.BLOOD_FLAME.get(), x, y, z, 1, tangent.x, tangent.y, tangent.z, 0.0D);
        }
    }

    private static void applyVortexEffects(LivingEntity caster, ServerLevel level, Vec3 center, int tick) {
        if (tick % EXPLODING_BLOOD_VORTEX_EFFECT_INTERVAL != 0) {
            return;
        }

        AABB area = new AABB(
                center.x - EXPLODING_BLOOD_VORTEX_RADIUS, center.y, center.z - EXPLODING_BLOOD_VORTEX_RADIUS,
                center.x + EXPLODING_BLOOD_VORTEX_RADIUS, center.y + EXPLODING_BLOOD_VORTEX_HEIGHT,
                center.z + EXPLODING_BLOOD_VORTEX_RADIUS);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && !living.isSpectator())) {
            if (!isInsideVortex(target, center)) {
                continue;
            }

            bloodFlameHit(caster, target, EXPLODING_BLOOD_VORTEX_DEMON_DAMAGE);
        }
    }

    private static void bloodFlameHit(LivingEntity source, LivingEntity target, float damage) {
        if (isExplodingBloodDamageTarget(target)) {
                Damager.hurt(source, target, damage, true);
                target.setDeltaMovement(target.getDeltaMovement().x, Math.max(target.getDeltaMovement().y, 0.85D),
                        target.getDeltaMovement().z);
                target.hurtMarked = true;
                target.setSecondsOnFire(4);
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, true, true));
                target.removeEffect(MobEffects.CONFUSION);
                target.removeEffect(MobEffects.POISON);
                target.removeEffect(MobEffects.WITHER);
            }
    }

    private static void cleanupForm(LivingEntity entity) {
        GuardStateHelper.clearGuardState(entity);
        GuardStateHelper.clearAttackFlag(entity);
        playAnimation(entity, "cancel", 1);
    }

    private static Vec3 getSafeHorizontalLookVector(LivingEntity entity) {
        Vec3 look = getSafeLookVector(entity);
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return horizontalLook.normalize();
    }

    private static Vec3 getForwardHitCenter(LivingEntity entity) {
        return entity.position()
            .add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)
            .add(getSafeHorizontalLookVector(entity).scale(CLAW_ATTACK_FORWARD_DISTANCE));
    }

    private static List<LivingEntity> getValidTargetsNear(
        ServerLevel level,
        LivingEntity source,
        Vec3 center,
        double radius
    ) {
        double radiusSqr = radius * radius;
        AABB area = new AABB(
            center.x - radius,
            center.y - radius,
            center.z - radius,
            center.x + radius,
            center.y + radius,
            center.z + radius
        );
        return level.getEntitiesOfClass(LivingEntity.class, area,
            target -> isValidTarget(source, target)
                && target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceToSqr(center) <= radiusSqr);
    }

    private static void damageTargetsNearPoint(
        LivingEntity source,
        ServerLevel level,
        Vec3 center,
        double radius,
        float damage,
        boolean resetInvulnerability
    ) {
        for (LivingEntity target : getValidTargetsNear(level, source, center, radius)) {
            Damager.hurt(source, target, damage, resetInvulnerability);
        }
    }

    private static void scheduleHeelBashDrop(
        LivingEntity entity,
        float damage,
        boolean spawnVortex,
        int resistanceAmplifier,
        int delayTicks
    ) {
        final boolean[] impacted = {false};

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel) || !entity.isAlive()) {
                cleanupForm(entity);
                return;
            }

            playAnimation(entity, "heel_drop", HEEL_BASH_DROP_TICKS);
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, resistanceAmplifier, true, false, false));
            MovementHelper.setVelocity(entity, 0.0D, HEEL_BASH_DOWNWARD_SPEED, 0.0D);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                @Override
                public void run() {
                    if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive() || impacted[0]) {
                        return;
                    }

                    if (isHeelImpactGrounded(entity, currentLevel)) {
                        impacted[0] = true;
                        triggerHeelBashImpact(entity, currentLevel, getHeelImpactPosition(entity), damage, spawnVortex);
                    }
                }
            }, 1, HEEL_BASH_DROP_TICKS);
        }, delayTicks);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (entity.level() instanceof ServerLevel currentLevel && entity.isAlive() && !impacted[0]) {
                impacted[0] = true;
                triggerHeelBashImpact(entity, currentLevel, getHeelImpactPosition(entity), damage, spawnVortex);
            }
            cleanupForm(entity);
        }, delayTicks + HEEL_BASH_DROP_TICKS);
    }

    private static boolean isHeelImpactGrounded(LivingEntity entity, ServerLevel level) {
        if (entity.onGround()) {
            return true;
        }

        BlockPos below = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY - 0.12D, entity.getZ());
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    private static void triggerHeelBashImpact(
        LivingEntity entity,
        ServerLevel level,
        Vec3 impactPos,
        float damage,
        boolean spawnVortex
    ) {
        triggerRuptureHeelImpact(level, impactPos);
        triggerRuptureBloodFlameBurst(entity, level, impactPos, damage);
        damageTargetsNearPoint(entity, level, impactPos, FRENZIED_SPIN_RADIUS, damage, true);
        level.explode(entity, impactPos.x, impactPos.y, impactPos.z, HEEL_BASH_EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
        if (spawnVortex) {
            spawnExplodingBloodVortex(entity, level, impactPos);
        }
    }

    private static void spawnNailsOfFurySlash(ServerLevel level, LivingEntity entity) {
        BonePositionTracker.sendRawVerticalSlashToClients(
            level,
            new Vec3(0.0D, -0.5D, 0.0D),
            RUPTURE_SLASH_MODEL_KEY,
            0.0F,
            true,
            RUPTURE_SLASH_ARC_RANGE,
            RUPTURE_SLASH_DURATION_MS,
            0.0F,
            0.0F,
            0.0F,
            0.5F,
            2.1F,
            0.0F,
            entity.getUUID(),
            "sword_to_upper"
        );
    }

    private static void applyFrenziedUpwardKick(LivingEntity entity, int delayTicks, float damage) {
        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!(entity.level() instanceof ServerLevel currentLevel) || !entity.isAlive()) {
                cleanupForm(entity);
                return;
            }

            for (LivingEntity target : getValidTargetsNear(currentLevel, entity, getForwardHitCenter(entity), FRENZIED_KICK_RADIUS)) {
                Damager.hurt(entity, target, damage, true);
                MovementHelper.setVelocity(target, target.getDeltaMovement().x,
                    Math.max(target.getDeltaMovement().y, FRENZIED_UPWARD_VELOCITY), target.getDeltaMovement().z);
            }
        }, delayTicks);
    }

    private static void spawnExplodingBloodStrikeTrail(ServerLevel level, LivingEntity entity, Vec3 direction) {
        Vec3 start = entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
        Vec3 end = start.add(direction.scale(EXPLODING_BLOOD_STRIKE_TRAIL_LENGTH));

        for (int i = 0; i <= EXPLODING_BLOOD_STRIKE_TRAIL_POINTS; i++) {
            double progress = i / (double) EXPLODING_BLOOD_STRIKE_TRAIL_POINTS;
            Vec3 point = start.lerp(end, progress);
            level.sendParticles(ModParticles.BLOOD_FLAME.get(), point.x, point.y, point.z,
                2, 0.035D, 0.035D, 0.035D, 0.01D);
            if ((i & 1) == 0) {
                level.sendParticles(EXPLODING_BLOOD_DUST, point.x, point.y, point.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
    }

    private static Vec3 findDropKickTeleportPosition(LivingEntity entity, ServerLevel level, double range) {
        Vec3 direction = getSafeHorizontalLookVector(entity);
        Vec3 eyeStart = entity.getEyePosition();
        Vec3 eyeEnd = eyeStart.add(direction.scale(range));
        BlockHitResult hitResult = level.clip(new ClipContext(
            eyeStart,
            eyeEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            entity
        ));

        double maxDistance = range;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            maxDistance = Math.max(0.0D, eyeStart.distanceTo(hitResult.getLocation()) - 1.0D);
        }

        Vec3 originalPos = entity.position();
        for (double distance = maxDistance; distance >= 0.0D; distance -= 0.5D) {
            Vec3 candidate = originalPos.add(direction.scale(distance));
            Vec3 offset = candidate.subtract(originalPos);
            if (level.noCollision(entity, entity.getBoundingBox().move(offset))) {
                return candidate;
            }
        }

        return originalPos;
    }

    private static void spawnDropKickBloodFlameBurst(ServerLevel level, Vec3 center) {
        level.sendParticles(ModParticles.BLOOD_FLAME.get(), center.x, center.y, center.z,
            90, 0.85D, 0.55D, 0.85D, 0.08D);
        level.sendParticles(EXPLODING_BLOOD_DUST, center.x, center.y, center.z,
            45, 0.65D, 0.35D, 0.65D, 0.04D);
        ParticleHelper.spawnCircleParticles(level, center, 2.2D, ModParticles.BLOOD_FLAME.get(), 28);
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.9F, 1.25F);
    }

    private static void spawnFierySlashX(ServerLevel level, LivingEntity entity) {
        BonePositionTracker.sendRawHorizontalSlashToClients(
            level,
            new Vec3(0.0D, 0.5D, 0.0D),
            RUPTURE_SLASH_MODEL_KEY,
            2.0F,
            false,
            RUPTURE_SLASH_ARC_RANGE,
            RUPTURE_SLASH_DURATION_MS,
            0.0F,
            0.0F,
            20.0F,
            0.6F,
            1.35F,
            15.0F,
            entity.getUUID(),
            "sword_to_right"
        );

        BonePositionTracker.sendRawHorizontalSlashToClients(
            level,
            new Vec3(0.0D, 0.5D, 0.0D),
            RUPTURE_SLASH_MODEL_KEY,
            -2.0F,
            true,
            RUPTURE_SLASH_ARC_RANGE,
            RUPTURE_SLASH_DURATION_MS,
            0.0F,
            0.0F,
            -20.0F,
            1.1F,
            1.35F,
            -15.0F,
            entity.getUUID(),
            "left_sword_to_left"
        );
    }
    
    private static BloodDemonArtForm hemokinesisForm() {
        return new BloodDemonArtForm(
                FORM_HEMOKINESIS,
                "Hemokinesis",
                "Manipulate objects and creatures around you with your blood.",
                6,
                CombustibleBlood::executeHemoKinesis);
    }
    
    private static void executeHemoKinesis(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        markAbilityUse(entity);
        MovementHelper.lookAtTarget(entity);

        Vec3 eyePosition = entity.getEyePosition();
        Vec3 look = entity.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();

        Entity target = findHemokinesisTarget(serverLevel, entity, eyePosition, look);
        if (target == null) {
            return;
        }

        final Entity[] lockedTarget = {target};
        final boolean[] spawnedFallingBlock = {target instanceof FallingBlockEntity};
        final boolean[] released = {false};

        if (target instanceof FallingBlockEntity fallingBlock) {
            prepareHemokinesisFallingBlock(fallingBlock);
            liftHemokinesisFallingBlockIfNeeded(serverLevel, fallingBlock);
            fallingBlock.setDeltaMovement(Vec3.ZERO);
        }

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            @Override
            public void run() {
                if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                    releaseHemokinesisTarget(lockedTarget, spawnedFallingBlock[0], released);
                    return;
                }

                Entity currentTarget = lockedTarget[0];
                if (!isHemokinesisTargetValid(currentTarget)) {
                    releaseHemokinesisTarget(lockedTarget, spawnedFallingBlock[0], released);
                    return;
                }

                MovementHelper.lookAtTarget(entity);

                Vec3 currentEye = entity.getEyePosition();
                Vec3 currentLook = entity.getLookAngle();
                if (currentLook.lengthSqr() < 1.0E-4D) {
                    currentLook = new Vec3(0.0D, 0.0D, 1.0D);
                }
                currentLook = currentLook.normalize();

                Vec3 currentTargetPos = getHemokinesisTargetPosition(currentTarget);
                drawHemokinesisBeam(activeLevel, currentEye, currentLook, currentTargetPos);

                Vec3 pullPoint = currentEye.add(currentLook.scale(HEMOKINESIS_TARGET_PULL_POINT_OFFSET));
                Vec3 velocity = pullPoint.subtract(currentTargetPos).scale(HEMOKINESIS_TARGET_VELOCITY_SCALE);
                applyHemokinesisVelocity(currentTarget, velocity);

                if (currentTarget instanceof FallingBlockEntity fallingBlock && spawnedFallingBlock[0]) {
                    prepareHemokinesisFallingBlock(fallingBlock);
                    liftHemokinesisFallingBlockIfNeeded(activeLevel, fallingBlock);
                }
            }
        }, 1, HEMOKINESIS_DURATION_TICKS);

        AbilityScheduler.scheduleOnce(
            entity,
            () -> releaseHemokinesisTarget(lockedTarget, spawnedFallingBlock[0], released),
            HEMOKINESIS_DURATION_TICKS
        );
    }

    private static Entity findHemokinesisTarget(ServerLevel level, LivingEntity caster, Vec3 eyePosition, Vec3 look) {
        Vec3 maxEnd = eyePosition.add(look.scale(HEMOKINESIS_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePosition,
            maxEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            caster
        ));

        double maxEntityDistanceSqr = HEMOKINESIS_RANGE * HEMOKINESIS_RANGE;
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            maxEntityDistanceSqr = eyePosition.distanceToSqr(blockHit.getLocation());
        }

        Entity bestEntity = null;
        double bestDistanceSqr = maxEntityDistanceSqr;
        AABB searchBox = new AABB(eyePosition, maxEnd).inflate(HEMOKINESIS_ENTITY_SEARCH_INFLATE);

        for (Entity candidate : level.getEntities(caster, searchBox,
                candidate -> candidate != null
                    && candidate != caster
                    && candidate.isAlive()
                    && !candidate.isRemoved()
                    && !(candidate instanceof LivingEntity living && living.isSpectator()))) {
            Optional<Vec3> clip = candidate.getBoundingBox().inflate(HEMOKINESIS_ENTITY_HIT_INFLATE).clip(eyePosition, maxEnd);
            if (clip.isEmpty()) {
                continue;
            }

            double distanceSqr = eyePosition.distanceToSqr(clip.get());
            if (distanceSqr <= bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                bestEntity = candidate;
            }
        }

        if (bestEntity != null) {
            return bestEntity;
        }

        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos blockPos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, blockPos) < 0.0F) {
            return null;
        }

        FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, blockPos, state);
        if (fallingBlock == null) {
            return null;
        }

        prepareHemokinesisFallingBlock(fallingBlock);
        liftHemokinesisFallingBlockIfNeeded(level, fallingBlock);
        fallingBlock.setDeltaMovement(Vec3.ZERO);
        return fallingBlock;
    }

    private static boolean isHemokinesisTargetValid(Entity target) {
        if (target == null || target.isRemoved()) {
            return false;
        }
        if (target instanceof LivingEntity livingTarget) {
            return livingTarget.isAlive() && !livingTarget.isSpectator();
        }
        return true;
    }

    private static Vec3 getHemokinesisTargetPosition(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            return livingTarget.getEyePosition();
        }
        return new Vec3(target.getX(), target.getY() + (target.getBbHeight() * 0.5D), target.getZ());
    }

    private static void applyHemokinesisVelocity(Entity target, Vec3 velocity) {
        if (target instanceof LivingEntity livingTarget) {
            MovementHelper.setVelocity(livingTarget, velocity);
            return;
        }
        if (target instanceof FallingBlockEntity fallingBlock && target.level() instanceof ServerLevel level) {
            applyHemokinesisFallingBlockVelocity(level, fallingBlock, velocity);
            return;
        }

        target.setDeltaMovement(velocity);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static void applyHemokinesisFallingBlockVelocity(ServerLevel level, FallingBlockEntity fallingBlock, Vec3 velocity) {
        prepareHemokinesisFallingBlock(fallingBlock);
        liftHemokinesisFallingBlockIfNeeded(level, fallingBlock);

        Vec3 adjustedVelocity = limitVector(velocity, HEMOKINESIS_FALLING_BLOCK_MAX_SPEED);
        AABB nextBounds = fallingBlock.getBoundingBox().move(adjustedVelocity);
        if (!level.noCollision(fallingBlock, nextBounds) || isHemokinesisFallingBlockNearGround(level, fallingBlock)) {
            liftHemokinesisFallingBlockIfNeeded(level, fallingBlock);
            adjustedVelocity = new Vec3(
                adjustedVelocity.x * 0.35D,
                Math.max(adjustedVelocity.y, HEMOKINESIS_FALLING_BLOCK_ESCAPE_SPEED),
                adjustedVelocity.z * 0.35D
            );
        }

        fallingBlock.setDeltaMovement(adjustedVelocity);
        fallingBlock.hasImpulse = true;
        fallingBlock.hurtMarked = true;
    }

    private static Vec3 limitVector(Vec3 vector, double maxLength) {
        double lengthSqr = vector.lengthSqr();
        if (lengthSqr <= maxLength * maxLength) {
            return vector;
        }
        return vector.normalize().scale(maxLength);
    }

    private static void prepareHemokinesisFallingBlock(FallingBlockEntity fallingBlock) {
        fallingBlock.setHurtsEntities(0.0F, 0);
        fallingBlock.setNoGravity(true);
        fallingBlock.setOnGround(false);
        fallingBlock.fallDistance = 0.0F;
    }

    private static boolean isHemokinesisFallingBlockNearGround(ServerLevel level, FallingBlockEntity fallingBlock) {
        return fallingBlock.onGround()
            || !level.noCollision(
                fallingBlock,
                fallingBlock.getBoundingBox().move(0.0D, -HEMOKINESIS_FALLING_BLOCK_GROUND_CHECK, 0.0D)
            );
    }

    private static void liftHemokinesisFallingBlockIfNeeded(ServerLevel level, FallingBlockEntity fallingBlock) {
        if (!isHemokinesisFallingBlockNearGround(level, fallingBlock)) {
            return;
        }

        Vec3 start = fallingBlock.position();
        for (int attempt = 1; attempt <= HEMOKINESIS_FALLING_BLOCK_LIFT_ATTEMPTS; attempt++) {
            Vec3 candidate = start.add(0.0D, HEMOKINESIS_FALLING_BLOCK_LIFT_STEP * attempt, 0.0D);
            AABB candidateBounds = fallingBlock.getBoundingBox().move(candidate.subtract(start));
            if (level.noCollision(fallingBlock, candidateBounds)) {
                fallingBlock.teleportTo(candidate.x, candidate.y, candidate.z);
                fallingBlock.setDeltaMovement(Vec3.ZERO);
                fallingBlock.setOnGround(false);
                fallingBlock.hasImpulse = true;
                fallingBlock.hurtMarked = true;
                return;
            }
        }
    }

    private static void drawHemokinesisBeam(ServerLevel level, Vec3 eyePosition, Vec3 look, Vec3 targetPosition) {
        Vec3 startControl = eyePosition.add(look.scale(HEMOKINESIS_BEAM_START_CONTROL));
        Vec3 targetDirection = targetPosition.subtract(eyePosition);
        if (targetDirection.lengthSqr() < 1.0E-4D) {
            targetDirection = look;
        } else {
            targetDirection = targetDirection.normalize();
        }
        Vec3 endControl = targetPosition.subtract(targetDirection.scale(HEMOKINESIS_BEAM_END_CONTROL));

        for (double t = 0.0D; t <= 1.0D; t += HEMOKINESIS_BEAM_POINT_STEP) {
            Vec3 point = cubicBezier(eyePosition, startControl, endControl, targetPosition, t);
            level.sendParticles(EXPLODING_BLOOD_DUST, point.x, point.y, point.z, 2, 0.012D, 0.012D, 0.012D, 0.0D);
        }
    }

    private static Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inverse = 1.0D - t;
        double a = inverse * inverse * inverse;
        double b = 3.0D * inverse * inverse * t;
        double c = 3.0D * inverse * t * t;
        double d = t * t * t;
        return new Vec3(
            (p0.x * a) + (p1.x * b) + (p2.x * c) + (p3.x * d),
            (p0.y * a) + (p1.y * b) + (p2.y * c) + (p3.y * d),
            (p0.z * a) + (p1.z * b) + (p2.z * c) + (p3.z * d)
        );
    }

    private static void releaseHemokinesisTarget(Entity[] lockedTarget, boolean spawnedFallingBlock, boolean[] released) {
        if (released[0]) {
            return;
        }
        released[0] = true;
        Entity target = lockedTarget[0];
        lockedTarget[0] = null;
        releaseHemokinesisTarget(target, spawnedFallingBlock);
    }

    private static void releaseHemokinesisTarget(Entity target, boolean spawnedFallingBlock) {
        if (!spawnedFallingBlock || !(target instanceof FallingBlockEntity fallingBlock) || fallingBlock.isRemoved()) {
            return;
        }

        fallingBlock.setNoGravity(false);
        fallingBlock.setOnGround(false);
        fallingBlock.fallDistance = 0.0F;
        fallingBlock.hurtMarked = true;
    }

    private static boolean isExplodingBloodDamageTarget(LivingEntity target) {
        return Damager.isDemon(target) || target.getMobType() == MobType.UNDEAD;
    }

    private static Vec3 getSafeLookVector(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            float yaw = (float) Math.toRadians(-entity.getYRot());
            look = new Vec3(Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        if (look.lengthSqr() < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return look.normalize();
    }

    private static Vec3 getHeelImpactPosition(LivingEntity entity) {
        Vec3 look = getSafeLookVector(entity);
        return new Vec3(entity.getX(), entity.getY(0.35D), entity.getZ()).add(look.scale(2.0D));
    }

    private static void triggerRuptureHeelImpact(ServerLevel level, Vec3 impactPos) {
        level.sendParticles(ParticleTypes.FLASH, impactPos.x, impactPos.y, impactPos.z, 5, 0.03D, 0.03D, 0.03D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, impactPos.x, impactPos.y, impactPos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void triggerRuptureBloodFlameBurst(LivingEntity entity, ServerLevel level, Vec3 impactPos, float damage) {
        Vec3 look = getSafeLookVector(entity);
        double baseAngle = Math.atan2(-look.x, look.z);

        for (int i = 0; i < RUPTURE_RIFT_POINT_COUNT; i++) {
            double angle = baseAngle + (i * (Math.PI * 2.0D / RUPTURE_RIFT_POINT_COUNT));
            Vec3 point = impactPos.add(Math.cos(angle) * RUPTURE_RIFT_RADIUS, 0.0D, Math.sin(angle) * RUPTURE_RIFT_RADIUS);
            level.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

            AABB hitArea = new AABB(
                point.x - RUPTURE_RIFT_BEAM_HIT_RADIUS, point.y - RUPTURE_RIFT_BEAM_HIT_RADIUS, point.z - RUPTURE_RIFT_BEAM_HIT_RADIUS,
                point.x + RUPTURE_RIFT_BEAM_HIT_RADIUS, point.y + RUPTURE_RIFT_BEAM_HIT_RADIUS, point.z + RUPTURE_RIFT_BEAM_HIT_RADIUS
            );
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitArea,
                living -> living.isAlive() && living != entity && !living.isSpectator())) {
                bloodFlameHit(entity, target, damage);
            }

            final Vec3 beamPoint = point;
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int beamTick = 0;

                @Override
                public void run() {
                    if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                        return;
                    }

                    double beamHeight = RUPTURE_RIFT_BEAM_HEIGHT * ((beamTick + 1.0D) / RUPTURE_RIFT_BEAM_DURATION_TICKS);
                    activeLevel.sendParticles(ModParticles.BLOOD_FLAME.get(),
                        beamPoint.x, beamPoint.y, beamPoint.z,
                        2, 0.05D, 0.05D, 0.05D, 0.02D);

                    for (double y = 0.0D; y <= beamHeight; y += 0.75D) {
                        activeLevel.sendParticles(ModParticles.BLOOD_FLAME.get(),
                            beamPoint.x, beamPoint.y + y, beamPoint.z,
                            1, 0.025D, 0.025D, 0.025D, 0.0D);
                    }

                    beamTick++;
                }
            }, 1, RUPTURE_RIFT_BEAM_DURATION_TICKS);
        }
    }

    private static boolean isInsideVortex(LivingEntity target, Vec3 center) {
        if (target.getY() < center.y || target.getY() > center.y + EXPLODING_BLOOD_VORTEX_HEIGHT) {
            return false;
        }
        double dx = target.getX() - center.x;
        double dz = target.getZ() - center.z;
        return (dx * dx) + (dz * dz) <= (EXPLODING_BLOOD_VORTEX_RADIUS * EXPLODING_BLOOD_VORTEX_RADIUS);
    }

    private static Vec3 rotateYaw(Vec3 vec, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = (vec.x * cos) - (vec.z * sin);
        double z = (vec.x * sin) + (vec.z * cos);
        return new Vec3(x, vec.y, z);
    }

    private static BloodDemonArtForm placeholderForm(int formId, String name) {
        return new BloodDemonArtForm(formId, name, "Placeholder form.", 2, CombustibleBlood::executePlaceholderForm);
    }

    private static void executePlaceholderForm(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        markAbilityUse(entity);
        playRegularMeleeCombo(entity);
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
            entity.getX(), entity.getY(1.0D), entity.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    public static void playRegularMeleeCombo(LivingEntity entity) {
        int comboIndex = Math.floorMod(entity.getPersistentData().getInt(COMBO_INDEX_TAG), COMBO_ANIMATIONS.length);
        playAnimation(entity, COMBO_ANIMATIONS[comboIndex], 10);
        entity.getPersistentData().putInt(COMBO_INDEX_TAG, (comboIndex + 1) % COMBO_ANIMATIONS.length);
    }

    public static void markAbilityUse(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        entity.getPersistentData().putLong(LAST_ABILITY_USE_TICK_TAG, serverLevel.getGameTime());
    }

    private static boolean isCombustibleBloodMeleeItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BloodDemonArtItem bloodDemonArtItem) {
            return ART_ID.equals(bloodDemonArtItem.getArtId());
        }
        return false;
    }

    private static boolean markLeftClickHandled(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        long gameTime = serverLevel.getGameTime();
        long lastHandled = player.getPersistentData().getLong(LAST_LEFT_CLICK_TICK_TAG);
        if (lastHandled == gameTime) {
            return false;
        }

        player.getPersistentData().putLong(LAST_LEFT_CLICK_TICK_TAG, gameTime);
        return true;
    }

    private static boolean shouldIgnoreSwingForAbilityUse(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        long gameTime = serverLevel.getGameTime();
        long lastAbilityUseTick = player.getPersistentData().getLong(LAST_ABILITY_USE_TICK_TAG);
        return gameTime - lastAbilityUseTick <= 2L;
    }

    private static void handlePlayerLeftClick(Player player) {
        if (!isCombustibleBloodMeleeItem(player.getMainHandItem()) || !markLeftClickHandled(player)) {
            return;
        }
        playRegularMeleeCombo(player);
    }

    private static void playAnimation(LivingEntity entity, String animation, int duration) {
        if (entity instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation(animation, duration);
            return;
        }
        if (entity instanceof Player player) {
            KnYAPI.playAnimation(player, KimetsunoyaibaMultiplayer.MODID + ":" + animation, duration);
        }
    }

    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
                return;
            }

            Player player = event.player;
            if (isCombustibleBloodMeleeItem(player.getMainHandItem())
                && player.swinging
                && player.swingTime == 0
                && !shouldIgnoreSwingForAbilityUse(player)) {
                handlePlayerLeftClick(player);
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }

            Player player = event.getEntity();
            handlePlayerLeftClick(player);
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }
            if (isCombustibleBloodMeleeItem(event.getItemStack())) {
                handlePlayerLeftClick(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }

            Entity source = event.getSource().getEntity();
            if (!(source instanceof LivingEntity attacker) || attacker instanceof Player) {
                return;
            }

            if (isCombustibleBloodMeleeItem(attacker.getMainHandItem())) {
                playRegularMeleeCombo(attacker);
            }
        }
    }
}
