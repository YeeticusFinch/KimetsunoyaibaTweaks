package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackBlockItem;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackContents;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VialRackBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String RACK_COUNT_TAG = "RackCount";

    private final NonNullList<ItemStack> items = NonNullList.withSize(VialRackContents.MAX_SLOT_COUNT, ItemStack.EMPTY);
    private int rackCount = 1;

    public VialRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIAL_RACK.get(), pos, state);
    }

    public void loadFromItem(ItemStack stack) {
        rackCount = 1;
        clearItemsWithoutSync();
        ItemStack[] storedItems = VialRackBlockItem.getStoredItems(stack);
        for (int slot = 0; slot < VialRackContents.SLOT_COUNT; slot++) {
            items.set(slot, VialRackContents.asSingleVial(storedItems[slot]));
        }
        sync();
    }

    public ItemStack createItemStackWithContents() {
        return createItemStackWithContents(0);
    }

    public List<ItemStack> createItemStacksWithContents() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int rack = 0; rack < rackCount; rack++) {
            stacks.add(createItemStackWithContents(rack));
        }
        return stacks;
    }

    private ItemStack createItemStackWithContents(int rack) {
        ItemStack stack = new ItemStack(getBlockState().getBlock());
        ItemStack[] storedItems = new ItemStack[VialRackContents.SLOT_COUNT];
        int start = rack * VialRackContents.SLOT_COUNT;
        for (int slot = 0; slot < storedItems.length; slot++) {
            storedItems[slot] = getItem(start + slot);
        }
        VialRackBlockItem.setStoredItems(stack, storedItems);
        return stack;
    }

    public boolean addRack(ItemStack stack) {
        if (rackCount >= VialRackContents.MAX_RACKS) {
            return false;
        }

        ItemStack[] storedItems = VialRackBlockItem.getStoredItems(stack);
        int start = rackCount * VialRackContents.SLOT_COUNT;
        rackCount++;
        for (int slot = 0; slot < VialRackContents.SLOT_COUNT; slot++) {
            items.set(start + slot, VialRackContents.asSingleVial(storedItems[slot]));
        }
        sync();
        return true;
    }

    public int getRackCount() {
        return rackCount;
    }

    public ItemStack getRenderItem(int slot) {
        return getItem(slot);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.kimetsunoyaibamultiplayer.vial_rack");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VialRackMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return rackCount * VialRackContents.SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < getContainerSize() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= getContainerSize()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            sync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= getContainerSize()) {
            return ItemStack.EMPTY;
        }

        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) {
            return;
        }

        items.set(slot, VialRackContents.asSingleVial(stack));
        sync();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < getContainerSize() && VialRackContents.isVial(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        clearItemsWithoutSync();
        sync();
    }

    private void clearItemsWithoutSync() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(RACK_COUNT_TAG, rackCount);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, VialRackContents.asSingleVial(items.get(slot)));
        }
        rackCount = tag.contains(RACK_COUNT_TAG)
            ? clampRackCount(tag.getInt(RACK_COUNT_TAG))
            : 1;
        rackCount = Math.max(rackCount, occupiedRackCount());
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

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private int occupiedRackCount() {
        int occupied = 1;
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) {
                occupied = Math.max(occupied, slot / VialRackContents.SLOT_COUNT + 1);
            }
        }
        return clampRackCount(occupied);
    }

    private static int clampRackCount(int count) {
        return Math.max(1, Math.min(VialRackContents.MAX_RACKS, count));
    }
}
