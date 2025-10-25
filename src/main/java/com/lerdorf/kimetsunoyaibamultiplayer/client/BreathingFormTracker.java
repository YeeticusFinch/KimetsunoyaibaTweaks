package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side tracker for breathing forms.
 * Remembers the last selected form for each sword item, even when breathes NBT is cleared.
 */
@OnlyIn(Dist.CLIENT)
public class BreathingFormTracker {

    // Map: Player UUID -> (Sword Item ID -> Last Breathes Value)
    private static final Map<UUID, Map<String, Double>> playerFormCache = new HashMap<>();

    // Map: Player UUID -> (Sword Item Type -> Display Text)
    // Stores the formatted breathing form display text from kimetsunoyaiba mod's chat messages
    // Keyed by both player UUID and sword item type, so switching swords remembers each sword's form
    private static final Map<UUID, Map<String, String>> formDisplayTextCache = new HashMap<>();

    /**
     * Updates the cached breathing form for a player's sword.
     */
    public static void updateForm(UUID playerUUID, ItemStack sword, double breathes) {
        if (playerUUID == null || sword == null || sword.isEmpty()) {
            return;
        }

        String swordId = getSwordIdentifier(sword);

        playerFormCache.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .put(swordId, breathes);
    }

    /**
     * Gets the cached breathing form for a player's sword.
     * Returns 0.0 if no form is cached.
     */
    public static double getCachedForm(UUID playerUUID, ItemStack sword) {
        if (playerUUID == null || sword == null || sword.isEmpty()) {
            return 0.0;
        }

        String swordId = getSwordIdentifier(sword);

        Map<String, Double> swordMap = playerFormCache.get(playerUUID);
        if (swordMap == null) {
            return 0.0;
        }

        return swordMap.getOrDefault(swordId, 0.0);
    }

    /**
     * Gets a unique identifier for a sword item.
     * For multi-style swords, includes the select offset.
     */
    private static String getSwordIdentifier(ItemStack sword) {
        String baseId = sword.getItem().toString();

        // For multi-style swords, include the selected style in the identifier
        if (sword.hasTag() && sword.getTag().contains("select")) {
            double select = sword.getTag().getDouble("select");
            baseId += "_select_" + (int)select;
        }

        return baseId;
    }

    /**
     * Clears all cached forms for a player (called on logout).
     */
    public static void clearPlayer(UUID playerUUID) {
        playerFormCache.remove(playerUUID);
    }

    /**
     * Clears all cached forms.
     */
    public static void clearAll() {
        playerFormCache.clear();
        formDisplayTextCache.clear();
    }

    /**
     * Updates the cached display text from a chat message.
     * This captures the formatted breathing form text from kimetsunoyaiba mod.
     * Stores per-sword so switching between swords remembers each sword's selected form.
     */
    public static void updateDisplayTextFromChat(UUID playerUUID, ItemStack sword, String displayText) {
        if (playerUUID == null || sword == null || sword.isEmpty() || displayText == null) {
            return;
        }

        String swordType = getSwordType(sword);
        formDisplayTextCache.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .put(swordType, displayText);
    }

    /**
     * Gets the cached display text for a player's current sword.
     * Returns null if no text is cached for this sword.
     */
    public static String getCachedDisplayText(UUID playerUUID, ItemStack sword) {
        if (playerUUID == null || sword == null || sword.isEmpty()) {
            return null;
        }

        String swordType = getSwordType(sword);
        Map<String, String> textMap = formDisplayTextCache.get(playerUUID);
        if (textMap == null) {
            return null;
        }

        return textMap.get(swordType);
    }

    /**
     * Gets the sword type identifier (item registry name).
     * This is simpler than getSwordIdentifier - just the item type, no NBT data.
     */
    private static String getSwordType(ItemStack sword) {
        // Just use the item's toString which gives us the registry name
        return sword.getItem().toString();
    }
}
