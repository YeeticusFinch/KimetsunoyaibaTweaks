package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.compat.GravityApiCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KyogaiClawEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.field.GravityField;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.field.GravityFieldManager;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.KyogaiGravityArrowPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Kyogai's drum blood demon art. */
public final class KyogaiDrumsArt {
    public static final String ART_ID = "kyogai_drums";
    private static final int KYOGAI_ARROW_COLOR = 0xFF8A00;

    public static final int FORM_RIGHT_SHOULDER_DRUM = 3700;
    public static final int FORM_LEFT_SHOULDER_DRUM = 3701;
    public static final int FORM_RIGHT_LEG_DRUM = 3702;
    public static final int FORM_LEFT_LEG_DRUM = 3703;
    public static final int FORM_NAVEL_DRUM = 3704;
    public static final int FORM_BACK_DRUM_SELF = 3705;
    public static final int FORM_BACK_DRUM_TARGET = 3706;

    private static final int DRUM_HIT_TICKS = 15;
    private static final int GRAVITY_CHANGE_DELAY_TICKS = 10;
    private static final int FIELD_SIZE = 30;
    private static final int FIELD_INFLATION = 3;
    private static final long FIELD_DURATION_TICKS = 20L * 60L;
    private static final int FALLBACK_PUSH_TICKS = 40;
    private static final double PRE_SWITCH_UPWARD_VELOCITY = 0.8D;
    private static final double PRE_SWITCH_GRAVITY_VELOCITY = 0.8D;
    private static final double UNSAFE_GRAVITY_VELOCITY = 3.0D;
    private static final float UNSAFE_TARGET_DAMAGE = 9.0F;
    private static final int GRAVITY_ARROW_DURATION_TICKS = 30;
    private static final double FACING_SAFE_DOT = Math.sqrt(0.5D);
    private static final int ROOM_TELEPORT_RETRIES = 10;
    private static final int ROOM_TELEPORT_ATTEMPTS = 80;
    private static final double CLAW_SURFACE_RANGE = 30.0D;
    private static final Map<UUID, FieldState> ACTIVE_FIELDS = new HashMap<>();
    private KyogaiDrumsArt() {
    }

