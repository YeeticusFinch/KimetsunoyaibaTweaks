package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class SlayerFleshHelper {
    public static final String SLAYER_FLESH_TAG = "KnYDemonSlayerFlesh";
    public static final String POWER_LEVEL_TAG = "KnYDemonSlayerPowerLevel";
    public static final String BREATHING_STYLE_TAG = "KnYDemonSlayerBreathingStyle";
    public static final String FEMALE_TAG = "KnYDemonSlayerFemale";
    public static final String TEXTURE_INDEX_TAG = "KnYDemonSlayerTextureIndex";
    public static final String LEVEL_TAG = "KnYDemonSlayerLevel";
    public static final String GENDER_TAG = "KnYDemonSlayerGender";
    public static final String SKIN_TAG = "KnYDemonSlayerSkin";

    private SlayerFleshHelper() {
    }

    public record SlayerFleshData(int powerLevel, String breathingStyleId, boolean female, int textureIndex) {
    }

    public static void applyDemonSlayerMetadata(ItemStack stack, DemonSlayerEntity slayer) {
        if (stack == null || stack.isEmpty() || slayer == null) {
            return;
        }
        applyMetadata(stack, slayer.getPowerLevel(), resolveBreathingStyleId(slayer), slayer.isFemale(), slayer.getTextureIndex());
    }

    public static void applyMetadata(ItemStack stack, int powerLevel, String breathingStyleId, boolean female, int textureIndex) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String styleId = breathingStyleId == null || breathingStyleId.isBlank() ? "unknown" : breathingStyleId;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(SLAYER_FLESH_TAG, true);
        tag.putInt(POWER_LEVEL_TAG, Math.max(0, powerLevel));
        tag.putInt(LEVEL_TAG, Math.max(0, powerLevel));
        tag.putString(BREATHING_STYLE_TAG, styleId);
        tag.putBoolean(FEMALE_TAG, female);
        tag.putString(GENDER_TAG, female ? "female" : "male");
        tag.putInt(TEXTURE_INDEX_TAG, Math.max(0, textureIndex));
        tag.putInt(SKIN_TAG, Math.max(0, textureIndex));

        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal("Demon Slayer Remains").withStyle(ChatFormatting.DARK_RED))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal("Level: " + Math.max(0, powerLevel)).withStyle(ChatFormatting.GRAY))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal("Breathing: " + formatStyleName(styleId)).withStyle(ChatFormatting.GRAY))));
        display.put("Lore", lore);
    }

    public static boolean isDemonSlayerFlesh(ItemStack stack) {
        CompoundTag tag = stack == null ? null : stack.getTag();
        return tag != null && tag.getBoolean(SLAYER_FLESH_TAG);
    }

    public static SlayerFleshData read(ItemStack stack) {
        CompoundTag tag = stack == null ? null : stack.getTag();
        if (tag == null || !tag.getBoolean(SLAYER_FLESH_TAG)) {
            return null;
        }
        boolean female = tag.contains(FEMALE_TAG)
            ? tag.getBoolean(FEMALE_TAG)
            : "female".equalsIgnoreCase(tag.getString(GENDER_TAG));
        return new SlayerFleshData(
            Math.max(0, tag.contains(POWER_LEVEL_TAG) ? tag.getInt(POWER_LEVEL_TAG) : tag.getInt(LEVEL_TAG)),
            tag.getString(BREATHING_STYLE_TAG),
            female,
            Math.max(0, tag.contains(TEXTURE_INDEX_TAG) ? tag.getInt(TEXTURE_INDEX_TAG) : tag.getInt(SKIN_TAG))
        );
    }

    public static String resolveBreathingStyleId(DemonSlayerEntity slayer) {
        if (slayer == null || slayer.getSwordId() == null || slayer.getSwordId().isBlank()) {
            return "unknown";
        }
        SwordRegistry.RegisteredSword registered = SwordRegistry.getSword(slayer.getSwordId());
        if (registered != null && registered.getStyleId() != null && !registered.getStyleId().isBlank()) {
            return registered.getStyleId();
        }
        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(slayer.getSwordId());
        if (metadata != null && metadata.getStyleId() != null && !metadata.getStyleId().isBlank()) {
            return metadata.getStyleId();
        }
        ItemStack swordStack = slayer.getMainHandItem();
        if (!swordStack.isEmpty()) {
            registered = SwordRegistry.getSword(swordStack.getItem());
            if (registered != null && registered.getStyleId() != null && !registered.getStyleId().isBlank()) {
                return registered.getStyleId();
            }
            metadata = SwordMetadataRegistry.getMetadata(swordStack.getItem());
            if (metadata != null && metadata.getStyleId() != null && !metadata.getStyleId().isBlank()) {
                return metadata.getStyleId();
            }
        }
        return "unknown";
    }

    public static String findSwordForStyle(String styleId, RandomSource random) {
        if (styleId == null || styleId.isBlank() || "unknown".equals(styleId)) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        for (SwordRegistry.RegisteredSword sword : SwordRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
            if (sword.isObtainableInSurvival()) {
                candidates.add(sword.getSwordId());
            }
        }
        for (SwordMetadataRegistry.SwordMetadata sword : SwordMetadataRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
            if (sword.isObtainableInSurvival() && canResolveSwordItem(sword.getSwordId(), sword.getSwordItem())) {
                candidates.add(sword.getSwordId());
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    public static String formatStyleName(String styleId) {
        if (styleId == null || styleId.isBlank() || "unknown".equals(styleId)) {
            return "Unknown";
        }
        BreathingStyleRegistry.RegisteredBreathingStyle registered = BreathingStyleRegistry.getStyle(styleId);
        if (registered != null && registered.getStyleName() != null && !registered.getStyleName().isBlank()) {
            return registered.getStyleName();
        }
        String cleaned = styleId.replace("_breathing", "").replace('_', ' ');
        String[] parts = cleaned.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (formatted.length() > 0) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                formatted.append(part.substring(1));
            }
        }
        return formatted.length() == 0 ? styleId : formatted.toString();
    }

    private static boolean canResolveSwordItem(String swordId, Item directItem) {
        if (directItem != null && !new ItemStack(directItem).isEmpty()) {
            return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(swordId);
        if (id == null) {
            id = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", swordId);
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item != null && !new ItemStack(item).isEmpty();
    }
}
