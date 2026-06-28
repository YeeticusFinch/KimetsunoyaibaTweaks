package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * - open: True when door is open and can teleport
 * - lifetime: How long to stay open (ticks, default=20 = 1 second)
 */
public class MugenDoorEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final ResourceLocation MUGEN_CASTLE_DIMENSION_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mugen_castle_dimension");
    private static final ResourceKey<Level> MUGEN_CASTLE_DIMENSION =
        ResourceKey.create(Registries.DIMENSION, MUGEN_CASTLE_DIMENSION_ID);

    // Synced data for animation states
    private static final EntityDataAccessor<Boolean> DATA_OPEN =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SPAWNING =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_STATE =
        SynchedEntityData.defineId(MugenDoorEntity.class, EntityDataSerializers.INT);

    // Animation timing constants (in ticks)
    private static final int OPEN_ANIMATION_DURATION = 6;  // 0.29 seconds
    private static final int CLOSE_ANIMATION_DURATION = 5; // 0.25 seconds
    private static final double TELEPORT_RADIUS = 2.0D;
    private static final double FORCED_TELEPORT_RADIUS = 3.5D;

    private int ticksAlive = 0;
    private int openDuration = 60; // Default 3 seconds (60 ticks)
    private boolean soundPlayed = false;
    private boolean forceTeleportation = false;

    // Animation state tracking
    private enum DoorState { OPENING, OPEN, CLOSING, CLOSED }
    private DoorState currentState = DoorState.OPENING;

    public MugenDoorEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Can pass through blocks
        this.setInvulnerable(true);
        this.setNoAi(true); // Disable AI since it's just visual
        this.setSilent(false); // Allow sound playback
        this.setNoGravity(true); // Disable gravity
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
        this.entityData.define(DATA_STATE, 0); // 0=OPENING, 1=OPEN, 2=CLOSING, 3=CLOSED
    }

    /**
     * Create a mugen door entity at a specific position.
     *
     * @param level The world
     * @param pos The spawn position
     * @param openDuration How long to stay open (in ticks, default=20)
     * @param spawning If true, spawning demons (no teleportation)
     * @return The created door entity
     */
    public static MugenDoorEntity create(Level level, BlockPos pos, int openDuration, boolean spawning) {
        MugenDoorEntity door = new MugenDoorEntity(ModEntities.MUGEN_DOOR.get(), level);
        door.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        door.setYRot(0); // Always face north (can be customized if needed)
        door.setXRot(0);
        door.openDuration = openDuration;
        door.entityData.set(DATA_SPAWNING, spawning);
        return door;
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

    public boolean isSpawning() {
        return this.entityData.get(DATA_SPAWNING);
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

    /**
     * Teleport nearby entities to the Mugen Castle dimension.
     */
    private void teleportNearbyEntities() {
        double teleportRadius = this.forceTeleportation ? FORCED_TELEPORT_RADIUS : TELEPORT_RADIUS;
        java.util.List<net.minecraft.world.entity.player.Player> nearbyPlayers = level().getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            this.getBoundingBox().inflate(teleportRadius),
            player -> player != null && !player.isSpectator()
        );

        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            if (player instanceof ServerPlayer serverPlayer) {
                teleportToMugenCastle(serverPlayer);
            }
        }
    }

    /**
     * Teleport a player through the mugen door.
     * - If in Mugen Castle → teleport to overworld
     * - If in any other dimension → teleport to Mugen Castle
     */
    private void teleportToMugenCastle(ServerPlayer player) {
        try {
            // Check if door is in Mugen Castle dimension
            boolean isInMugenCastle = this.level().dimension().equals(MUGEN_CASTLE_DIMENSION);

            ServerLevel targetDimension;
            String message;

            if (isInMugenCastle) {
                // Door is in Mugen Castle → teleport to overworld
                targetDimension = player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
                message = "§aYou have returned to the overworld!";
            } else {
                // Door is in overworld or other dimension → teleport to Mugen Castle
                targetDimension = player.getServer().getLevel(MUGEN_CASTLE_DIMENSION);
                message = "§5You have been transported to the Mugen Castle!";
            }

            if (targetDimension == null) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Target dimension not found");
                return;
            }

            // Teleport player to spawn position in target dimension
            BlockPos spawnPos = targetDimension.getSharedSpawnPos();
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
                (isInMugenCastle ? " to overworld" : " to Mugen Castle"));
        } catch (Exception e) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Failed to teleport player: " + e.getMessage());
        }
    }

    @Override
    public boolean isPickable() {
        return false; // Cannot be clicked/targeted
    }

    @Override
    public boolean isPushable() {
        return false; // Cannot be pushed
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false; // Cannot be hurt
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false; // Cannot deal damage
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksAlive = tag.getInt("TicksAlive");
        this.openDuration = tag.getInt("OpenDuration");
        this.soundPlayed = tag.getBoolean("SoundPlayed");
        this.forceTeleportation = tag.getBoolean("ForceTeleportation");
        if (tag.contains("State")) {
            DoorState loadedState = DoorState.valueOf(tag.getString("State"));
            setCurrentState(loadedState);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putInt("OpenDuration", this.openDuration);
        tag.putBoolean("SoundPlayed", this.soundPlayed);
        tag.putBoolean("ForceTeleportation", this.forceTeleportation);
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
            // Play sound on first tick
            if (!soundPlayed) {
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

            ticksAlive++;

            // State machine for door animation and behavior
            switch (currentState) {
                case OPENING:
                    // Opening animation (6 ticks / 0.29 seconds)
                    if (ticksAlive >= OPEN_ANIMATION_DURATION) {
                        setCurrentState(DoorState.OPEN);
                        this.entityData.set(DATA_OPEN, true);
                        com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Mugen door opened");
                    }
                    break;

                case OPEN:
                    // While open, teleport entities if not spawning
                    if (!isSpawning()
                        && (this.forceTeleportation
                            || com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig.enableMugenDoorTeleportation.get())) {
                        teleportNearbyEntities();
                    }

                    // Check if it's time to close
                    if (ticksAlive >= OPEN_ANIMATION_DURATION + openDuration) {
                        setCurrentState(DoorState.CLOSING);
                        this.entityData.set(DATA_OPEN, false);
                        com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Mugen door closing");
                    }
                    break;

                case CLOSING:
                    // Closing animation (5 ticks / 0.25 seconds)
                    if (ticksAlive >= OPEN_ANIMATION_DURATION + openDuration + CLOSE_ANIMATION_DURATION) {
                        setCurrentState(DoorState.CLOSED);
                        com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Mugen door closed, removing");
                        this.discard();
                    }
                    break;

                case CLOSED:
                    // Should never reach here, but just in case
                    this.discard();
                    break;
            }
        }
    }
}
