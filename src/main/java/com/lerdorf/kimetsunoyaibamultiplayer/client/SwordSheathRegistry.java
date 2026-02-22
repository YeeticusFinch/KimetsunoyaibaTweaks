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
     * Maps sword items to alternate display items when sheathed.
     * e.g., sword_kokushibo_2 displays as sword_kokushibo_1 when on hip/back.
     */
    private static final Map<Item, Item> sheathDisplayOverrides = new HashMap<>();

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
     * Registers a display override so that when a sword is sheathed, it renders
     * as a different item model. For example, sword_kokushibo_2 can display as
     * sword_kokushibo_1 when on the player's hip/back.
     *
     * @param sword The sword item that should be overridden
     * @param displayAs The item whose model should be rendered instead
     */
    public static void registerSheathDisplayOverride(Item sword, Item displayAs) {
        sheathDisplayOverrides.put(sword, displayAs);
        Log.debug("Registered sheath display override: {} displays as {}", sword, displayAs);
    }

    /**
     * Gets the display override item for a sword when sheathed.
     * If no override is registered, returns the original sword's item.
     *
     * @param swordStack The sword being displayed
     * @return An ItemStack with the display override item, or the original stack if no override
     */
    public static ItemStack getSheathDisplayItem(ItemStack swordStack) {
        if (swordStack.isEmpty()) {
            return swordStack;
        }
        Item override = sheathDisplayOverrides.get(swordStack.getItem());
        if (override != null) {
            return new ItemStack(override);
        }
        return swordStack;
    }

    /**
     * Clears all registered sheaths (for cleanup)
     */
    public static void clear() {
        swordSheathMap.clear();
        sheathDisplayOverrides.clear();
        defaultSheathItem = null;
        //Log.debug("Cleared all sword sheath registrations");
    }
}