    public static void register() {
        if (!BloodDemonArtRegistry.isRegistered(ART_ID)) {
            KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Kyogai's Drums", createTechnique());
        }
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Kyogai's Drums",
            List.of(
                form(FORM_RIGHT_SHOULDER_DRUM, "Right Shoulder Drum", "drum_right_shoulder", "Rotate the room's gravity to the right.", ModSounds.DRUM_RIGHT_SHOULDER.get(), KyogaiDrumsArt::rightShoulder),
                form(FORM_LEFT_SHOULDER_DRUM, "Left Shoulder Drum", "drum_left_shoulder", "Rotate the room's gravity to the left.", ModSounds.DRUM_LEFT_SHOULDER.get(), KyogaiDrumsArt::leftShoulder),
                form(FORM_RIGHT_LEG_DRUM, "Right Leg Drum", "drum_right_leg", "Rotate the room's gravity forward.", ModSounds.DRUM_RIGHT_LEG.get(), KyogaiDrumsArt::forward),
                form(FORM_LEFT_LEG_DRUM, "Left Leg Drum", "drum_left_leg", "Rotate the room's gravity backward.", ModSounds.DRUM_LEFT_LEG.get(), KyogaiDrumsArt::backward),
                form(FORM_NAVEL_DRUM, "Navel Drum", "drum_naval", "Launch three spinning claw slashes.", ModSounds.DRUM_NAVEL.get(), KyogaiDrumsArt::navel),
                form(FORM_BACK_DRUM_SELF, "Back Drum (self)", "drum_back", "Teleport to another room in the mansion.", ModSounds.DRUM_BACK.get(), KyogaiDrumsArt::teleportSelf),
                form(FORM_BACK_DRUM_TARGET, "Back Drum (target)", "drum_back", "Teleport the entity in your crosshair.", ModSounds.DRUM_BACK.get(), KyogaiDrumsArt::teleportTarget)
            ),
            0xAA1E2F
        );
    }

    private static BloodDemonArtForm form(int id, String name, String animation, String description,
                                          SoundEvent sound, Ability ability) {
        return new BloodDemonArtForm(id, name, description, 2,
            (entity, level, ignored) -> executeDrum(entity, level, animation, sound, ability));
    }

    private static void executeDrum(LivingEntity entity, Level level, String animation,
                                    SoundEvent sound, Ability ability) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (entity instanceof Player player) {
            KnYAPI.playAnimation(player, "kimetsunoyaibamultiplayer:" + animation, 25);
        }
        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive()) {
                return;
            }
            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                sound, SoundSource.HOSTILE, 1.0F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
            ability.execute(entity, serverLevel);
        }, DRUM_HIT_TICKS);
    }

    private static void rightShoulder(LivingEntity entity, ServerLevel level) {
        AbilityScheduler.scheduleOnce(entity,
            () -> rotate(entity, level, true, -1.0D), GRAVITY_CHANGE_DELAY_TICKS);
    }

    private static void leftShoulder(LivingEntity entity, ServerLevel level) {
        AbilityScheduler.scheduleOnce(entity,
            () -> rotate(entity, level, true, 1.0D), GRAVITY_CHANGE_DELAY_TICKS);
    }

    private static void forward(LivingEntity entity, ServerLevel level) {
        AbilityScheduler.scheduleOnce(entity,
            () -> rotateLeg(entity, level, true), GRAVITY_CHANGE_DELAY_TICKS);
    }

    private static void backward(LivingEntity entity, ServerLevel level) {
        AbilityScheduler.scheduleOnce(entity,
            () -> rotateLeg(entity, level, false), GRAVITY_CHANGE_DELAY_TICKS);
    }

    private static void rotate(LivingEntity owner, ServerLevel level, boolean aroundForward, double quarterTurns) {
        FieldState state = ACTIVE_FIELDS.computeIfAbsent(owner.getUUID(), ignored ->
            new FieldState(owner.getUUID(), level.dimension(), KNYGravity.getGravityDirection(owner)));
        Direction oldDirection = state.gravity;
        Vec3 gravity = Vec3.atLowerCornerOf(oldDirection.getNormal());
        Vec3 forward = cardinalLookDirection(owner);
        Vec3 up = gravity.scale(-1.0D);
        forward = projectOntoFloor(forward, up);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = projectOntoFloor(new Vec3(0.0D, 0.0D, 1.0D), up);
        }
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = projectOntoFloor(new Vec3(1.0D, 0.0D, 0.0D), up);
        }
        forward = forward.normalize();
        Vec3 right = forward.cross(up).normalize();
        Vec3 axis = aroundForward ? forward : right;
        Vec3 rotatedGravity = rotateAroundAxis(gravity, axis, quarterTurns * Math.PI * 0.5D);
        Direction newDirection = Direction.getNearest(rotatedGravity.x, rotatedGravity.y, rotatedGravity.z);
        beginGravityChange(owner, level, state, newDirection);
    }

    private static void rotateLeg(LivingEntity owner, ServerLevel level, boolean rightLeg) {
        FieldState state = ACTIVE_FIELDS.computeIfAbsent(owner.getUUID(), ignored ->
            new FieldState(owner.getUUID(), level.dimension(), KNYGravity.getGravityDirection(owner)));
        Vec3 forwardVector = cardinalLookDirection(owner);
        Direction forward = Direction.getNearest(forwardVector.x, 0.0D, forwardVector.z);
        Direction backward = forward.getOpposite();
        Vec3 rightVector = forwardVector.cross(new Vec3(0.0D, 1.0D, 0.0D));
        Direction right = Direction.getNearest(rightVector.x, rightVector.y, rightVector.z);
        Direction left = right.getOpposite();
        Direction current = state.gravity;
        Direction newDirection;

        if (current == Direction.DOWN || current == left || current == right) {
            newDirection = rightLeg ? forward : backward;
        } else if (current == forward) {
            newDirection = Direction.UP;
        } else if (current == Direction.UP) {
            newDirection = rightLeg ? backward : forward;
        } else {
            newDirection = Direction.DOWN;
        }

        beginGravityChange(owner, level, state, newDirection);
    }

    private static void beginGravityChange(LivingEntity owner, ServerLevel level, FieldState state,
                                           Direction newDirection) {
        state.expiresAt = level.getGameTime() + FIELD_DURATION_TICKS;
        state.box = makeFieldBox(owner);
        PendingGravityChange pending = new PendingGravityChange(newDirection);
        state.pending = pending;

        Vec3 newGravity = directionVector(newDirection);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, state.box,
            entity -> entity != owner && entity.isAlive())) {
            pending.targetIds.add(target.getUUID());
            Direction currentGravity = KNYGravity.getGravityDirection(target);
            Vec3 localUp = directionVector(currentGravity.getOpposite());
            setWorldVelocity(target, localUp.scale(PRE_SWITCH_UPWARD_VELOCITY)
                .add(newGravity.scale(PRE_SWITCH_GRAVITY_VELOCITY)));
        }

        if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
            ModNetworking.sendToPlayer(new KyogaiGravityArrowPacket(
                newDirection, GRAVITY_ARROW_DURATION_TICKS, KYOGAI_ARROW_COLOR), player);
        }

        AbilityScheduler.scheduleOnce(owner,
            () -> punishUnsafeTargets(owner, level, pending), GRAVITY_CHANGE_DELAY_TICKS - 1);
        AbilityScheduler.scheduleOnce(owner,
            () -> applyGravityChange(owner, level, state, pending), GRAVITY_CHANGE_DELAY_TICKS);
    }

    private static void punishUnsafeTargets(LivingEntity owner, ServerLevel level,
                                            PendingGravityChange pending) {
        FieldState state = ACTIVE_FIELDS.get(owner.getUUID());
        if (state == null || state.pending != pending) {
            return;
        }

        Vec3 newGravity = directionVector(pending.newDirection);
        for (UUID targetId : pending.targetIds) {
            if (!(level.getEntity(targetId) instanceof LivingEntity target)
                || !target.isAlive() || target == owner) {
                continue;
            }
            Vec3 facing = lookWorld(target);
            if (facing.dot(newGravity) >= FACING_SAFE_DOT) {
                continue;
            }

            setWorldVelocity(target, newGravity.scale(UNSAFE_GRAVITY_VELOCITY));
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_BIG_FALL, SoundSource.HOSTILE, 1.0F, 1.0F);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5D), target.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            Damager.hurt(owner, target, UNSAFE_TARGET_DAMAGE);
        }
    }

    private static void applyGravityChange(LivingEntity owner, ServerLevel level, FieldState state,
                                            PendingGravityChange pending) {
        if (state.pending != pending) {
            return;
        }
        state.pending = null;
        state.gravity = pending.newDirection;
        updateField(owner, level, state);

        if (!GravityApiCompat.isAvailable()) {
            state.fallbackUntil = level.getGameTime() + FALLBACK_PUSH_TICKS;
            state.fallbackDirection = pending.newDirection;
            applyFallbackPush(owner, level, state);
        }
    }

    private static void updateField(LivingEntity owner, ServerLevel level, FieldState state) {
        AABB box = makeFieldBox(owner);
        state.box = box;
        if (!GravityApiCompat.isAvailable()) {
            return;
        }
        Vec3 forward = cardinalLookDirection(owner);
        BlockPos shiftedSource = BlockPos.containing(owner.position().add(forward.scale(15.0D)));
        GravityFieldManager.register(new GravityField(
            owner.getUUID(), level.dimension(), box, state.gravity, state.gravity,
            FIELD_SIZE + FIELD_INFLATION * 2, FIELD_SIZE + FIELD_INFLATION * 2,
            FIELD_SIZE + FIELD_INFLATION * 2, true, 75.0D,
            shiftedSource, owner.getUUID()));
    }

    private static void applyFallbackPush(LivingEntity owner, ServerLevel level, FieldState state) {
        Vec3 push = Vec3.atLowerCornerOf(state.fallbackDirection.getNormal()).scale(0.65D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, state.box,
            entity -> entity != owner && entity.isAlive())) {
            MovementHelper.setVelocity(target, push);
        }
    }

    private static void setWorldVelocity(LivingEntity target, Vec3 worldVelocity) {
        CombatGravityFrame.run(target,
            () -> MovementHelper.setVelocity(target, CombatGravityFrame.local(worldVelocity)));
    }

    private static Vec3 directionVector(Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }

    private static void navel(LivingEntity owner, ServerLevel level) {
        FieldState field = ACTIVE_FIELDS.get(owner.getUUID());
        Vec3 launch = owner.getEyePosition().add(lookWorld(owner).scale(0.75D));
        Direction spreadGravity = field != null && field.box.contains(launch) ? field.gravity : Direction.DOWN;
        Vec3 spreadDown = Vec3.atLowerCornerOf(spreadGravity.getNormal());
        Vec3 spreadUp = spreadDown.scale(-1.0D);
        Vec3 forward = projectClawForward(lookWorld(owner), owner, spreadUp);
        Vec3 right = forward.cross(spreadUp);
        if (right.lengthSqr() < 1.0E-4D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        int count = owner.getHealth() < owner.getMaxHealth() * 0.30F ? 5 : 3;
        for (int i = 0; i < count; i++) {
            double centeredIndex = i - (count - 1) / 2.0D;
            double spread = centeredIndex * 0.5D;
            double angle = Math.toRadians(centeredIndex * 5.0D);
            Vec3 start = launch.add(right.scale(spread));
            Direction clawGravity = chooseClawGravity(level, field, start);
            Vec3 clawDown = Vec3.atLowerCornerOf(clawGravity.getNormal());
            Vec3 clawUp = clawDown.scale(-1.0D);
            Vec3 clawForward = projectClawForward(lookWorld(owner), owner, clawUp);
            clawForward = rotateAroundAxis(clawForward, clawDown, angle).normalize();
            Vec3 grounded = findClawSurface(level, start, clawDown, CLAW_SURFACE_RANGE);
            KyogaiClawEntity claw = KyogaiClawEntity.create(level, grounded, clawForward, owner,
                clawGravity);
            level.addFreshEntity(claw);
        }
    }

    private static Direction chooseClawGravity(ServerLevel level, FieldState field, Vec3 start) {
        if (field == null || !field.box.contains(start)) {
            return Direction.DOWN;
        }

        Direction fieldGravity = field.gravity;
        Vec3 down = Vec3.atLowerCornerOf(fieldGravity.getNormal());
        return hasClawSurface(level, start, down, CLAW_SURFACE_RANGE)
            ? fieldGravity : Direction.DOWN;
    }

    private static boolean hasClawSurface(ServerLevel level, Vec3 start, Vec3 down, double distance) {
        return raycastSolidSurface(level, start, down, distance) != null;
    }

    private static Vec3 findClawSurface(ServerLevel level, Vec3 start, Vec3 down, double distance) {
        BlockHitResult hit = raycastSolidSurface(level, start, down, distance);
        return hit != null ? hit.getLocation().subtract(down.scale(0.15D)) : start;
    }

    private static BlockHitResult raycastSolidSurface(ServerLevel level, Vec3 start, Vec3 down, double distance) {
        Vec3 end = start.add(down.scale(distance));
        HitResult hit = level.clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos blockPos = blockHit.getBlockPos();
        return level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()
            ? null : blockHit;
    }

    private static Vec3 projectClawForward(Vec3 preferred, LivingEntity owner, Vec3 up) {
        Vec3 forward = projectOntoFloor(preferred, up);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = projectOntoFloor(cardinalLookDirection(owner), up);
        }
        if (forward.lengthSqr() < 1.0E-4D) {
            Vec3 fallback = Math.abs(up.y) < 0.9D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
            forward = projectOntoFloor(fallback, up);
        }
        return forward.normalize();
    }

    private static void teleportSelf(LivingEntity entity, ServerLevel level) {
        teleportToRoom(entity, level);
    }

    private static void teleportTarget(LivingEntity entity, ServerLevel level) {
        LivingEntity target = findCrosshairTarget(entity, level);
        if (target != null) {
            teleportToRoom(target, level);
        }
    }

    private static LivingEntity findCrosshairTarget(LivingEntity owner, ServerLevel level) {
        Vec3 start = owner.getEyePosition();
        Vec3 end = start.add(lookWorld(owner).scale(50.0D));
        HitResult blockHit = level.clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        double maxDistance = blockHit.getType() == HitResult.Type.BLOCK
            ? start.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(start, end).inflate(1.0D), entity -> entity != owner && entity.isAlive())) {
            java.util.Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance && distance <= maxDistance) {
                    closestDistance = distance;
                    closest = candidate;
                }
            }
        }
        return closest;
    }

    private static void teleportToRoom(LivingEntity entity, ServerLevel level) {
        BlockPos origin = entity.blockPosition();
        for (int retry = 0; retry < ROOM_TELEPORT_RETRIES; retry++) {
            for (int attempt = 0; attempt < ROOM_TELEPORT_ATTEMPTS; attempt++) {
                int x = origin.getX() + level.random.nextInt(101) - 50;
                int y = origin.getY() + level.random.nextInt(21) - 10;
                int z = origin.getZ() + level.random.nextInt(101) - 50;
                BlockPos candidate = new BlockPos(x, y, z);
                if (!isSafeRoom(level, entity, candidate)) {
                    continue;
                }
                entity.teleportTo(x + 0.5D, y, z + 0.5D);
                entity.setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
    }

    private static boolean isSafeRoom(ServerLevel level, LivingEntity entity, BlockPos candidate) {
        if (level.canSeeSky(candidate.above(2)) || !level.getBlockState(candidate.below()).isFaceSturdy(level,
            candidate.below(), Direction.UP)) {
            return false;
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = candidate.offset(x, y, z);
                    if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        || !level.getFluidState(pos).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return level.noCollision(entity, entity.getBoundingBox().move(
            candidate.getX() + 0.5D - entity.getX(), candidate.getY() - entity.getY(),
            candidate.getZ() + 0.5D - entity.getZ()));
    }

    private static AABB makeFieldBox(LivingEntity owner) {
        Vec3 forward = cardinalLookDirection(owner);
        // Rebuild the fixed-size world-space box each time; the margin is not
        // applied to the previous box, so repeated drum hits never accumulate.
        // Gravity changes the direction applied inside it, not the box shape.
        Vec3 down = Vec3.atLowerCornerOf(Direction.DOWN.getNormal());
        Vec3 up = down.scale(-1.0D);
        // The horizontal left axis is based only on the resolved cardinal look
        // direction. For example, north points left toward west.
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 firstCorner = owner.position()
            .add(left.scale(15.0D))
            .add(down.scale(15.0D));
        Vec3 secondCorner = owner.position()
            .add(left.scale(-15.0D))
            .add(forward.scale(FIELD_SIZE))
            .add(up.scale(15.0D));

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        Vec3[] corners = {firstCorner, secondCorner};
        for (Vec3 corner : corners) {
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(FIELD_INFLATION);
    }

    private static Vec3 lookWorld(LivingEntity entity) {
        if (!KNYGravity.isEnabled()) {
            return entity.getLookAngle().normalize();
        }
        return GravityDirectionHelper.getLookDirection(entity).normalize();
    }

    private static Vec3 cardinalLookDirection(LivingEntity entity) {
        Vec3 look = lookWorld(entity);
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = Vec3.atLowerCornerOf(Direction.fromYRot(entity.getYRot()).getNormal());
        }
        Direction cardinal = Direction.getNearest(horizontal.x, 0.0D, horizontal.z);
        return Vec3.atLowerCornerOf(cardinal.getNormal());
    }

    private static Vec3 projectOntoFloor(Vec3 vector, Vec3 up) {
        return vector.subtract(up.scale(vector.dot(up)));
    }

    private static Vec3 rotateAroundAxis(Vec3 vector, Vec3 axis, double angle) {
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return vector.scale(cosine)
            .add(axis.cross(vector).scale(sine))
            .add(axis.scale(axis.dot(vector) * (1.0D - cosine)));
    }

    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
                return;
            }
            List<UUID> removed = new ArrayList<>();
            for (FieldState state : ACTIVE_FIELDS.values()) {
                Entity entity = event.getServer().getLevel(state.dimension) == null
                    ? null : event.getServer().getLevel(state.dimension).getEntity(state.owner);
                if (!(entity instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) {
                    GravityFieldManager.unregister(state.dimension, state.owner);
                    removed.add(state.owner);
                    continue;
                }
                ServerLevel level = (ServerLevel) owner.level();
                if (level.getGameTime() >= state.expiresAt) {
                    GravityFieldManager.unregister(state.dimension, state.owner);
                    removed.add(state.owner);
                    continue;
                }
                if (state.pending == null) {
                    updateField(owner, level, state);
                }
                if (!GravityApiCompat.isAvailable() && level.getGameTime() < state.fallbackUntil) {
                    applyFallbackPush(owner, level, state);
                }
            }
            for (UUID owner : removed) {
                ACTIVE_FIELDS.remove(owner);
            }
        }
    }

    private static final class FieldState {
        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private Direction gravity;
        private Direction fallbackDirection = Direction.DOWN;
        private long fallbackUntil;
        private long expiresAt;
        private AABB box = new AABB(0, 0, 0, 0, 0, 0);
        private PendingGravityChange pending;

        private FieldState(UUID owner, net.minecraft.resources.ResourceKey<Level> dimension, Direction gravity) {
            this.owner = owner;
            this.dimension = dimension;
            this.gravity = gravity;
        }
    }

    private static final class PendingGravityChange {
        private final Direction newDirection;
        private final Set<UUID> targetIds = new HashSet<>();

        private PendingGravityChange(Direction newDirection) {
            this.newDirection = newDirection;
        }
    }

    @FunctionalInterface
    private interface Ability {
        void execute(LivingEntity entity, ServerLevel level);
    }
}
