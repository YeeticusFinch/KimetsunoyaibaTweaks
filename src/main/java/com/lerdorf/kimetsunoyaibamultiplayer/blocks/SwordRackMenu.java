package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.SwordRackBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
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

public class SwordRackMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;
    private final int playerInvStart;
    private final int playerInvEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    public SwordRackMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public SwordRackMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        this(containerId, inventory, resolveContainer(inventory, blockPos), ContainerLevelAccess.create(inventory.player.level(), blockPos));
    }

    public SwordRackMenu(int containerId, Inventory inventory, Container container) {
        this(containerId, inventory, container, ContainerLevelAccess.NULL);
    }

    private SwordRackMenu(int containerId, Inventory inventory, Container container, ContainerLevelAccess access) {
        super(ModMenus.SWORD_RACK.get(), containerId);
        checkContainerSize(container, 3);
        this.container = container;
        this.access = access;

        for (int slot = 0; slot < 3; slot++) {
            addSlot(new SwordSlot(container, slot, 62 + slot * 18, 20));
        }

        this.playerInvStart = 3;
        this.playerInvEnd = playerInvStart + 27;
        this.hotbarStart = playerInvEnd;
        this.hotbarEnd = hotbarStart + 9;

        addPlayerInventory(inventory);
    }

    private static Container resolveContainer(Inventory inventory, BlockPos blockPos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(blockPos);
        return blockEntity instanceof SwordRackBlockEntity rack ? rack : new SimpleContainer(3);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 109));
        }
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

        if (index < 3) {
            if (!moveItemStackTo(stack, playerInvStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (BreathingInfoDetector.isNichirinSword(stack)) {
            if (!moveItemStackTo(stack, 0, 3, false)) {
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
        if (!player.level().isClientSide && !(container instanceof SwordRackBlockEntity)) {
            clearContainer(player, container);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModBlocks.SWORD_RACK.get())
                    && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            container.stillValid(player));
    }

    private static class SwordSlot extends Slot {
        SwordSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BreathingInfoDetector.isNichirinSword(stack);
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
}
