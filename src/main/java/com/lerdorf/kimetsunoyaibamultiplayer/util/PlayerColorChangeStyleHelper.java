package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Shared helper for persistent breathing-style assignment during color change and ore selection.
 */
public final class PlayerColorChangeStyleHelper {
    public static final String PLAYER_COLOR_CHANGE_STYLE_TAG = "KnYMPColorChangeStyleId";

    private static final Random RANDOM = new Random();
    private static final Map<String, Integer> STYLE_COLORS = createStyleColors();

    private PlayerColorChangeStyleHelper() {
    }

    public static StyleMetadataRegistry.StyleMetadata resolveOrAssignColorChangeStyle(Player player) {
        return resolveOrAssignColorChangeStyle(player, StyleMetadataRegistry.getColorChangeEligibleStyles());
    }

    public static StyleMetadataRegistry.StyleMetadata resolveOrAssignColorChangeStyle(
            Player player,
            List<StyleMetadataRegistry.StyleMetadata> eligibleStyles) {

        if (eligibleStyles == null || eligibleStyles.isEmpty()) {
            return null;
        }

        String resolvedStyleId = resolveAssignedColorChangeStyleId(player, eligibleStyles, true);
        return resolvedStyleId == null ? null : findEligibleStyle(eligibleStyles, resolvedStyleId);
    }

    public static String getAssignedColorChangeStyleId(Player player) {
        String resolvedStyleId = resolveAssignedColorChangeStyleId(
            player,
            StyleMetadataRegistry.getColorChangeEligibleStyles(),
            false
        );
        return resolvedStyleId != null ? resolvedStyleId : "";
    }

    public static void persistAssignedColorChangeStyle(Player player, String styleId) {
        if (player == null || styleId == null || styleId.isEmpty()) {
            return;
        }
        player.getPersistentData().putString(PLAYER_COLOR_CHANGE_STYLE_TAG, styleId);
    }

    public static void copyPersistentStyleData(Player original, Player clone) {
        if (original == null || clone == null) {
            return;
        }

        String savedStyleId = original.getPersistentData().getString(PLAYER_COLOR_CHANGE_STYLE_TAG);
        if (!savedStyleId.isEmpty()) {
            clone.getPersistentData().putString(PLAYER_COLOR_CHANGE_STYLE_TAG, savedStyleId);
        }

        NichirinSwordBlack.copyRememberedPlayerStyle(original, clone);
    }

    public static @Nullable String restoreAssignedColorChangeStyle(Player player) {
        return resolveAssignedColorChangeStyleId(
            player,
            StyleMetadataRegistry.getColorChangeEligibleStyles(),
            false
        );
    }

    public static String formatBlackOreName(String styleId) {
        String styleName = formatStyleName(styleId);
        if (styleName.endsWith(" Breathing")) {
            styleName = styleName.substring(0, styleName.length() - " Breathing".length());
        }
        return "Black " + styleName;
    }

