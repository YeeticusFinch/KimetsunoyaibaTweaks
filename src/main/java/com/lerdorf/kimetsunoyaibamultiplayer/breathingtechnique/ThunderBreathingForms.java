package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ThunderLightningSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ThunderLightningSparkSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Enhanced Thunder Breathing forms and their shared lightning presentation. */
public final class ThunderBreathingForms {
    private static final double FIRST_FORM_RANGE = 20.0D;
    private static final double SIXFOLD_RANGE = 15.0D;
    private static final double EIGHTFOLD_RANGE = 10.0D;
    private static final double FIRST_FORM_START_OFFSET = 10.0D;
    private static final Vec3 NORMAL_PRIMARY = new Vec3(1.0D, 0.9D, 0.12D);
    private static final Vec3 NORMAL_SECONDARY = new Vec3(0.42D, 0.86D, 1.0D);
    private static final Vec3 BLUE_PRIMARY = new Vec3(0.05D, 0.16D, 0.72D);
    private static final Vec3 BLUE_SECONDARY = new Vec3(0.38D, 0.86D, 1.0D);
    private static final Vec3 KAIGAKU_SECONDARY = Vec3.ZERO;
    private static final Map<UUID, ZenitsuState> ZENITSU_STATES = new HashMap<>();

    private ThunderBreathingForms() {
    }

    public static BreathingTechnique createThunderBreathing(boolean blueVariant) {
        return new BreathingTechnique(
            "Thunder Breathing",
            List.of(firstForm(blueVariant ? LightningVariant.BLUE : LightningVariant.NORMAL)),
            "§e",
            "§e"
        );
    }

    public static BreathingTechnique createKaigakuBreathing() {
        return new BreathingTechnique(
            "Thunder Breathing",
            List.of(firstForm(LightningVariant.KAIGAKU)),
            "§e",
            "§e"
        );
    }

    public static BreathingTechnique createZenitsuBreathing() {
        return new BreathingTechnique(
            "Thunder Breathing",
            List.of(firstForm(LightningVariant.NORMAL), chainedForm(false), chainedForm(true)),
            "§e",
            "§e"
        );
    }

    /** First Form: Thunderclap and Flash. */
    private static BreathingForm firstForm(LightningVariant lightningVariant) {
        return new BreathingForm(
            30001,
            "First Form: Thunderclap and Flash",
            "Hold an unsheathing stance, then dash through opponents in a lightning-fast slash",
            2,
            (entity, level, formId) -> executeFirstForm(entity, level, formId, lightningVariant)
        );
    }

    private static BreathingForm chainedForm(boolean eightfold) {
        int formId = eightfold ? 30003 : 30002;
        String name = eightfold
            ? "First Form: Thunderclap and Flash, Eightfold"
            : "First Form: Thunderclap and Flash, Sixfold";
        String description = eightfold
            ? "Chain up to eight lightning dashes from successive clicks"
            : "Chain up to six lightning dashes from successive clicks";
        int cooldownSeconds = eightfold ? 3 : 2;
        return new BreathingForm(
            formId,
            name,
            description,
            cooldownSeconds,
            (entity, level, ignoredFormId) -> startZenitsuForm(entity, level, formId, eightfold)
        );
    }

    private static void executeFirstForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                         int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final Vec3 startLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        final Vec3 startWorld = firstFormStartPoint(entity);
        final Vec3[] targetLocal = {null};
        final Vec3[] targetWorld = {null};
        final boolean originalNoGravity = entity.isNoGravity();
        final int[] currentTick = {0};

        GuardStateHelper.setGuardState(entity, 12.0D, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", 20, 1.0F, 4000);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = currentTick[0]++;

            if (tick < 20) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                entity.setSprinting(false);
                if (!(entity instanceof Player)) {
                    MovementHelper.lookAtTarget(entity);
                }
                return;
            }

            if (tick == 20) {
                targetWorld[0] = raycastTarget(serverLevel, entity, FIRST_FORM_RANGE);
                targetLocal[0] = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(targetWorld[0]);
                return;
            }

