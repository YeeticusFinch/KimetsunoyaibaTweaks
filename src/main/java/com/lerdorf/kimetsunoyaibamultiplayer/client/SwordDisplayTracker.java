package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * Tracks which swords should be displayed on which players
 * Monitors inventory changes and held item changes
 * Also tracks sheath visibility when swords are drawn
 * Now tracks by hotbar slot to support duplicate swords
 */
public class SwordDisplayTracker {
    // Per-player tracking: UUID -> SwordDisplayState
    private static final Map<UUID, SwordDisplayState> playerStates = new HashMap<>();

    // Track previous held slot to detect changes
    private static final Map<UUID, Integer> previousHeldSlots = new HashMap<>();

    // Track previous hotbar contents to detect inventory changes
    private static final Map<UUID, ItemStack[]> previousHotbarContents = new HashMap<>();

    /**
     * Represents a sword display with its source hotbar slot
     */
    public static class SlotSwordEntry {
        public int hotbarSlot;
        public ItemStack sword;
        public SwordDisplayConfig.SwordDisplayPosition displayPosition;

        public SlotSwordEntry(int slot, ItemStack sword) {
            this.hotbarSlot = slot;
            this.sword = sword.copy();
            // Get per-sword position
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(sword.getItem());
            this.displayPosition = SwordDisplayConfig.getPositionForSword(itemId.toString());
        }

        public boolean isEmpty() {
            return sword.isEmpty();
        }
    }

    public static class SwordDisplayState {
        // Now using slot-based tracking for up to 2 displayed swords
        public SlotSwordEntry leftDisplay = null;
        public SlotSwordEntry rightDisplay = null;

        // Track sheaths that should persist when sword is drawn
        public Item leftSheathItem = null;
        public Item rightSheathItem = null;
        private boolean leftSheathPersists = false;
        private boolean rightSheathPersists = false;
        // Track which slot the persisting sheath's sword came from
        private int leftSheathOriginalSlot = -1;
        private int rightSheathOriginalSlot = -1;

        public boolean hasLeftSword() {
            return leftDisplay != null && !leftDisplay.isEmpty();
        }

        public boolean hasRightSword() {
            return rightDisplay != null && !rightDisplay.isEmpty();
        }

        public ItemStack getLeftHipSword() {
            return leftDisplay != null ? leftDisplay.sword : ItemStack.EMPTY;
        }

        public ItemStack getRightHipSword() {
            return rightDisplay != null ? rightDisplay.sword : ItemStack.EMPTY;
        }

        public SwordDisplayConfig.SwordDisplayPosition getLeftPosition() {
            return leftDisplay != null ? leftDisplay.displayPosition : SwordDisplayConfig.position;
        }

        public SwordDisplayConfig.SwordDisplayPosition getRightPosition() {
            return rightDisplay != null ? rightDisplay.displayPosition : SwordDisplayConfig.position;
        }

        public boolean shouldShowLeftSheath() {
            return leftSheathItem != null && leftSheathPersists;
        }

        public boolean shouldShowRightSheath() {
            return rightSheathItem != null && rightSheathPersists;
        }

        /**
         * Checks if a specific hotbar slot is already being displayed
         */
        public boolean isSlotDisplayed(int slot) {
            return (leftDisplay != null && leftDisplay.hotbarSlot == slot) ||
                   (rightDisplay != null && rightDisplay.hotbarSlot == slot);
        }

        /**
         * Removes display for a specific hotbar slot
         */
        public boolean removeSlot(int slot) {
            if (leftDisplay != null && leftDisplay.hotbarSlot == slot) {
                leftDisplay = null;
                leftSheathItem = null;
                leftSheathPersists = false;
                leftSheathOriginalSlot = -1;
                return true;
            }
            if (rightDisplay != null && rightDisplay.hotbarSlot == slot) {
                rightDisplay = null;
                rightSheathItem = null;
                rightSheathPersists = false;
                rightSheathOriginalSlot = -1;
                return true;
            }
            return false;
        }

        /**
         * Adds a sword to display from a specific slot
         * @return true if added successfully
         */
        public boolean addSword(int slot, ItemStack sword) {
            if (isSlotDisplayed(slot)) {
                return false;
            }

            SlotSwordEntry entry = new SlotSwordEntry(slot, sword);

            if (leftDisplay == null) {
                leftDisplay = entry;
                leftSheathItem = null;
                leftSheathPersists = false;
                return true;
            } else if (rightDisplay == null) {
                rightDisplay = entry;
                rightSheathItem = null;
                rightSheathPersists = false;
                return true;
            }
            return false;
        }

