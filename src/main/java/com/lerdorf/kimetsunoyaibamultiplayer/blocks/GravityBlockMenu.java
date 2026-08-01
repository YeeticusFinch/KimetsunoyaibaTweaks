package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class GravityBlockMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private final BlockPos startOffset;
    private final BlockPos size;
    private final Direction gravityDirection;
    private final ContainerLevelAccess access;

    public GravityBlockMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readEnum(Direction.class));
    }

    public GravityBlockMenu(int containerId, Inventory inventory, BlockPos blockPos, BlockPos startOffset, BlockPos size, Direction gravityDirection) {
        super(ModMenus.GRAVITY_BLOCK.get(), containerId);
        this.blockPos = blockPos;
        this.startOffset = startOffset;
        this.size = size;
        this.gravityDirection = gravityDirection == null ? Direction.DOWN : gravityDirection;
        this.access = ContainerLevelAccess.create(inventory.player.level(), blockPos);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockPos getStartOffset() {
        return startOffset;
    }

    public BlockPos getSize() {
        return size;
    }

    public Direction getGravityDirection() {
        return gravityDirection;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
            level.getBlockState(pos).is(ModBlocks.GRAVITY_BLOCK.get())
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            false);
    }
}
