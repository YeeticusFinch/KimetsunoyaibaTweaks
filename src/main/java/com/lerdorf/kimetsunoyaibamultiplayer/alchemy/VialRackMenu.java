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
    private static final int PLAYER_INV_START = VialRackContents.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerLevelAccess access;

    public VialRackMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public VialRackMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        this(containerId, inventory, resolveContainer(inventory, blockPos),
            ContainerLevelAccess.create(inventory.player.level(), blockPos));
    }

    public VialRackMenu(int containerId, Inventory inventory, Container container) {
        this(containerId, inventory, container, ContainerLevelAccess.NULL);
    }

    public VialRackMenu(int containerId, Inventory inventory, Container container, ContainerLevelAccess access) {
        super(ModAlchemyMenus.VIAL_RACK.get(), containerId);
        checkContainerSize(container, VialRackContents.SLOT_COUNT);
        this.container = container;
        this.access = access;

        for (int slot = 0; slot < VialRackContents.SLOT_COUNT; slot++) {
            addSlot(new VialSlot(container, slot, 44 + slot * 18, 20));
        }
        addPlayerInventory(inventory);
    }

    private static Container resolveContainer(Inventory inventory, BlockPos blockPos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(blockPos);
        return blockEntity instanceof VialRackBlockEntity rack ? rack : new SimpleContainer(VialRackContents.SLOT_COUNT);
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
        if (index < VialRackContents.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (VialRackContents.isVial(stack)) {
            if (!moveItemStackTo(stack, 0, VialRackContents.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
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
}
