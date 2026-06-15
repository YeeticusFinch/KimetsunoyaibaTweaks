package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;

public class VialRackBlockItem extends BlockItem {
    public static final String STORED_ITEMS_TAG = "VialRackItems";

    public VialRackBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack getStoredItem(ItemStack rackStack, int slot) {
        if (slot < 0 || slot >= VialRackContents.SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        CompoundTag tag = rackStack.getTag();
        if (tag == null || !tag.contains(STORED_ITEMS_TAG, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        ListTag list = tag.getList(STORED_ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            if (itemTag.getByte("Slot") == slot) {
                ItemStack stored = ItemStack.of(itemTag);
                stored.setCount(Math.min(stored.getCount(), 1));
                return VialRackContents.isVial(stored) ? stored : ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void setStoredItem(ItemStack rackStack, int slot, ItemStack storedStack) {
        ItemStack[] items = getStoredItems(rackStack);
        items[slot] = storedStack;
        setStoredItems(rackStack, items);
    }

    public static ItemStack[] getStoredItems(ItemStack rackStack) {
        ItemStack[] items = new ItemStack[VialRackContents.SLOT_COUNT];
        for (int slot = 0; slot < items.length; slot++) {
            items[slot] = getStoredItem(rackStack, slot);
        }
        return items;
    }

    public static void setStoredItems(ItemStack rackStack, ItemStack[] items) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < Math.min(items.length, VialRackContents.SLOT_COUNT); slot++) {
            ItemStack stored = items[slot];
            if (stored.isEmpty() || !VialRackContents.isVial(stored)) {
                continue;
            }

            ItemStack single = stored.copy();
            single.setCount(1);
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) slot);
            single.save(itemTag);
            list.add(itemTag);
        }

        CompoundTag tag = rackStack.getOrCreateTag();
        if (list.isEmpty()) {
            tag.remove(STORED_ITEMS_TAG);
            if (tag.isEmpty()) {
                rackStack.setTag(null);
            }
        } else {
            tag.put(STORED_ITEMS_TAG, list);
        }
    }

    public static boolean hasStoredItems(ItemStack rackStack) {
        for (ItemStack stored : getStoredItems(rackStack)) {
            if (!stored.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        ItemStack[] storedItems = getStoredItems(stack);
        boolean hasAny = false;
        for (int i = 0; i < storedItems.length; i++) {
            ItemStack stored = storedItems[i];
            if (stored.isEmpty()) {
                continue;
            }

            if (!hasAny) {
                tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.vial_rack.contents")
                    .withStyle(ChatFormatting.GRAY));
                hasAny = true;
            }
            tooltip.add(Component.literal("  ")
                .append(Component.translatable("tooltip.kimetsunoyaibamultiplayer.vial_rack.slot", i + 1))
                .append(Component.literal(": "))
                .append(stored.getHoverName())
                .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = createRenderer();
                }
                return renderer;
            }
        });
    }

    private static net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer createRenderer() {
        try {
            Class<?> rendererClass = Class.forName(
                "com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.VialRackItemRenderer");
            Constructor<?> constructor = rendererClass.getConstructor();
            return (net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create VialRackItemRenderer", e);
        }
    }
}