            if (tick >= 21 && tick <= 25 && targetLocal[0] != null) {
                if (tick == 21) {
                    playEntityAnimation(entity, "sword_to_right");
                    level.playSound(null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.PLAYERS, 0.65F, 1.35F);
                    syncLightning(serverLevel, entity, true, true,
                        new Vec3[] {startWorld, entity.position()}, lightningVariant);
                }

                if (tick < 25) {
                    MovementHelper.setVelocity(entity, targetLocal[0].subtract(startLocal).scale(0.2D));
                } else {
                    MovementHelper.setVelocity(entity, Vec3.ZERO);
                    Vec3 finalWorldPosition = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(targetLocal[0]);
                    entity.teleportTo(finalWorldPosition.x, finalWorldPosition.y, finalWorldPosition.z);
                    targetWorld[0] = entity.position();
                    syncLightning(serverLevel, entity, true, false,
                        new Vec3[] {startWorld, targetWorld[0]}, lightningVariant);

                    GuardStateHelper.setAttackState(entity, 12.0D);
                    damageTrajectory(serverLevel, entity, startWorld, targetWorld[0]);
                    spawnZenitsuImpact(serverLevel, targetWorld[0]);
                    spawnLightningSparks(serverLevel, entity, lightningVariant);
                    GuardStateHelper.clearAttackFlag(entity);
                }

                if (tick > 21 && tick < 25) {
                    syncLightning(serverLevel, entity, true, false,
                        new Vec3[] {startWorld, entity.position()}, lightningVariant);
                }
            }
        }, 1, 37);

        AbilityScheduler.scheduleOnce(entity, () -> {
            syncLightning(serverLevel, entity, false, false,
                new Vec3[] {startWorld, entity.position()}, lightningVariant);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "iai1");
        }, 37);
    }

    private static void startZenitsuForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                         int formId, boolean eightfold) {
        if (!(level instanceof ServerLevel serverLevel) || ZENITSU_STATES.containsKey(entity.getUUID())) {
            return;
        }

        int chargeTicks = eightfold ? 40 : 30;
        int launchWindowTicks = eightfold ? 60 : 40;
        int launchTicks = eightfold ? 3 : 5;
        int endingTicks = eightfold ? 15 : 20;
        ZenitsuState state = new ZenitsuState(
            entity.getUUID(),
            eightfold,
            eightfold ? 8 : 6,
            chargeTicks,
            launchWindowTicks,
            launchTicks,
            endingTicks,
            firstFormStartPoint(entity),
            entity.isNoGravity()
        );
        ZENITSU_STATES.put(entity.getUUID(), state);

        GuardStateHelper.setGuardState(entity, 12.0D, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", chargeTicks, 1.0F, 4000);

        int totalTicks = chargeTicks + launchWindowTicks + endingTicks + 2;
        AbilityScheduler.scheduleRepeating(entity, () -> tickZenitsuForm(serverLevel, entity, state), 1, totalTicks);
    }

    private static void tickZenitsuForm(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (ZENITSU_STATES.get(entity.getUUID()) != state || state.cleaned) {
            return;
        }

        int tick = state.elapsedTicks++;
        if (!entity.isAlive()) {
            finishZenitsuForm(level, entity, state);
            return;
        }

        if (state.ending) {
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            state.endingTicksElapsed++;
            if (state.endingTicksElapsed >= state.endingTicks) {
                finishZenitsuForm(level, entity, state);
            }
            return;
        }

        if (tick < state.chargeTicks) {
            freezeZenitsuEntity(entity);
            return;
        }

        if (tick == state.chargeTicks) {
            state.windowOpen = true;
            state.path.clear();
            state.path.add(state.startWorld);
            state.path.add(entity.position());
            syncLightning(level, entity, true, true, state.pathArray(), LightningVariant.NORMAL, 40);
            freezeZenitsuEntity(entity);
            return;
        }

        if (tick < state.chargeTicks + state.launchWindowTicks) {
            if (state.launching) {
                advanceZenitsuLaunch(level, entity, state);
            } else {
                freezeZenitsuEntity(entity);
            }
            return;
        }

        beginZenitsuEnding(entity, state);
    }

    /**
     * Handles either mouse button during a Zenitsu chain. Returning true means the
     * click belongs to the ability and should not become a normal sword swing.
     */
    public static boolean handleZenitsuClick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }

        ZenitsuState state = ZENITSU_STATES.get(entity.getUUID());
        if (state == null || state.ending) {
            return false;
        }

        if (state.launching) {
            if (state.launchesCompleted + state.queuedLaunches < state.maxLaunches) {
                state.queuedLaunches++;
            }
            return true;
        }

        if (state.windowOpen && !state.launching && state.launchesCompleted < state.maxLaunches) {
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.run(entity,
                () -> beginZenitsuLaunch(level, entity, state));
        }
        return true;
    }

    private static void beginZenitsuLaunch(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (state.launching || state.ending) {
            return;
        }

        double range = state.eightfold ? EIGHTFOLD_RANGE : SIXFOLD_RANGE;
        Vec3 targetWorld = raycastTarget(level, entity, range);
        Vec3 startLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        Vec3 targetLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(targetWorld);
        state.launchStartWorld = entity.position();
        state.targetWorld = targetWorld;
        state.targetLocal = targetLocal;
        state.dashVelocityLocal = targetLocal.subtract(startLocal).scale(1.0D / state.launchTicks);
        state.launchTick = 0;
        state.launching = true;

        playEntityAnimation(entity, "sword_to_right");
        level.playSound(null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundSource.PLAYERS, 0.55F, 1.4F);
    }

    private static Vec3 firstFormStartPoint(LivingEntity entity) {
        Vec3 startLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        Vec3 lookLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity).normalize();
        return com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            startLocal.subtract(lookLocal.scale(FIRST_FORM_START_OFFSET)));
    }

    private static void advanceZenitsuLaunch(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        MovementHelper.setVelocity(entity, state.dashVelocityLocal);
        state.launchTick++;
        updateZenitsuPath(level, entity, state);

        if (state.launchTick >= state.launchTicks) {
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.teleportTo(state.targetWorld.x, state.targetWorld.y, state.targetWorld.z);
            state.path.set(state.path.size() - 1, state.targetWorld);
            syncLightning(level, entity, true, false, state.pathArray(), LightningVariant.NORMAL, 40);

            GuardStateHelper.setAttackState(entity, 12.0D);
            damageTrajectory(level, entity, state.launchStartWorld, state.targetWorld);
            GuardStateHelper.clearAttackFlag(entity);
            spawnZenitsuImpact(level, state.targetWorld);

            state.launchesCompleted++;
            state.launching = false;
            if (state.launchesCompleted >= state.maxLaunches) {
                beginZenitsuEnding(entity, state);
            } else {
                state.path.add(state.targetWorld);
                state.path.add(entity.position());
                syncLightning(level, entity, true, false, state.pathArray(), LightningVariant.NORMAL, 40);
                if (state.queuedLaunches > 0) {
                    state.queuedLaunches--;
                    beginZenitsuLaunch(level, entity, state);
                }
            }
        }
    }

    private static void updateZenitsuPath(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (!state.path.isEmpty()) {
            state.path.set(state.path.size() - 1, entity.position());
            syncLightning(level, entity, true, false, state.pathArray(), LightningVariant.NORMAL, 40);
        }
    }

    private static void beginZenitsuEnding(LivingEntity entity, ZenitsuState state) {
        if (state.ending) {
            return;
        }
        state.ending = true;
        state.windowOpen = false;
        state.endingTicksElapsed = 0;
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        playEntityAnimationOnLayer(entity, "iai1", state.endingTicks, 1.0F, 4000);
    }

    private static void finishZenitsuForm(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (state.cleaned) {
            return;
        }
        state.cleaned = true;
        ZENITSU_STATES.remove(entity.getUUID(), state);
        syncLightning(level, entity, false, false, state.pathArray(), LightningVariant.NORMAL, 40);
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        entity.setNoGravity(state.originalNoGravity);
        GuardStateHelper.clearGuardState(entity);
        setCancelAttackSwing(entity, false);
        stopEntityAnimation(entity, "iai1");
    }

    private static void freezeZenitsuEntity(LivingEntity entity) {
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        entity.setSprinting(false);
        if (!(entity instanceof Player)) {
            MovementHelper.lookAtTarget(entity);
        }
    }

    private static Vec3 raycastTarget(ServerLevel level, LivingEntity entity, double distance) {
        Vec3 eyeLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity);
        Vec3 lookLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity).normalize();
        Vec3 eyeWorld = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(eyeLocal);
        Vec3 lookWorld = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(lookLocal).normalize();
        Vec3 rayEnd = eyeWorld.add(lookWorld.scale(distance));
        BlockHitResult hit = level.clip(new ClipContext(
            eyeWorld, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 hitWorld = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? rayEnd : hit.getLocation();
        Vec3 eyeHeightWorld = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            new Vec3(0.0D, entity.getEyeHeight(), 0.0D));
        Vec3 desiredFeetPosition = hitWorld.subtract(eyeHeightWorld);
        return findNearestSafePosition(level, entity, desiredFeetPosition);
    }

    /**
     * Raycast landing points can be inside the block whose face was hit, especially
     * when looking down. Search the nearest position around that point where the
     * entity's complete collision box is clear before allowing a teleport.
     */
    private static Vec3 findNearestSafePosition(ServerLevel level, LivingEntity entity, Vec3 desiredPosition) {
        final double step = 0.25D;
        final int radiusInSteps = 8; // Two blocks in every direction.
        Vec3 bestPosition = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -radiusInSteps; x <= radiusInSteps; x++) {
            for (int y = -radiusInSteps; y <= radiusInSteps; y++) {
                for (int z = -radiusInSteps; z <= radiusInSteps; z++) {
                    double offsetX = x * step;
                    double offsetY = y * step;
                    double offsetZ = z * step;
                    double distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
                    if (distanceSquared > 4.0D || distanceSquared >= bestDistance) {
                        continue;
                    }

                    Vec3 candidate = desiredPosition.add(offsetX, offsetY, offsetZ);
                    Vec3 movement = candidate.subtract(entity.position());
                    if (level.noCollision(entity, entity.getBoundingBox().move(movement))) {
                        bestPosition = candidate;
                        bestDistance = distanceSquared;
                    }
                }
            }
        }

        return bestPosition != null ? bestPosition : entity.position();
    }

    private static void damageTrajectory(ServerLevel level, LivingEntity attacker, Vec3 start, Vec3 end) {
        AABB trajectory = new AABB(start, end).inflate(2.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, trajectory,
            candidate -> candidate != attacker && candidate.isAlive()
                && !CrowEnhancementHandler.isKasugaiCrow(candidate));
        for (LivingEntity target : targets) {
            double reach = 2.0D + target.getBbWidth() * 0.5D;
            if (distanceToSegmentSquared(target.getBoundingBox().getCenter(), start, end) <= reach * reach) {
                Damager.hurt(attacker, target, 12.0F);
                spawnZenitsuImpact(level, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            }
        }
    }

    private static void spawnZenitsuImpact(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.FLASH, position.x, position.y, position.z,
            1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z,
            12, 0.35D, 0.35D, 0.35D, 0.04D);
    }

    private static void spawnLightningSparks(ServerLevel level, LivingEntity entity,
                                             LightningVariant lightningVariant) {
        Vec3 localFeet = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        Vec3 localHead = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity);
        Vec3 start = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            localFeet.lerp(localHead, 0.5D));
        Vec3 outerColor = switch (lightningVariant) {
            case KAIGAKU -> BLUE_PRIMARY;
            case BLUE -> BLUE_SECONDARY;
            case NORMAL -> NORMAL_SECONDARY;
        };

        for (int i = 0; i < 4; i++) {
            SparkEndpoint endpoint = findSparkEndpoint(level, start);
            ThunderLightningSparkSyncPacket packet = new ThunderLightningSparkSyncPacket(
                UUID.randomUUID(), true, true, start, endpoint.position(), Vec3.ZERO,
                endpoint.solidBlock() ? new Vec3(0.0D, 1.0D, 0.0D) : Vec3.ZERO,
                 outerColor, new Vec3(1.0D, 1.0D, 1.0D), 0.0875F, 8, true);
            Vec3 end = endpoint.position();
            ModNetworking.sendToNearby(packet, level, end.x, end.y, end.z, 64.0D);
        }
    }

    private static SparkEndpoint findSparkEndpoint(ServerLevel level, Vec3 start) {
        List<Vec3> solidBlocks = new ArrayList<>();
        BlockPos origin = BlockPos.containing(start);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    Vec3 center = Vec3.atCenterOf(candidate);
                    if (center.distanceToSqr(start) <= 4.0D
                        && level.getBlockState(candidate).isSolidRender(level, candidate)) {
                        solidBlocks.add(center);
                    }
                }
            }
        }

        if (!solidBlocks.isEmpty()) {
            return new SparkEndpoint(solidBlocks.get(level.random.nextInt(solidBlocks.size())), true);
        }

        double theta = level.random.nextDouble() * Math.PI * 2.0D;
        double vertical = level.random.nextDouble() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        Vec3 direction = new Vec3(Math.cos(theta) * horizontal, vertical, Math.sin(theta) * horizontal);
        return new SparkEndpoint(start.add(direction.scale(2.0D)), false);
    }

    private static void syncLightning(ServerLevel level, LivingEntity entity, boolean active, boolean spawn,
                                      Vec3[] positions, LightningVariant lightningVariant) {
        syncLightning(level, entity, active, spawn, positions, lightningVariant, 15);
    }

    private static void syncLightning(ServerLevel level, LivingEntity entity, boolean active, boolean spawn,
                                      Vec3[] positions, LightningVariant lightningVariant, int lifetime) {
        Vec3 primary = lightningVariant == LightningVariant.NORMAL ? NORMAL_PRIMARY : BLUE_PRIMARY;
        Vec3 secondary = switch (lightningVariant) {
            case NORMAL -> NORMAL_SECONDARY;
            case BLUE -> BLUE_SECONDARY;
            case KAIGAKU -> KAIGAKU_SECONDARY;
        };
        Vec3[] elevatedPositions = new Vec3[positions.length];
        for (int i = 0; i < positions.length; i++) {
            Vec3 position = positions[i];
            elevatedPositions[i] = position.add(0.0D, 1.0D, 0.0D);
        }
        ThunderLightningSyncPacket packet = new ThunderLightningSyncPacket(
            entity.getUUID(), active, spawn, elevatedPositions, primary, secondary,
            lightningVariant == LightningVariant.NORMAL, lifetime);
        Vec3 end = elevatedPositions.length == 0
            ? entity.position().add(0.0D, 1.0D, 0.0D)
            : elevatedPositions[elevatedPositions.length - 1];
        ModNetworking.sendToNearby(packet, level, end.x, end.y, end.z, 64.0D);
    }

    private static double distanceToSegmentSquared(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-8D) {
            return point.distanceToSqr(start);
        }
        double progress = point.subtract(start).dot(segment) / lengthSquared;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return point.distanceToSqr(start.add(segment.scale(progress)));
    }

    private static void playEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimation(player, animationName);
        } else {
            AnimationHelper.playAnimation(entity, animationName);
        }
    }

    private static void playEntityAnimationOnLayer(LivingEntity entity, String animationName,
                                                   int maxTicks, float speed, int layer) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimationOnLayer(player, animationName, maxTicks, speed, layer);
        } else {
            AnimationHelper.playAnimationOnLayer(entity, animationName, maxTicks, speed, layer);
        }
    }

    private static void setCancelAttackSwing(LivingEntity entity, boolean value) {
        if (!(entity instanceof Player player)) {
            return;
        }
        player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA)
            .ifPresent(data -> data.setCancelAttackSwing(value));
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(new SwordWielderSyncPacket(player.getUUID(), value), serverPlayer);
        }
    }

    private static void stopEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            ResourceLocation animation = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
            ModNetworking.sendToAllClients(new AnimationSyncPacket(
                player.getUUID(), animation, 0, 0, false, true));
        } else if (!entity.level().isClientSide) {
            ModNetworking.sendToAllClients(new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket(
                entity.getId(), "__stop__"));
        }
    }

    private static final class ZenitsuState {
        private final UUID entityId;
        private final boolean eightfold;
        private final int maxLaunches;
        private final int chargeTicks;
        private final int launchWindowTicks;
        private final int launchTicks;
        private final int endingTicks;
        private final Vec3 startWorld;
        private final boolean originalNoGravity;
        private final List<Vec3> path = new ArrayList<>();
        private int elapsedTicks;
        private int launchesCompleted;
        private int queuedLaunches;
        private int launchTick;
        private int endingTicksElapsed;
        private boolean windowOpen;
        private boolean launching;
        private boolean ending;
        private boolean cleaned;
        private Vec3 launchStartWorld;
        private Vec3 targetWorld;
        private Vec3 targetLocal;
        private Vec3 dashVelocityLocal;

        private ZenitsuState(UUID entityId, boolean eightfold, int maxLaunches, int chargeTicks,
                             int launchWindowTicks, int launchTicks, int endingTicks,
                             Vec3 startWorld, boolean originalNoGravity) {
            this.entityId = entityId;
            this.eightfold = eightfold;
            this.maxLaunches = maxLaunches;
            this.chargeTicks = chargeTicks;
            this.launchWindowTicks = launchWindowTicks;
            this.launchTicks = launchTicks;
            this.endingTicks = endingTicks;
            this.startWorld = startWorld;
            this.originalNoGravity = originalNoGravity;
        }

        private Vec3[] pathArray() {
            return path.toArray(new Vec3[0]);
        }
    }

    private enum LightningVariant {
        NORMAL,
        BLUE,
        KAIGAKU
    }

    private record SparkEndpoint(Vec3 position, boolean solidBlock) {
    }
}
