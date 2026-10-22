package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class BlockDoorMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private final BlockPos cornerA;
    private final BlockPos cornerB;
    private final Direction facing;
    private final ContainerLevelAccess access;

    public BlockDoorMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos(),
            buffer.readEnum(Direction.class));
    }

    public BlockDoorMenu(int containerId, Inventory inventory, BlockPos blockPos, BlockPos cornerA,
                         BlockPos cornerB, Direction facing) {
        super(ModMenus.BLOCK_DOOR.get(), containerId);
        this.blockPos = blockPos;
        this.cornerA = cornerA;
        this.cornerB = cornerB;
        this.facing = facing == null ? Direction.NORTH : facing;
        this.access = ContainerLevelAccess.create(inventory.player.level(), blockPos);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockPos getCornerA() {
        return cornerA;
    }

    public BlockPos getCornerB() {
        return cornerB;
    }

    public Direction getFacing() {
        return facing;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
            level.getBlockState(pos).is(ModBlocks.BLOCK_DOOR.get())
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            false);
    }
}
