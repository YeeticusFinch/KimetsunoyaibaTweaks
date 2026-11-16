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
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodTornadoEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodTornadoSpawner;
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
import net.minecraft.world.entity.player.Player;
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
    private static final int MERCILESS_END_TICK = 7;
    private static final double MERCILESS_RANGE = 40.0D;
    private static final int ULTIMATE_END_TICK = 21;
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
                   "Dash to the nearest target and claw it twice with both arms.",
                   3,
                   Biokineses::executeMercilessPunishment
               ),
               new BloodDemonArtForm(
                   FORM_ULTIMATE_AUTHORITY,
                   "Ultimate Authority",
                   "Unleash a red claw storm and launch a blood tornado across the ground.",
                   4,
                   Biokineses::executeUltimateAuthority
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
            final Vec3 target = findMercilessTarget(entity, serverLevel);
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        return;
                    }

                    if (tick == 0) {
                        AnimationHelper.playAnimation(entity, "sword_to_right", 6);
                        GuardStateHelper.setGuardState(entity, CLAW_DAMAGE, formId, false);
                    } else if (tick == 1) {
                        spawnImpactLine(serverLevel, entity.getEyePosition(), target);
                        entity.teleportTo(target.x, target.y, target.z);
                        performClawSlash(entity, null, "sword_to_right", formId);
                    } else if (tick == 5) {
                        performClawSlash(entity, null, "left_sword_to_left", formId);
                    } else if (tick == MERCILESS_END_TICK) {
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
            Vec3 localDirection = CombatGravityFrame.look(entity).normalize();
            Vec3 worldDirection = CombatGravityFrame.world(localDirection).normalize();
            Vec3 position = CombatGravityFrame.world(CombatGravityFrame.position(entity));
            BloodTornadoEntity tornado = BloodTornadoSpawner.spawn(
                serverLevel, position, worldDirection, entity, ULTIMATE_END_TICK);

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
                        AnimationHelper.playAnimation(entity, "left_sword_overhead", ULTIMATE_END_TICK);
                        performClawSlash(entity, null, "left_sword_overhead", formId);
                    } else if (tick == ULTIMATE_END_TICK) {
                        if (tornado != null) {
                            tornado.discard();
                        }
                        cleanupForm(entity);
                    }

                    tick++;
                }
            }, 1, ULTIMATE_END_TICK + 1);
        });
    }

    private static Vec3 findMercilessTarget(LivingEntity entity, ServerLevel level) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(entity));
        Vec3 direction = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
        Vec3 end = start.add(direction.scale(MERCILESS_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));

        Vec3 target = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        double targetDistance = start.distanceToSqr(target);
        AABB searchArea = new AABB(start, end).inflate(1.5D);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchArea,
            living -> living != entity && living.isAlive() && !living.isSpectator())) {
            var hit = candidate.getBoundingBox().clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < targetDistance) {
                    target = hit.get();
                    targetDistance = distance;
                }
            }
        }
        return target;
    }

    private static void spawnImpactLine(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 step = delta.scale(1.0D / length);
        for (double distance = 0.0D; distance <= length; distance += 0.5D) {
            Vec3 point = start.add(step.scale(distance));
            CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_IMPACT.get(),
                point.x, point.y, point.z, 1, 0.04D, 0.04D, 0.04D, 0.02D);
        }
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

    private enum BrambleColor {
        BLACK_ONLY,
        RED_ONLY,
        BOTH
    }
}
