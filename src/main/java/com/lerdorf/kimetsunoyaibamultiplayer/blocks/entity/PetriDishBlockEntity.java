package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PetriDishBlockEntity extends BlockEntity {
    public static final int MAX_DISHES = 3;

    private final NonNullList<ItemStack> dishes = NonNullList.withSize(MAX_DISHES, ItemStack.EMPTY);

    public PetriDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PETRI_DISH.get(), pos, state);
    }

    public void loadFromItem(ItemStack stack) {
        clearWithoutSync();
        dishes.set(0, asSingleDish(stack));
        sync();
    }

    public boolean addDish(ItemStack stack) {
        if (!BloodDemonArtAlchemyCatalog.isPetriDishDisplayItem(stack)) {
            return false;
        }

        for (int slot = 0; slot < dishes.size(); slot++) {
            if (dishes.get(slot).isEmpty()) {
                dishes.set(slot, asSingleDish(stack));
                compactDishes();
                sync();
                return true;
            }
        }
        return false;
    }

    public int getDishCount() {
        int count = 0;
        for (ItemStack dish : dishes) {
            if (!dish.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public ItemStack getRenderItem(int slot) {
        return slot >= 0 && slot < dishes.size() ? dishes.get(slot) : ItemStack.EMPTY;
    }

    public List<ItemStack> createDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack dish : dishes) {
            if (!dish.isEmpty()) {
                drops.add(dish.copy());
            }
        }
        return drops;
    }

    private void clearWithoutSync() {
        for (int slot = 0; slot < dishes.size(); slot++) {
            dishes.set(slot, ItemStack.EMPTY);
        }
    }

    private void compactDishes() {
        List<ItemStack> occupied = createDrops();
        clearWithoutSync();
        for (int slot = 0; slot < occupied.size() && slot < dishes.size(); slot++) {
            dishes.set(slot, asSingleDish(occupied.get(slot)));
        }
    }

    private static ItemStack asSingleDish(ItemStack stack) {
        if (!BloodDemonArtAlchemyCatalog.isPetriDishDisplayItem(stack)) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, dishes);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, dishes);
        for (int slot = 0; slot < dishes.size(); slot++) {
            dishes.set(slot, asSingleDish(dishes.get(slot)));
        }
        compactDishes();
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
}
