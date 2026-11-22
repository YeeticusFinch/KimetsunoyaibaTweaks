package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.IBiokinesesArmData;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BiokinesesArmsSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * High-level control surface for Biokineses' six arms.
 *
 * <p>Forms should use this class instead of manipulating capability fields
 * directly. The server remains authoritative; clients only receive a copy
 * for rendering.</p>
 */
public final class BiokinesesArmController {
    public static final int ARM_LEFT_UPPER = 0;
    public static final int ARM_LEFT_LOWER = 1;
    public static final int ARM_LEFT_COMBAT = 2;
    public static final int ARM_RIGHT_UPPER = 3;
    public static final int ARM_RIGHT_LOWER = 4;
    public static final int ARM_RIGHT_COMBAT = 5;

    private static final int ACTIVE_TICKS = 2400;
    private static final double ARM_REACH = 8.0D;
    private static final double FLIGHT_BASE_SPEED = 0.9D;
    private static final double CLIMBING_MAX_DOWNWARD_SPEED = -0.03D;
    private static final double FRONT_ANCHOR_PROJECTION = 1.0D;
    private static final double FORCED_FRONT_PROJECTION = 3.0D;
    private static final double REAR_ANCHOR_PROJECTION = -1.0D;
    private static final double EXTENDED_ANCHOR_RANGE_FACTOR = 1.5D;
    private static final double LATERAL_ANCHOR_DISTANCE = 3.0D;
    private static final double LATERAL_DOWNWARD_SLOPE = 0.4D;
    private static final int AIRBORNE_TICKS_BEFORE_ANCHOR = 10;
    private static final int CLIMBING_GROUND_GRACE_TICKS = 30;
    private static final float COMBAT_DAMAGE = 6.0F;
    private static final long ANCHOR_RECHECK_INTERVAL = 5L;
    private static final long RETRACT_TICKS = 10L;
    private static final double ANCHOR_BEHIND_DISTANCE = ARM_REACH * 0.5D;
    private static final float ATTACK_EXTEND_SPEED = 0.42F;
    private static final float FAST_RETRACT_SPEED = 0.35F;
    private static final long ATTACK_MAX_TICKS = 12L;
    private static final long TRAVERSAL_INPUT_TIMEOUT = 4L;
    private static final int SYNC_INTERVAL = 2;

    private BiokinesesArmController() {
    }

    public static IBiokinesesArmData data(LivingEntity entity) {
        return entity.getCapability(KimetsunoyaibaMultiplayer.BIOKINESES_ARMS).orElse(null);
    }

    public static boolean isActive(LivingEntity entity) {
        IBiokinesesArmData data = data(entity);
        return data != null && data.isActive();
    }

    public static boolean isTraversalActive(LivingEntity entity) {
        IBiokinesesArmData data = data(entity);
        if (data == null || !data.isActive() || !data.isClimbing() || data.isRetracting()
            || data.getAllocationMode() != IBiokinesesArmData.ALLOCATION_NORMAL) {
            return false;
        }
        for (int arm : climbingAnchorArms()) {
            if (data.getMode(arm) == IBiokinesesArmData.MODE_ANCHOR && data.hasAnchor(arm)) {
                return true;
            }
        }
        return false;
    }

    public static void setTraversalInput(ServerPlayer player, float forward, float strafe,
                                         boolean jumping, boolean descending) {
        if (player == null || !Float.isFinite(forward) || !Float.isFinite(strafe)) {
            return;
        }
        IBiokinesesArmData data = data(player);
        if (data == null || !data.isActive() || !data.isClimbing() || data.isRetracting()
            || data.getAllocationMode() != IBiokinesesArmData.ALLOCATION_NORMAL) {
            return;
        }
        data.setTraversalInput(forward, strafe, jumping, descending, player.level().getGameTime());
    }

    /** Activates the arms and extends their lifetime by two minutes. */
    public static void activateForAbility(LivingEntity entity) {
        activateArms(entity);
        IBiokinesesArmData data = data(entity);
        if (data != null && !entity.level().isClientSide()) {
            data.setClimbing(false);
            sync(entity, true);
        }
    }

