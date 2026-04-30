package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyTableMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyTableBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.NonNullList;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;

public class AlchemyTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int TOP_SLOT = 0;
    public static final int BOTTOM_SLOT = 1;
    public static final int FUEL_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private static final int COOK_TIME_TOTAL = 50;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int cookTime;
    private int litTime;
    private int litDuration;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> COOK_TIME_TOTAL;
                case 2 -> litTime;
                case 3 -> litDuration;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                cookTime = value;
            } else if (index == 2) {
                litTime = value;
            } else if (index == 3) {
                litDuration = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public AlchemyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMY_TABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyTableBlockEntity table) {
        table.tickCooking();
    }

    private void tickCooking() {
        boolean wasCooking = isCooking();
        boolean wasLit = isLit();
        boolean changed = false;

        if (isLit()) {
            litTime--;
            changed = true;
        }

        if (!isLit() && canCook()) {
            litTime = getFuelBurnTime(items.get(FUEL_SLOT));
            litDuration = litTime;
            if (isLit()) {
                consumeFuel();
                changed = true;
            }
        }

        if (isLit() && canCook()) {
            cookTime++;
            changed = true;
        } else if (cookTime != 0) {
            cookTime = 0;
            changed = true;
        }

        if (cookTime >= COOK_TIME_TOTAL) {
            finishCooking();
            changed = true;
        }

        if (changed) {
            boolean litChanged = wasLit != isLit();
            if (litChanged) {
                updateLitState();
            }
            setChanged();
            if (wasCooking != isCooking() || litChanged) {
                sync();
            }
        } else {
            updateLitState();
        }
    }

    public boolean isLit() {
        return litTime > 0;
    }

    private void cancelCooking() {
        if (cookTime != 0) {
            cookTime = 0;
            sync();
        }
    }

    private void finishCooking() {
        ItemStack output = getRecipeOutput();
        if (output.isEmpty()) {
            cancelCooking();
            return;
        }

        shrinkInput(TOP_SLOT);
        items.set(BOTTOM_SLOT, output.copy());
        cookTime = 0;
    }

    private void consumeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return;
        }

        if (fuel.hasCraftingRemainingItem()) {
            items.set(FUEL_SLOT, fuel.getCraftingRemainingItem());
            return;
        }

        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(FUEL_SLOT, ItemStack.EMPTY);
        }
    }

    private void shrinkInput(int slot) {
        ItemStack stack = items.get(slot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
    }

    private boolean canCook() {
        return !getRecipeOutput().isEmpty() && items.get(BOTTOM_SLOT).getCount() == 1;
    }

    public static int getFuelBurnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    public static boolean isFuel(ItemStack stack) {
        return getFuelBurnTime(stack) > 0;
    }

    public ItemStack getRecipeOutput() {
        ItemStack top = items.get(TOP_SLOT);
        ItemStack bottom = items.get(BOTTOM_SLOT);
        if (level == null || top.isEmpty() || bottom.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SimpleContainer input = new SimpleContainer(2);
        input.setItem(0, top);
        input.setItem(1, bottom);

        return level.getRecipeManager()
            .getRecipeFor(ModAlchemyRecipes.ALCHEMY_TABLE_TYPE, input, level)
            .map(recipe -> recipe.assemble(input, level.registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    public boolean isCooking() {
        return cookTime > 0;
    }

    public int getCookTime() {
        return cookTime;
    }

    public int getCookTimeTotal() {
        return COOK_TIME_TOTAL;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public ItemStack getBottomDisplayStack() {
        ItemStack stack = items.get(BOTTOM_SLOT);
        return BloodDemonArtAlchemyCatalog.isAlchemyTableDisplayContainer(stack) ? stack : ItemStack.EMPTY;
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropContents(level, worldPosition, this);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.kimetsunoyaibamultiplayer.alchemy_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AlchemyTableMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
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
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            onSlotChanged(slot);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            onSlotChanged(slot);
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return;
        }

        items.set(slot, stack);
        int maxStackSize = slot == BOTTOM_SLOT ? 1 : getMaxStackSize();
        if (!stack.isEmpty() && stack.getCount() > maxStackSize) {
            stack.setCount(maxStackSize);
        }
        onSlotChanged(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BOTTOM_SLOT) {
            return BloodDemonArtAlchemyCatalog.isAlchemyTableDisplayContainer(stack);
        }
        if (slot == FUEL_SLOT) {
            return isFuel(stack);
        }
        return true;
    }

    private void onSlotChanged(int slot) {
        if ((slot == TOP_SLOT || slot == BOTTOM_SLOT) && isCooking()) {
            cookTime = 0;
        }
        sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        clearItems();
        litTime = 0;
        litDuration = 0;
        updateLitState();
        cancelCooking();
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("CookTime", cookTime);
        tag.putInt("LitTime", litTime);
        tag.putInt("LitDuration", litDuration);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        clearItems();
        ContainerHelper.loadAllItems(tag, items);
        cookTime = tag.getInt("CookTime");
        litTime = tag.getInt("LitTime");
        litDuration = tag.getInt("LitDuration");
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

    private void updateLitState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(AlchemyTableBlock.LIT) && state.getValue(AlchemyTableBlock.LIT) != isLit()) {
            level.setBlock(worldPosition, state.setValue(AlchemyTableBlock.LIT, isLit()), Block.UPDATE_ALL);
        }
    }

    private void clearItems() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
}