        public void clear() {
            leftDisplay = null;
            rightDisplay = null;
            leftSheathItem = null;
            rightSheathItem = null;
            leftSheathPersists = false;
            rightSheathPersists = false;
            leftSheathOriginalSlot = -1;
            rightSheathOriginalSlot = -1;
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

        // Get current held slot
        int currentSlot = player.getInventory().selected;
        int previousSlot = previousHeldSlots.getOrDefault(playerUUID, -1);
        ItemStack heldItem = player.getMainHandItem();

        // Get previous hotbar contents
        ItemStack[] prevHotbar = previousHotbarContents.get(playerUUID);
        if (prevHotbar == null) {
            prevHotbar = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                prevHotbar[i] = ItemStack.EMPTY;
            }
        }

        boolean stateChanged = false;

        // Check if held slot changed
        boolean slotChanged = currentSlot != previousSlot;

        if (slotChanged && previousSlot >= 0 && previousSlot < 9) {
            ItemStack previousHeld = prevHotbar[previousSlot];

            if (Config.logDebug) {
                Log.debug("Player {} switched slots: {} -> {} (prev item: {}, new item: {})",
                    player.getName().getString(),
                    previousSlot, currentSlot,
                    previousHeld.isEmpty() ? "empty" : previousHeld.getItem().toString(),
                    heldItem.isEmpty() ? "empty" : heldItem.getItem().toString());
            }

            // If new slot has a sword that was displayed, remove it from display (drawing it)
            if (SwordParticleMapping.isKimetsunoyaibaSword(heldItem) && state.isSlotDisplayed(currentSlot)) {
                // Get sheath info before removing
                Item sheathToKeep = null;
                boolean isLeftSlot = state.leftDisplay != null && state.leftDisplay.hotbarSlot == currentSlot;
                SwordDisplayConfig.SwordDisplayPosition sheathPosition = null;

                if (isLeftSlot) {
                    SwordSheathRegistry.SheathInfo sheathInfo = SwordSheathRegistry.getSheathInfo(state.leftDisplay.sword);
                    sheathPosition = state.leftDisplay.displayPosition;
                    if (sheathInfo != null) {
                        if (sheathInfo.persistsWhenDrawn()) {
                            sheathToKeep = sheathInfo.getSheathItem();
                        } else {
                            // Spawn cloud particles for temporary sheaths (Uzui, Inosuke)
                            spawnSheathDisappearParticles(player, sheathPosition, true);
                        }
                    }
                } else if (state.rightDisplay != null && state.rightDisplay.hotbarSlot == currentSlot) {
                    SwordSheathRegistry.SheathInfo sheathInfo = SwordSheathRegistry.getSheathInfo(state.rightDisplay.sword);
                    sheathPosition = state.rightDisplay.displayPosition;
                    if (sheathInfo != null) {
                        if (sheathInfo.persistsWhenDrawn()) {
                            sheathToKeep = sheathInfo.getSheathItem();
                        } else {
                            // Spawn cloud particles for temporary sheaths (Uzui, Inosuke)
                            spawnSheathDisappearParticles(player, sheathPosition, false);
                        }
                    }
                }

                // Remove sword from display
                state.removeSlot(currentSlot);

                // Restore sheath if it should persist
                if (sheathToKeep != null) {
                    if (isLeftSlot) {
                        state.leftSheathItem = sheathToKeep;
                        state.leftSheathPersists = true;
                        state.leftSheathOriginalSlot = currentSlot;
                    } else {
                        state.rightSheathItem = sheathToKeep;
                        state.rightSheathPersists = true;
                        state.rightSheathOriginalSlot = currentSlot;
                    }
                }

                stateChanged = true;
            }

            // If previous slot had a nichirin sword, add it to display (sheathing it)
            // This now works for nichirin-to-nichirin swaps!
            if (SwordParticleMapping.isKimetsunoyaibaSword(previousHeld) &&
                !SwordParticleMapping.isSheathExempt(previousHeld) &&
                !state.isSlotDisplayed(previousSlot)) {

                if (state.addSword(previousSlot, previousHeld)) {
                    if (Config.logDebug) {
                        Log.debug("Adding sword from slot {} to display for player {}",
                            previousSlot, player.getName().getString());
                    }
                    stateChanged = true;
                }
            }
        }

