package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.AlchemyTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AlchemyTableMenu extends AbstractContainerMenu {
    private static final int TOP_SLOT = AlchemyTableBlockEntity.TOP_SLOT;
    private static final int BOTTOM_SLOT = AlchemyTableBlockEntity.BOTTOM_SLOT;
    private static final int FUEL_SLOT = AlchemyTableBlockEntity.FUEL_SLOT;
    private static final int DATA_COUNT = 4;
    private static final int PLAYER_INV_START = AlchemyTableBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Slot ingredientSlot;

    public AlchemyTableMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public AlchemyTableMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        this(containerId, inventory, resolveContainer(inventory, blockPos),
            resolveData(inventory, blockPos), ContainerLevelAccess.create(inventory.player.level(), blockPos));
    }

    public AlchemyTableMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        this(containerId, inventory, container, data, ContainerLevelAccess.NULL);
    }

    public AlchemyTableMenu(int containerId, Inventory inventory, Container container, ContainerData data, ContainerLevelAccess access) {
        super(ModAlchemyMenus.ALCHEMY_TABLE.get(), containerId);
        checkContainerSize(container, AlchemyTableBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.access = access;
        this.data = data;

        this.ingredientSlot = addSlot(new Slot(container, TOP_SLOT, 79, 17));
        addSlot(new AlchemyContainerSlot(container, BOTTOM_SLOT, 79, 58));
        addSlot(new FuelSlot(container, FUEL_SLOT, 17, 17));

        addDataSlots(data);
        addPlayerInventory(inventory);
    }

    private static Container resolveContainer(Inventory inventory, BlockPos blockPos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(blockPos);
        return blockEntity instanceof AlchemyTableBlockEntity table ? table : new SimpleContainer(AlchemyTableBlockEntity.SLOT_COUNT);
    }

    private static ContainerData resolveData(Inventory inventory, BlockPos blockPos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(blockPos);
        return blockEntity instanceof AlchemyTableBlockEntity table ? table.dataAccess() : new SimpleContainerData(DATA_COUNT);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
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
        if (index < AlchemyTableBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (shouldPreferIngredientSlot(stack)) {
            if (!moveItemStackTo(stack, TOP_SLOT, TOP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (FuelSlot.mayPlaceItem(stack)) {
            if (!moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)
                && !moveItemStackTo(stack, TOP_SLOT, TOP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BloodDemonArtAlchemyCatalog.isLikelyAlchemyTableBottomInput(stack)) {
            if (!moveItemStackTo(stack, BOTTOM_SLOT, BOTTOM_SLOT + 1, false)
                && !moveItemStackTo(stack, TOP_SLOT, TOP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ingredientSlot.mayPlace(stack)) {
            if (!moveItemStackTo(stack, TOP_SLOT, TOP_SLOT + 1, false)) {
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
        } else if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) {
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

    private boolean shouldPreferIngredientSlot(ItemStack stack) {
        ItemStack bottom = container.getItem(BOTTOM_SLOT);
        if (bottom.isEmpty()) {
            return false;
        }

        SimpleContainer input = new SimpleContainer(2);
        input.setItem(0, stack);
        input.setItem(1, bottom);
        return access.evaluate((level, pos) ->
            level.getRecipeManager().getRecipeFor(ModAlchemyRecipes.ALCHEMY_TABLE_TYPE, input, level).isPresent(),
            false);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && !(container instanceof AlchemyTableBlockEntity)) {
            clearContainer(player, container);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModAlchemyBlocks.ALCHEMY_TABLE.get())
                    && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            container.stillValid(player));
    }

    public int getScaledCookProgress(int pixels) {
        int cookTime = data.get(0);
        int cookTimeTotal = data.get(1);
        return cookTimeTotal > 0 && cookTime > 0 ? Mth.clamp(cookTime * pixels / cookTimeTotal, 0, pixels) : 0;
    }

    public int getScaledBurnProgress(int pixels) {
        int litTime = data.get(2);
        int litDuration = data.get(3);
        return litDuration > 0 && litTime > 0 ? Mth.clamp((litTime * pixels + litDuration - 1) / litDuration, 1, pixels) : 0;
    }

    public boolean isCooking() {
        return data.get(0) > 0;
    }

    public boolean isLit() {
        return data.get(2) > 0;
    }

    private static class AlchemyContainerSlot extends Slot {
        AlchemyContainerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BloodDemonArtAlchemyCatalog.isAlchemyTableDisplayContainer(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return mayPlaceItem(stack);
        }

        static boolean mayPlaceItem(ItemStack stack) {
            return AlchemyTableBlockEntity.isFuel(stack);
        }
    }
}
