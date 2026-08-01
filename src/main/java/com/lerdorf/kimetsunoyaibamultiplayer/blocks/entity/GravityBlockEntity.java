package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.GravityBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.GravityBlockMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.field.GravityFieldManager;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class GravityBlockEntity extends BlockEntity implements MenuProvider {
    private BlockPos startOffset = new BlockPos(-3, 0, -3);
    private BlockPos size = new BlockPos(7, 6, 7);
    private boolean enabled = true;

    public GravityBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITY_BLOCK.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            GravityFieldManager.registerBlock(this);
        }
        syncClient();
    }

    @Override
    public void onChunkUnloaded() {
        clearFieldEffects();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        clearFieldEffects();
        super.setRemoved();
    }

    public void updateField() {
        if (level instanceof ServerLevel) {
            GravityFieldManager.registerBlock(this);
        }
        syncClient();
    }

    public void applySettings(BlockPos startOffset, BlockPos size, Direction gravityDirection) {
        this.startOffset = clampOffset(startOffset);
        this.size = clampSize(size);
        setFacing(gravityDirection == null ? Direction.DOWN : gravityDirection);
        syncClient();
    }

    public void tickServer(ServerLevel level) {
        GravityFieldManager.registerBlock(this);
    }

    public void clearFieldEffects() {
        GravityFieldManager.unregisterBlock(this);
    }

    public boolean isGravityFieldActive() {
        return level instanceof ServerLevel && !isRemoved() && enabled && KNYGravity.isEnabled();
    }

    public AABB getFieldBox() {
        BlockPos endOffset = startOffset.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        BlockPos worldA = worldPosition.offset(rotateLocalOffset(startOffset, getFacing()));
        BlockPos worldB = worldPosition.offset(rotateLocalOffset(endOffset, getFacing()));
        return new AABB(
            Math.min(worldA.getX(), worldB.getX()),
            Math.min(worldA.getY(), worldB.getY()),
            Math.min(worldA.getZ(), worldB.getZ()),
            Math.max(worldA.getX(), worldB.getX()) + 1.0D,
            Math.max(worldA.getY(), worldB.getY()) + 1.0D,
            Math.max(worldA.getZ(), worldB.getZ()) + 1.0D
        );
    }

    public static BlockPos rotateLocalOffset(BlockPos offset, Direction facing) {
        Direction gravityDirection = getGravityDirection(facing);
        Vec3 rotated = RotationUtil.vecPlayerToWorld((double) offset.getX(), (double) offset.getY(), (double) offset.getZ(), gravityDirection);
        return new BlockPos((int) Math.round(rotated.x), (int) Math.round(rotated.y), (int) Math.round(rotated.z));
    }

    public static Direction getGravityDirection(Direction facing) {
        return facing;
    }

    public Direction getWorldGravityDirection() {
        return getFacing();
    }

    public Direction getFacing() {
        return getBlockState().hasProperty(GravityBlock.FACING) ? getBlockState().getValue(GravityBlock.FACING) : Direction.UP;
    }

    public void setFacing(Direction facing) {
        if (level == null || level.isClientSide || !getBlockState().hasProperty(GravityBlock.FACING)) {
            return;
        }
        Direction sanitized = facing == null ? Direction.DOWN : facing;
        if (getBlockState().getValue(GravityBlock.FACING) != sanitized) {
            level.setBlock(worldPosition, getBlockState().setValue(GravityBlock.FACING, sanitized), 3);
        }
    }

    private static BlockPos clampOffset(BlockPos offset) {
        int max = 64;
        return new BlockPos(
            Mth.clamp(offset.getX(), -max, max),
            Mth.clamp(offset.getY(), -max, max),
            Mth.clamp(offset.getZ(), -max, max)
        );
    }

    private static BlockPos clampSize(BlockPos size) {
        int max = 128;
        return new BlockPos(
            Mth.clamp(Math.abs(size.getX()), 1, max),
            Mth.clamp(Math.abs(size.getY()), 1, max),
            Mth.clamp(Math.abs(size.getZ()), 1, max)
        );
    }

    public BlockPos getStartOffset() {
        return startOffset;
    }

    public BlockPos getSize() {
        return size;
    }

    public Direction getConfiguredGravityDirection() {
        return getFacing();
    }

    public void writeMenuData(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeBlockPos(startOffset);
        buffer.writeBlockPos(size);
        buffer.writeEnum(getFacing());
    }

    public void syncClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.kimetsunoyaibamultiplayer.gravity_block");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GravityBlockMenu(containerId, inventory, worldPosition, startOffset, size, getFacing());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StartX", startOffset.getX());
        tag.putInt("StartY", startOffset.getY());
        tag.putInt("StartZ", startOffset.getZ());
        tag.putInt("SizeX", size.getX());
        tag.putInt("SizeY", size.getY());
        tag.putInt("SizeZ", size.getZ());
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("StartX")) {
            startOffset = clampOffset(new BlockPos(
                tag.getInt("StartX"),
                tag.getInt("StartY"),
                tag.getInt("StartZ")
            ));
            size = clampSize(new BlockPos(
                tag.contains("SizeX") ? tag.getInt("SizeX") : 7,
                tag.contains("SizeY") ? tag.getInt("SizeY") : 6,
                tag.contains("SizeZ") ? tag.getInt("SizeZ") : 7
            ));
        } else {
            BlockPos cornerA = clampOffset(new BlockPos(
                tag.contains("CornerAX") ? tag.getInt("CornerAX") : -3,
                tag.contains("CornerAY") ? tag.getInt("CornerAY") : 0,
                tag.contains("CornerAZ") ? tag.getInt("CornerAZ") : -3
            ));
            BlockPos cornerB = clampOffset(new BlockPos(
                tag.contains("CornerBX") ? tag.getInt("CornerBX") : 3,
                tag.contains("CornerBY") ? tag.getInt("CornerBY") : 5,
                tag.contains("CornerBZ") ? tag.getInt("CornerBZ") : 3
            ));
            startOffset = new BlockPos(
                Math.min(cornerA.getX(), cornerB.getX()),
                Math.min(cornerA.getY(), cornerB.getY()),
                Math.min(cornerA.getZ(), cornerB.getZ())
            );
            size = clampSize(new BlockPos(
                Math.abs(cornerA.getX() - cornerB.getX()) + 1,
                Math.abs(cornerA.getY() - cornerB.getY()) + 1,
                Math.abs(cornerA.getZ() - cornerB.getZ()) + 1
            ));
        }
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        if (level != null && !level.isClientSide) {
            updateField();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static boolean isHoldingGravityBlock(Player player) {
        return isGravityBlockStack(player.getMainHandItem()) || isGravityBlockStack(player.getOffhandItem());
    }

    private static boolean isGravityBlockStack(ItemStack stack) {
        return stack.is(ModBlocks.GRAVITY_BLOCK.get().asItem());
    }
}