    public static String formatStyleName(String styleId) {
        if (styleId == null || styleId.isEmpty()) {
            return "Unknown Breathing";
        }

        BreathingStyleRegistry.RegisteredBreathingStyle registered = BreathingStyleRegistry.getStyle(styleId);
        if (registered != null && registered.getStyleName() != null && !registered.getStyleName().isEmpty()) {
            return registered.getStyleName();
        }

        String[] words = styleId.replace('_', ' ').split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (formatted.length() > 0) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                formatted.append(word.substring(1));
            }
        }
        return formatted.toString();
    }

    public static int getStyleColor(String styleId) {
        Integer mappedColor = STYLE_COLORS.get(styleId);
        if (mappedColor != null) {
            return mappedColor;
        }

        BreathingStyleRegistry.RegisteredBreathingStyle registered = BreathingStyleRegistry.getStyle(styleId);
        if (registered != null) {
            BreathingTechnique technique = registered.getTechnique();
            if (technique != null) {
                int color = colorFromLegacyCode(technique.getFormColor());
                if (color != 0xFFFFFF) {
                    return color;
                }
                return colorFromLegacyCode(technique.getTechniqueColor());
            }
        }

        return 0xFFFFFF;
    }

    public static String getTopLevelFamilyStyleId(String styleId) {
        if (styleId == null || styleId.isEmpty()) {
            return "";
        }

        StyleMetadataRegistry.StyleMetadata current = StyleMetadataRegistry.getMetadata(styleId);
        if (current == null) {
            return styleId;
        }

        while (current.getParentStyleId() != null) {
            StyleMetadataRegistry.StyleMetadata parent = StyleMetadataRegistry.getMetadata(current.getParentStyleId());
            if (parent == null || parent.getParentStyleId() == null) {
                break;
            }
            current = parent;
        }

        return current.getStyleId();
    }

    public static List<StyleMetadataRegistry.StyleMetadata> getFamilyOreSelectionStyles(String assignedStyleId) {
        String familyRootId = getTopLevelFamilyStyleId(assignedStyleId);
        List<StyleMetadataRegistry.StyleMetadata> orderedFamily = getFamilyStylesBreadthFirst(familyRootId);
        orderedFamily.removeIf(style -> !style.isOreSelectionEligible());

        if (assignedStyleId == null || assignedStyleId.isEmpty() || orderedFamily.isEmpty()) {
            return orderedFamily;
        }

        if (familyRootId.equals(assignedStyleId)) {
            return orderedFamily;
        }

        List<StyleMetadataRegistry.StyleMetadata> reordered = new ArrayList<>();
        StyleMetadataRegistry.StyleMetadata assigned = null;
        StyleMetadataRegistry.StyleMetadata root = null;
        for (StyleMetadataRegistry.StyleMetadata style : orderedFamily) {
            if (style.getStyleId().equals(assignedStyleId)) {
                assigned = style;
            } else if (style.getStyleId().equals(familyRootId)) {
                root = style;
            } else {
                reordered.add(style);
            }
        }

        reordered.sort(Comparator
            .comparingInt((StyleMetadataRegistry.StyleMetadata style) -> getDistanceToAssigned(style.getStyleId(), assignedStyleId))
            .thenComparing(style -> formatStyleName(style.getStyleId())));

        List<StyleMetadataRegistry.StyleMetadata> result = new ArrayList<>();
        if (assigned != null) {
            result.add(assigned);
        }
        result.addAll(reordered);
        if (root != null) {
            result.add(root);
        }
        return result;
    }

    private static @Nullable String resolveAssignedColorChangeStyleId(
            Player player,
            List<StyleMetadataRegistry.StyleMetadata> eligibleStyles,
            boolean assignRandomIfMissing) {

        Map<String, StyleMetadataRegistry.StyleMetadata> eligibleById = new HashMap<>();
        for (StyleMetadataRegistry.StyleMetadata style : eligibleStyles) {
            eligibleById.put(style.getStyleId(), style);
        }

        if (eligibleById.isEmpty()) {
            return null;
        }

        CompoundTag persistentData = player.getPersistentData();
        String savedStyleId = persistentData.getString(PLAYER_COLOR_CHANGE_STYLE_TAG);
        if (eligibleById.containsKey(savedStyleId)) {
            return savedStyleId;
        }

        String advancementStyleId = resolveStyleFromAdvancement(player, eligibleById.keySet());
        if (eligibleById.containsKey(advancementStyleId)) {
            persistAssignedColorChangeStyle(player, advancementStyleId);
            return advancementStyleId;
        }

        String inventoryStyleId = resolveStyleFromTrainingSwordInventory(player, eligibleById.keySet());
        if (eligibleById.containsKey(inventoryStyleId)) {
            persistAssignedColorChangeStyle(player, inventoryStyleId);
            return inventoryStyleId;
        }

        if (!assignRandomIfMissing) {
            return null;
        }

        StyleMetadataRegistry.StyleMetadata chosen = eligibleStyles.get(RANDOM.nextInt(eligibleStyles.size()));
        persistAssignedColorChangeStyle(player, chosen.getStyleId());
        return chosen.getStyleId();
    }

    private static @Nullable String resolveStyleFromAdvancement(Player player, Collection<String> eligibleStyleIds) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        return TrainingSwordAdvancementHelper.findAwardedTrainingSwordStyleId(serverPlayer, eligibleStyleIds);
    }

    private static @Nullable String resolveStyleFromTrainingSwordInventory(Player player, Set<String> eligibleStyleIds) {
        Set<String> eligible = new HashSet<>(eligibleStyleIds);

        for (int slot = 0; slot < 9; slot++) {
            String styleId = getTrainingSwordStyleId(player, player.getInventory().getItem(slot));
            if (eligible.contains(styleId)) {
                return styleId;
            }
        }

        for (int slot = 9; slot < player.getInventory().getContainerSize(); slot++) {
            String styleId = getTrainingSwordStyleId(player, player.getInventory().getItem(slot));
            if (eligible.contains(styleId)) {
                return styleId;
            }
        }

        for (ItemStack offhandStack : player.getInventory().offhand) {
            String styleId = getTrainingSwordStyleId(player, offhandStack);
            if (eligible.contains(styleId)) {
                return styleId;
            }
        }

        return null;
    }

    private static @Nullable String getTrainingSwordStyleId(Player player, ItemStack stack) {
        if (stack.isEmpty() || !TrainingSwordHelper.isTrainingSword(stack)) {
            return null;
        }

        if (stack.getItem() instanceof NichirinSwordBlack) {
            String blackSwordStyleId = NichirinSwordBlack.getAssignedStyleId(stack);
            if (blackSwordStyleId != null && !blackSwordStyleId.isEmpty()) {
                NichirinSwordBlack.rememberPlayerStyle(player, blackSwordStyleId);
            }
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(stack.getItem());
        if (registeredSword != null) {
            return registeredSword.getStyleId();
        }

        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(stack.getItem());
        return metadata != null ? metadata.getStyleId() : null;
    }

    private static @Nullable StyleMetadataRegistry.StyleMetadata findEligibleStyle(
            List<StyleMetadataRegistry.StyleMetadata> eligibleStyles,
            String styleId) {
        for (StyleMetadataRegistry.StyleMetadata style : eligibleStyles) {
            if (style.getStyleId().equals(styleId)) {
                return style;
            }
        }
        return null;
    }

    private static List<StyleMetadataRegistry.StyleMetadata> getFamilyStylesBreadthFirst(String familyRootId) {
        List<StyleMetadataRegistry.StyleMetadata> ordered = new ArrayList<>();
        StyleMetadataRegistry.StyleMetadata familyRoot = StyleMetadataRegistry.getMetadata(familyRootId);
        if (familyRoot == null) {
            return ordered;
        }

        Deque<StyleMetadataRegistry.StyleMetadata> queue = new ArrayDeque<>();
        queue.add(familyRoot);

        while (!queue.isEmpty()) {
            StyleMetadataRegistry.StyleMetadata current = queue.removeFirst();
            ordered.add(current);

            List<StyleMetadataRegistry.StyleMetadata> children = new ArrayList<>(StyleMetadataRegistry.getChildStyles(current.getStyleId()));
            children.sort(Comparator.comparing(style -> formatStyleName(style.getStyleId())));
            queue.addAll(children);
        }

        return ordered;
    }

    private static int getDistanceToAssigned(String styleId, String assignedStyleId) {
        int distance = 0;
        StyleMetadataRegistry.StyleMetadata current = StyleMetadataRegistry.getMetadata(styleId);
        while (current != null && !current.getStyleId().equals(assignedStyleId)) {
            String parentId = current.getParentStyleId();
            if (parentId == null) {
                break;
            }
            current = StyleMetadataRegistry.getMetadata(parentId);
            distance++;
        }
        return distance;
    }

    private static int colorFromLegacyCode(String colorCode) {
        if (colorCode == null || colorCode.length() < 2 || colorCode.charAt(0) != '§') {
            return 0xFFFFFF;
        }

        return switch (Character.toLowerCase(colorCode.charAt(1))) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    private static Map<String, Integer> createStyleColors() {
        Map<String, Integer> colors = new HashMap<>();
        colors.put("black", 0x303030);
        colors.put("water_breathing", 0x4A8CFF);
        colors.put("flower_breathing", 0xFF72C1);
        colors.put("insect_breathing", 0xB785FF);
        colors.put("serpent_breathing", 0x8B5CF6);
        colors.put("flame_breathing", 0xFF6A2A);
        colors.put("love_breathing", 0xFF5FA2);
        colors.put("wind_breathing", 0x58C26D);
        colors.put("mist_breathing", 0x8FD8E6);
        colors.put("beast_breathing", 0x40C7C7);
        colors.put("thunder_breathing", 0xFFD84D);
        colors.put("sound_breathing", 0xD89B3C);
        colors.put("stone_breathing", 0x8E8E8E);
        colors.put("moon_breathing", 0xD49CFF);
        colors.put("sun_breathing", 0xFFB347);
        return colors;
    }
}
