package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.compat.MinecraftComesAliveCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodTornadoEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodTornadoSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FleshVineEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonEyesSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BrambleSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Muzan's Biokineses blood demon art. */
public final class Biokineses {
    public static final String ART_ID = "biokineses";
    public static final int FORM_BLACK_BLOOD_BRAMBLES = 3900;
    public static final int FORM_SUMMON_BIOKINESIS_ARMS = 3901;
    public static final int FORM_RETRACT_BIOKINESIS_ARMS = 3902;
    public static final int FORM_BIOKINETIC_GRASP = 3903;
    public static final int FORM_MERCILESS_PUNISHMENT = 3904;
    public static final int FORM_ULTIMATE_AUTHORITY = 3905;
    public static final int FORM_BLACK_BLOOD_DESCENDENCE = 3906;
    public static final int FORM_FORCED_TRANSITION = 3907;

    private static final int CHARGE_TICKS = 18;
    private static final int BRAMBLE_END_TICK = 60;
    private static final int SEQUENCE_END_TICK = 61;
    private static final int BRAMBLE_COUNT = 5;
    private static final double BRAMBLE_RANGE_STEP = 6.0D;
    private static final double BRAMBLE_MAX_RANGE = 60.0D;
    private static final double BRAMBLE_CONE_RADIUS_RATIO = 1.0D / 3.0D;
    private static final double BRAMBLE_RAYCAST_RANGE = 60.0D;
    private static final double BRAMBLE_START_FORWARD_OFFSET = 0.45D;
    private static final double BRAMBLE_START_LEFT_OFFSET = 0.3D;
    private static final double BRAMBLE_START_DOWN_OFFSET = 0.25D;
    private static final double BRAMBLE_START_OFFSET = 0.1D;
    private static final float BRAMBLE_MIN_WIDTH = 0.05F;
    private static final float BRAMBLE_MAX_WIDTH = 0.1F;
    private static final float BRAMBLE_DAMAGE = 4.0F;
    private static final int BRAMBLE_LIFETIME = BRAMBLE_END_TICK - CHARGE_TICKS + 2;
    private static final int CHARGE_RING_COUNT = 5;
    private static final int CHARGE_RING_PARTICLES = 8;
    private static final double CHARGE_RING_RADIUS = 0.3D;
    private static final double CHARGE_COLUMN_HEIGHT = 0.8D;
    private static final float CLAW_DAMAGE = 14.0F;
    private static final int CLAW_GUARD_TICKS = 10;
    private static final double CLAW_AOE_SIZE = 5.0D;
    private static final int MERCILESS_END_TICK = 21;
    private static final int ULTIMATE_CHARGE_END_TICK = 15;
    private static final int ULTIMATE_SLAM_TICK = 17;
    private static final int ULTIMATE_CANCEL_TICK = 57;
    private static final int ULTIMATE_SEQUENCE_END_TICK = 62;
    private static final int ULTIMATE_CHARGE_VINE_END_TICK = 19;
    private static final int ULTIMATE_CHARGE_VINE_LIFETIME = ULTIMATE_CHARGE_VINE_END_TICK + 1;
    private static final int ULTIMATE_SURGE_COUNT = 5;
    private static final double ULTIMATE_HOVER_HEIGHT = 3.0D;
    private static final double ULTIMATE_CHARGE_START_BELOW = 1.0D;
    private static final double ULTIMATE_CHARGE_END_ABOVE = 2.0D;
    private static final double ULTIMATE_SURGE_SPACING = 3.0D;
    private static final double ULTIMATE_SURGE_HALF_WIDTH = 6.0D;
    private static final double ULTIMATE_SURGE_START_BELOW_GROUND = 1.0D;
    private static final int DESCENDENCE_CHARGE_END_TICK = 20;
    private static final int DESCENDENCE_IMPACT_TICK = 25;
    private static final int DESCENDENCE_CLEANUP_TICK = 56;
    private static final int DESCENDENCE_BRAMBLE_LIFETIME = 30;
    private static final int DESCENDENCE_BRAMBLE_COUNT = 10;
    private static final double DESCENDENCE_HOVER_HEIGHT = 20.0D;
    private static final double DESCENDENCE_BACKWARD_DISTANCE = 10.0D;
    private static final double DESCENDENCE_TARGET_RANGE = 40.0D;
    private static final double DESCENDENCE_FALLBACK_RAYCAST = 80.0D;
    private static final double DESCENDENCE_IMPACT_RADIUS = 6.0D;
    private static final float DESCENDENCE_IMPACT_DAMAGE = 14.0F;
    private static final float DESCENDENCE_BRAMBLE_DAMAGE = 6.0F;
    private static final double DESCENDENCE_KNOCKBACK = 1.35D;
    private static final double DESCENDENCE_BRAMBLE_HEIGHT = 30.0D;
    private static final double DESCENDENCE_BRAMBLE_HORIZONTAL_OFFSET = 6.0D;
    private static final double DESCENDENCE_BRAMBLE_ARC = 3.0D;
    private static final float DESCENDENCE_BRAMBLE_WIDTH = 0.12F;
    private static final double DESCENDENCE_BRAMBLE_START_SPREAD = 6.0D;
    private static final int FORCED_TRANSITION_END_TICK = 50;
    private static final int FORCED_TRANSITION_LATCH_TICKS = 20;
    private static final int FORCED_TRANSITION_DAMAGE_INTERVAL = 10;
    private static final float FORCED_TRANSITION_DAMAGE = 30.0F;
    private static final int FORCED_TRANSITION_DEBUFF_TICKS = 200;
    private static final double FORCED_TRANSITION_RANGE = 80.0D;
    private static final double FORCED_TRANSITION_VINE_SPEED = 2.0D;
    private static final double FORCED_TRANSITION_VINE_WIDTH = 0.75D;
    private static final double FORCED_TRANSITION_SLITHER_AMPLITUDE = 0.9D;
    private static final String LAST_CLAW_TICK_TAG = "BiokinesesLastClawTick";
    private static final String CLAW_ANIMATION_INDEX_TAG = "BiokinesesClawAnimationIndex";
    private static final String[] CLAW_ANIMATIONS = {
        "sword_to_left",
        "sword_to_right",
        "left_sword_to_left",
        "left_sword_to_right",
        "left_sword_overhead"
    };
    private static final Vec3 RED = new Vec3(0.72D, 0.02D, 0.02D);
    private static final Vec3 BLACK = new Vec3(0.01D, 0.005D, 0.005D);
    private static final DustParticleOptions RED_DUST =
        new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.02F), 0.08F);
    private static final DustParticleOptions BLACK_DUST =
        new DustParticleOptions(new Vector3f(0.01F, 0.005F, 0.005F), 0.08F);
    private static final DustParticleOptions CHARGE_RED_DUST =
        new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.02F), 0.1F);
    private static final DustParticleOptions CHARGE_BLACK_DUST =
        new DustParticleOptions(new Vector3f(0.01F, 0.005F, 0.005F), 0.1F);

    private Biokineses() {
    }

    public static void register() {
        if (!BloodDemonArtRegistry.isRegistered(ART_ID)) {
            KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Biokineses", createTechnique());
        }
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Biokineses",
            List.of(new BloodDemonArtForm(
                FORM_BLACK_BLOOD_BRAMBLES,
             "Black Blood, Brambles",
                 "Charge red-and-black blood brambles that slash through a long cone.",
                 3,
                 Biokineses::executeBlackBloodBrambles
             ),
             new BloodDemonArtForm(
                 FORM_SUMMON_BIOKINESIS_ARMS,
                 "Biokinesis Arms",
                 "Summon six blood arms that anchor movement and lash out at attackers.",
                 3,
                 Biokineses::executeSummonArms
             ),
              new BloodDemonArtForm(
                  FORM_RETRACT_BIOKINESIS_ARMS,
                  "Retract Biokinesis Arms",
                  "Release the anchors and retract all six Biokinesis arms.",
                  1,
                  Biokineses::executeRetractArms
              ),
              new BloodDemonArtForm(
                   FORM_BIOKINETIC_GRASP,
                   "Biokinetic Grasp",
                   "Use a Biokinesis arm to seize and suspend the living target in your sight.",
                   2,
                   Biokineses::executeBiokineticGrasp
               ),
               new BloodDemonArtForm(
                   FORM_MERCILESS_PUNISHMENT,
                   "Merciless Punishment",
                  "Swipe with a red claw storm and launch a blood tornado along the ground.",
                   3,
                   Biokineses::executeMercilessPunishment
               ),
                new BloodDemonArtForm(
                    FORM_ULTIMATE_AUTHORITY,
                    "Ultimate Authority",
                     "Stomp and send five sinusoidal flesh vines surging forward.",
                     4,
                     Biokineses::executeUltimateAuthority
                ),
                new BloodDemonArtForm(
                    FORM_BLACK_BLOOD_DESCENDENCE,
                    "Black Blood, Descendence",
                    "Leap into the air and crash down in a storm of red-and-black brambles.",
                    4,
                    Biokineses::executeBlackBloodDescendence
                ),
                new BloodDemonArtForm(
                    FORM_FORCED_TRANSITION,
                    "Forced Transition",
                    "Latch onto a living target with a flesh vine and force a demonic transformation.",
                    4,
                    Biokineses::executeForcedTransition
                )
             ),
            0x8B1018
        );
    }

    private static void executeSummonArms(LivingEntity entity, Level level, int formId) {
        if (!level.isClientSide()) {
            BiokinesesArmController.activateForAbility(entity);
        }
    }

    private static void executeRetractArms(LivingEntity entity, Level level, int formId) {
        if (!level.isClientSide()) {
            BiokinesesArmController.retract(entity);
        }
    }

    private static void executeBiokineticGrasp(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = findGraspTarget(entity, serverLevel);
        if (target == null) {
            return;
        }
        BiokinesesArmController.claimAllForAbility(entity);
        BiokinesesArmController.grabEntity(entity,
            BiokinesesArmController.ARM_LEFT_COMBAT, target);
    }

    private static LivingEntity findGraspTarget(LivingEntity entity, ServerLevel level) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(entity));
        Vec3 direction = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
        Vec3 end = start.add(direction.scale(8.0D));
        AABB searchArea = new AABB(start, end).inflate(1.0D);
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> target != entity && target.isAlive() && !target.isSpectator())) {
            var hit = candidate.getBoundingBox().clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private static void executeMercilessPunishment(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            final Vec3 direction = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
            final Vec3 position = CombatGravityFrame.world(CombatGravityFrame.position(entity));
            final BloodTornadoEntity tornado = BloodTornadoSpawner.spawn(
                serverLevel, position, direction, entity, MERCILESS_END_TICK);
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        if (tornado != null) {
                            tornado.discard();
                        }
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= 20) {
                        limitMovement(entity, 0.5D);
                    }
                    if (tick == 0) {
                        performClawSlash(entity, null, "left_arm_overhead", formId);
                    } else if (tick == MERCILESS_END_TICK) {
                        if (tornado != null) {
                            tornado.discard();
                        }
                        cleanupForm(entity);
                    }

                    tick++;
                }
            }, 1, MERCILESS_END_TICK + 1);
        });
    }

    private static void executeUltimateAuthority(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            Vec3 startPosition = entity.position();
            List<FleshVineEntity> chargeVines = new ArrayList<>();
            spawnChargeVines(serverLevel, entity, chargeVines, startPosition, 0);
            Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            Vec3 hoverTarget = startPosition.add(up.scale(ULTIMATE_HOVER_HEIGHT));
            GuardStateHelper.setGuardState(entity, CLAW_DAMAGE, formId, true);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;
                private boolean slamming;
                private boolean impacted;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardVines(chargeVines);
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= ULTIMATE_CHARGE_END_TICK) {
                        limitMovement(entity, 0.25D);
                        if (tick <= 5) {
                            double rise = ULTIMATE_HOVER_HEIGHT * tick / 5.0D;
                            entity.teleportTo(startPosition.add(up.scale(rise)).x,
                                startPosition.add(up.scale(rise)).y,
                                startPosition.add(up.scale(rise)).z);
                        } else {
                            entity.teleportTo(hoverTarget.x, hoverTarget.y, hoverTarget.z);
                        }
                        updateChargeVines(entity, chargeVines, tick);
                    } else if (tick <= ULTIMATE_CHARGE_VINE_END_TICK) {
                        // Keep the vine bases attached to the user while the slam begins.
                        updateChargeVines(entity, chargeVines, tick);
                    }
                    if (tick == 0) {
                        AnimationHelper.playAnimation(entity, "right_arm_chargeup", ULTIMATE_SLAM_TICK);
                    } else if (tick == ULTIMATE_SLAM_TICK) {
                        slamming = true;
                        AnimationHelper.playAnimation(entity, "heel_drop", ULTIMATE_CANCEL_TICK - tick);
                        MovementHelper.setVelocity(entity,
                            GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, -1.1D, 0.0D)));
                    } else if (slamming && !impacted && findGroundHit(serverLevel, entity) != null) {
                        impacted = true;
                        discardVines(chargeVines);
                        spawnSurgeVines(serverLevel, entity, findGroundHit(serverLevel, entity));
                        MovementHelper.setVelocity(entity, Vec3.ZERO);
                    } else if (slamming && !impacted && tick >= ULTIMATE_CANCEL_TICK) {
                        discardVines(chargeVines);
                        cleanupForm(entity);
                    } else if (impacted && tick >= ULTIMATE_SEQUENCE_END_TICK) {
                        cleanupForm(entity);
                    }

                    if (tick == ULTIMATE_CHARGE_VINE_END_TICK + 1 && !impacted) {
                        discardVines(chargeVines);
                    }

                    tick++;
                }
            }, 1, ULTIMATE_SEQUENCE_END_TICK + 1);
        });
    }

    private static void executeBlackBloodDescendence(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            Vec3 startPosition = entity.position();
            Vec3 up = CombatGravityFrame.world(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            Vec3 forward = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
            Vec3 hoverTarget = startPosition.add(up.scale(DESCENDENCE_HOVER_HEIGHT))
                .subtract(forward.scale(DESCENDENCE_BACKWARD_DISTANCE));
            GuardStateHelper.setGuardState(entity, DESCENDENCE_IMPACT_DAMAGE, formId, true);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;
                private Vec3 targetPosition;
                private final List<DescendenceBramble> brambles = new ArrayList<>();

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        clearDescendenceBrambles(serverLevel, entity, brambles);
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= DESCENDENCE_CHARGE_END_TICK) {
                        limitMovement(entity, 0.25D);
                        if (tick <= 10) {
                            double progress = tick / 10.0D;
                            Vec3 position = startPosition.lerp(hoverTarget, progress);
                            entity.teleportTo(position.x, position.y, position.z);
                        } else {
                            entity.teleportTo(hoverTarget.x, hoverTarget.y, hoverTarget.z);
                        }
                        spawnDescendenceDust(serverLevel, entity, tick);
                    }

                    if (tick == 0) {
                        AnimationHelper.playAnimation(entity, "right_arm_chargeup", DESCENDENCE_CHARGE_END_TICK);
                    } else if (tick == DESCENDENCE_CHARGE_END_TICK) {
                        targetPosition = findDescendenceTarget(serverLevel, entity);
                        AnimationHelper.playAnimation(entity, "heel_drop",
                            DESCENDENCE_CLEANUP_TICK - DESCENDENCE_CHARGE_END_TICK);
                    } else if (tick > DESCENDENCE_CHARGE_END_TICK && tick <= DESCENDENCE_IMPACT_TICK) {
                        double progress = (tick - DESCENDENCE_CHARGE_END_TICK)
                            / (double) (DESCENDENCE_IMPACT_TICK - DESCENDENCE_CHARGE_END_TICK);
                        Vec3 position = hoverTarget.lerp(targetPosition, progress);
                        entity.teleportTo(position.x, position.y, position.z);
                        MovementHelper.setVelocity(entity, Vec3.ZERO);
                        if (tick == DESCENDENCE_IMPACT_TICK) {
                            applyDescendenceImpact(serverLevel, entity, targetPosition);
                            spawnDescendenceBrambles(serverLevel, entity, targetPosition, brambles);
                        }
                    } else if (tick <= DESCENDENCE_CLEANUP_TICK) {
                        limitMovement(entity, 0.25D);
                        if (tick == DESCENDENCE_CLEANUP_TICK) {
                            clearDescendenceBrambles(serverLevel, entity, brambles);
                            cleanupForm(entity);
                        }
                    }

                    tick++;
                }
            }, 1, DESCENDENCE_CLEANUP_TICK + 1);
        });
    }

    private static Vec3 findDescendenceTarget(ServerLevel level, LivingEntity entity) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(entity));
        Vec3 forward = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
        Vec3 end = start.add(forward.scale(DESCENDENCE_TARGET_RANGE));
        BlockHitResult forwardHit = level.clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 up = CombatGravityFrame.world(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        if (forwardHit.getType() == HitResult.Type.BLOCK) {
            return forwardHit.getLocation().add(up.scale(0.1D));
        }

        Vec3 down = up.scale(-1.0D);
        Vec3 fallbackEnd = end.add(down.scale(DESCENDENCE_FALLBACK_RAYCAST));
        BlockHitResult groundHit = level.clip(new ClipContext(end, fallbackEnd,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return groundHit.getType() == HitResult.Type.BLOCK
            ? groundHit.getLocation().add(up.scale(0.1D)) : end;
    }

    private static void applyDescendenceImpact(ServerLevel level, LivingEntity attacker, Vec3 impact) {
        CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_IMPACT.get(),
            impact.x, impact.y, impact.z, 24, 1.0D, 0.25D, 1.0D, 0.05D);

        AABB area = new AABB(impact, impact).inflate(DESCENDENCE_IMPACT_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            candidate -> candidate != attacker && candidate.isAlive() && !candidate.isSpectator())) {
            if (!EnhancedLoveForms.isTargetable(attacker, target)
                || target.distanceToSqr(impact) > DESCENDENCE_IMPACT_RADIUS * DESCENDENCE_IMPACT_RADIUS) {
                continue;
            }
            if (Damager.hurt(attacker, target, DESCENDENCE_IMPACT_DAMAGE, true)) {
                Vec3 localOffset = CombatGravityFrame.local(target.position().subtract(impact));
                if (localOffset.lengthSqr() < 1.0E-8D) {
                    localOffset = new Vec3(0.0D, 0.0D, 1.0D);
                }
                Vec3 knockback = localOffset.normalize().scale(DESCENDENCE_KNOCKBACK)
                    .add(0.0D, 0.75D, 0.0D);
                MovementHelper.addVelocity(target, knockback);
            }
        }
    }

    private static void spawnDescendenceBrambles(ServerLevel level, LivingEntity owner,
                                                  Vec3 impact, List<DescendenceBramble> brambles) {
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(owner);
        forward = forward.subtract(up.scale(forward.dot(up)));
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 side = up.cross(forward).normalize();

        for (int index = 0; index < DESCENDENCE_BRAMBLE_COUNT; index++) {
            double angle = owner.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(owner.getRandom().nextDouble())
                * DESCENDENCE_BRAMBLE_START_SPREAD;
            Vec3 rawStart = impact
                .add(side.scale(Math.cos(angle) * distance))
                .add(forward.scale(Math.sin(angle) * distance));
            BlockHitResult ground = findGroundBelow(level, owner, rawStart, up);
            Vec3 start = ground == null ? rawStart : ground.getLocation();
            Vec3 horizontalOffset = side.scale((owner.getRandom().nextDouble() * 2.0D - 1.0D)
                * DESCENDENCE_BRAMBLE_HORIZONTAL_OFFSET)
                .add(forward.scale((owner.getRandom().nextDouble() * 2.0D - 1.0D)
                    * DESCENDENCE_BRAMBLE_HORIZONTAL_OFFSET));
            Vec3 end = start.add(up.scale(DESCENDENCE_BRAMBLE_HEIGHT)).add(horizontalOffset);
            Vec3 arcing = randomUnitVector().scale(owner.getRandom().nextDouble()
                * DESCENDENCE_BRAMBLE_ARC);
            DescendenceBramble bramble = new DescendenceBramble(UUID.randomUUID(), start, end,
                arcing, DESCENDENCE_BRAMBLE_WIDTH);
            brambles.add(bramble);
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, true, true, bramble.start, bramble.end, Vec3.ZERO, bramble.arcing,
                RED, BLACK, bramble.width, DESCENDENCE_BRAMBLE_LIFETIME), level,
                start.x, start.y, start.z, 128.0D);
            damageDescendenceBramble(level, owner, bramble);
        }
    }

    private static void damageDescendenceBramble(ServerLevel level, LivingEntity attacker,
                                                 DescendenceBramble bramble) {
        AABB area = new AABB(bramble.start, bramble.end).inflate(1.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            candidate -> candidate != attacker && candidate.isAlive() && !candidate.isSpectator())) {
            if (!EnhancedLoveForms.isTargetable(attacker, target)
                || !isNearDescendenceBramble(target, bramble)) {
                continue;
            }
            if (Damager.hurt(attacker, target, DESCENDENCE_BRAMBLE_DAMAGE, true)) {
                Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_SLASH.get(),
                    impact.x, impact.y, impact.z, 2, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
    }

    private static boolean isNearDescendenceBramble(LivingEntity target, DescendenceBramble bramble) {
        Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 previous = bramble.start;
        for (int segment = 1; segment <= 8; segment++) {
            double progress = segment / 8.0D;
            Vec3 current = bramble.start.lerp(bramble.end, progress)
                .add(bramble.arcing.scale(1.0D - Math.abs(progress * 2.0D - 1.0D)));
            if (distanceToSegmentSqr(targetPoint, previous, current)
                <= Math.max(0.85D, target.getBbWidth() * 0.5D + bramble.width)
                    * Math.max(0.85D, target.getBbWidth() * 0.5D + bramble.width)) {
                return true;
            }
            previous = current;
        }
        return false;
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-8D) {
            return point.distanceToSqr(start);
        }
        double progress = point.subtract(start).dot(segment) / lengthSqr;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return point.distanceToSqr(start.lerp(end, progress));
    }

    private static void clearDescendenceBrambles(ServerLevel level, LivingEntity owner,
                                                  List<DescendenceBramble> brambles) {
        Vec3 position = owner.position();
        for (DescendenceBramble bramble : brambles) {
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, false, false, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO,
                RED, BLACK, bramble.width, 1), level,
                position.x, position.y, position.z, 128.0D);
        }
        brambles.clear();
    }

    private static void spawnDescendenceDust(ServerLevel level, LivingEntity entity, int tick) {
        Vec3 center = CombatGravityFrame.position(entity).add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        for (int index = 0; index < 10; index++) {
            double angle = tick * 0.35D + index * (Math.PI * 2.0D / 10.0D);
            double radius = 0.65D + (index % 3) * 0.2D;
            Vec3 point = center.add(Math.cos(angle) * radius, Math.sin(tick * 0.2D + index) * 0.8D,
                Math.sin(angle) * radius);
            DustParticleOptions dust = (index & 1) == 0 ? CHARGE_RED_DUST : CHARGE_BLACK_DUST;
            CombatGravityFrame.sendParticles(level, dust, point.x, point.y, point.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void executeForcedTransition(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            TransitionTarget selectedTarget = selectForcedTransitionTarget(serverLevel, entity);
            List<FleshVineEntity> vines = new ArrayList<>();
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(serverLevel);
            if (vine != null) {
                Vec3 start = forcedTransitionArmPosition(entity);
                vine.initializeTransition(start, entity, entity.getRandom().nextInt(),
                    FleshVineEntity.DEFAULT_DIAMETER, FORCED_TRANSITION_END_TICK + 4);
                serverLevel.addFreshEntity(vine);
                vines.add(vine);
            }
            GuardStateHelper.setGuardState(entity, FORCED_TRANSITION_DAMAGE, formId, true);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;
                 private LivingEntity latchedTarget;
                 private int latchTick = -1;
                 private boolean outcomeResolved;
                 private Vec3 endpoint;
                 private final List<Vec3> traversedPath = new ArrayList<>();

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardVines(vines);
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= 40) {
                        limitMovement(entity, 0.5D);
                        AnimationHelper.playAnimation(entity, "right_arm_forward", 2);
                        Vec3 start = forcedTransitionArmPosition(entity);
                         Vec3 targetPoint = selectedTarget == null
                             ? start : forcedTransitionTargetPoint(selectedTarget);

                         if (traversedPath.isEmpty()) {
                             traversedPath.add(start);
                             endpoint = start;
                         } else {
                             traversedPath.set(0, start);
                         }

                         if (latchedTarget == null) {
                             endpoint = forcedTransitionNextEndpoint(endpoint, targetPoint, entity, tick);
                             traversedPath.add(endpoint);
                             if (!vines.isEmpty() && vines.get(0).isAlive()) {
                                 vines.get(0).updateTransition(start, endpoint, traversedPath);
                             }
                            LivingEntity hit = findForcedTransitionHit(serverLevel, entity, start, endpoint,
                                selectedTarget == null ? null : selectedTarget.entity);
                            if (hit != null) {
                                latchedTarget = hit;
                                latchTick = tick;
                            }
                        } else {
                             Vec3 attachedPoint = latchedTarget.position()
                                 .add(0.0D, latchedTarget.getBbHeight() * 0.5D, 0.0D);
                             if (!traversedPath.isEmpty()) {
                                 traversedPath.set(traversedPath.size() - 1, attachedPoint);
                             }
                             if (!vines.isEmpty() && vines.get(0).isAlive()) {
                                 vines.get(0).updateTransition(start, attachedPoint, traversedPath);
                            }
                            int attachedTicks = tick - latchTick;
                            if (attachedTicks >= 0 && attachedTicks < FORCED_TRANSITION_LATCH_TICKS
                                && attachedTicks % FORCED_TRANSITION_DAMAGE_INTERVAL == 0) {
                                outcomeResolved = damageForcedTransitionTarget(entity, latchedTarget);
                            }
                            if (attachedTicks >= FORCED_TRANSITION_LATCH_TICKS && !outcomeResolved) {
                                applyForcedTransitionDebuffs(latchedTarget);
                                outcomeResolved = true;
                            }
                        }
                     } else if (tick == FORCED_TRANSITION_END_TICK) {
                         discardVines(vines);
                         AnimationHelper.stopAnimation(entity, "right_arm_forward");
                         cleanupForm(entity);
                    }

                    tick++;
                }
            }, 1, FORCED_TRANSITION_END_TICK + 1);
        });
    }

    private static TransitionTarget selectForcedTransitionTarget(ServerLevel level, LivingEntity owner) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(owner));
        Vec3 direction = CombatGravityFrame.world(CombatGravityFrame.look(owner)).normalize();
        Vec3 maxEnd = start.add(direction.scale(FORCED_TRANSITION_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 rayEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : maxEnd;

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        AABB searchArea = new AABB(start, rayEnd).inflate(FORCED_TRANSITION_VINE_WIDTH);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> target != owner && target.isAlive() && !target.isSpectator())) {
            var hit = candidate.getBoundingBox().clip(start, rayEnd);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
        }
        return new TransitionTarget(closest, rayEnd);
    }

    private static Vec3 forcedTransitionArmPosition(LivingEntity entity) {
        // PlayerAnimator's arm bone is client-only. Use the same head-relative
        // right-arm anchor on the server, recalculated while the user moves.
        Vec3 head = CombatGravityFrame.eye(entity);
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 localArm = head.add(0.0D, -0.4D, 0.0D)
            .add(right.scale(0.3D)).add(forward.scale(0.2D));
        return CombatGravityFrame.world(localArm);
    }

    private static Vec3 forcedTransitionTargetPoint(TransitionTarget target) {
        if (target.entity != null && target.entity.isAlive()) {
            return target.entity.position().add(0.0D, target.entity.getBbHeight() * 0.5D, 0.0D);
        }
        return target.location;
    }

    private static Vec3 forcedTransitionNextEndpoint(Vec3 current, Vec3 target,
                                                      LivingEntity owner, int tick) {
        Vec3 toTarget = target.subtract(current);
        if (toTarget.lengthSqr() <= FORCED_TRANSITION_VINE_SPEED * FORCED_TRANSITION_VINE_SPEED) {
            return target;
        }
        Vec3 direction = toTarget.normalize();
        Vec3 up = CombatGravityFrame.world(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 side = up.cross(direction);
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 otherSide = direction.cross(side).normalize();
        double phase = owner.getId() * 0.37D;
        Vec3 slither = side.scale(Math.sin(tick * 0.75D + phase) * FORCED_TRANSITION_SLITHER_AMPLITUDE)
            .add(otherSide.scale(Math.cos(tick * 0.61D + phase) * 0.6D));
        return current.add(direction.add(slither).normalize().scale(FORCED_TRANSITION_VINE_SPEED));
    }

    private static LivingEntity findForcedTransitionHit(ServerLevel level, LivingEntity owner,
                                                         Vec3 start, Vec3 end, LivingEntity preferred) {
        if (preferred != null && preferred.isAlive()
            && preferred.getBoundingBox().inflate(FORCED_TRANSITION_VINE_WIDTH).clip(start, end).isPresent()) {
            return preferred;
        }

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        AABB area = new AABB(start, end).inflate(FORCED_TRANSITION_VINE_WIDTH);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area,
            target -> target != owner && target.isAlive() && !target.isSpectator())) {
            var hit = candidate.getBoundingBox().inflate(FORCED_TRANSITION_VINE_WIDTH).clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private static boolean damageForcedTransitionTarget(LivingEntity owner, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return true;
        }

        if (target.getHealth() <= FORCED_TRANSITION_DAMAGE
            && target.getRandom().nextBoolean() && tryInstantDemonize(target)) {
            return true;
        }
        Damager.hurt(owner, target, FORCED_TRANSITION_DAMAGE, true);
        return !target.isAlive();
    }

    private static void applyForcedTransitionDebuffs(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WITHER,
            FORCED_TRANSITION_DEBUFF_TICKS, 4, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
            FORCED_TRANSITION_DEBUFF_TICKS, 2, false, true, true));
    }

    private static boolean tryInstantDemonize(LivingEntity target) {
        if (Damager.isDemon(target)) {
            return false;
        }
        if (target instanceof ServerPlayer player) {
            DemonTransformationHandler.cancelTransformation(player);
            DemonTransformationHandler.consumeBaseMuzanBlood(player, 10 + player.getRandom().nextInt(11));
            player.getPersistentData().putBoolean("oni", true);
            DemonEyesSyncHandler.broadcastState(player);
            return true;
        }
        if (target instanceof BreathingSlayerEntity slayer && !slayer.isDemonized()) {
            slayer.demonize();
            return true;
        }
        if (MinecraftComesAliveCompat.demonize(target)) {
            return true;
        }
        Entity replacement = createForcedTransitionDemon(target);
        return replacement != null;
    }

    private static Entity createForcedTransitionDemon(LivingEntity source) {
        Entity replacement;
        if (source instanceof Villager) {
            replacement = ModEntities.DEMON_VILLAGER.get().create(source.level());
        } else if (source instanceof Pillager) {
            replacement = ModEntities.DEMON_PILLAGER.get().create(source.level());
        } else if (source instanceof Creeper) {
            replacement = ModEntities.DEMON_CREEPER.get().create(source.level());
        } else if (source instanceof Vindicator) {
            replacement = ModEntities.DEMON_VINDICATOR.get().create(source.level());
        } else {
            return null;
        }
        if (replacement == null) {
            return null;
        }
        replacement.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        source.level().addFreshEntity(replacement);
        source.discard();
        return replacement;
    }

    private static void spawnChargeVines(ServerLevel level, LivingEntity owner,
                                          List<FleshVineEntity> vines, Vec3 center, int tick) {
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        for (int index = 0; index < 3; index++) {
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(level);
            if (vine == null) {
                continue;
            }
            Vec3 start = center.subtract(up.scale(ULTIMATE_CHARGE_START_BELOW));
            Vec3 end = center.add(up.scale(ULTIMATE_CHARGE_END_ABOVE))
                .add(randomPerpendicular(up, owner).scale(1.5D));
            vine.initializeCharge(start, end, owner, owner.getRandom().nextInt(),
                FleshVineEntity.DEFAULT_DIAMETER, ULTIMATE_CHARGE_VINE_LIFETIME);
            vine.setUp(up);
            level.addFreshEntity(vine);
            vines.add(vine);
        }
    }

    private static void updateChargeVines(LivingEntity owner, List<FleshVineEntity> vines, int tick) {
        Vec3 center = owner.position();
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 side = GravityDirectionHelper.getLookDirection(owner);
        side = side.subtract(up.scale(side.dot(up)));
        if (side.lengthSqr() < 1.0E-8D) {
            side = randomPerpendicular(up, owner);
        } else {
            side = side.normalize();
        }
        Vec3 otherSide = up.cross(side).normalize();
        for (int index = 0; index < vines.size(); index++) {
            FleshVineEntity vine = vines.get(index);
            if (!vine.isAlive()) {
                continue;
            }
            Vec3 start = center.subtract(up.scale(ULTIMATE_CHARGE_START_BELOW));
            double angle = tick * 0.55D + index * Math.PI * 2.0D / 3.0D;
            Vec3 offset = side.scale(Math.cos(angle) * 1.5D)
                .add(otherSide.scale(Math.sin(angle) * 1.5D));
            Vec3 end = center.add(up.scale(ULTIMATE_CHARGE_END_ABOVE))
                .add(offset);
            vine.updateCharge(start, end);
        }
    }

    private static void spawnSurgeVines(ServerLevel level, LivingEntity owner, BlockHitResult impact) {
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(owner);
        forward = forward.subtract(up.scale(forward.dot(up)));
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 line = forward.cross(up);
        if (line.lengthSqr() < 1.0E-8D) {
            line = new Vec3(1.0D, 0.0D, 0.0D);
        }
        line = line.normalize();

        for (int index = 0; index < ULTIMATE_SURGE_COUNT; index++) {
            double offset = (index - (ULTIMATE_SURGE_COUNT - 1) * 0.5D) * ULTIMATE_SURGE_SPACING;
            Vec3 point = impact.getLocation().add(line.scale(offset));
            BlockHitResult ground = findGroundBelow(level, owner, point, up);
            Vec3 surface = ground == null ? point : ground.getLocation();
            Vec3 start = surface.subtract(up.scale(ULTIMATE_SURGE_START_BELOW_GROUND));
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(level);
            if (vine == null) {
                continue;
            }
            vine.initializeSurge(start, forward, up, owner, owner.getRandom().nextInt(),
                FleshVineEntity.DEFAULT_DIAMETER, owner.getRandom().nextDouble() * Math.PI * 2.0D);
            vine.setHomingEnabled(true);
            level.addFreshEntity(vine);
        }
    }

    private static BlockHitResult findGroundHit(ServerLevel level, LivingEntity entity) {
        Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 position = entity.position();
        return findGroundBelow(level, entity, position, up);
    }

    private static BlockHitResult findGroundBelow(ServerLevel level, LivingEntity entity,
                                                   Vec3 point, Vec3 up) {
        Vec3 from = point.add(up.scale(0.25D));
        Vec3 to = point.subtract(up.scale(2.0D));
        BlockHitResult hit = level.clip(new ClipContext(from, to,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static void discardVines(List<FleshVineEntity> vines) {
        for (FleshVineEntity vine : vines) {
            if (vine.isAlive()) {
                vine.beginFade();
            }
        }
        vines.clear();
    }

    private static Vec3 randomPerpendicular(Vec3 normal, LivingEntity owner) {
        Vec3 random = new Vec3(owner.getRandom().nextDouble() * 2.0D - 1.0D,
            owner.getRandom().nextDouble() * 2.0D - 1.0D,
            owner.getRandom().nextDouble() * 2.0D - 1.0D);
        Vec3 perpendicular = random.subtract(normal.scale(random.dot(normal)));
        if (perpendicular.lengthSqr() < 1.0E-8D) {
            perpendicular = normal.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return perpendicular.lengthSqr() < 1.0E-8D
            ? new Vec3(0.0D, 0.0D, 1.0D) : perpendicular.normalize();
    }

    private static void executeBlackBloodBrambles(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof Player player) {
            KnYAPI.playAnimation(player, "brambles", BRAMBLE_END_TICK);
        }
        GuardStateHelper.setGuardState(entity, BRAMBLE_DAMAGE, formId, true);

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int tick;
            private final List<Bramble> brambles = new ArrayList<>();

            @Override
            public void run() {
                if (!entity.isAlive()) {
                    clearBrambles(serverLevel, entity, brambles);
                    cleanupForm(entity);
                    return;
                }

                if (tick < CHARGE_TICKS) {
                    limitChargeMovement(entity);
                    spawnChargeDust(serverLevel, entity, tick);
                } else if (tick <= BRAMBLE_END_TICK) {
                    double range = Math.min(BRAMBLE_MAX_RANGE,
                        (tick - CHARGE_TICKS + 1) * BRAMBLE_RANGE_STEP);
                    if (tick == CHARGE_TICKS) {
                        createBrambles(brambles);
                    }
                    updateBrambles(serverLevel, entity, brambles, range, tick == CHARGE_TICKS);
                    spawnConeParticles(serverLevel, brambleStartLocal(entity),
                        CombatGravityFrame.look(entity).normalize(), range, tick);
                    if ((tick - CHARGE_TICKS) % 2 == 0) {
                        damageCone(serverLevel, entity, range);
                        spawnRaycastSlashes(serverLevel, entity);
                    }
                } else if (tick == SEQUENCE_END_TICK) {
                    clearBrambles(serverLevel, entity, brambles);
                    cleanupForm(entity);
                }

                tick++;
            }
        }, 1, SEQUENCE_END_TICK + 1);
    }

    public static boolean isBiokinesesItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem item
            && ART_ID.equals(item.getArtId());
    }

    private static void handleLeftClick(LivingEntity attacker, Entity clickedEntity) {
        if (!isBiokinesesItem(attacker.getMainHandItem())) {
            return;
        }
        if (!markClawHandled(attacker)) {
            return;
        }
        if (attacker instanceof Player player) {
            BiokinesesArmController.startCombatWhip(player, clickedEntity);
        }
        performClawSlash(attacker, clickedEntity == null ? null : clickedEntity.getUUID());
    }

    public static void handleClientLeftClick(Player player, java.util.UUID excludedTargetId) {
        if (!isBiokinesesItem(player.getMainHandItem()) || !markClawHandled(player)) {
            return;
        }
        BiokinesesArmController.startCombatWhip(player, null);
        performClawSlash(player, excludedTargetId);
    }

    private static boolean markClawHandled(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        long gameTime = serverLevel.getGameTime();
        if (attacker.getPersistentData().getLong(LAST_CLAW_TICK_TAG) == gameTime) {
            return false;
        }
        attacker.getPersistentData().putLong(LAST_CLAW_TICK_TAG, gameTime);
        return true;
    }

    private static void performClawSlash(LivingEntity attacker, java.util.UUID excludedTargetId) {
        int index = Math.floorMod(attacker.getPersistentData().getInt(CLAW_ANIMATION_INDEX_TAG), CLAW_ANIMATIONS.length);
        String animation = CLAW_ANIMATIONS[index];
        attacker.getPersistentData().putInt(CLAW_ANIMATION_INDEX_TAG, (index + 1) % CLAW_ANIMATIONS.length);
        performClawSlash(attacker, excludedTargetId, animation, FORM_BLACK_BLOOD_BRAMBLES);
    }

    private static void performClawSlash(LivingEntity attacker, java.util.UUID excludedTargetId,
                                         String animation, int formId) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var previousGravityFrame = CombatGravityFrame.enter(attacker);
        try {
            AnimationHelper.playAnimation(attacker, animation, CLAW_GUARD_TICKS);
            if (attacker instanceof ServerPlayer serverPlayer) {
                ModNetworking.sendToPlayer(new AnimationSyncPacket(
                    attacker.getUUID(),
                    ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animation),
                    0,
                    CLAW_GUARD_TICKS,
                    false,
                    false,
                    null,
                    1.0F,
                    3000
                ), serverPlayer);
            }
            ModNetworking.sendToAllClients(new MobSwordSlashPacket(
                attacker.getUUID(), animation, 0, "claw_nezuko"));
            attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.0F);

            GuardStateHelper.setGuardState(attacker, CLAW_DAMAGE, formId, false);
            AbilityScheduler.scheduleOnce(attacker, () -> GuardStateHelper.clearGuardState(attacker), CLAW_GUARD_TICKS);
            damageClawArea(serverLevel, attacker, excludedTargetId);
        } finally {
            CombatGravityFrame.restore(previousGravityFrame);
        }
    }

    private static void damageClawArea(ServerLevel level, LivingEntity attacker, java.util.UUID excludedTargetId) {
        Vec3 eye = CombatGravityFrame.eye(attacker);
        Vec3 forward = CombatGravityFrame.look(attacker).normalize();
        Vec3 center = eye.add(forward.scale(CLAW_AOE_SIZE / 1.5D));
        AABB localArea = new AABB(center, center).inflate(CLAW_AOE_SIZE * 0.5D);
        AABB worldArea = CombatGravityFrame.world(localArea);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, worldArea,
            entity -> entity != attacker && entity.isAlive() && !entity.isSpectator()
                && (excludedTargetId == null || !entity.getUUID().equals(excludedTargetId)))) {
            if (EnhancedLoveForms.isTargetable(attacker, target)) {
                Damager.hurt(attacker, target, CLAW_DAMAGE, false, true);
            }
        }
    }

    private static void createBrambles(List<Bramble> brambles) {
        for (int i = 0; i < BRAMBLE_COUNT; i++) {
            Vec3 deviation = randomUnitVector().scale(Math.random() * BRAMBLE_CONE_RADIUS_RATIO);
            BrambleColor color = i == 0 ? BrambleColor.BLACK_ONLY
                : i == 1 ? BrambleColor.RED_ONLY : BrambleColor.BOTH;
            float width = (float) (BRAMBLE_MIN_WIDTH
                + Math.random() * (BRAMBLE_MAX_WIDTH - BRAMBLE_MIN_WIDTH));
            brambles.add(new Bramble(UUID.randomUUID(), deviation, color, width));
        }
    }

    private static void updateBrambles(ServerLevel level, LivingEntity entity, List<Bramble> brambles,
                                       double range, boolean spawn) {
        Vec3 baseStartLocal = brambleStartLocal(entity);
        Vec3 forward = CombatGravityFrame.look(entity).normalize();

        for (Bramble bramble : brambles) {
            Vec3 startLocal = baseStartLocal.add(randomUnitVector().scale(Math.random() * BRAMBLE_START_OFFSET));
            bramble.deviation = randomUnitVector().scale(Math.random() * BRAMBLE_CONE_RADIUS_RATIO);
            Vec3 arcing = CombatGravityFrame.world(randomUnitVector().scale(Math.random() * range * 0.2D));
            Vec3 endLocal = startLocal.add(forward.scale(range)).add(bramble.deviation.scale(range));
            Vec3 startWorld = CombatGravityFrame.world(startLocal);
            Vec3 endWorld = CombatGravityFrame.world(endLocal);
            Vec3 brambleRed = bramble.color == BrambleColor.BLACK_ONLY ? BLACK : RED;
            Vec3 brambleBlack = bramble.color == BrambleColor.RED_ONLY ? RED : BLACK;
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, true, spawn, startWorld, endWorld, Vec3.ZERO, arcing,
                brambleRed, brambleBlack, bramble.width, BRAMBLE_LIFETIME), level,
                startWorld.x, startWorld.y, startWorld.z, 128.0D);
            CombatGravityFrame.sendParticles(level, ModParticles.BLACKRED_SLASH.get(),
                endLocal.x, endLocal.y, endLocal.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void clearBrambles(ServerLevel level, LivingEntity entity, List<Bramble> brambles) {
        Vec3 position = entity.position();
        for (Bramble bramble : brambles) {
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, false, false, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO,
                RED, BLACK, bramble.width, 1), level,
                position.x, position.y, position.z, 128.0D);
        }
        brambles.clear();
    }

    private static Vec3 brambleStartLocal(LivingEntity entity) {
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Vec3 left = localLeft(forward);
        Vec3 down = CombatGravityFrame.local(GravityDirectionHelper.getLocalDown(entity)).normalize();
        return CombatGravityFrame.eye(entity)
            .add(forward.scale(BRAMBLE_START_FORWARD_OFFSET))
            .add(left.scale(BRAMBLE_START_LEFT_OFFSET))
            .add(down.scale(BRAMBLE_START_DOWN_OFFSET));
    }

    private static void limitChargeMovement(LivingEntity entity) {
        limitMovement(entity, 0.25D);
    }

    private static void limitMovement(LivingEntity entity, double horizontalScale) {
        Vec3 velocity = CombatGravityFrame.velocity(entity);
        MovementHelper.setVelocity(entity, velocity.multiply(horizontalScale, 1.0D, horizontalScale));
        entity.setSprinting(false);
    }

    private static void spawnChargeDust(ServerLevel level, LivingEntity entity, int tick) {
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-8D) {
            horizontalForward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3 left = localLeft(horizontalForward);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 center = CombatGravityFrame.eye(entity).add(forward.scale(0.5D)).add(left.scale(0.4D));

        for (int ring = 0; ring < CHARGE_RING_COUNT; ring++) {
            double height = (ring / (double) (CHARGE_RING_COUNT - 1) - 0.5D) * CHARGE_COLUMN_HEIGHT;
            double spiralOffset = tick * 0.2D + ring * 0.55D;
            for (int particleIndex = 0; particleIndex < CHARGE_RING_PARTICLES; particleIndex++) {
                double angle = spiralOffset + particleIndex * (Math.PI * 2.0D / CHARGE_RING_PARTICLES);
                Vec3 particle = center.add(up.scale(height))
                    .add(horizontalForward.scale(Math.cos(angle) * CHARGE_RING_RADIUS))
                    .add(left.scale(Math.sin(angle) * CHARGE_RING_RADIUS));
                DustParticleOptions dust = ((ring + particleIndex) & 1) == 0
                    ? CHARGE_RED_DUST : CHARGE_BLACK_DUST;
                CombatGravityFrame.sendParticles(level, dust, particle.x, particle.y, particle.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static Vec3 localLeft(Vec3 forward) {
        Vec3 left = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (left.lengthSqr() < 1.0E-8D) {
            return new Vec3(-1.0D, 0.0D, 0.0D);
        }
        return left.normalize();
    }

    private static Vec3 localUp(Vec3 forward) {
        Vec3 left = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (left.lengthSqr() < 1.0E-8D) {
            left = new Vec3(-1.0D, 0.0D, 0.0D);
        }
        Vec3 up = left.cross(forward);
        return up.normalize();
    }

    private static void spawnConeParticles(ServerLevel level, Vec3 start, Vec3 forward,
                                           double range, int tick) {
        Vec3 side = randomPerpendicular(forward);
        Vec3 otherSide = forward.cross(side).normalize();
        for (int i = 0; i < 12; i++) {
            double progress = level.random.nextDouble();
            double distance = Math.max(0.15D, progress * range);
            double coneRadius = range * BRAMBLE_CONE_RADIUS_RATIO * progress;
            double angle = level.random.nextDouble() * Math.PI * 2.0D + tick * 0.35D;
            double offset = Math.sqrt(level.random.nextDouble()) * coneRadius;
            Vec3 point = start.add(forward.scale(distance))
                .add(side.scale(Math.cos(angle) * offset))
                .add(otherSide.scale(Math.sin(angle) * offset));
            DustParticleOptions dust = (i & 1) == 0 ? RED_DUST : BLACK_DUST;
            CombatGravityFrame.sendParticles(level, dust, point.x, point.y, point.z,
                1, 0.035D, 0.035D, 0.035D, 0.025D);
            if ((i & 3) == 0) {
                CombatGravityFrame.sendParticles(level, ParticleTypes.CRIT, point.x, point.y, point.z,
                    1, 0.04D, 0.04D, 0.04D, 0.08D);
            }
        }
    }

    private static Vec3 randomPerpendicular(Vec3 forward) {
        Vec3 random = randomUnitVector();
        Vec3 side = random.subtract(forward.scale(random.dot(forward)));
        if (side.lengthSqr() < 1.0E-8D) {
            side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        return side.lengthSqr() < 1.0E-8D
            ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
    }

    private static void damageCone(ServerLevel level, LivingEntity attacker, double range) {
        Vec3 start = brambleStartLocal(attacker);
        Vec3 forward = CombatGravityFrame.look(attacker).normalize();
        double radius = range * BRAMBLE_CONE_RADIUS_RATIO;
        Vec3 center = start.add(forward.scale(range * 0.5D));
        AABB localArea = new AABB(center, center).inflate(range * 0.5D + radius);
        AABB worldArea = CombatGravityFrame.world(localArea);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, worldArea,
            candidate -> candidate != attacker && candidate.isAlive() && !candidate.isSpectator())) {
            Vec3 targetCenter = CombatGravityFrame.local(target.position()
                .add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            Vec3 offset = targetCenter.subtract(start);
            double projection = offset.dot(forward);
            if (projection < 0.0D || projection > range) {
                continue;
            }
            double lateralDistance = offset.subtract(forward.scale(projection)).length();
            double allowedRadius = radius * (projection / range) + target.getBbWidth() * 0.5D;
            if (lateralDistance <= allowedRadius && Damager.hurt(attacker, target, BRAMBLE_DAMAGE, true)) {
                Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_IMPACT.get(),
                    impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void spawnRaycastSlashes(ServerLevel level, LivingEntity entity) {
        Vec3 start = CombatGravityFrame.eye(entity);
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Set<BlockPos> visitedBlocks = new HashSet<>();
        for (double distance = 0.0D; distance <= BRAMBLE_RAYCAST_RANGE; distance += 0.2D) {
            Vec3 point = start.add(forward.scale(distance));
            Vec3 worldPoint = CombatGravityFrame.world(point);
            BlockPos blockPos = BlockPos.containing(worldPoint);
            if (visitedBlocks.add(blockPos)
                && level.getBlockState(blockPos).isSolidRender(level, blockPos)) {
                CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_SLASH.get(),
                    worldPoint.x, worldPoint.y, worldPoint.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                for (int particle = 0; particle < 3; particle++) {
                    Vec3 offset = randomUnitVector().scale(level.random.nextDouble() * 7.0D);
                    Vec3 nearbySlash = worldPoint.add(offset);
                    CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_SLASH.get(),
                        nearbySlash.x, nearbySlash.y, nearbySlash.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static Vec3 randomUnitVector() {
        Vec3 vector = new Vec3(
            Math.random() * 2.0D - 1.0D,
            Math.random() * 2.0D - 1.0D,
            Math.random() * 2.0D - 1.0D);
        return vector.lengthSqr() < 1.0E-8D ? new Vec3(1.0D, 0.0D, 0.0D) : vector.normalize();
    }

    @Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide() && BiokinesesArmController.isActive(entity)) {
                try {
                    BiokinesesArmController.tick(entity);
                } catch (Exception exception) {
                    System.err.println("[Biokineses] Arm controller tick failed: " + exception);
                }
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            if (!event.getEntity().level().isClientSide()) {
                handleLeftClick(event.getEntity(), event.getTarget());
            }
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (!event.getEntity().level().isClientSide()) {
                handleLeftClick(event.getEntity(), null);
            }
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            Entity source = event.getSource().getEntity();
            if (source instanceof LivingEntity attacker && !(source instanceof Player)
                && isBiokinesesItem(attacker.getMainHandItem())) {
                handleLeftClick(attacker, event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !event.player.swinging || event.player.swingTime != 0
                || SwampDemonArt.shouldIgnoreSwingForAbilityUse(event.player)) {
                return;
            }
            handleLeftClick(event.player, null);
        }
    }

    private static void cleanupForm(LivingEntity entity) {
        GuardStateHelper.clearGuardState(entity);
        GuardStateHelper.clearAttackFlag(entity);
        if (entity instanceof Player player) {
            ModNetworking.sendToAllClients(AnimationSyncPacket.createStopPacket(player.getUUID()));
        }
    }

    private static final class Bramble {
        private final UUID id;
        private Vec3 deviation;
        private final BrambleColor color;
        private final float width;

        private Bramble(UUID id, Vec3 deviation, BrambleColor color, float width) {
            this.id = id;
            this.deviation = deviation;
            this.color = color;
            this.width = width;
        }
    }

    private static final class DescendenceBramble {
        private final UUID id;
        private final Vec3 start;
        private final Vec3 end;
        private final Vec3 arcing;
        private final float width;

        private DescendenceBramble(UUID id, Vec3 start, Vec3 end, Vec3 arcing, float width) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.arcing = arcing;
            this.width = width;
        }
    }

    private static final class TransitionTarget {
        private final LivingEntity entity;
        private final Vec3 location;

        private TransitionTarget(LivingEntity entity, Vec3 location) {
            this.entity = entity;
            this.location = location;
        }
    }

    private enum BrambleColor {
        BLACK_ONLY,
        RED_ONLY,
        BOTH
    }
}
