package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/**
 * Tracks which swords should be displayed on which players
 * Monitors inventory changes and held item changes
 * Also tracks sheath visibility when swords are drawn
 */
public class SwordDisplayTracker {
    // Per-player tracking: UUID -> SwordDisplayState
    private static final Map<UUID, SwordDisplayState> playerStates = new HashMap<>();

    // Track previous held items to detect changes
    private static final Map<UUID, ItemStack> previousHeldItems = new HashMap<>();

    public static class SwordDisplayState {
        public ItemStack leftHipSword = ItemStack.EMPTY;
        public ItemStack rightHipSword = ItemStack.EMPTY;

        // Track sheaths that should persist when sword is drawn
        public Item leftSheathItem = null;
        public Item rightSheathItem = null;
        private boolean leftSheathPersists = false;
        private boolean rightSheathPersists = false;

        public boolean hasLeftSword() {
            return !leftHipSword.isEmpty();
        }

        public boolean hasRightSword() {
            return !rightHipSword.isEmpty();
        }

        public boolean shouldShowLeftSheath() {
            return leftSheathItem != null && leftSheathPersists;
        }

        public boolean shouldShowRightSheath() {
            return rightSheathItem != null && rightSheathPersists;
        }

        public void clear() {
            leftHipSword = ItemStack.EMPTY;
            rightHipSword = ItemStack.EMPTY;
            leftSheathItem = null;
            rightSheathItem = null;
            leftSheathPersists = false;
            rightSheathPersists = false;
        }
    }

