package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.BlockDoorBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.BlockDoorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlockDoorBlockEntity extends BlockEntity implements MenuProvider {
    private static final int MAX_OFFSET = 64;
    private static final int MAX_SLICE_COUNT = 128;

    private BlockPos cornerA = new BlockPos(-3, 0, -3);
    private BlockPos cornerB = new BlockPos(3, 5, 3);
    private final List<SavedBlock> savedBlocks = new ArrayList<>();
    private List<CompoundTag> pendingSavedBlocks;
    private boolean[] removedSlices = new boolean[0];
    private DoorPhase phase = DoorPhase.CLOSED;

    public BlockDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOCK_DOOR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            ensureSavedBlocksLoaded();
        }
    }

    public Direction getFacing() {
        return getBlockState().hasProperty(BlockDoorBlock.FACING)
            ? getBlockState().getValue(BlockDoorBlock.FACING) : Direction.NORTH;
    }

    public BlockPos getCornerA() {
        return cornerA;
    }

    public BlockPos getCornerB() {
        return cornerB;
    }

    public AABB getRegionBox() {
        BlockPos worldA = worldPosition.offset(localToWorld(cornerA, getFacing()));
        BlockPos worldB = worldPosition.offset(localToWorld(cornerB, getFacing()));
        return new AABB(
            Math.min(worldA.getX(), worldB.getX()),
            Math.min(worldA.getY(), worldB.getY()),
            Math.min(worldA.getZ(), worldB.getZ()),
            Math.max(worldA.getX(), worldB.getX()) + 1.0D,
            Math.max(worldA.getY(), worldB.getY()) + 1.0D,
            Math.max(worldA.getZ(), worldB.getZ()) + 1.0D);
    }

    public static boolean isHoldingBlockDoor(Player player) {
        return player.getMainHandItem().is(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.BLOCK_DOOR.get().asItem())
            || player.getOffhandItem().is(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.BLOCK_DOOR.get().asItem());
    }

    public void applySettings(BlockPos firstCorner, BlockPos secondCorner, Direction facing) {
        if (!savedBlocks.isEmpty() || pendingSavedBlocks != null) {
            return;
        }
        BlockPos[] corners = normalizeCorners(firstCorner, secondCorner);
        cornerA = corners[0];
        cornerB = corners[1];
        Direction sanitizedFacing = facing == null ? Direction.NORTH : facing;
        if (level != null && !level.isClientSide && getFacing() != sanitizedFacing) {
            level.setBlock(worldPosition, getBlockState().setValue(BlockDoorBlock.FACING, sanitizedFacing), 3);
        }
        syncClient();
    }

    public void writeMenuData(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeBlockPos(cornerA);
        buffer.writeBlockPos(cornerB);
        buffer.writeEnum(getFacing());
    }

    public void tickServer(ServerLevel level) {
        ensureSavedBlocksLoaded();
        boolean powered = level.hasNeighborSignal(worldPosition);

        if (phase == DoorPhase.CLOSED && savedBlocks.isEmpty()) {
            if (!powered) {
                return;
            }
            captureRegion(level);
            phase = DoorPhase.OPENING;
        } else if (!powered && phase != DoorPhase.CLOSED) {
            phase = DoorPhase.CLOSING;
        } else if (powered && phase == DoorPhase.CLOSING) {
            phase = DoorPhase.OPENING;
        }

        if (powered && phase == DoorPhase.OPENING) {
            removeNextSlice(level);
        } else if (!powered && phase == DoorPhase.CLOSING) {
            restoreNextSlice(level);
        }
    }

    private void captureRegion(ServerLevel level) {
        savedBlocks.clear();
        int minX = cornerA.getX();
        int maxX = cornerB.getX();
        int sliceCount = maxX - minX + 1;
        removedSlices = new boolean[sliceCount];

        for (int x = minX; x <= maxX; x++) {
            for (int y = cornerA.getY(); y <= cornerB.getY(); y++) {
                for (int z = cornerA.getZ(); z <= cornerB.getZ(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    BlockPos target = worldPosition.offset(localToWorld(local, getFacing()));
                    if (target.equals(worldPosition)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(target);
                    CompoundTag blockEntityTag = null;
                    BlockEntity blockEntity = level.getBlockEntity(target);
                    if (blockEntity != null) {
                        blockEntityTag = blockEntity.saveWithoutMetadata();
                    }
                    savedBlocks.add(new SavedBlock(local, x - minX, state, blockEntityTag));
                }
            }
        }
        setChanged();
    }

    private void removeNextSlice(ServerLevel level) {
        int slice = firstSlice(false);
        if (slice < 0) {
            phase = DoorPhase.OPEN;
            return;
        }
        for (SavedBlock block : savedBlocks) {
            if (block.slice == slice) {
                BlockPos target = worldPosition.offset(localToWorld(block.localPos, getFacing()));
                level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        removedSlices[slice] = true;
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 1.0F);
        setChanged();
        if (firstSlice(false) < 0) {
            phase = DoorPhase.OPEN;
        }
    }

    private void restoreNextSlice(ServerLevel level) {
        int slice = firstSlice(true);
        if (slice < 0) {
            phase = DoorPhase.CLOSED;
            savedBlocks.clear();
            removedSlices = new boolean[0];
            setChanged();
            return;
        }
        for (SavedBlock block : savedBlocks) {
            if (block.slice != slice) {
                continue;
            }
            BlockPos target = worldPosition.offset(localToWorld(block.localPos, getFacing()));
            level.setBlock(target, block.state, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            if (block.blockEntityTag != null) {
                BlockEntity blockEntity = level.getBlockEntity(target);
                if (blockEntity != null) {
                    blockEntity.load(block.blockEntityTag.copy());
                    blockEntity.setChanged();
                }
            }
        }
        removedSlices[slice] = false;
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 1.0F);
        setChanged();
        if (firstSlice(true) < 0) {
            phase = DoorPhase.CLOSED;
            savedBlocks.clear();
            removedSlices = new boolean[0];
            setChanged();
        }
    }

    private int firstSlice(boolean removed) {
        if (removed) {
            for (int i = removedSlices.length - 1; i >= 0; i--) {
                if (removedSlices[i]) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < removedSlices.length; i++) {
                if (!removedSlices[i]) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void ensureSavedBlocksLoaded() {
        if (pendingSavedBlocks == null || level == null) {
            return;
        }
        savedBlocks.clear();
        var blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (CompoundTag tag : pendingSavedBlocks) {
            try {
                BlockState state = NbtUtils.readBlockState(blockLookup, tag.getCompound("State"));
                CompoundTag blockEntityTag = tag.contains("BlockEntity", Tag.TAG_COMPOUND)
                    ? tag.getCompound("BlockEntity") : null;
                savedBlocks.add(new SavedBlock(
                    new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                    tag.getInt("Slice"), state, blockEntityTag));
            } catch (RuntimeException ignored) {
                // Ignore invalid saved entries rather than preventing the chunk from loading.
            }
        }
        pendingSavedBlocks = null;
    }

    private static BlockPos[] normalizeCorners(BlockPos first, BlockPos second) {
        BlockPos a = clampCorner(first);
        BlockPos b = clampCorner(second);
        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.min(Math.max(a.getX(), b.getX()), minX + MAX_SLICE_COUNT - 1);
        return new BlockPos[] {
            new BlockPos(minX, Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
            new BlockPos(maxX, Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()))
        };
    }

    private static BlockPos clampCorner(BlockPos corner) {
        return new BlockPos(
            Mth.clamp(corner.getX(), -MAX_OFFSET, MAX_OFFSET),
            Mth.clamp(corner.getY(), -MAX_OFFSET, MAX_OFFSET),
            Mth.clamp(corner.getZ(), -MAX_OFFSET, MAX_OFFSET));
    }

    /** Maps the menu's local +X axis to the block's facing direction. */
    public static BlockPos localToWorld(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(local.getX(), local.getY(), local.getZ());
            case WEST -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case SOUTH -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case NORTH -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case UP -> new BlockPos(local.getZ(), local.getX(), local.getY());
            case DOWN -> new BlockPos(-local.getZ(), -local.getX(), local.getY());
        };
    }

    private void syncClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.kimetsunoyaibamultiplayer.block_door");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BlockDoorMenu(containerId, inventory, worldPosition, cornerA, cornerB, getFacing());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("CornerAX", cornerA.getX());
        tag.putInt("CornerAY", cornerA.getY());
        tag.putInt("CornerAZ", cornerA.getZ());
        tag.putInt("CornerBX", cornerB.getX());
        tag.putInt("CornerBY", cornerB.getY());
        tag.putInt("CornerBZ", cornerB.getZ());
        tag.putString("Phase", phase.name());
        for (int i = 0; i < removedSlices.length; i++) {
            if (removedSlices[i]) {
                tag.putBoolean("RemovedSlice" + i, true);
            }
        }

        ListTag blocks = new ListTag();
        for (SavedBlock block : savedBlocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("X", block.localPos.getX());
            blockTag.putInt("Y", block.localPos.getY());
            blockTag.putInt("Z", block.localPos.getZ());
            blockTag.putInt("Slice", block.slice);
            blockTag.put("State", NbtUtils.writeBlockState(block.state));
            if (block.blockEntityTag != null) {
                blockTag.put("BlockEntity", block.blockEntityTag.copy());
            }
            blocks.add(blockTag);
        }
        tag.put("SavedBlocks", blocks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cornerA = new BlockPos(
            tag.contains("CornerAX") ? tag.getInt("CornerAX") : -3,
            tag.contains("CornerAY") ? tag.getInt("CornerAY") : 0,
            tag.contains("CornerAZ") ? tag.getInt("CornerAZ") : -3);
        cornerB = new BlockPos(
            tag.contains("CornerBX") ? tag.getInt("CornerBX") : 3,
            tag.contains("CornerBY") ? tag.getInt("CornerBY") : 5,
            tag.contains("CornerBZ") ? tag.getInt("CornerBZ") : 3);
        BlockPos[] normalized = normalizeCorners(cornerA, cornerB);
        cornerA = normalized[0];
        cornerB = normalized[1];
        try {
            phase = DoorPhase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException ignored) {
            phase = DoorPhase.CLOSED;
        }

        int sliceCount = cornerB.getX() - cornerA.getX() + 1;
        removedSlices = new boolean[sliceCount];
        for (int i = 0; i < sliceCount; i++) {
            removedSlices[i] = tag.getBoolean("RemovedSlice" + i);
        }
        pendingSavedBlocks = new ArrayList<>();
        if (tag.contains("SavedBlocks", Tag.TAG_LIST)) {
            ListTag blocks = tag.getList("SavedBlocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocks.size(); i++) {
                pendingSavedBlocks.add(blocks.getCompound(i).copy());
            }
        }
        if (level != null) {
            ensureSavedBlocksLoaded();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.remove("SavedBlocks");
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private enum DoorPhase {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    private static final class SavedBlock {
        private final BlockPos localPos;
        private final int slice;
        private final BlockState state;
        @Nullable
        private final CompoundTag blockEntityTag;

        private SavedBlock(BlockPos localPos, int slice, BlockState state, @Nullable CompoundTag blockEntityTag) {
            this.localPos = localPos;
            this.slice = slice;
            this.state = state;
            this.blockEntityTag = blockEntityTag;
        }
    }
}
