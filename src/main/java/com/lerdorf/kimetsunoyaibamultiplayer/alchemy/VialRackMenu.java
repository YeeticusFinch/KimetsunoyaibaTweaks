package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.VialRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class VialRackMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;
    private final int rackCount;
    private final int rackSlots;
    private final int playerInvStart;
    private final int playerInvEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    public VialRackMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), extraData.readVarInt());
    }

    public VialRackMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        this(containerId, inventory, blockPos, rackCountFor(inventory, blockPos));
    }

    public VialRackMenu(int containerId, Inventory inventory, BlockPos blockPos, int rackCount) {
        this(containerId, inventory, resolveContainer(inventory, blockPos, rackCount),
            ContainerLevelAccess.create(inventory.player.level(), blockPos));
    }

    public VialRackMenu(int containerId, Inventory inventory, Container container) {
        this(containerId, inventory, container, ContainerLevelAccess.NULL);
    }

    public VialRackMenu(int containerId, Inventory inventory, Container container, ContainerLevelAccess access) {
        super(ModAlchemyMenus.VIAL_RACK.get(), containerId);
        checkContainerSize(container, Math.min(container.getContainerSize(), VialRackContents.MAX_SLOT_COUNT));
        this.container = container;
        this.access = access;
        this.rackCount = clampRackCount((container.getContainerSize() + VialRackContents.SLOT_COUNT - 1) / VialRackContents.SLOT_COUNT);
        this.rackSlots = this.rackCount * VialRackContents.SLOT_COUNT;
        this.playerInvStart = rackSlots;
        this.playerInvEnd = playerInvStart + 27;
        this.hotbarStart = playerInvEnd;
        this.hotbarEnd = hotbarStart + 9;

        for (int rack = 0; rack < this.rackCount; rack++) {
            for (int slot = 0; slot < VialRackContents.SLOT_COUNT; slot++) {
                addSlot(new VialSlot(container, rack * VialRackContents.SLOT_COUNT + slot, 44 + slot * 18, 20 + rack * 18));
            }
        }
        addPlayerInventory(inventory);
    }

    private static Container resolveContainer(Inventory inventory, BlockPos blockPos, int rackCount) {
        int clampedRackCount = clampRackCount(rackCount);
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(blockPos);
        return blockEntity instanceof VialRackBlockEntity rack && rack.getRackCount() == clampedRackCount
            ? rack
            : new SimpleContainer(clampedRackCount * VialRackContents.SLOT_COUNT);
    }

    private static int rackCountFor(Inventory inventory, BlockPos blockPos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(blockPos);
        return blockEntity instanceof VialRackBlockEntity rack ? rack.getRackCount() : 1;
    }

    private void addPlayerInventory(Inventory inventory) {
        int offset = (rackCount - 1) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 51 + offset + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 109 + offset));
        }
    }

    public int getRackCount() {
        return rackCount;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        copy = stack.copy();
        if (index < rackSlots) {
            if (!moveItemStackTo(stack, playerInvStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (VialRackContents.isVial(stack)) {
            if (!moveItemStackTo(stack, 0, rackSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerInvStart && index < playerInvEnd) {
            if (!moveItemStackTo(stack, hotbarStart, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= hotbarStart && index < hotbarEnd) {
            if (!moveItemStackTo(stack, playerInvStart, playerInvEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && !(container instanceof VialRackBlockEntity)) {
            clearContainer(player, container);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModAlchemyBlocks.VIAL_RACK.get())
                    && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            container.stillValid(player));
    }

    private static class VialSlot extends Slot {
        VialSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return VialRackContents.isVial(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    private static int clampRackCount(int count) {
        return Math.max(1, Math.min(VialRackContents.MAX_RACKS, count));
    }
}
