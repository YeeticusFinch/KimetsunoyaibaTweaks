package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.util.PlayerColorChangeStyleHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Nichirin ore item whose color and display name are driven by the breathing style stored in stack NBT.
 */
public class NichirinOreItem extends Item {
    public static final String STYLE_ID_TAG = "KnYMPNichirinOreStyleId";
    public static final String BLACK_VARIANT_TAG = "KnYMPNichirinOreBlackVariant";

    public NichirinOreItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createForStyle(Item item, String styleId) {
        ItemStack stack = new ItemStack(item, 2);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(STYLE_ID_TAG, styleId);
        return stack;
    }

    public static ItemStack createBlackForStyle(Item item, String styleId) {
        ItemStack stack = createForStyle(item, styleId);
        stack.getOrCreateTag().putBoolean(BLACK_VARIANT_TAG, true);
        return stack;
    }

    public static @Nullable String getStyleId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(STYLE_ID_TAG) ? tag.getString(STYLE_ID_TAG) : null;
    }

    public static int getStyleColor(ItemStack stack) {
        if (isBlackVariant(stack)) {
            return PlayerColorChangeStyleHelper.getStyleColor("black");
        }
        String styleId = getStyleId(stack);
        return styleId != null ? PlayerColorChangeStyleHelper.getStyleColor(styleId) : 0xFFFFFF;
    }

    public static boolean isBlackVariant(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(BLACK_VARIANT_TAG);
    }

    @Override
    public Component getName(ItemStack stack) {
        String styleId = getStyleId(stack);
        if (styleId != null && !styleId.isEmpty()) {
            String displayName = isBlackVariant(stack)
                ? PlayerColorChangeStyleHelper.formatBlackOreName(styleId)
                : PlayerColorChangeStyleHelper.formatStyleName(styleId);
            return Component.literal("Scarlet Ore (" + displayName + ")")
                .withStyle(style -> style.withItalic(false));
        }
        return Component.literal("Scarlet Ore").withStyle(style -> style.withItalic(false));
    }
}