    /**
     * Called every client tick to update sword display states
     */
    public static void tick() {
        if (!SwordDisplayConfig.enabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        // Update for all players in view
        for (Player player : mc.level.players()) {
            updatePlayerSwordDisplay(player);
        }
    }

    /**
     * Updates the sword display state for a single player
     */
    private static void updatePlayerSwordDisplay(Player player) {
        UUID playerUUID = player.getUUID();

        // Get or create state
        SwordDisplayState state = playerStates.computeIfAbsent(playerUUID, k -> new SwordDisplayState());

        // Get currently held item
        ItemStack heldItem = player.getMainHandItem();
        ItemStack previousHeld = previousHeldItems.getOrDefault(playerUUID, ItemStack.EMPTY);

        // Check if held item changed
        boolean heldChanged = !ItemStack.matches(heldItem, previousHeld);

        if (heldChanged) {
            if (Config.logDebug) {
                Log.debug("Player {} held item changed: {} -> {}",
                    player.getName().getString(),
                    previousHeld.isEmpty() ? "empty" : previousHeld.getItem().toString(),
                    heldItem.isEmpty() ? "empty" : heldItem.getItem().toString());
            }

            // If player just started holding a sword that was displayed, remove sword but keep sheath if it persists
            if (SwordParticleMapping.isKimetsunoyaibaSword(heldItem)) {
                if (ItemStack.isSameItemSameTags(heldItem, state.leftHipSword)) {
                    // Sword is being drawn - check if sheath should persist
                    SwordSheathRegistry.SheathInfo sheathInfo = SwordSheathRegistry.getSheathInfo(state.leftHipSword);
                    if (sheathInfo != null) {
                        state.leftSheathItem = sheathInfo.getSheathItem();
                        state.leftSheathPersists = sheathInfo.persistsWhenDrawn();
                    }

                    state.leftHipSword = ItemStack.EMPTY;
                    sendDisplayUpdateToServer(player, state);
                } else if (ItemStack.isSameItemSameTags(heldItem, state.rightHipSword)) {
                    // Sword is being drawn - check if sheath should persist
                    SwordSheathRegistry.SheathInfo sheathInfo = SwordSheathRegistry.getSheathInfo(state.rightHipSword);
                    if (sheathInfo != null) {
                        state.rightSheathItem = sheathInfo.getSheathItem();
                        state.rightSheathPersists = sheathInfo.persistsWhenDrawn();
                    }

                    state.rightHipSword = ItemStack.EMPTY;
                    sendDisplayUpdateToServer(player, state);
                }
            }

            // If player just stopped holding a sword, add it to display
            // Skip sheath-exempt items (like Himejima's axe and ball)
            if (SwordParticleMapping.isKimetsunoyaibaSword(previousHeld) &&
                !SwordParticleMapping.isSheathExempt(previousHeld) &&
                !SwordParticleMapping.isKimetsunoyaibaSword(heldItem) &&
                hasItemInInventory(player, previousHeld)) {

                // Add to left hip if empty, otherwise right hip
                if (!state.hasLeftSword()) {
                    state.leftHipSword = previousHeld.copy();
                    state.leftSheathItem = null;  // Clear sheath when sword is sheathed
                    state.leftSheathPersists = false;
                    if (Config.logDebug) {
                        Log.debug("Adding sword to left hip for player {}", player.getName().getString());
                    }
                } else if (!state.hasRightSword()) {
                    state.rightHipSword = previousHeld.copy();
                    state.rightSheathItem = null;  // Clear sheath when sword is sheathed
                    state.rightSheathPersists = false;
                    if (Config.logDebug) {
                        Log.debug("Adding sword to right hip for player {}", player.getName().getString());
                    }
                }
                sendDisplayUpdateToServer(player, state);
            }

            previousHeldItems.put(playerUUID, heldItem.copy());
        }

        // Check if displayed swords or persistent sheaths are still in hotbar
        boolean stateChanged = false;

        // Check left sword/sheath
        if (state.hasLeftSword() && !hasItemInHotbar(player, state.leftHipSword)) {
            if (Config.logDebug) {
                Log.debug("Removing left hip sword for player {} (not in hotbar)", player.getName().getString());
            }
            state.leftHipSword = ItemStack.EMPTY;
            state.leftSheathItem = null;
            state.leftSheathPersists = false;
            stateChanged = true;
        } else if (state.shouldShowLeftSheath()) {
            // If only sheath is showing, check if the sword is still in hotbar
            ItemStack swordForSheath = findSwordWithSheath(player, state.leftSheathItem);
            if (swordForSheath.isEmpty() || !hasItemInHotbar(player, swordForSheath)) {
                state.leftSheathItem = null;
                state.leftSheathPersists = false;
                stateChanged = true;
            }
        }

        // Check right sword/sheath
        if (state.hasRightSword() && !hasItemInHotbar(player, state.rightHipSword)) {
            if (Config.logDebug) {
                Log.debug("Removing right hip sword for player {} (not in hotbar)", player.getName().getString());
            }
            state.rightHipSword = ItemStack.EMPTY;
            state.rightSheathItem = null;
            state.rightSheathPersists = false;
            stateChanged = true;
        } else if (state.shouldShowRightSheath()) {
            // If only sheath is showing, check if the sword is still in hotbar
            ItemStack swordForSheath = findSwordWithSheath(player, state.rightSheathItem);
            if (swordForSheath.isEmpty() || !hasItemInHotbar(player, swordForSheath)) {
                state.rightSheathItem = null;
                state.rightSheathPersists = false;
                stateChanged = true;
            }
        }

        if (stateChanged) {
            sendDisplayUpdateToServer(player, state);
        }
    }

    /**
     * Finds a sword in the player's inventory that uses the given sheath item
     */
    private static ItemStack findSwordWithSheath(Player player, Item sheathItem) {
        if (sheathItem == null) {
            return ItemStack.EMPTY;
        }

        for (ItemStack invItem : player.getInventory().items) {
            if (SwordParticleMapping.isKimetsunoyaibaSword(invItem)) {
                Item itemSheathItem = SwordSheathRegistry.getSheathItem(invItem);
                if (sheathItem == itemSheathItem) {
                    return invItem;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Checks if a player has a specific item in their hotbar (slots 0-8 only)
     * This ensures swords are removed from display when moved to main inventory
     */
    private static boolean hasItemInHotbar(Player player, ItemStack itemToFind) {
        if (itemToFind.isEmpty()) {
            return false;
        }

        // Check only hotbar slots (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarItem = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(hotbarItem, itemToFind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a player has a specific item in their entire inventory
     */
    private static boolean hasItemInInventory(Player player, ItemStack itemToFind) {
        if (itemToFind.isEmpty()) {
            return false;
        }

        for (ItemStack invItem : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(invItem, itemToFind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a display update to the server for syncing with other clients
     */
    private static void sendDisplayUpdateToServer(Player player, SwordDisplayState state) {
        // Only send if this is the local player
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(player.getUUID())) {
            // Send packet to server
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordDisplaySyncPacket(
                    player.getUUID(),
                    state.leftHipSword.copy(),
                    state.rightHipSword.copy(),
                    SwordDisplayConfig.position
                )
            );

            if (Config.logDebug) {
                Log.debug("Sent sword display update to server for player {}", player.getName().getString());
            }
        }
    }

    /**
     * Updates the display state for a remote player (called when receiving network packet)
     */
    public static void updateRemotePlayerDisplay(UUID playerUUID, ItemStack leftSword, ItemStack rightSword) {
        SwordDisplayState state = playerStates.computeIfAbsent(playerUUID, k -> new SwordDisplayState());
        state.leftHipSword = leftSword.copy();
        state.rightHipSword = rightSword.copy();

        if (Config.logDebug) {
            Log.debug("Updated remote player display: UUID={}, left={}, right={}",
                playerUUID,
                leftSword.isEmpty() ? "empty" : leftSword.getItem().toString(),
                rightSword.isEmpty() ? "empty" : rightSword.getItem().toString());
        }
    }

    /**
     * Gets the current display state for a player
     */
    public static SwordDisplayState getDisplayState(UUID playerUUID) {
        return playerStates.getOrDefault(playerUUID, new SwordDisplayState());
    }

    /**
     * Clears all tracked sword displays (called on disconnect)
     */
    public static void clearAll() {
        playerStates.clear();
        previousHeldItems.clear();
        Log.debug("Cleared all sword display tracking data");
    }

    /**
     * Clears tracking for a specific player
     */
    public static void clearPlayer(UUID playerUUID) {
        playerStates.remove(playerUUID);
        previousHeldItems.remove(playerUUID);
    }
}
