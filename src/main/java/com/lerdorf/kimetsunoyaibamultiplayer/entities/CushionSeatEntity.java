package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.events.CushionSeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class CushionSeatEntity extends Mob {
    private static final double SEAT_Y_OFFSET = -0.65D;

    private static final EntityDataAccessor<BlockPos> SEAT_POS =
        SynchedEntityData.defineId(CushionSeatEntity.class, EntityDataSerializers.BLOCK_POS);

    public CushionSeatEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvisible(true);
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public static CushionSeatEntity create(Level level, BlockPos pos) {
        CushionSeatEntity seat = new CushionSeatEntity(ModEntities.CUSHION_SEAT.get(), level);
        seat.entityData.set(SEAT_POS, pos.immutable());
        seat.refreshSeatPosition();
        return seat;
    }

    public BlockPos getSeatPos() {
        return this.entityData.get(SEAT_POS);
    }

    public boolean matchesSeat(BlockPos pos) {
        return getSeatPos().equals(pos);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SEAT_POS, BlockPos.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        refreshSeatPosition();

        if (level().isClientSide()) {
            return;
        }

        if (!isValidSeatBlock(level().getBlockState(getSeatPos())) || getPassengers().isEmpty()) {
            discard();
        }
    }

    private void refreshSeatPosition() {
        BlockPos pos = getSeatPos();
        setPos(pos.getX() + 0.5D, pos.getY() + SEAT_Y_OFFSET, pos.getZ() + 0.5D);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }

        Vec3 seatPos = position().add(0.0D, 0.05D, 0.0D);
        moveFunction.accept(passenger, seatPos.x, seatPos.y, seatPos.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        return Vec3.atBottomCenterOf(getSeatPos()).add(0.0D, 0.1D, 0.0D);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(0.01F, 0.01F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        BlockPos pos = getSeatPos();
        tag.putInt("SeatX", pos.getX());
        tag.putInt("SeatY", pos.getY());
        tag.putInt("SeatZ", pos.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(SEAT_POS, new BlockPos(tag.getInt("SeatX"), tag.getInt("SeatY"), tag.getInt("SeatZ")));
        refreshSeatPosition();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public static boolean isValidSeatBlock(BlockState state) {
        return CushionSeatHandler.isCushionBlock(state);
    }
}
