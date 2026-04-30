package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class MicroscopeMenu extends AbstractContainerMenu {
    private static final int LENS_SLOT = MicroscopeStationSavedData.LENS_SLOT;
    private static final int ITEM_SLOT = MicroscopeStationSavedData.ITEM_SLOT;
    private static final int SLOT_COUNT = MicroscopeStationSavedData.SLOT_COUNT;
    private static final int PLAYER_INV_START = SLOT_COUNT;

    private final Container container;
    private final ContainerLevelAccess access;

    public MicroscopeMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public MicroscopeMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        this(containerId, inventory, resolveContainer(inventory, blockPos),
            ContainerLevelAccess.create(inventory.player.level(), blockPos));
    }

    public MicroscopeMenu(int containerId, Inventory inventory, Container container, ContainerLevelAccess access) {
        super(ModAlchemyMenus.MICROSCOPE.get(), containerId);
        checkContainerSize(container, SLOT_COUNT);
        this.container = container;
        this.access = access;

        addSlot(new Slot(container, LENS_SLOT, 79, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BloodDemonArtAlchemyCatalog.isLens(stack);
            }
        });
        addSlot(new Slot(container, ITEM_SLOT, 79, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BloodDemonArtAlchemyCatalog.isUnidentifiedExtract(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addPlayerInventory(inventory);
    }

    private static Container resolveContainer(Inventory inventory, BlockPos blockPos) {
        if (inventory.player.level() instanceof ServerLevel serverLevel) {
            return MicroscopeStationSavedData.containerFor(serverLevel, blockPos);
        }
        return new SimpleContainer(SLOT_COUNT);
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
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0) {
            return false;
        }

        return false;
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
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (BloodDemonArtAlchemyCatalog.isLens(stack)) {
            if (!moveItemStackTo(stack, LENS_SLOT, LENS_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BloodDemonArtAlchemyCatalog.isUnidentifiedExtract(stack)) {
            if (!moveItemStackTo(stack, ITEM_SLOT, ITEM_SLOT + 1, false)) {
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

        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        net.minecraft.resources.ResourceLocation microscopeId = net.minecraft.resources.ResourceLocation.tryParse(BloodDemonArtAlchemyCatalog.MICROSCOPE_BLOCK_ID);
        return access.evaluate((level, pos) ->
            microscopeId != null && level.getBlockState(pos).is(ForgeRegistries.BLOCKS.getValue(microscopeId))
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            true);
    }

    public boolean hasActiveSample() {
        ItemStack lens = container.getItem(LENS_SLOT);
        ItemStack sample = container.getItem(ITEM_SLOT);
        if (lens.isEmpty() || sample.isEmpty()) {
            return false;
        }

        SimpleContainer input = new SimpleContainer(2);
        input.setItem(0, lens);
        input.setItem(1, sample);
        return access.evaluate((level, pos) ->
            level.getRecipeManager().getRecipeFor(ModAlchemyRecipes.MICROSCOPE_TYPE, input, level).isPresent(),
            false);
    }

    public int getSampleTint() {
        ItemStack sample = container.getItem(ITEM_SLOT);
        return sample.isEmpty() ? 0x89D6D0 : BloodDemonArtAlchemyCatalog.tintFor(sample);
    }
}
