package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping swords to their sheath items and behavior.
 */
public class SwordSheathRegistry {

    /**
     * Maps sword items to their sheath items and persistence behavior
     */
    private static final Map<Item, SheathInfo> swordSheathMap = new HashMap<>();

    /**
     * The default sheath item used for swords without a specific sheath defined
     */
    private static Item defaultSheathItem = null;

    /**
     * Information about a sword's sheath
     */
    public static class SheathInfo {
        private final Item sheathItem;
        private final boolean persistsWhenDrawn;

        public SheathInfo(Item sheathItem, boolean persistsWhenDrawn) {
            this.sheathItem = sheathItem;
            this.persistsWhenDrawn = persistsWhenDrawn;
        }

        public Item getSheathItem() {
            return sheathItem;
        }

        public boolean persistsWhenDrawn() {
            return persistsWhenDrawn;
        }
    }

    /**
     * Sets the default sheath item for all swords
     */
    public static void setDefaultSheath(Item sheathItem) {
        defaultSheathItem = sheathItem;
        Log.debug("Set default sheath item: {}", sheathItem);
    }

    /**
     * Registers a sheath for a specific sword
     */
    public static void registerSheath(Item sword, Item sheathItem, boolean persistsWhenDrawn) {
        swordSheathMap.put(sword, new SheathInfo(sheathItem, persistsWhenDrawn));
        Log.debug("Registered sheath {} for sword {} (persists: {})",
            sheathItem, sword, persistsWhenDrawn);
    }

    /**
     * Registers a sheath that persists when the sword is drawn
     */
    public static void registerPersistentSheath(Item sword, Item sheathItem) {
        registerSheath(sword, sheathItem, true);
    }

    /**
     * Registers a sheath that disappears when the sword is drawn
     */
    public static void registerTemporarySheath(Item sword, Item sheathItem) {
        registerSheath(sword, sheathItem, false);
    }

    /**
     * Gets the sheath info for a sword, or the default sheath if none is registered
     */
    public static SheathInfo getSheathInfo(ItemStack swordStack) {
        if (swordStack.isEmpty()) {
            return defaultSheathItem != null ? new SheathInfo(defaultSheathItem, true) : null;
        }

        SheathInfo customSheath = swordSheathMap.get(swordStack.getItem());
        if (customSheath != null) {
            return customSheath;
        }

        // Return default sheath with persistent behavior
        return defaultSheathItem != null ? new SheathInfo(defaultSheathItem, true) : null;
    }

    /**
     * Gets the sheath item for a sword
     */
    public static Item getSheathItem(ItemStack swordStack) {
        SheathInfo info = getSheathInfo(swordStack);
        return info != null ? info.getSheathItem() : null;
    }

    /**
     * Checks if a sword's sheath should persist when the sword is drawn
     */
    public static boolean sheathPersistsWhenDrawn(ItemStack swordStack) {
        SheathInfo info = getSheathInfo(swordStack);
        return info != null && info.persistsWhenDrawn();
    }

    /**
     * Clears all registered sheaths (for cleanup)
     */
    public static void clear() {
        swordSheathMap.clear();
        defaultSheathItem = null;
        Log.debug("Cleared all sword sheath registrations");
    }
}