package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.IBiokinesesArmData;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BiokinesesArmsSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
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
    private static final double PULL_STRENGTH = 0.06D;
    private static final double COMBAT_RANGE = 10.0D;
    private static final float COMBAT_DAMAGE = 6.0F;
    private static final long COMBAT_ATTACK_INTERVAL = 14L;
    private static final long ANCHOR_RECHECK_INTERVAL = 5L;
    private static final long RETRACT_TICKS = 10L;
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

    /** Activates the arms and extends their lifetime by two minutes. */
    public static void activateForAbility(LivingEntity entity) {
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
        if (data.getAllocationMode() != IBiokinesesArmData.ALLOCATION_ALL) {
            data.setAllocationMode(IBiokinesesArmData.ALLOCATION_NORMAL);
        }
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (data.getMode(arm) == IBiokinesesArmData.MODE_IDLE) {
                data.setMode(arm, isAnchorArm(arm)
                    ? IBiokinesesArmData.MODE_ANCHOR : IBiokinesesArmData.MODE_COMBAT);
                data.setTarget(arm, defaultTarget(entity, arm));
                data.setTargetEntityId(arm, -1);
                data.setAttackCooldownUntil(arm, 0L);
            }
        }
        sync(entity, true);
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
            data.setMode(arm, isAnchorArm(arm)
                ? IBiokinesesArmData.MODE_ANCHOR : IBiokinesesArmData.MODE_COMBAT);
            data.setTarget(arm, defaultTarget(entity, arm));
        }
        sync(entity, true);
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
        releaseHeldEntities(entity, data);
        data.setActive(true);
        data.setRetracting(true);
        data.setExpiresAt(entity.level().getGameTime() + RETRACT_TICKS);
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

    /** Starts a synchronized whip motion with the two normal combat arms. */
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
        long until = attacker.level().getGameTime() + 10L;
        Vec3 target = clickedTarget instanceof LivingEntity living
            ? living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D)
            : raycastTarget(attacker);
        for (int arm : combatArms()) {
            data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
            data.setTarget(arm, target);
            data.setAttackUntil(arm, until);
        }
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
        if (data == null || !data.isActive()) {
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

        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (data.getAttackUntil(arm) > 0L && now >= data.getAttackUntil(arm)) {
                data.setAttackUntil(arm, 0L);
                data.setTargetEntityId(arm, -1);
                data.setTarget(arm, defaultTarget(entity, arm));
                data.setMode(arm, isAnchorArm(arm)
                    ? IBiokinesesArmData.MODE_ANCHOR : IBiokinesesArmData.MODE_COMBAT);
            }
        }

        if (data.getAllocationMode() == IBiokinesesArmData.ALLOCATION_NORMAL) {
            tickAnchors(entity, data);
            tickCombatArms(entity, data, now);
        }
        tickHeldEntities(entity, data);
        updateProgress(data);

        if (entity.level().getGameTime() % SYNC_INTERVAL == 0L) {
            sync(entity, false);
        }
    }

    private static void tickRetraction(LivingEntity entity, IBiokinesesArmData data) {
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
            data.setRetracting(false);
            data.setExpiresAt(0L);
            for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
                data.setProgress(arm, 0.0F);
            }
        }
        sync(entity, true);
    }

    private static void tickAnchors(LivingEntity entity, IBiokinesesArmData data) {
        List<Vec3> anchors = new ArrayList<>();
        for (int arm : anchorArms()) {
            if (hasValidAnchor(entity, data, arm)) {
                anchors.add(data.getTarget(arm));
            } else if (entity.level().getGameTime() % ANCHOR_RECHECK_INTERVAL == 0L
                || !data.hasAnchor(arm)) {
                AnchorTarget anchor = findAnchor(entity, arm);
                if (anchor != null) {
                    data.setAnchor(arm, anchor.blockPos().asLong(), anchor.face().get3DDataValue());
                    data.setTarget(arm, anchor.position());
                    anchors.add(anchor.position());
                } else {
                    data.clearAnchor(arm);
                    data.setTarget(arm, defaultTarget(entity, arm));
                }
            } else {
                data.clearAnchor(arm);
                data.setTarget(arm, defaultTarget(entity, arm));
            }
            data.setMode(arm, IBiokinesesArmData.MODE_ANCHOR);
        }

        if (anchors.size() >= 2 && shouldApplyLocomotion(entity)) {
            Vec3 average = Vec3.ZERO;
            for (Vec3 anchor : anchors) {
                average = average.add(anchor);
            }
            average = average.scale(1.0D / anchors.size());
            Vec3 pull = average.subtract(entity.position()).scale(PULL_STRENGTH);
            Vec3 velocity = CombatGravityFrame.velocity(entity).add(CombatGravityFrame.local(pull));
            if (velocity.lengthSqr() > 1.0D) {
                velocity = velocity.normalize();
            }
            CombatGravityFrame.velocity(entity, velocity);
        }
    }

    private static boolean shouldApplyLocomotion(LivingEntity entity) {
        return !(entity instanceof Player player) || player.zza != 0.0F || player.xxa != 0.0F
            || player.isSprinting() || player.isShiftKeyDown();
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
            } else {
                data.setProgress(arm, Math.min(1.0F, progress + 0.12F));
            }
        }
    }

    private static void tickCombatArms(LivingEntity entity, IBiokinesesArmData data, long now) {
        for (int arm : combatArms()) {
            if (data.getAttackUntil(arm) > 0L) {
                continue;
            }

            LivingEntity target = null;
            int targetId = data.getTargetEntityId(arm);
            if (targetId >= 0 && entity.level() instanceof ServerLevel level) {
                Entity candidate = level.getEntity(targetId);
                if (candidate instanceof LivingEntity living && isCombatTarget(entity, living)
                    && entity.distanceTo(living) <= COMBAT_RANGE && hasLineOfSight(entity, living)) {
                    target = living;
                }
            }

            if (target == null) {
                data.setTargetEntityId(arm, -1);
                target = findCombatTarget(entity, data, arm);
                if (target != null) {
                    data.setTargetEntityId(arm, target.getId());
                }
            }

            data.setMode(arm, IBiokinesesArmData.MODE_COMBAT);
            if (target == null) {
                data.setTarget(arm, defaultTarget(entity, arm));
                continue;
            }

            Vec3 targetPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            data.setTarget(arm, targetPosition);
            if (data.getProgress(arm) >= 0.92F && now >= data.getAttackCooldownUntil(arm)) {
                Damager.hurt(entity, target, COMBAT_DAMAGE, true, true);
                data.setAttackCooldownUntil(arm, now + COMBAT_ATTACK_INTERVAL);
            }
        }
    }

    private static LivingEntity findCombatTarget(LivingEntity owner, IBiokinesesArmData data, int arm) {
        AABB searchArea = owner.getBoundingBox().inflate(COMBAT_RANGE);
        LivingEntity closest = null;
        double closestScore = Double.MAX_VALUE;
        Set<Integer> assignedTargets = new HashSet<>();
        for (int otherArm : combatArms()) {
            if (otherArm != arm && data.getTargetEntityId(otherArm) >= 0) {
                assignedTargets.add(data.getTargetEntityId(otherArm));
            }
        }

        for (LivingEntity candidate : owner.level().getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> target != owner && target.isAlive() && !target.isSpectator()
                && !target.isInvulnerable() && !target.isAlliedTo(owner)
                && isCombatTarget(owner, target))) {
            if (owner.distanceTo(candidate) > COMBAT_RANGE || !hasLineOfSight(owner, candidate)) {
                continue;
            }
            double score = owner.distanceToSqr(candidate);
            if (assignedTargets.contains(candidate.getId())) {
                score += 4.0D;
            }
            if (score < closestScore) {
                closestScore = score;
                closest = candidate;
            }
        }
        return closest;
    }

    private static boolean isCombatTarget(LivingEntity owner, LivingEntity candidate) {
        return Damager.isAngry(candidate, owner) || Damager.isAngry(owner, candidate);
    }

    private static boolean hasLineOfSight(LivingEntity owner, LivingEntity target) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(owner));
        Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        BlockHitResult hit = owner.level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        return hit.getType() == HitResult.Type.MISS
            || hit.getLocation().distanceToSqr(start) + 0.25D >= start.distanceToSqr(end);
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
        return entity.position().distanceToSqr(data.getTarget(arm)) <= (ARM_REACH + 2.0D) * (ARM_REACH + 2.0D);
    }

    private static AnchorTarget findAnchor(LivingEntity entity, int arm) {
        Vec3 localForward = CombatGravityFrame.look(entity).normalize();
        if (localForward.lengthSqr() < 1.0E-6D) {
            localForward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 localUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 localLeft = localUp.cross(localForward);
        if (localLeft.lengthSqr() < 1.0E-6D) {
            localLeft = new Vec3(-1.0D, 0.0D, 0.0D);
        } else {
            localLeft = localLeft.normalize();
        }

        Vec3 forward = CombatGravityFrame.world(localForward).normalize();
        Vec3 up = CombatGravityFrame.world(localUp).normalize();
        Vec3 left = CombatGravityFrame.world(localLeft).normalize();
        Vec3 side = isLeftArm(arm) ? left : left.scale(-1.0D);
        Vec3 start = entity.position().add(up.scale(entity.getBbHeight() * 0.65D)).add(side.scale(0.35D));
        boolean lowerArm = arm == ARM_LEFT_LOWER || arm == ARM_RIGHT_LOWER;
        Vec3 down = up.scale(-1.0D);
        Vec3 preferred = entity.getDeltaMovement().lengthSqr() > 0.01D
            ? entity.getDeltaMovement().normalize() : forward;
        Vec3[] directions = lowerArm
            ? new Vec3[] { down, preferred.add(down.scale(0.35D)), side.add(down.scale(0.2D)),
                forward.add(down.scale(0.15D)), forward.add(up.scale(0.25D)) }
            : new Vec3[] { preferred.add(down.scale(0.15D)), forward.add(up.scale(0.3D)),
                side.add(forward.scale(0.35D)), down, forward };

        AnchorTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (Vec3 rawDirection : directions) {
            Vec3 direction = rawDirection.normalize();
            BlockHitResult hit = entity.level().clip(new ClipContext(start, start.add(direction.scale(ARM_REACH)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            BlockPos blockPos = hit.getBlockPos();
            BlockState state = entity.level().getBlockState(blockPos);
            if (state.isAir() || state.getCollisionShape(entity.level(), blockPos).isEmpty()) {
                continue;
            }
            Vec3 position = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.06D));
            double score = start.distanceToSqr(position);
            if (lowerArm && hit.getDirection() == Direction.UP) {
                score -= 2.0D;
            }
            if (!lowerArm && hit.getDirection() != Direction.UP) {
                score -= 0.5D;
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
        Vec3 left = up.cross(forward).normalize();
        double side = isLeftArm(arm) ? 1.0D : -1.0D;
        double radius = isCombatArm(arm) ? 1.15D : 1.35D;
        double height = arm == ARM_LEFT_UPPER || arm == ARM_RIGHT_UPPER ? 1.75D
            : arm == ARM_LEFT_LOWER || arm == ARM_RIGHT_LOWER ? 0.95D : 1.35D;
        double forwardOffset = isCombatArm(arm) ? 0.45D : 0.1D;
        Vec3 localOffset = left.scale(side * radius).add(up.scale(height)).add(forward.scale(forwardOffset));
        return entity.position().add(CombatGravityFrame.world(localOffset));
    }

    private static Vec3 raycastTarget(LivingEntity entity) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(entity));
        Vec3 end = start.add(CombatGravityFrame.world(CombatGravityFrame.look(entity)).scale(6.0D));
        BlockHitResult hit = entity.level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
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

    private static int[] anchorArms() {
        return new int[] { ARM_LEFT_UPPER, ARM_LEFT_LOWER, ARM_RIGHT_UPPER, ARM_RIGHT_LOWER };
    }

    private static int[] combatArms() {
        return new int[] { ARM_LEFT_COMBAT, ARM_RIGHT_COMBAT };
    }

    private static boolean isLeftArm(int arm) {
        return arm == ARM_LEFT_UPPER || arm == ARM_LEFT_LOWER || arm == ARM_LEFT_COMBAT;
    }

    private static boolean isCombatArm(int arm) {
        return arm == ARM_LEFT_COMBAT || arm == ARM_RIGHT_COMBAT;
    }

    private record AnchorTarget(Vec3 position, BlockPos blockPos, Direction face) {
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