        // Update previous slot tracking
        previousHeldSlots.put(playerUUID, currentSlot);

        // Check if any displayed slots' items changed (removed from hotbar)
        if (state.leftDisplay != null) {
            int slot = state.leftDisplay.hotbarSlot;
            ItemStack currentInSlot = player.getInventory().getItem(slot);
            if (!ItemStack.isSameItemSameTags(currentInSlot, state.leftDisplay.sword)) {
                if (Config.logDebug) {
                    Log.debug("Removing left display for player {} (slot {} contents changed)",
                        player.getName().getString(), slot);
                }
                state.leftDisplay = null;
                state.leftSheathItem = null;
                state.leftSheathPersists = false;
                stateChanged = true;
            }
        }

        if (state.rightDisplay != null) {
            int slot = state.rightDisplay.hotbarSlot;
            ItemStack currentInSlot = player.getInventory().getItem(slot);
            if (!ItemStack.isSameItemSameTags(currentInSlot, state.rightDisplay.sword)) {
                if (Config.logDebug) {
                    Log.debug("Removing right display for player {} (slot {} contents changed)",
                        player.getName().getString(), slot);
                }
                state.rightDisplay = null;
                state.rightSheathItem = null;
                state.rightSheathPersists = false;
                stateChanged = true;
            }
        }

        // Check sheath persistence - sheath stays only if sword is still in its original slot
        if (state.shouldShowLeftSheath() && state.leftDisplay == null) {
            // Sheath is showing but no sword displayed - check if sword is still in original slot
            boolean swordInOriginalSlot = false;
            if (state.leftSheathOriginalSlot >= 0 && state.leftSheathOriginalSlot < 9) {
                ItemStack slotItem = player.getInventory().getItem(state.leftSheathOriginalSlot);
                if (SwordParticleMapping.isKimetsunoyaibaSword(slotItem)) {
                    Item sheathItem = SwordSheathRegistry.getSheathItem(slotItem);
                    if (sheathItem == state.leftSheathItem) {
                        swordInOriginalSlot = true;
                    }
                }
            }
            if (!swordInOriginalSlot) {
                state.leftSheathItem = null;
                state.leftSheathPersists = false;
                state.leftSheathOriginalSlot = -1;
                stateChanged = true;
            }
        }

        if (state.shouldShowRightSheath() && state.rightDisplay == null) {
            // Sheath is showing but no sword displayed - check if sword is still in original slot
            boolean swordInOriginalSlot = false;
            if (state.rightSheathOriginalSlot >= 0 && state.rightSheathOriginalSlot < 9) {
                ItemStack slotItem = player.getInventory().getItem(state.rightSheathOriginalSlot);
                if (SwordParticleMapping.isKimetsunoyaibaSword(slotItem)) {
                    Item sheathItem = SwordSheathRegistry.getSheathItem(slotItem);
                    if (sheathItem == state.rightSheathItem) {
                        swordInOriginalSlot = true;
                    }
                }
            }
            if (!swordInOriginalSlot) {
                state.rightSheathItem = null;
                state.rightSheathPersists = false;
                state.rightSheathOriginalSlot = -1;
                stateChanged = true;
            }
        }

