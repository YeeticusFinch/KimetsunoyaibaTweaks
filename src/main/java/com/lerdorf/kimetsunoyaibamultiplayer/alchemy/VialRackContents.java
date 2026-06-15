package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class VialRackContents {
    public static final int SLOT_COUNT = 5;

    private VialRackContents() {
    }

    public static boolean isVial(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation id = ResourceLocation.tryParse(BloodDemonArtAlchemyCatalog.id(stack));
        String path = id == null ? "" : id.getPath();
        return BloodDemonArtAlchemyCatalog.isVialDisplayItem(stack)
            || path.endsWith("_extract")
            || path.contains("_extract_")
            || path.contains("infusion")
            || path.contains("catalyst")
            || path.contains("sample")
            || "potion_effect_binder".equals(path);
    }

    public static ItemStack asSingleVial(ItemStack stack) {
        if (!isVial(stack)) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
