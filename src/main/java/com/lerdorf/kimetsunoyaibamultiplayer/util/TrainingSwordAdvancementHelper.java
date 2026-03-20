package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TrainingSwordAdvancementHelper {
    public static final String ADVANCEMENT_FOLDER = "training_swords";

    private TrainingSwordAdvancementHelper() {
    }

    public static List<TrainingSwordAdvancementDefinition> getDefinitions() {
        List<TrainingSwordAdvancementDefinition> definitions = new ArrayList<>();
        List<StyleMetadataRegistry.StyleMetadata> eligibleStyles = new ArrayList<>(StyleMetadataRegistry.getColorChangeEligibleStyles());
        eligibleStyles.sort(Comparator.comparing(style -> PlayerColorChangeStyleHelper.formatStyleName(style.getStyleId())));

        for (StyleMetadataRegistry.StyleMetadata style : eligibleStyles) {
            Item iconItem = resolveRepresentativeSword(style.getStyleId());
            if (iconItem == null) {
                continue;
            }

            String styleName = PlayerColorChangeStyleHelper.formatStyleName(style.getStyleId());
            String title = styleName + " Training";
            String description = "Acquired a " + styleName.toLowerCase(Locale.ROOT) + " training sword.";
            ResourceLocation advancementId = getAdvancementId(style.getStyleId());
            String relativePath = "data/" + KimetsunoyaibaMultiplayer.MODID + "/advancements/" + ADVANCEMENT_FOLDER
                + "/" + sanitizeStyleId(style.getStyleId()) + ".json";

            definitions.add(new TrainingSwordAdvancementDefinition(
                style.getStyleId(),
                advancementId,
                relativePath,
                iconItem,
                title,
                description
            ));
        }

        return definitions;
    }

    public static ResourceLocation getAdvancementId(String styleId) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            ADVANCEMENT_FOLDER + "/" + sanitizeStyleId(styleId)
        );
    }

    public static void awardTrainingSwordAdvancement(ServerPlayer player, String styleId) {
        if (player == null || styleId == null || styleId.isEmpty()) {
            return;
        }

        Advancement advancement = player.server.getAdvancements().getAdvancement(getAdvancementId(styleId));
        if (advancement == null) {
            return;
        }

        var progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }

        List<String> remainingCriteria = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) {
            remainingCriteria.add(criterion);
        }

        for (String criterion : remainingCriteria) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    public static @Nullable String findAwardedTrainingSwordStyleId(ServerPlayer player, Collection<String> eligibleStyleIds) {
        if (player == null || eligibleStyleIds == null || eligibleStyleIds.isEmpty()) {
            return null;
        }

        List<String> matches = new ArrayList<>();
        for (String styleId : eligibleStyleIds) {
            Advancement advancement = player.server.getAdvancements().getAdvancement(getAdvancementId(styleId));
            if (advancement == null) {
                continue;
            }

            if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                matches.add(styleId);
            }
        }

        if (matches.size() != 1) {
            return null;
        }

        return matches.get(0);
    }

    public static String buildPackMetadataJson() {
        return """
            {
              "pack": {
                "pack_format": 15,
                "description": "KnY Multiplayer generated training sword advancements"
              }
            }
            """;
    }

    public static String buildAdvancementJson(TrainingSwordAdvancementDefinition definition) {
        ResourceLocation iconId = ForgeRegistries.ITEMS.getKey(definition.iconItem());
        String iconItemId = iconId != null ? iconId.toString() : "minecraft:wooden_sword";

        return """
            {
              "display": {
                "icon": {
                  "item": "%s"
                },
                "title": "%s",
                "description": "%s",
                "frame": "task",
                "show_toast": true,
                "announce_to_chat": false,
                "hidden": false
              },
              "parent": "kimetsunoyaibamultiplayer:demon_slayer_corps",
              "criteria": {
                "granted_training_sword": {
                  "trigger": "minecraft:impossible"
                }
              }
            }
            """.formatted(
            escapeJson(iconItemId),
            escapeJson(definition.title()),
            escapeJson(definition.description())
        );
    }

    private static @Nullable Item resolveRepresentativeSword(String styleId) {
        Item levelZero = findPreferredLevelZeroSword(styleId);
        if (levelZero != null) {
            return levelZero;
        }

        List<Item> allStyleSwords = new ArrayList<>();
        for (SwordRegistry.RegisteredSword registeredSword : SwordRegistry.getAllSwords()) {
            if (styleId.equals(registeredSword.getStyleId()) && registeredSword.getSwordItem() != null) {
                allStyleSwords.add(registeredSword.getSwordItem());
            }
        }
        for (SwordMetadataRegistry.SwordMetadata metadata : SwordMetadataRegistry.getAllSwords()) {
            if (styleId.equals(metadata.getStyleId()) && metadata.getSwordItem() != null) {
                allStyleSwords.add(metadata.getSwordItem());
            }
        }

        allStyleSwords.sort(Comparator.comparing(TrainingSwordAdvancementHelper::getItemSortKey));
        return allStyleSwords.isEmpty() ? Items.WOODEN_SWORD : allStyleSwords.get(0);
    }

    private static @Nullable Item findPreferredLevelZeroSword(String styleId) {
        List<Item> swords = new ArrayList<>();

        for (SwordRegistry.RegisteredSword registeredSword : SwordRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
            if (registeredSword.getSwordItem() != null) {
                swords.add(registeredSword.getSwordItem());
            }
        }

        if (swords.isEmpty()) {
            for (SwordMetadataRegistry.SwordMetadata metadata : SwordMetadataRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
                if (metadata.getSwordItem() != null) {
                    swords.add(metadata.getSwordItem());
                }
            }
        }

        swords.sort(Comparator.comparing(TrainingSwordAdvancementHelper::getItemSortKey));
        return swords.isEmpty() ? null : swords.get(0);
    }

    private static String getItemSortKey(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null ? id.toString() : "";
    }

    private static String sanitizeStyleId(String styleId) {
        return styleId == null ? "unknown" : styleId.replace(':', '_');
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    public record TrainingSwordAdvancementDefinition(
        String styleId,
        ResourceLocation advancementId,
        String relativePath,
        Item iconItem,
        String title,
        String description
    ) {
    }
}