        // Update previous hotbar contents
        ItemStack[] newHotbar = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            newHotbar[i] = player.getInventory().getItem(i).copy();
        }
        previousHotbarContents.put(playerUUID, newHotbar);

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
            // Send packet to server with slot info and per-sword positions
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToServer(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordDisplaySyncPacket(
                    player.getUUID(),
                    state.getLeftHipSword(),
                    state.getRightHipSword(),
                    state.getLeftPosition(),
                    state.getRightPosition(),
                    state.leftDisplay != null ? state.leftDisplay.hotbarSlot : -1,
                    state.rightDisplay != null ? state.rightDisplay.hotbarSlot : -1
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
    public static void updateRemotePlayerDisplay(UUID playerUUID, ItemStack leftSword, ItemStack rightSword,
                                                  SwordDisplayConfig.SwordDisplayPosition leftPos,
                                                  SwordDisplayConfig.SwordDisplayPosition rightPos,
                                                  int leftSlot, int rightSlot) {
        SwordDisplayState state = playerStates.computeIfAbsent(playerUUID, k -> new SwordDisplayState());

        // Update left display
        if (!leftSword.isEmpty() && leftSlot >= 0) {
            state.leftDisplay = new SlotSwordEntry(leftSlot, leftSword);
            state.leftDisplay.displayPosition = leftPos;
        } else {
            state.leftDisplay = null;
        }

        // Update right display
        if (!rightSword.isEmpty() && rightSlot >= 0) {
            state.rightDisplay = new SlotSwordEntry(rightSlot, rightSword);
            state.rightDisplay.displayPosition = rightPos;
        } else {
            state.rightDisplay = null;
        }

        if (Config.logDebug) {
            Log.debug("Updated remote player display: UUID={}, left={}@{} ({}), right={}@{} ({})",
                playerUUID,
                leftSword.isEmpty() ? "empty" : leftSword.getItem().toString(), leftSlot, leftPos,
                rightSword.isEmpty() ? "empty" : rightSword.getItem().toString(), rightSlot, rightPos);
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
        previousHeldSlots.clear();
        previousHotbarContents.clear();
        //Log.debug("Cleared all sword display tracking data");
    }

    /**
     * Clears tracking for a specific player
     */
    public static void clearPlayer(UUID playerUUID) {
        playerStates.remove(playerUUID);
        previousHeldSlots.remove(playerUUID);
        previousHotbarContents.remove(playerUUID);
    }

    /**
     * Spawns dust particles at the sheath position when a temporary sheath disappears
     */
    private static void spawnSheathDisappearParticles(Player player, SwordDisplayConfig.SwordDisplayPosition position, boolean isLeft) {
        if (player == null || player.level() == null) return;

        // Calculate offset based on position (hip or back) and side (left or right)
        // These are in model space: +X = player's right, +Y = up, +Z = forward (in front of player)
        double offsetX, offsetY, offsetZ;

        if (position == SwordDisplayConfig.SwordDisplayPosition.BACK) {
            // Back position
            if (isLeft) {
                offsetX = SwordDisplayConfig.backLeftTranslateX;
                offsetY = SwordDisplayConfig.backLeftTranslateY;
                offsetZ = SwordDisplayConfig.backLeftTranslateZ;
            } else {
                offsetX = SwordDisplayConfig.backRightTranslateX;
                offsetY = SwordDisplayConfig.backRightTranslateY;
                offsetZ = SwordDisplayConfig.backRightTranslateZ;
            }
        } else {
            // Hip position (default)
            if (isLeft) {
                offsetX = SwordDisplayConfig.hipLeftTranslateX;
                offsetY = SwordDisplayConfig.hipLeftTranslateY;
                offsetZ = SwordDisplayConfig.hipLeftTranslateZ;
            } else {
                offsetX = SwordDisplayConfig.hipRightTranslateX;
                offsetY = SwordDisplayConfig.hipRightTranslateY;
                offsetZ = SwordDisplayConfig.hipRightTranslateZ;
            }
        }

        // Convert model space offset to world position based on player's rotation
        // Player yaw: 0 = south (+Z), 90 = west (-X), 180 = north (-Z), 270 = east (+X)
        double yawRad = Math.toRadians(-player.getYRot()); // Negate for correct rotation direction
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        // Model space to world space transformation
        // In model space: X is right, Z is forward
        // Rotate around Y axis by player's yaw
        double worldOffsetX = offsetX * cosYaw + offsetZ * sinYaw;
        double worldOffsetZ = -offsetX * sinYaw + offsetZ * cosYaw;

        // Calculate final world position
        Vec3 playerPos = player.position();
        double particleX = playerPos.x + worldOffsetX;
        double particleY = playerPos.y + 1.0 + offsetY; // Add 1.0 for player height offset
        double particleZ = playerPos.z + worldOffsetZ;

        // Spawn small dust/poof particles
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            player.level().addParticle(
                ParticleTypes.POOF,
                particleX + (rand.nextDouble() - 0.5) * 0.15,
                particleY + (rand.nextDouble() - 0.5) * 0.15,
                particleZ + (rand.nextDouble() - 0.5) * 0.15,
                0, 0, 0
            );
        }

        if (Config.logDebug) {
            Log.debug("Spawned sheath disappear particles at ({}, {}, {}) for player {}",
                particleX, particleY, particleZ, player.getName().getString());
        }
    }
}
