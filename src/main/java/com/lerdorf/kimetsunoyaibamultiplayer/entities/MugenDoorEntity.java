package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.compat.InfinityCastleCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Mugen Door entity - dimensional portal for kizuki demons.
 *
 * Timeline:
 * - 0.0s: Spawn, play sound, start "open" animation (0.29s / 6 ticks)
 * - 0.29s: Open complete, set open=true
 * - 0.29s to (0.29+lifetime): Stay open, teleport entities if spawning=false
 * - (0.29+lifetime)s: Start "close" animation (0.25s / 5 ticks)
 * - (0.29+lifetime+0.25)s: Close complete, set open=false, remove entity
 *
 * Properties:
 * - spawning: If true, spawning demons (no teleportation)
 * - opening: True while the door is actively playing its opening animation
 * - open: True when door is open and can teleport
 * - lifetime: How long to stay open (ticks, default=100 = 5 seconds)
 */
public class MugenDoorEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Synced data for animation states
    private static final EntityDataAccessor<Boolean> DATA_OPEN =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SPAWNING =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_OPENING =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_STATE =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PORTAL_SKYBOX =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_DESTINATION_DIMENSION =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_DESTINATION_CONFIGURED =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DESTINATION_X =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DESTINATION_Y =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DESTINATION_Z =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.INT);

    // Animation timing constants (in ticks)
    private static final int OPEN_ANIMATION_DURATION = 6;  // 0.29 seconds
    private static final int CLOSE_ANIMATION_DURATION = 5; // 0.25 seconds
    private static final int OPEN_HOLD_DURATION = 100; // 5 seconds
    private static final int BREAK_HITS_REQUIRED = 4;
    private static final int BREAK_HIT_RESET_TICKS = 50;
    private static final int SKYBOX_ABYSSAL_TOWERS = 0;
    private static final int SKYBOX_SHATTERED_GRAVEYARD = 1;
    private static final int SKYBOX_DAY = 2;
    private static final int SKYBOX_NIGHT = 3;

    private int ticksAlive = 0;
    private int openDuration = OPEN_HOLD_DURATION;
    private boolean soundPlayed = false;
    private boolean forceTeleportation = false;
    private int breakHitCount = 0;
    private int lastBreakHitTick = -BREAK_HIT_RESET_TICKS;

    // Animation state tracking
    private enum DoorState { OPENING, OPEN, CLOSING, CLOSED }
    private DoorState currentState = DoorState.CLOSED;

    public record Destination(ResourceKey<Level> dimension, BlockPos pos) {
    }

    public MugenDoorEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Can pass through blocks
        this.setInvulnerable(true);
        this.setNoAi(true); // Disable AI since it's just visual
        this.setSilent(false); // Allow sound playback
        this.setNoGravity(true); // Disable gravity
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OPEN, false);
        this.entityData.define(DATA_SPAWNING, false);
        this.entityData.define(DATA_OPENING, false);
        this.entityData.define(DATA_STATE, DoorState.CLOSED.ordinal()); // 0=OPENING, 1=OPEN, 2=CLOSING, 3=CLOSED
        this.entityData.define(DATA_PORTAL_SKYBOX, 0); // 0=abyssal_towers, 1=shattered_graveyard
        this.entityData.define(DATA_DESTINATION_DIMENSION, "");
        this.entityData.define(DATA_DESTINATION_CONFIGURED, false);
        this.entityData.define(DATA_DESTINATION_X, 0);
        this.entityData.define(DATA_DESTINATION_Y, 64);
        this.entityData.define(DATA_DESTINATION_Z, 0);
    }

    /**
     * Create a mugen door entity at a specific position.
     *
     * @param level The world
     * @param pos The spawn position
     * @param openDuration Deprecated. Doors now stay open for 5 seconds after the opening animation.
     * @param spawning If true, spawning demons (no teleportation)
     * @return The created door entity
     */
    public static MugenDoorEntity create(Level level, BlockPos pos, int openDuration, boolean spawning) {
        return create(level, pos, openDuration, spawning, true);
    }

    /**
     * Create a mugen door entity with control over whether it starts opening immediately.
     */
    public static MugenDoorEntity create(Level level, BlockPos pos, int openDuration, boolean spawning, boolean opening) {
        return create(level, pos, openDuration, spawning, opening, null);
    }

    /**
     * Create a mugen door entity with an optional destination.
     */
    public static MugenDoorEntity create(
        Level level,
        BlockPos pos,
        int openDuration,
        boolean spawning,
        boolean opening,
        Destination destination
    ) {
        MugenDoorEntity door = new MugenDoorEntity(ModEntities.MUGEN_DOOR.get(), level);
        door.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        door.setYRot(0); // Always face north (can be customized if needed)
        door.setXRot(0);
        door.openDuration = OPEN_HOLD_DURATION;
        door.entityData.set(DATA_SPAWNING, spawning);
        door.configureDestination(destination);
        if (opening) {
            door.beginOpening();
        } else {
            door.entityData.set(DATA_OPEN, false);
            door.entityData.set(DATA_OPENING, false);
            door.setCurrentState(DoorState.CLOSED);
        }
        return door;
    }

    /**
     * Convenience method - create a persistent closed door that opens when right-clicked.
     */
    public static MugenDoorEntity createClosed(Level level, BlockPos pos, boolean spawning) {
        return create(level, pos, OPEN_HOLD_DURATION, spawning, false);
    }

    public static MugenDoorEntity createClosed(Level level, BlockPos pos, boolean spawning, Destination destination) {
        return create(level, pos, OPEN_HOLD_DURATION, spawning, false, destination);
    }

    /**
     * Convenience method - create door for spawning demons (no teleportation).
     */
    public static MugenDoorEntity createForSpawning(Level level, BlockPos pos, int openDuration) {
        return create(level, pos, openDuration, true);
    }

    /**
     * Convenience method - create door for teleportation (default 3 seconds open).
     */
    public static MugenDoorEntity createForTeleportation(Level level, BlockPos pos) {
        return create(level, pos, 60, false);
    }

    public static MugenDoorEntity createForcedTeleportation(Level level, BlockPos pos) {
        MugenDoorEntity door = createForTeleportation(level, pos);
        door.forceTeleportation = true;
        return door;
    }

    public boolean isOpen() {
        return this.entityData.get(DATA_OPEN);
    }

    public boolean isOpening() {
        return this.entityData.get(DATA_OPENING);
    }

    public boolean isSpawning() {
        return this.entityData.get(DATA_SPAWNING);
    }

    public int getPortalSkyboxVariant() {
        return Math.max(0, Math.min(SKYBOX_NIGHT, this.entityData.get(DATA_PORTAL_SKYBOX)));
    }

    public boolean isPortalActive() {
        return getCurrentState() != DoorState.CLOSED;
    }

    @Override
    protected AABB makeBoundingBox() {
        double halfWidth = this.getBbWidth() * 0.5D;
        double halfHeight = this.getBbHeight() * 0.5D;
        return new AABB(
            this.getX() - halfWidth,
            this.getY() - halfHeight,
            this.getZ() - halfWidth,
            this.getX() + halfWidth,
            this.getY() + halfHeight,
            this.getZ() + halfWidth
        );
    }

    /**
     * Get the current door state (synced to client)
     */
    private DoorState getCurrentState() {
        int stateId = this.entityData.get(DATA_STATE);
        return DoorState.values()[stateId];
    }

    /**
     * Set the current door state (syncs to client)
     */
    private void setCurrentState(DoorState state) {
        this.currentState = state;
        this.entityData.set(DATA_STATE, state.ordinal());
    }

    private void beginOpening() {
        this.breakHitCount = 0;
        this.ticksAlive = 0;
        this.soundPlayed = false;
        this.entityData.set(DATA_OPEN, false);
        this.entityData.set(DATA_OPENING, true);
        setCurrentState(DoorState.OPENING);
    }

    private void configureDestination(Destination explicitDestination) {
        Destination destination = explicitDestination != null ? explicitDestination : defaultDestination();
        setDestination(destination);
        this.entityData.set(DATA_PORTAL_SKYBOX, chooseSkybox(destination.dimension()));
    }

    private Destination defaultDestination() {
        BlockPos sourcePos = this.blockPosition();
        if (this.level() instanceof ServerLevel serverLevel && InfinityCastleCompat.isCastleDimension(this.level().dimension())) {
            ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
            BlockPos scaled = new BlockPos(sourcePos.getX() * 8, sourcePos.getY(), sourcePos.getZ() * 8);
            return new Destination(Level.OVERWORLD, overworld != null ? findNearestSafeOverworldLocation(overworld, scaled) : scaled);
        }

        ResourceKey<Level> castleDimension = this.level() instanceof ServerLevel serverLevel
            ? InfinityCastleCompat.resolveCastleEntryDimension(serverLevel.getServer())
            : InfinityCastleCompat.BASE_INFINITY_CASTLE;
        return new Destination(castleDimension, new BlockPos(Mth.floor(sourcePos.getX() / 8.0D), sourcePos.getY(), Mth.floor(sourcePos.getZ() / 8.0D)));
    }

    private void setDestination(Destination destination) {
        this.entityData.set(DATA_DESTINATION_DIMENSION, destination.dimension().location().toString());
        this.entityData.set(DATA_DESTINATION_CONFIGURED, true);
        this.entityData.set(DATA_DESTINATION_X, destination.pos().getX());
        this.entityData.set(DATA_DESTINATION_Y, destination.pos().getY());
        this.entityData.set(DATA_DESTINATION_Z, destination.pos().getZ());
    }

    private Destination getDestination() {
        ResourceLocation id = ResourceLocation.tryParse(this.entityData.get(DATA_DESTINATION_DIMENSION));
        ResourceKey<Level> dimension = id != null
            ? ResourceKey.create(Registries.DIMENSION, id)
            : Level.OVERWORLD;
        return new Destination(
            dimension,
            new BlockPos(
                this.entityData.get(DATA_DESTINATION_X),
                this.entityData.get(DATA_DESTINATION_Y),
                this.entityData.get(DATA_DESTINATION_Z)
            )
        );
    }

    private void ensureDestinationConfigured() {
        if (!this.entityData.get(DATA_DESTINATION_CONFIGURED) && !this.level().isClientSide) {
            configureDestination(null);
        }
    }

    private int chooseSkybox(ResourceKey<Level> destinationDimension) {
        if (InfinityCastleCompat.isCastleDimension(destinationDimension)) {
            return this.random.nextBoolean() ? SKYBOX_ABYSSAL_TOWERS : SKYBOX_SHATTERED_GRAVEYARD;
        }

        if (Level.OVERWORLD.equals(destinationDimension)) {
            MinecraftServer server = this.level() instanceof ServerLevel serverLevel ? serverLevel.getServer() : null;
            ServerLevel overworld = server != null ? server.getLevel(Level.OVERWORLD) : null;
            return overworld != null && overworld.isDay() ? SKYBOX_DAY : SKYBOX_NIGHT;
        }

        return this.level().isDay() ? SKYBOX_DAY : SKYBOX_NIGHT;
    }

    /**
     * Teleport nearby entities to the Mugen Castle dimension.
     */
    private void teleportNearbyEntities() {
        AABB doorBox = this.getBoundingBox();
        java.util.List<net.minecraft.world.entity.player.Player> nearbyPlayers = level().getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            doorBox,
            player -> player != null && !player.isSpectator() && player.getBoundingBox().intersects(doorBox)
        );

        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            if (player instanceof ServerPlayer serverPlayer) {
                teleportToDestination(serverPlayer);
            }
        }
    }

    /**
     * Teleport a player through the mugen door to this door's destination.
     */
    private void teleportToDestination(ServerPlayer player) {
        try {
            Destination destination = getDestination();
            ServerLevel targetDimension = player.getServer().getLevel(destination.dimension());
            String message = InfinityCastleCompat.isCastleDimension(destination.dimension())
                ? "§5You have been transported to the Mugen Castle!"
                : "§aYou have stepped through the Mugen Door!";

            if (targetDimension == null) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Target dimension not found");
                return;
            }

            BlockPos spawnPos = Level.OVERWORLD.equals(destination.dimension())
                ? findNearestSafeOverworldLocation(targetDimension, destination.pos())
                : destination.pos();
            player.teleportTo(targetDimension,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
            );

            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(message),
                false
            );

            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Teleported " + player.getName().getString() +
                " to " + destination.dimension().location() + " at " + spawnPos);
        } catch (Exception e) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Failed to teleport player: " + e.getMessage());
        }
    }

    private static BlockPos findNearestSafeOverworldLocation(ServerLevel level, BlockPos target) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        BlockPos clamped = new BlockPos(target.getX(), Mth.clamp(target.getY(), minY, maxY), target.getZ());
        if (isSafeTeleportPosition(level, clamped)) {
            return clamped;
        }

        for (int radius = 0; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = clamped.getX() + dx;
                    int z = clamped.getZ() + dz;
                    for (int dy = 0; dy <= 48; dy++) {
                        int upY = clamped.getY() + dy;
                        if (upY <= maxY) {
                            BlockPos candidate = new BlockPos(x, upY, z);
                            if (isSafeTeleportPosition(level, candidate)) {
                                return candidate;
                            }
                        }

                        int downY = clamped.getY() - dy;
                        if (dy > 0 && downY >= minY) {
                            BlockPos candidate = new BlockPos(x, downY, z);
                            if (isSafeTeleportPosition(level, candidate)) {
                                return candidate;
                            }
                        }
                    }

                    BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                    if (isSafeTeleportPosition(level, surface)) {
                        return surface;
                    }
                }
            }
        }

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, clamped);
        return isSafeTeleportPosition(level, surface) ? surface : clamped;
    }

    private static boolean isSafeTeleportPosition(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() >= level.getMaxBuildHeight() - 1) {
            return false;
        }

        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(head).getCollisionShape(level, head).isEmpty()
            && level.getBlockState(floor).blocksMotion();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false; // Cannot be pushed
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player)) {
            return false;
        }

        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            handleBreakHit(serverLevel);
        }

        return true;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false; // Cannot deal damage
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!isOpen() && !isOpening() && getCurrentState() == DoorState.CLOSED) {
            if (!level().isClientSide) {
                beginOpening();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    private void handleBreakHit(ServerLevel serverLevel) {
        if (this.breakHitCount > 0 && this.tickCount - this.lastBreakHitTick > BREAK_HIT_RESET_TICKS) {
            this.breakHitCount = 0;
        }

        this.breakHitCount++;
        this.lastBreakHitTick = this.tickCount;

        serverLevel.playSound(null, blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 1.0F);
        spawnCritParticles(serverLevel);

        if (this.breakHitCount >= BREAK_HITS_REQUIRED) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 1.0F);
            spawnBreakParticles(serverLevel);
            this.discard();
        }
    }

    private void spawnCritParticles(ServerLevel serverLevel) {
        serverLevel.sendParticles(
            ParticleTypes.CRIT,
            this.getX(),
            this.getY() + this.getBbHeight() * 0.5D,
            this.getZ(),
            16,
            0.35D,
            0.45D,
            0.35D,
            0.12D
        );
    }

    private void spawnBreakParticles(ServerLevel serverLevel) {
        serverLevel.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            this.getX(),
            this.getY() + this.getBbHeight() * 0.5D,
            this.getZ(),
            1,
            0.0D,
            0.0D,
            0.0D,
            0.0D
        );
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksAlive = tag.getInt("TicksAlive");
        if (tag.contains("OpenDuration")) {
            this.openDuration = OPEN_HOLD_DURATION;
        }
        this.soundPlayed = tag.getBoolean("SoundPlayed");
        this.forceTeleportation = tag.getBoolean("ForceTeleportation");
        this.breakHitCount = tag.getInt("BreakHitCount");
        this.lastBreakHitTick = tag.getInt("LastBreakHitTick");
        if (tag.contains("Open")) {
            this.entityData.set(DATA_OPEN, tag.getBoolean("Open"));
        }
        if (tag.contains("Opening")) {
            this.entityData.set(DATA_OPENING, tag.getBoolean("Opening"));
        }
        if (tag.contains("PortalSkybox")) {
            this.entityData.set(DATA_PORTAL_SKYBOX, tag.getInt("PortalSkybox"));
        }
        if (tag.contains("DestinationDimension")) {
            this.entityData.set(DATA_DESTINATION_DIMENSION, tag.getString("DestinationDimension"));
            this.entityData.set(DATA_DESTINATION_CONFIGURED, true);
            this.entityData.set(DATA_DESTINATION_X, tag.getInt("DestinationX"));
            this.entityData.set(DATA_DESTINATION_Y, tag.getInt("DestinationY"));
            this.entityData.set(DATA_DESTINATION_Z, tag.getInt("DestinationZ"));
        }
        if (tag.contains("DestinationConfigured")) {
            this.entityData.set(DATA_DESTINATION_CONFIGURED, tag.getBoolean("DestinationConfigured"));
        }
        if (tag.contains("State")) {
            try {
                setCurrentState(DoorState.valueOf(tag.getString("State")));
            } catch (IllegalArgumentException ignored) {
                setCurrentState(isOpen() ? DoorState.OPEN : (isOpening() ? DoorState.OPENING : DoorState.CLOSED));
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putInt("OpenDuration", this.openDuration);
        tag.putBoolean("SoundPlayed", this.soundPlayed);
        tag.putBoolean("ForceTeleportation", this.forceTeleportation);
        tag.putBoolean("Open", this.isOpen());
        tag.putBoolean("Opening", this.isOpening());
        tag.putInt("PortalSkybox", this.getPortalSkyboxVariant());
        Destination destination = getDestination();
        tag.putBoolean("DestinationConfigured", this.entityData.get(DATA_DESTINATION_CONFIGURED));
        tag.putString("DestinationDimension", destination.dimension().location().toString());
        tag.putInt("DestinationX", destination.pos().getX());
        tag.putInt("DestinationY", destination.pos().getY());
        tag.putInt("DestinationZ", destination.pos().getZ());
        tag.putInt("BreakHitCount", this.breakHitCount);
        tag.putInt("LastBreakHitTick", this.lastBreakHitTick);
        tag.putString("State", this.currentState.name());
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // Controller for door animation
        // Animation file should have:
        // - "kimetsunoyaibamultiplayer.mugen_door.open" (0.29 seconds)
        // - "kimetsunoyaibamultiplayer.mugen_door.close" (0.25 seconds)
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            // Use synced state data
            DoorState doorState = getCurrentState();
            switch (doorState) {
                case OPENING:
                case OPEN:
                    // Play "open" animation and hold on last frame
                    state.getController().setAnimation(RawAnimation.begin()
                        .then("kimetsunoyaibamultiplayer.mugen_door.open",
                              software.bernie.geckolib.core.animation.Animation.LoopType.HOLD_ON_LAST_FRAME));
                    return software.bernie.geckolib.core.object.PlayState.CONTINUE;
                case CLOSING:
                case CLOSED:
                    // Play "close" animation (play once, then hold)
                    state.getController().setAnimation(RawAnimation.begin()
                        .then("kimetsunoyaibamultiplayer.mugen_door.close",
                              software.bernie.geckolib.core.animation.Animation.LoopType.HOLD_ON_LAST_FRAME));
                    return software.bernie.geckolib.core.object.PlayState.CONTINUE;
                default:
                    state.getController().setAnimation(RawAnimation.begin()
                        .then("kimetsunoyaibamultiplayer.mugen_door.open",
                              software.bernie.geckolib.core.animation.Animation.LoopType.HOLD_ON_LAST_FRAME));
                    return software.bernie.geckolib.core.object.PlayState.CONTINUE;
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // No collisions
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return false; // No collisions
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        // Prevent any travel/movement
        // Don't call super.travel() - this prevents physics updates
    }

    @Override
    public void move(net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 pos) {
        // Prevent any movement
        // Don't call super.move() - door stays exactly where spawned
    }

    @Override
    public void tick() {
        // Override tick to prevent entity ticking that might apply physics
        // We'll handle our own tick logic without calling super.tick()
        this.baseTick(); // Only do base entity updates (like NBT, effects)

        // Aggressively prevent any movement or gravity
        this.setNoGravity(true);
        this.setDeltaMovement(0, 0, 0); // Zero out velocity every tick
        this.setOnGround(true); // Prevent falling calculations

        if (!level().isClientSide) {
            ensureDestinationConfigured();

            if (this.breakHitCount > 0 && this.tickCount - this.lastBreakHitTick > BREAK_HIT_RESET_TICKS) {
                this.breakHitCount = 0;
            }

            DoorState doorState = getCurrentState();

            // Play sound on first active opening tick
            if (doorState == DoorState.OPENING && !soundPlayed) {
                level().playSound(
                    null, // null = all players can hear
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    ModSounds.DEMON_SLAYER_DOOR_OPEN.get(),
                    SoundSource.AMBIENT,
                    1.0f, // volume
                    1.0f  // pitch
                );
                soundPlayed = true;
            }

            // State machine for door animation and behavior
            switch (doorState) {
                case CLOSED:
                    this.ticksAlive = 0;
                    this.entityData.set(DATA_OPEN, false);
                    this.entityData.set(DATA_OPENING, false);
                    break;

                case OPENING:
                    ticksAlive++;
                    // Opening animation (6 ticks / 0.29 seconds)
                    if (ticksAlive >= OPEN_ANIMATION_DURATION) {
                        setCurrentState(DoorState.OPEN);
                        this.entityData.set(DATA_OPEN, true);
                        this.entityData.set(DATA_OPENING, false);
                        this.ticksAlive = 0;
                        com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Mugen door opened");
                    }
                    break;

                case OPEN:
                    ticksAlive++;
                    // While open, teleport entities if not spawning
                    if (!isSpawning()
                        && (this.forceTeleportation
                            || com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig.enableMugenDoorTeleportation.get())) {
                        teleportNearbyEntities();
                    }

                    // Check if it's time to close
                    if (ticksAlive >= openDuration) {
                        setCurrentState(DoorState.CLOSING);
                        this.entityData.set(DATA_OPEN, false);
                        this.entityData.set(DATA_OPENING, false);
                        this.ticksAlive = 0;
                        com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Mugen door closing");
                    }
                    break;

                case CLOSING:
                    ticksAlive++;
                    // Closing animation (5 ticks / 0.25 seconds)
                    if (ticksAlive >= CLOSE_ANIMATION_DURATION) {
                        setCurrentState(DoorState.CLOSED);
                        this.entityData.set(DATA_OPEN, false);
                        this.entityData.set(DATA_OPENING, false);
                        com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Mugen door closed, removing");
                        this.discard();
                    }
                    break;
            }
        }
    }
}