    /** Activates the arms and enables the separate Biokineses climbing mode. */
    public static void activateForClimbing(LivingEntity entity) {
        activateArms(entity);
        if (entity.level().isClientSide()) {
            return;
        }
        IBiokinesesArmData data = data(entity);
        if (data == null) {
            return;
        }
        data.setClimbing(true);
        data.setAirborneTicks(0);
        data.setClimbingTicks(0);
        data.setAllocationMode(IBiokinesesArmData.ALLOCATION_NORMAL);
        releaseHeldEntities(entity, data);
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            data.clearAnchor(arm);
            data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
            data.setTarget(arm, defaultTarget(entity, arm));
        }
        liftForClimbing(entity);
        sync(entity, true);
    }

    private static void liftForClimbing(LivingEntity entity) {
        if (!entity.onGround()) {
            return;
        }
        AABB liftedBox = entity.getBoundingBox().move(0.0D, 1.0D, 0.0D);
        if (entity.level().noCollision(entity, liftedBox)) {
            entity.setPos(entity.getX(), entity.getY() + 1.0D, entity.getZ());
            entity.setOnGround(false);
            entity.setDeltaMovement(entity.getDeltaMovement().x, 0.0D, entity.getDeltaMovement().z);
        }
    }

    private static void activateArms(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        IBiokinesesArmData data = data(entity);
        if (data == null) {
            return;
        }
        long now = entity.level().getGameTime();
        data.setActive(true);
        data.setRetracting(false);
        data.setExpiresAt(Math.max(now, data.getExpiresAt()) + ACTIVE_TICKS);
        data.setAirborneTicks(0);
        data.setClimbingTicks(0);
        if (data.getAllocationMode() != IBiokinesesArmData.ALLOCATION_ALL) {
            data.setAllocationMode(IBiokinesesArmData.ALLOCATION_NORMAL);
        }
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (data.getMode(arm) == IBiokinesesArmData.MODE_IDLE) {
                data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
                data.setTarget(arm, defaultTarget(entity, arm));
                data.setTargetEntityId(arm, -1);
                data.setAttackCooldownUntil(arm, 0L);
            }
        }
    }

    /** Claims all six arms for a form until that form releases them. */
    public static void claimAllForAbility(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        activateForAbility(entity);
        IBiokinesesArmData data = data(entity);
        if (data != null) {
            data.setAllocationMode(IBiokinesesArmData.ALLOCATION_ALL);
            for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
                data.setMode(arm, IBiokinesesArmData.MODE_ABILITY);
                data.setTarget(arm, defaultTarget(entity, arm));
            }
            sync(entity, true);
        }
    }

    /** Returns a full arm allocation to the normal four-anchor/two-combat split. */
    public static void releaseAbilityClaim(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        IBiokinesesArmData data = data(entity);
        if (data == null || !data.isActive()) {
            return;
        }
        data.setAllocationMode(IBiokinesesArmData.ALLOCATION_NORMAL);
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
            data.setTarget(arm, defaultTarget(entity, arm));
        }
        sync(entity, true);
    }

    /** Updates one claimed ability arm's world-space target for scripted forms. */
    public static void setAbilityTarget(LivingEntity entity, int arm, Vec3 target) {
        if (entity.level().isClientSide() || arm < 0 || arm >= IBiokinesesArmData.ARM_COUNT
            || target == null) {
            return;
        }
        IBiokinesesArmData data = data(entity);
        if (data != null && data.getAllocationMode() == IBiokinesesArmData.ALLOCATION_ALL
            && data.getMode(arm) == IBiokinesesArmData.MODE_ABILITY) {
            data.setTarget(arm, target);
        }
    }

    /** Starts a short synchronized retraction and releases transient targets. */
    public static void retract(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        IBiokinesesArmData data = data(entity);
        if (data == null) {
            return;
        }
        boolean wasClimbing = data.isClimbing();
        stopLocomotion(entity, data);
        if (wasClimbing) {
            MovementHelper.setVelocity(entity, Vec3.ZERO);
        }
        data.clearTraversalInput();
        releaseHeldEntities(entity, data);
        data.setActive(true);
        data.setClimbing(false);
        data.setRetracting(true);
        data.setExpiresAt(entity.level().getGameTime() + RETRACT_TICKS);
        data.setAirborneTicks(0);
        data.setClimbingTicks(0);
        data.setAllocationMode(IBiokinesesArmData.ALLOCATION_NORMAL);
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            data.setMode(arm, IBiokinesesArmData.MODE_IDLE);
            data.setTarget(arm, Vec3.ZERO);
            data.setHeldEntityId(arm, -1);
            data.setAttackUntil(arm, 0L);
            data.setAttackCooldownUntil(arm, 0L);
            data.setTargetEntityId(arm, -1);
            data.clearAnchor(arm);
        }
        sync(entity, true);
    }

    /** Starts one synchronized tentacle strike while preserving the vanilla hand attack. */
    public static void startCombatWhip(LivingEntity attacker, Entity clickedTarget) {
        if (attacker.level().isClientSide() || !isActive(attacker)) {
            return;
        }
        CombatGravityFrame.run(attacker, () -> startCombatWhipInFrame(attacker, clickedTarget));
    }

    private static void startCombatWhipInFrame(LivingEntity attacker, Entity clickedTarget) {
        IBiokinesesArmData data = data(attacker);
        if (data == null || data.getAllocationMode() != IBiokinesesArmData.ALLOCATION_NORMAL) {
            return;
        }
        List<Integer> candidates = new ArrayList<>();
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if ((attacker.onGround() || isCombatArm(arm))
                && data.getMode(arm) == IBiokinesesArmData.MODE_COMBAT) {
                candidates.add(arm);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        int arm = candidates.get(attacker.getRandom().nextInt(candidates.size()));
        AttackTarget target = resolveAttackTarget(attacker, clickedTarget);
        data.setMode(arm, IBiokinesesArmData.MODE_ATTACK);
        data.setProgress(arm, 0.0F);
        data.setTarget(arm, target.position());
        data.setTargetEntityId(arm, target.entityId());
        data.setAttackUntil(arm, attacker.level().getGameTime() + ATTACK_MAX_TICKS);
        sync(attacker, true);
    }

    public static void setTarget(LivingEntity entity, int arm, Vec3 target, int mode) {
        if (entity.level().isClientSide() || target == null || arm < 0 || arm >= IBiokinesesArmData.ARM_COUNT) {
            return;
        }
        IBiokinesesArmData data = data(entity);
        if (data == null) {
            return;
        }
        data.setTarget(arm, target);
        data.setMode(arm, mode);
    }

    /** Moves an arm to an entity and keeps that entity suspended at its tip. */
    public static void grabEntity(LivingEntity owner, int arm, LivingEntity target) {
        if (owner.level().isClientSide() || target == null || target == owner
            || arm < 0 || arm >= IBiokinesesArmData.ARM_COUNT) {
            return;
        }
        IBiokinesesArmData data = data(owner);
        if (data == null || !data.isActive()) {
            return;
        }
        if (data.isRetracting()) {
            return;
        }
        data.setTarget(arm, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
        data.setHeldEntityId(arm, target.getId());
        data.setTargetEntityId(arm, -1);
        data.setAttackCooldownUntil(arm, 0L);
        data.setMode(arm, IBiokinesesArmData.MODE_GRAB);
        sync(owner, true);
    }

    /** Releases an entity from one arm without retracting the other arms. */
    public static void releaseEntity(LivingEntity owner, int arm) {
        if (owner.level().isClientSide() || arm < 0 || arm >= IBiokinesesArmData.ARM_COUNT) {
            return;
        }
        IBiokinesesArmData data = data(owner);
        if (data == null) {
            return;
        }
        data.setHeldEntityId(arm, -1);
        data.setTargetEntityId(arm, -1);
        data.setMode(arm, isAnchorArm(arm)
            ? IBiokinesesArmData.MODE_ANCHOR : IBiokinesesArmData.MODE_COMBAT);
        data.setTarget(arm, defaultTarget(owner, arm));
        sync(owner, true);
    }

    /** Ticks extension, entity holding, block anchors, and owner locomotion. */
    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        CombatGravityFrame.run(entity, () -> tickInFrame(entity));
    }

    private static void tickInFrame(LivingEntity entity) {
        IBiokinesesArmData data = data(entity);
        if (data == null) {
            return;
        }
        if (!data.isActive()) {
            stopLocomotion(entity, data);
            data.clearTraversalInput();
            return;
        }
        if (data.isRetracting()) {
            tickRetraction(entity, data);
            return;
        }
        long now = entity.level().getGameTime();
        if (now >= data.getExpiresAt()) {
            retract(entity);
            return;
        }

        if (!data.isClimbing()) {
            stopLocomotion(entity, data);
            data.clearTraversalInput();
            data.setAirborneTicks(0);
            if (data.getAllocationMode() == IBiokinesesArmData.ALLOCATION_NORMAL) {
                clearAnchorAllocation(entity, data);
            }
            refreshFollowTargets(entity, data);
            tickHeldEntities(entity, data);
            updateProgress(data);
            tickAttackArms(entity, data, now);
            if (now % SYNC_INTERVAL == 0L) {
                sync(entity, false);
            }
            return;
        }

        data.setClimbingTicks(data.getClimbingTicks() + 1);
        if (data.getClimbingTicks() > CLIMBING_GROUND_GRACE_TICKS
            && entity.onGround() && data.getAirborneTicks() >= AIRBORNE_TICKS_BEFORE_ANCHOR) {
            retract(entity);
            return;
        }

        updateAirborneTicks(entity, data);
        updateAllocation(entity, data);
        refreshFollowTargets(entity, data);
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (data.getMode(arm) == IBiokinesesArmData.MODE_ATTACK
                && data.getAttackUntil(arm) > 0L && now >= data.getAttackUntil(arm)) {
                beginFastRetraction(entity, data, arm);
            }
        }

        List<Vec3> anchors = List.of();
        if (data.getAllocationMode() == IBiokinesesArmData.ALLOCATION_NORMAL) {
            anchors = tickAnchors(entity, data);
        }
        tickHeldEntities(entity, data);
        updateProgress(data);
        tickAttackArms(entity, data, now);
        if (data.getAllocationMode() != IBiokinesesArmData.ALLOCATION_NORMAL) {
            stopLocomotion(entity, data);
        }

        if (entity.level().getGameTime() % SYNC_INTERVAL == 0L) {
            sync(entity, false);
        }
    }

    private static void clearAnchorAllocation(LivingEntity entity, IBiokinesesArmData data) {
        for (int arm : climbingAnchorArms()) {
            data.clearAnchor(arm);
            data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
            data.setTarget(arm, defaultTarget(entity, arm));
        }
    }

    private static void tickRetraction(LivingEntity entity, IBiokinesesArmData data) {
        stopLocomotion(entity, data);
        updateProgress(data);
        boolean finished = true;
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (data.getProgress(arm) > 0.01F) {
                finished = false;
                break;
            }
        }
        if (finished || entity.level().getGameTime() >= data.getExpiresAt()) {
            data.setActive(false);
            data.setClimbing(false);
            data.setRetracting(false);
            data.setClimbingTicks(0);
            data.setExpiresAt(0L);
            for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
                data.setProgress(arm, 0.0F);
            }
        }
        sync(entity, true);
    }

    private static List<Vec3> tickAnchors(LivingEntity entity, IBiokinesesArmData data) {
        List<Vec3> anchors = new ArrayList<>();
        if (data.getAirborneTicks() < AIRBORNE_TICKS_BEFORE_ANCHOR) {
            for (int arm : climbingAnchorArms()) {
                data.clearAnchor(arm);
                data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
                data.setTarget(arm, defaultTarget(entity, arm));
            }
            data.clearTraversalInput();
            stopLocomotion(entity, data);
            return anchors;
        }
        if (entity.onGround()) {
            for (int arm : climbingAnchorArms()) {
                data.clearAnchor(arm);
                data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
                data.setTarget(arm, defaultTarget(entity, arm));
            }
            data.clearTraversalInput();
            stopLocomotion(entity, data);
            return anchors;
        }
        Set<BlockPos> occupiedBlocks = new HashSet<>();
        AnchorAdvance advance = chooseAnchorAdvance(entity, data);
        int forcedFrontArm = advance == null ? -1 : advance.arm();
        Vec3 movementDirection = advance == null ? Vec3.ZERO : climbingVelocityDirection(entity);
        for (int arm : climbingAnchorArms()) {
            boolean forcedFrontAnchor = arm == forcedFrontArm;
            boolean needsNewAnchor = hasValidAnchor(entity, data, arm)
                && shouldReanchor(entity, data, data.getTarget(arm));
            boolean duplicateAnchor = data.hasAnchor(arm)
                && occupiedBlocks.contains(BlockPos.of(data.getAnchorBlockPos(arm)));
            if (hasValidAnchor(entity, data, arm) && !needsNewAnchor && !duplicateAnchor && !forcedFrontAnchor) {
                anchors.add(data.getTarget(arm));
                occupiedBlocks.add(BlockPos.of(data.getAnchorBlockPos(arm)));
            } else if (entity.level().getGameTime() % ANCHOR_RECHECK_INTERVAL == 0L
                || !data.hasAnchor(arm) || needsNewAnchor || duplicateAnchor || forcedFrontAnchor) {
                AnchorTarget anchor = findAnchor(entity, data, arm, occupiedBlocks,
                    forcedFrontAnchor ? movementDirection : null,
                    forcedFrontAnchor ? advance.minimumForwardProjection() : 0.0D, false);
                if (anchor == null) {
                    anchor = findAnchor(entity, data, arm, occupiedBlocks,
                        forcedFrontAnchor ? movementDirection : null,
                        forcedFrontAnchor ? advance.minimumForwardProjection() : 0.0D, true);
                }
                if (anchor == null && forcedFrontAnchor && advance.minimumForwardProjection() > 0.0D) {
                    anchor = findAnchor(entity, data, arm, occupiedBlocks, movementDirection,
                        0.0D, false);
                }
                if (anchor != null) {
                    data.setAnchor(arm, anchor.blockPos().asLong(), anchor.face().get3DDataValue());
                    data.setTarget(arm, anchor.position());
                    anchors.add(anchor.position());
                    occupiedBlocks.add(anchor.blockPos());
                    data.setMode(arm, IBiokinesesArmData.MODE_ANCHOR);
                } else {
                    data.clearAnchor(arm);
                    data.setTarget(arm, defaultTarget(entity, arm));
                    data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
                }
            } else if (forcedFrontAnchor && hasValidAnchor(entity, data, arm)) {
                anchors.add(data.getTarget(arm));
                occupiedBlocks.add(BlockPos.of(data.getAnchorBlockPos(arm)));
            } else {
                data.clearAnchor(arm);
                data.setTarget(arm, defaultTarget(entity, arm));
                data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
            }
        }

        if (!anchors.isEmpty()) {
            applyLocomotion(entity, data);
            startLocomotion(entity, data);
        } else {
            boolean wasFlying = data.isLocomotionActive();
            data.clearTraversalInput();
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            stopLocomotion(entity, data);
            if (wasFlying) {
                retract(entity);
            }
        }
        return anchors;
    }

    private static void tickHeldEntities(LivingEntity owner, IBiokinesesArmData data) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            int entityId = data.getHeldEntityId(arm);
            if (entityId < 0) {
                continue;
            }
            Entity held = level.getEntity(entityId);
            if (!(held instanceof LivingEntity living) || !living.isAlive() || living == owner) {
                data.setHeldEntityId(arm, -1);
                continue;
            }
            Vec3 target = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
            data.setTarget(arm, target);
            living.teleportTo(target.x, target.y - living.getBbHeight() * 0.5D, target.z);
            living.setDeltaMovement(Vec3.ZERO);
            living.fallDistance = 0.0F;
        }
    }

    private static void updateProgress(IBiokinesesArmData data) {
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            float progress = data.getProgress(arm);
            int mode = data.getMode(arm);
            if (mode == IBiokinesesArmData.MODE_IDLE) {
                data.setProgress(arm, Math.max(0.0F, progress - 0.12F));
            } else if (mode == IBiokinesesArmData.MODE_FAST_RETRACT) {
                data.setProgress(arm, Math.max(0.0F, progress - FAST_RETRACT_SPEED));
            } else if (mode == IBiokinesesArmData.MODE_ATTACK) {
                data.setProgress(arm, Math.min(1.0F, progress + ATTACK_EXTEND_SPEED));
            } else {
                data.setProgress(arm, Math.min(1.0F, progress + 0.12F));
            }
        }
    }

    private static void updateAllocation(LivingEntity entity, IBiokinesesArmData data) {
        if (data.getAllocationMode() != IBiokinesesArmData.ALLOCATION_NORMAL) {
            return;
        }
        boolean airborne = data.getAirborneTicks() >= AIRBORNE_TICKS_BEFORE_ANCHOR;
        for (int arm : climbingAnchorArms()) {
            if (airborne) {
                if (hasValidAnchor(entity, data, arm)) {
                    data.setMode(arm, IBiokinesesArmData.MODE_ANCHOR);
                } else {
                    data.clearAnchor(arm);
                    data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
                }
            } else {
                data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
                data.clearAnchor(arm);
            }
        }
    }

    private static void updateAirborneTicks(LivingEntity entity, IBiokinesesArmData data) {
        if (entity.onGround()) {
            data.setAirborneTicks(0);
        } else {
            data.setAirborneTicks(Math.min(AIRBORNE_TICKS_BEFORE_ANCHOR,
                data.getAirborneTicks() + 1));
        }
    }

    private static void refreshFollowTargets(LivingEntity entity, IBiokinesesArmData data) {
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if ((data.getMode(arm) == IBiokinesesArmData.MODE_COMBAT
                && data.getAllocationMode() == IBiokinesesArmData.ALLOCATION_NORMAL)
                || data.getMode(arm) == IBiokinesesArmData.MODE_ABILITY) {
                data.setTarget(arm, defaultTarget(entity, arm));
            }
        }
    }

    private static void applyLocomotion(LivingEntity entity, IBiokinesesArmData data) {
        Vec3 input = traversalInputLocal(entity, data);
        double speed = flightSpeed(entity);
        Vec3 velocity = input.lengthSqr() > 1.0E-6D
            ? input.normalize().scale(speed)
            : Vec3.ZERO;
        if (Math.abs(input.y) < 1.0E-6D) {
            double currentVerticalVelocity = CombatGravityFrame.velocity(entity).y;
            double verticalVelocity = Math.max(CLIMBING_MAX_DOWNWARD_SPEED,
                Math.min(0.0D, currentVerticalVelocity));
            velocity = new Vec3(velocity.x, verticalVelocity, velocity.z);
        }
        MovementHelper.setVelocity(entity, velocity);
    }

    private static double flightSpeed(LivingEntity entity) {
        double speed = FLIGHT_BASE_SPEED;
        var speedEffect = entity.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speedEffect != null) {
            speed *= 1.0D + 0.2D * (speedEffect.getAmplifier() + 1);
        }
        if (entity.isSprinting()) {
            speed *= 2.0D;
        }
        return speed;
    }

    private static Vec3 traversalInputLocal(LivingEntity entity, IBiokinesesArmData data) {
        long now = entity.level().getGameTime();
        if (data.getTraversalInputTick() < now - TRAVERSAL_INPUT_TIMEOUT) {
            data.clearTraversalInput();
            return Vec3.ZERO;
        }

        Vec3 forward = CombatGravityFrame.look(entity);
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        double vertical = data.isTraversalJumping() == data.isTraversalDescending()
            ? 0.0D : data.isTraversalJumping() ? 1.0D : -1.0D;
        return forward.scale(data.getTraversalForwardInput())
            .add(right.scale(data.getTraversalStrafeInput()))
            .add(0.0D, vertical, 0.0D);
    }

    private static void tickAttackArms(LivingEntity attacker, IBiokinesesArmData data, long now) {
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (data.getMode(arm) == IBiokinesesArmData.MODE_ATTACK) {
                if (data.getProgress(arm) >= 0.99F && data.getAttackCooldownUntil(arm) != Long.MAX_VALUE) {
                    damageTentacle(attacker, data, arm);
                    data.setAttackCooldownUntil(arm, Long.MAX_VALUE);
                    beginFastRetraction(attacker, data, arm);
                } else if (now >= data.getAttackUntil(arm)) {
                    beginFastRetraction(attacker, data, arm);
                }
            } else if (data.getMode(arm) == IBiokinesesArmData.MODE_FAST_RETRACT
                && data.getProgress(arm) <= 0.01F) {
                data.setAttackUntil(arm, 0L);
                data.setAttackCooldownUntil(arm, 0L);
                data.setTargetEntityId(arm, -1);
                data.clearAnchor(arm);
                data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
                data.setTarget(arm, defaultTarget(attacker, arm));
            }
        }
    }

    private static void beginFastRetraction(LivingEntity entity, IBiokinesesArmData data, int arm) {
        data.setMode(arm, IBiokinesesArmData.MODE_FAST_RETRACT);
        data.setAttackUntil(arm, 0L);
        data.setProgress(arm, Math.min(1.0F, data.getProgress(arm)));
    }

    private static void damageTentacle(LivingEntity attacker, IBiokinesesArmData data, int arm) {
        Vec3 start = armStart(attacker, arm);
        Vec3 end = data.getTarget(arm);
        LivingEntity target = null;
        if (attacker.level() instanceof ServerLevel level) {
            int targetId = data.getTargetEntityId(arm);
            if (targetId >= 0 && level.getEntity(targetId) instanceof LivingEntity living
                && living.isAlive() && EnhancedLoveForms.isTargetable(attacker, living)) {
                target = living;
                end = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
            }
            if (target != null) {
                Damager.hurt(attacker, target, COMBAT_DAMAGE, true, true);
            }
            level.sendParticles(ModParticles.BLACKRED_SLASH.get(), end.x, end.y, end.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static Vec3 armStart(LivingEntity entity, int arm) {
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 left = forward.cross(up);
        if (left.lengthSqr() < 1.0E-6D) {
            left = new Vec3(-1.0D, 0.0D, 0.0D);
        } else {
            left = left.normalize();
        }
        double side = isLeftArm(arm) ? 1.0D : -1.0D;
        Vec3 local = up.scale(entity.getBbHeight() * 0.58D)
            .add(left.scale(side * 0.4D)).add(forward.scale(0.18D));
        return entity.position().add(CombatGravityFrame.world(local));
    }

    private static AttackTarget resolveAttackTarget(LivingEntity attacker, Entity clickedTarget) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(attacker));
        Vec3 direction = CombatGravityFrame.world(CombatGravityFrame.look(attacker)).normalize();
        Vec3 maximum = start.add(direction.scale(ARM_REACH));
        if (clickedTarget instanceof LivingEntity living && living != attacker && living.isAlive()
            && EnhancedLoveForms.isTargetable(attacker, living)
            && attacker.distanceTo(living) <= ARM_REACH) {
            return new AttackTarget(living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D), living.getId());
        }

        BlockHitResult blockHit = attacker.level().clip(new ClipContext(start, maximum,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
        Vec3 end = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : maximum;
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : attacker.level().getEntitiesOfClass(LivingEntity.class,
            new AABB(start, end).inflate(0.8D), living -> living != attacker && living.isAlive()
                && !living.isSpectator() && EnhancedLoveForms.isTargetable(attacker, living))) {
            Vec3 candidatePosition = candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D);
            double alongRay = candidatePosition.subtract(start).dot(direction);
            if (alongRay < 0.0D || alongRay > start.distanceTo(end)) {
                continue;
            }
            Vec3 closestPoint = start.add(direction.scale(alongRay));
            if (!candidate.getBoundingBox().inflate(0.45D).contains(closestPoint)) {
                continue;
            }
            if (alongRay < closestDistance) {
                closestDistance = alongRay;
                closest = candidate;
            }
        }
        if (closest != null) {
            return new AttackTarget(closest.position().add(0.0D, closest.getBbHeight() * 0.5D, 0.0D), closest.getId());
        }
        return new AttackTarget(end, -1);
    }

    private static void startLocomotion(LivingEntity entity, IBiokinesesArmData data) {
        if (!data.isLocomotionActive()) {
            data.setLocomotionActive(true);
        }
    }

    private static void stopLocomotion(LivingEntity entity, IBiokinesesArmData data) {
        if (!data.isLocomotionActive()) {
            return;
        }
        data.setLocomotionActive(false);
    }

    private static boolean shouldReanchor(LivingEntity entity, IBiokinesesArmData data, Vec3 anchor) {
        Vec3 movement = movementDirectionLocal(entity, data);
        if (movement.lengthSqr() < 1.0E-6D) {
            return false;
        }
        Vec3 anchorLocal = CombatGravityFrame.local(anchor.subtract(entity.position()));
        return anchorLocal.dot(movement.normalize()) < -ANCHOR_BEHIND_DISTANCE;
    }

    private static Vec3 movementInputLocal(LivingEntity entity, IBiokinesesArmData data) {
        Vec3 input = traversalInputLocal(entity, data);
        input = new Vec3(input.x, 0.0D, input.z);
        return input.lengthSqr() < 1.0E-6D ? Vec3.ZERO : input.normalize();
    }

    private static Vec3 movementDirectionLocal(LivingEntity entity, IBiokinesesArmData data) {
        Vec3 input = movementInputLocal(entity, data);
        if (input.lengthSqr() > 1.0E-6D) {
            return input;
        }
        Vec3 velocity = CombatGravityFrame.velocity(entity);
        velocity = new Vec3(velocity.x, 0.0D, velocity.z);
        return velocity.lengthSqr() < 1.0E-6D ? Vec3.ZERO : velocity.normalize();
    }

    private static boolean hasValidAnchor(LivingEntity entity, IBiokinesesArmData data, int arm) {
        if (!data.hasAnchor(arm)) {
            return false;
        }
        BlockPos blockPos = BlockPos.of(data.getAnchorBlockPos(arm));
        BlockState state = entity.level().getBlockState(blockPos);
        if (state.isAir() || state.getCollisionShape(entity.level(), blockPos).isEmpty()) {
            return false;
        }
        double maxDistance = Config.biokinesesClimbingAnchorRange * EXTENDED_ANCHOR_RANGE_FACTOR;
        return entity.position().distanceToSqr(data.getTarget(arm)) <= maxDistance * maxDistance;
    }

    private static AnchorAdvance chooseAnchorAdvance(LivingEntity entity, IBiokinesesArmData data) {
        Vec3 movement = climbingVelocityDirection(entity);
        if (movement.lengthSqr() < 1.0E-4D) {
            return null;
        }
        int frontCount = 0;
        int leastForwardArm = -1;
        double leastForwardProjection = Double.POSITIVE_INFINITY;
        int rearArm = -1;
        double furthestRearProjection = REAR_ANCHOR_PROJECTION;
        for (int arm : climbingAnchorArms()) {
            if (!hasValidAnchor(entity, data, arm)) {
                continue;
            }
            Vec3 relative = CombatGravityFrame.local(data.getTarget(arm).subtract(entity.position()));
            double projection = relative.dot(movement);
            if (projection > FRONT_ANCHOR_PROJECTION) {
                frontCount++;
            }
            if (projection < leastForwardProjection) {
                leastForwardProjection = projection;
                leastForwardArm = arm;
            }
            if (projection < furthestRearProjection) {
                furthestRearProjection = projection;
                rearArm = arm;
            }
        }
        if (frontCount < 2 && leastForwardArm >= 0) {
            return new AnchorAdvance(leastForwardArm, FORCED_FRONT_PROJECTION);
        }
        return rearArm >= 0 ? new AnchorAdvance(rearArm, FRONT_ANCHOR_PROJECTION) : null;
    }

    private static Vec3 climbingVelocityDirection(LivingEntity entity) {
        Vec3 movement = CombatGravityFrame.local(CombatGravityFrame.velocity(entity));
        return movement.lengthSqr() < 1.0E-4D ? Vec3.ZERO : movement.normalize();
    }

    private static AnchorTarget findAnchor(LivingEntity entity, IBiokinesesArmData data, int arm) {
        return findAnchor(entity, data, arm, Set.of(), null, 0.0D, false);
    }

    private static AnchorTarget findAnchor(LivingEntity entity, IBiokinesesArmData data, int arm,
                                           Set<BlockPos> occupiedBlocks) {
        return findAnchor(entity, data, arm, occupiedBlocks, null, 0.0D, false);
    }

    private static AnchorTarget findAnchor(LivingEntity entity, IBiokinesesArmData data, int arm,
                                           Set<BlockPos> occupiedBlocks, Vec3 preferredForward,
                                           double minimumForwardProjection, boolean allowOccupied) {
        Vec3 localForward = CombatGravityFrame.look(entity).normalize();
        if (localForward.lengthSqr() < 1.0E-6D) {
            localForward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        if (preferredForward != null && preferredForward.lengthSqr() > 1.0E-6D) {
            localForward = preferredForward.normalize();
        }
        Vec3 localUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 localLeft = localForward.cross(localUp);
        if (localLeft.lengthSqr() < 1.0E-6D) {
            localLeft = new Vec3(-1.0D, 0.0D, 0.0D);
        } else {
            localLeft = localLeft.normalize();
        }

        Vec3 up = CombatGravityFrame.world(localUp).normalize();
        Vec3 left = CombatGravityFrame.world(localLeft).normalize();
        Vec3 localSide = isLeftArm(arm) ? localLeft : localLeft.scale(-1.0D);
        Vec3 side = isLeftArm(arm) ? left : left.scale(-1.0D);
        Vec3 start = entity.position().add(up.scale(entity.getBbHeight() * 0.65D)).add(side.scale(0.35D));
        Vec3 backward = localForward.scale(-1.0D);
        Vec3 lateralDown = localSide.add(localUp.scale(-LATERAL_DOWNWARD_SLOPE));
        Vec3[] directions;
        if (isTopAnchorArm(arm)) {
            directions = new Vec3[] {
                localUp.add(localForward), localUp.add(localForward.scale(0.4D)), localUp,
                localForward, localUp.add(localSide), localSide, lateralDown,
                localUp.scale(-1.0D), backward
            };
        } else if (isMiddleAnchorArm(arm)) {
            directions = new Vec3[] {
                localForward, localSide, lateralDown, localSide.add(localForward.scale(0.45D)), backward,
                localForward.add(localUp.scale(0.25D)), localForward.add(localUp.scale(-0.25D)),
                localUp, localUp.scale(-1.0D)
            };
        } else {
            directions = new Vec3[] {
                lateralDown, localUp.scale(-1.0D).add(backward.scale(0.55D)), localUp.scale(-1.0D), backward,
                localUp.scale(-1.0D).add(localSide), localSide.add(backward.scale(0.35D)),
                localForward, localUp
            };
        }

        AnchorTarget best = null;
        double bestScore = Double.MAX_VALUE;
        double searchRange = preferredForward != null
            ? Config.biokinesesClimbingAnchorRange * EXTENDED_ANCHOR_RANGE_FACTOR
            : Config.biokinesesClimbingAnchorRange;
        for (Vec3 rawDirection : directions) {
            Vec3 direction = rawDirection.normalize();
            Vec3 worldDirection = CombatGravityFrame.world(direction).normalize();
            BlockHitResult hit = entity.level().clip(new ClipContext(start,
                start.add(worldDirection.scale(searchRange)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            BlockPos blockPos = hit.getBlockPos();
            boolean occupied = occupiedBlocks.contains(blockPos);
            if (occupied && !allowOccupied) {
                continue;
            }
            BlockState state = entity.level().getBlockState(blockPos);
            if (state.isAir() || state.getCollisionShape(entity.level(), blockPos).isEmpty()) {
                continue;
            }
            Vec3 position = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.06D));
            Vec3 relativeLocal = CombatGravityFrame.local(position.subtract(entity.position()));
            double forwardProjection = preferredForward == null ? 0.0D
                : relativeLocal.dot(preferredForward);
            if (forwardProjection < minimumForwardProjection) {
                continue;
            }
            double distance = start.distanceTo(position);
            double distanceScore = Math.max(0.0D,
                Config.biokinesesClimbingAnchorRange - distance) * 0.12D;
            Vec3 relativeDirection = relativeLocal.lengthSqr() > 1.0E-6D
                ? relativeLocal.normalize() : Vec3.ZERO;
            double preferenceScore = relativeDirection.dot(anchorPreference(arm, localForward,
                localUp, localSide));
            double sameLevelPenalty = isMiddleAnchorArm(arm) ? Math.abs(relativeLocal.y) * 2.0D : 0.0D;
            double duplicatePenalty = occupied ? 6.0D : 0.0D;
            double lateralProjection = relativeLocal.dot(localSide);
            double lateralFallbackPenalty = relativeLocal.y < -0.25D
                ? Math.abs(lateralProjection - LATERAL_ANCHOR_DISTANCE) * 1.25D
                : 0.0D;
            double score = distanceScore - preferenceScore * 4.0D + sameLevelPenalty
                + duplicatePenalty + lateralFallbackPenalty;
            Vec3 faceNormalLocal = CombatGravityFrame.local(
                Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
            if (isTopAnchorArm(arm) && faceNormalLocal.y < -0.5D) {
                score -= 1.0D;
            } else if (isBottomAnchorArm(arm) && faceNormalLocal.y > 0.5D) {
                score -= 1.0D;
            }
            if (score < bestScore) {
                bestScore = score;
                best = new AnchorTarget(position, blockPos, hit.getDirection());
            }
        }
        return best;
    }

    private static Vec3 defaultTarget(LivingEntity entity, int arm) {
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 left = forward.cross(up).normalize();
        double side = isLeftArm(arm) ? 1.0D : -1.0D;
        double radius = isCombatArm(arm) ? 1.15D : 1.35D;
        double height = arm == ARM_LEFT_UPPER || arm == ARM_RIGHT_UPPER ? 1.75D
            : arm == ARM_LEFT_LOWER || arm == ARM_RIGHT_LOWER ? 0.95D : 1.35D;
        double forwardOffset = isCombatArm(arm) ? 0.45D : 0.1D;
        Vec3 localOffset = left.scale(side * radius).add(up.scale(height)).add(forward.scale(forwardOffset));
        return entity.position().add(CombatGravityFrame.world(localOffset));
    }

    private static void releaseHeldEntities(LivingEntity owner, IBiokinesesArmData data) {
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            int entityId = data.getHeldEntityId(arm);
            if (entityId >= 0 && owner.level() instanceof ServerLevel level) {
                Entity held = level.getEntity(entityId);
                if (held != null) {
                    held.setDeltaMovement(Vec3.ZERO);
                }
            }
            data.setHeldEntityId(arm, -1);
        }
    }

    private static boolean isAnchorArm(int arm) {
        return arm == ARM_LEFT_UPPER || arm == ARM_LEFT_LOWER || arm == ARM_RIGHT_UPPER || arm == ARM_RIGHT_LOWER;
    }

    private static boolean isTopAnchorArm(int arm) {
        return arm == ARM_LEFT_UPPER || arm == ARM_RIGHT_UPPER;
    }

    private static boolean isMiddleAnchorArm(int arm) {
        return arm == ARM_LEFT_COMBAT || arm == ARM_RIGHT_COMBAT;
    }

    private static boolean isBottomAnchorArm(int arm) {
        return arm == ARM_LEFT_LOWER || arm == ARM_RIGHT_LOWER;
    }

    private static Vec3 anchorPreference(int arm, Vec3 forward, Vec3 up, Vec3 side) {
        if (isTopAnchorArm(arm)) {
            return up.scale(0.8D).add(forward.scale(1.6D)).add(side.scale(1.4D)).normalize();
        }
        if (isMiddleAnchorArm(arm)) {
            return forward.scale(0.8D).add(side.scale(1.8D)).normalize();
        }
        return up.scale(-1.0D).add(forward.scale(-0.35D)).add(side.scale(1.4D)).normalize();
    }

    private static int[] anchorArms() {
        return new int[] { ARM_LEFT_UPPER, ARM_LEFT_LOWER, ARM_RIGHT_UPPER, ARM_RIGHT_LOWER };
    }

    private static int[] climbingAnchorArms() {
        return new int[] { ARM_LEFT_UPPER, ARM_LEFT_LOWER, ARM_LEFT_COMBAT,
            ARM_RIGHT_UPPER, ARM_RIGHT_LOWER, ARM_RIGHT_COMBAT };
    }

    private static boolean isLeftArm(int arm) {
        return arm == ARM_LEFT_UPPER || arm == ARM_LEFT_LOWER || arm == ARM_LEFT_COMBAT;
    }

    private static boolean isCombatArm(int arm) {
        return arm == ARM_LEFT_COMBAT || arm == ARM_RIGHT_COMBAT;
    }

    private record AnchorTarget(Vec3 position, BlockPos blockPos, Direction face) {
    }

    private record AnchorAdvance(int arm, double minimumForwardProjection) {
    }

    private record AttackTarget(Vec3 position, int entityId) {
    }

    private static void sync(LivingEntity entity, boolean force) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!force && level.getGameTime() % SYNC_INTERVAL != 0L) {
            return;
        }
        ModNetworking.sendToTrackingAndSelf(new BiokinesesArmsSyncPacket(entity.getId(), data(entity)), entity);
    }
}
