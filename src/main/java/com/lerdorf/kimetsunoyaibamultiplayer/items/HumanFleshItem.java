package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;

public class HumanFleshItem extends GeckolibItem {
    public static final String TEXTURE_PATH_TAG = "HumanFleshTexture";
    public static final String PLAYER_UUID_TAG = "HumanFleshPlayerUuid";
    public static final String PLAYER_NAME_TAG = "HumanFleshPlayerName";

    private final String modelPath;
    private final ResourceLocation defaultTexture;

    public HumanFleshItem(Properties properties, String modelPath, ResourceLocation defaultTexture) {
        super(properties);
        this.modelPath = modelPath;
        this.defaultTexture = defaultTexture;
    }

    public String getModelPath() {
        return modelPath;
    }

    public ResourceLocation getDefaultTexture() {
        return defaultTexture;
    }

    @Override
    protected Supplier<BlockEntityWithoutLevelRenderer> getRendererSupplier() {
        return () -> {
            try {
                Class<?> rendererClass = Class.forName(
                    "com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.HumanFleshRenderer");
                Constructor<?> constructor = rendererClass.getConstructor(HumanFleshItem.class);
                return (BlockEntityWithoutLevelRenderer) constructor.newInstance(this);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to create HumanFleshRenderer", e);
            }
        };
    }

    @Override
    protected boolean supportsGeckolibItemAnimations(ItemStack stack) {
        return false;
    }

    public static void setTexture(ItemStack stack, ResourceLocation texture) {
        if (!stack.isEmpty() && texture != null) {
            stack.getOrCreateTag().putString(TEXTURE_PATH_TAG, texture.toString());
        }
    }

    public static void setPlayerSkin(ItemStack stack, UUID playerUuid, String playerName) {
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        if (playerUuid != null) {
            tag.putUUID(PLAYER_UUID_TAG, playerUuid);
        }
        if (playerName != null && !playerName.isBlank()) {
            tag.putString(PLAYER_NAME_TAG, playerName);
        }
    }

    public static ResourceLocation getTexture(ItemStack stack, ResourceLocation fallback) {
        if (stack.isEmpty()) {
            return fallback;
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TEXTURE_PATH_TAG)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(TEXTURE_PATH_TAG));
            if (parsed != null) {
                return parsed;
            }
        }

        return fallback;
    }

    public static UUID getPlayerUuid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(PLAYER_UUID_TAG)) {
            return tag.getUUID(PLAYER_UUID_TAG);
        }
        return null;
    }

    public static String getPlayerName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(PLAYER_NAME_TAG)) {
            return tag.getString(PLAYER_NAME_TAG);
        }
        return "";
    }
}
