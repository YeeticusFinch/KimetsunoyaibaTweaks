package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinOreItem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Chest-backed ore selection inventory. Taking a single ore completes selection immediately.
 */
public class OreSelectionContainer extends SimpleContainer {
    private final Player player;
    private final Consumer<ItemStack> onSelection;
    private boolean selectionComplete = false;
    private boolean sanitizing = false;

    public OreSelectionContainer(Player player, Consumer<ItemStack> onSelection) {
        super(27);
        this.player = player;
        this.onSelection = onSelection;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = super.removeItem(slot, amount);
        handleSelection(removed);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = super.removeItemNoUpdate(slot);
        handleSelection(removed);
        return removed;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void setChanged() {
        if (sanitizing) {
            super.setChanged();
            return;
        }

        sanitizing = true;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && !(stack.getItem() instanceof NichirinOreItem)) {
                super.setItem(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        sanitizing = false;
        super.setChanged();
    }

    private void handleSelection(ItemStack removed) {
        if (selectionComplete || removed.isEmpty()) {
            return;
        }

        selectionComplete = true;
        onSelection.accept(removed.copy());
        if (player.level().getServer() != null) {
            player.level().getServer().execute(player::closeContainer);
        }
    }
}
