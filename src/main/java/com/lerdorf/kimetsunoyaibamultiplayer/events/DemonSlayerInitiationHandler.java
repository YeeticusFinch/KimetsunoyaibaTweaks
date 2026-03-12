package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles blocking/undoing the base mod's demon slayer initiation rewards.
 *
 * When disableBaseModDemonSlayerInitiation is enabled, this handler:
 * 1. Removes uniform items and nichirinsword after they're given
 * 2. Prevents kasugai_crow from spawning
 * 3. Unregisters base advancement handlers that auto-grant rank progression
 *
 * We can't prevent the base mod's event handlers from firing, but we can
 * undo what they did immediately after.
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class DemonSlayerInitiationHandler {

    // Track players who need items removed (player UUID -> tick when demon_slayer_corps was granted)
    private static final Map<UUID, Long> pendingItemRemoval = new ConcurrentHashMap<>();

    // Track players who need crow spawning blocked (player UUID -> tick when demon_slayer_corps was granted)
    // This is separate from pendingItemRemoval because we need to block crows for the full 10 ticks
    private static final Map<UUID, Long> pendingCrowBlock = new ConcurrentHashMap<>();

    // Track recently spawned crows that should be removed (entity ID -> spawn tick)
    private static final Map<Integer, Long> pendingCrowRemoval = new ConcurrentHashMap<>();

    // How many ticks to keep blocking items and crows after demon_slayer_corps is granted
    private static final int BLOCKING_DURATION_TICKS = 30;

    // Base mod item registry names
    private static final String UNIFORM_CHESTPLATE = "kimetsunoyaiba:uniform_chestplate";
    private static final String UNIFORM_LEGGINGS = "kimetsunoyaiba:uniform_leggings";
    private static final String UNIFORM_BOOTS = "kimetsunoyaiba:uniform_boots";
    private static final String NICHIRINSWORD = "kimetsunoyaiba:nichirinsword";

    // Base mod entity type
    private static final String KASUGAI_CROW = "kimetsunoyaiba:kasugai_crow";

    // Base mod advancement that triggers initiation rewards
    private static final ResourceLocation DEMON_SLAYER_CORPS = ResourceLocation.parse("kimetsunoyaiba:demon_slayer_corps");
    private static final ResourceLocation MIZUNOTO = ResourceLocation.parse("kimetsunoyaiba:mizunoto");
    private static final ResourceLocation KINOE = ResourceLocation.parse("kimetsunoyaiba:kinoe");
    private static final ResourceLocation KILL_12_MOONS = ResourceLocation.parse("kimetsunoyaiba:kill_12_moons");
    private static final ResourceLocation COMPLETED_FINAL_SELECTION = ResourceLocation.parse("kimetsunoyaibamultiplayer:completed_final_selectioni");
    private static final Set<ResourceLocation> GATED_RANK_AND_KILL_ADVANCEMENTS = Set.of(
        MIZUNOTO,
        ResourceLocation.parse("kimetsunoyaiba:mizunoe"),
        ResourceLocation.parse("kimetsunoyaiba:kanoto"),
        ResourceLocation.parse("kimetsunoyaiba:kanoe"),
        ResourceLocation.parse("kimetsunoyaiba:tsuchinoto"),
        ResourceLocation.parse("kimetsunoyaiba:tsuchinoe"),
        ResourceLocation.parse("kimetsunoyaiba:hinoto"),
        ResourceLocation.parse("kimetsunoyaiba:hinoe"),
        ResourceLocation.parse("kimetsunoyaiba:kinoto"),
        ResourceLocation.parse("kimetsunoyaiba:kinoe"),
        ResourceLocation.parse("kimetsunoyaiba:hashira"),
        ResourceLocation.parse("kimetsunoyaiba:demon_kill_count_10"),
        ResourceLocation.parse("kimetsunoyaiba:demon_kill_count_20"),
        ResourceLocation.parse("kimetsunoyaiba:demon_kill_count_30"),
        ResourceLocation.parse("kimetsunoyaiba:demon_kill_count_40"),
        ResourceLocation.parse("kimetsunoyaiba:demon_kill_count_50")
    );
    private static boolean baseAdvancementHandlersDisabled = false;
    private static final String[] BASE_ADVANCEMENT_HANDLER_CLASSES = new String[] {
        "net.mcreator.kimetsunoyaiba.procedures.SupplyProcedure",
        "net.mcreator.kimetsunoyaiba.procedures.AdvancementRewardProcedure",
        "net.mcreator.kimetsunoyaiba.procedures.CheckAdvancementDemonProcedure",
        "net.mcreator.kimetsunoyaiba.procedures.Advanvement1Procedure",
        "net.mcreator.kimetsunoyaiba.procedures.ColorChangeProcedure"
    };

    /**
     * Listen for advancement grants. When demon_slayer_corps is granted and config is enabled,
     * schedule cleanup to remove base initiation rewards.
     *
     * IMPORTANT: Uses HIGHEST priority to run BEFORE the base mod's handlers.
     * This ensures we add the player to pendingCrowBlock BEFORE the base mod spawns the crow.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)  // Run BEFORE base mod handlers
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        try {
            if (!CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
                return;
            }

            Player player = event.getEntity();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            Level level = player.level();
            if (level.getServer() == null) {
                return;
            }

            // Get the advancement from the event and compare with known advancements
            Advancement eventAdvancement = event.getAdvancement();
            if (eventAdvancement == null) {
                return;
            }

            ResourceLocation advancementId = eventAdvancement.getId();
            if (advancementId == null) {
                return;
            }

            // Rank and demon kill count progression is gated behind our custom
            // "completed_final_selectioni" advancement.
            if (GATED_RANK_AND_KILL_ADVANCEMENTS.contains(advancementId) &&
                !hasAdvancement(serverPlayer, COMPLETED_FINAL_SELECTION)) {
                revokeAdvancement(serverPlayer, eventAdvancement);

                if (CustomProgressionConfig.enableDebugLogging.get()) {
                    System.out.println("[KnY-MP Progression] Revoked gated advancement '" + advancementId +
                        "' from " + player.getName().getString() + " (missing completed_final_selectioni)");
                }
                return;
            }

            // When demon_slayer_corps is granted, schedule item/crow cleanup.
            if (advancementId.equals(DEMON_SLAYER_CORPS)) {
                long currentTick = serverPlayer.level().getGameTime();
                pendingItemRemoval.put(player.getUUID(), currentTick);
                pendingCrowBlock.put(player.getUUID(), currentTick);

                if (CustomProgressionConfig.enableDebugLogging.get()) {
                    System.out.println("[KnY-MP Progression] Player " + player.getName().getString() +
                        " earned demon_slayer_corps - scheduling item removal and crow blocking for " + BLOCKING_DURATION_TICKS + " ticks");
                }
            }

            // If kizuki progression goals fire first, ensure demon_slayer_corps is awarded so
            // custom initiation flow still executes.
            if (advancementId.equals(KINOE) || advancementId.equals(KILL_12_MOONS)) {
                ensureDemonSlayerCorpsAdvancement(serverPlayer);
            }

        } catch (Exception e) {
            // CRITICAL: Never use log4j in exception handlers - causes LinkageError crashes
            System.err.println("[KnY-MP Progression] Error in onAdvancement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prevent kasugai_crow from spawning when a player in our tracking just earned demon_slayer_corps.
     * The base mod spawns the crow immediately when the advancement is granted.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        try {
            if (!CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
                return;
            }

            Entity entity = event.getEntity();
            if (entity.level().isClientSide()) {
                return;
            }

            // Check if this is a kasugai_crow
            ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityType == null || !entityType.toString().equals(KASUGAI_CROW)) {
                return;
            }

            // Check if any player is in the crow block window (means they just got demon_slayer_corps)
            // The crow spawns near the player, so check distance
            if (!pendingCrowBlock.isEmpty()) {
                for (UUID playerUuid : pendingCrowBlock.keySet()) {
                    Player player = entity.level().getPlayerByUUID(playerUuid);
                    if (player != null && entity.distanceToSqr(player) < 400) { // Within 20 blocks (increased range)
                        // Cancel the spawn
                        event.setCanceled(true);

                        if (CustomProgressionConfig.enableDebugLogging.get()) {
                            System.out.println("[KnY-MP Progression] Prevented kasugai_crow spawn near " +
                                player.getName().getString() + " (distance: " + Math.sqrt(entity.distanceToSqr(player)) + " blocks)");
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // CRITICAL: Never use log4j in exception handlers - causes LinkageError crashes
            System.err.println("[KnY-MP Progression] Error in onEntityJoinWorld: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * On server tick, process pending item removals and crow removals.
     * We check every tick during the blocking window to catch items given at different times.
     *
     * IMPORTANT: This method is wrapped in try-catch to prevent log4j LinkageError crashes.
     * Never use log4j/LOGGER in exception handlers - use System.err.println() instead.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            if (!CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
                // Clear any pending operations if config was disabled
                pendingItemRemoval.clear();
                pendingCrowBlock.clear();
                pendingCrowRemoval.clear();
                baseAdvancementHandlersDisabled = false;
                return;
            }

            if (event.getServer() == null) {
                return;
            }

            // Disable the base mod's advancement handlers once per server run when enabled.
            disableBaseAdvancementHandlers();

            long currentTick = event.getServer().overworld().getGameTime();

            // Process pending item removals every tick during the blocking window
            Iterator<Map.Entry<UUID, Long>> itemIter = pendingItemRemoval.entrySet().iterator();
            while (itemIter.hasNext()) {
                Map.Entry<UUID, Long> entry = itemIter.next();
                long ticksSinceGrant = currentTick - entry.getValue();
                UUID playerUuid = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUuid);

                if (player != null) {
                    // Remove items every tick during the blocking window
                    int itemsRemoved = removeInitiationItems(player);

                    // Also try to remove any crows that slipped through
                    removeRecentlyTamedCrows(player);

                    if (CustomProgressionConfig.enableDebugLogging.get() && itemsRemoved > 0) {
                        System.out.println("[KnY-MP Progression] Tick " + ticksSinceGrant + "/" + BLOCKING_DURATION_TICKS +
                            ": Removed " + itemsRemoved + " initiation items from " + player.getName().getString());
                    }
                }

                // Remove from tracking after the blocking window
                if (ticksSinceGrant >= BLOCKING_DURATION_TICKS) {
                    itemIter.remove();

                    // Grant training sword if enabled
                    if (player != null && CustomProgressionConfig.grantTrainingSword.get()) {
                        grantTrainingSword(player);
                    }

                    if (CustomProgressionConfig.enableDebugLogging.get()) {
                        System.out.println("[KnY-MP Progression] Finished blocking initiation for " +
                            (player != null ? player.getName().getString() : playerUuid));
                    }
                }
            }

            // Explicitly try to remove crows at 10, 20, and 30 ticks after advancement
            // This catches crows that might spawn with a delay
            Iterator<Map.Entry<UUID, Long>> crowIter = pendingCrowBlock.entrySet().iterator();
            while (crowIter.hasNext()) {
                Map.Entry<UUID, Long> entry = crowIter.next();
                long ticksSinceGrant = currentTick - entry.getValue();
                UUID playerUuid = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUuid);

                // Try removing crows at specific intervals: 10, 20, and 30 ticks
                if (player != null && (ticksSinceGrant == 10 || ticksSinceGrant == 20 || ticksSinceGrant == 30)) {
                    removeRecentlyTamedCrows(player);
                    if (CustomProgressionConfig.enableDebugLogging.get()) {
                        System.out.println("[KnY-MP Progression] Crow removal attempt at tick " + ticksSinceGrant +
                            " for " + player.getName().getString());
                    }
                }

                // Clean up after the blocking window
                if (ticksSinceGrant >= BLOCKING_DURATION_TICKS) {
                    crowIter.remove();
                }
            }

        } catch (Exception e) {
            // CRITICAL: Never use log4j in exception handlers - causes LinkageError crashes
            // Always use System.err.println() instead
            System.err.println("[KnY-MP Progression] Error in onServerTick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Remove the initiation items from a player's inventory.
     * @return The number of items removed
     */
    private static int removeInitiationItems(ServerPlayer player) {
        int removedCount = 0;

        // Get the item references from the registry
        Item chestplate = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(UNIFORM_CHESTPLATE));
        Item leggings = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(UNIFORM_LEGGINGS));
        Item boots = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(UNIFORM_BOOTS));
        Item sword = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(NICHIRINSWORD));

        // Remove from inventory (check all slots)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            if (item == chestplate || item == leggings || item == boots || item == sword) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                removedCount++;
            }
        }

        // Also check armor slots
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            if (item == chestplate || item == leggings || item == boots) {
                player.getInventory().armor.set(i, ItemStack.EMPTY);
                removedCount++;
            }
        }

        return removedCount;
    }

    /**
     * Grant a training sword to the player.
     * Called when grantTrainingSword config is enabled and initiation blocking finishes.
     *
     * IMPORTANT: This method catches all exceptions to prevent log4j LinkageError crashes.
     */
    private static void grantTrainingSword(ServerPlayer player) {
        try {
            if (CustomProgressionConfig.enableDebugLogging.get()) {
                System.out.println("[KnY-MP Progression] Attempting to grant training sword to " + player.getName().getString());
            }

            // Get the nichirinsword item from the base mod
            Item nichirinsword = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(NICHIRINSWORD));
            if (nichirinsword == null) {
                System.err.println("[KnY-MP Progression] Could not find nichirinsword item (" + NICHIRINSWORD + ") to grant training sword");
                return;
            }

            if (CustomProgressionConfig.enableDebugLogging.get()) {
                System.out.println("[KnY-MP Progression] Found nichirinsword item: " + ForgeRegistries.ITEMS.getKey(nichirinsword));
            }

            // Create a new nichirinsword and convert it to a training sword
            ItemStack trainingSword = new ItemStack(nichirinsword);

            if (CustomProgressionConfig.enableDebugLogging.get()) {
                System.out.println("[KnY-MP Progression] Created ItemStack, checking if it's a valid sword...");
                System.out.println("[KnY-MP Progression] isNichirinOrBreathingSword: " + TrainingSwordHelper.isNichirinOrBreathingSword(trainingSword));
            }

            boolean success = TrainingSwordHelper.makeTrainingSword(trainingSword, player);

            if (success) {
                // Give the training sword to the player
                if (!player.getInventory().add(trainingSword)) {
                    // If inventory is full, drop it at player's feet
                    player.drop(trainingSword, false);
                }

                System.out.println("[KnY-MP Progression] Granted training sword to " + player.getName().getString());

                // Also grant Ubuyashiki's Invitation to guide them to the Toril Gate
                try {
                    ItemStack invitation = new ItemStack(com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems.UBUYASHIKI_INVITATION.get());
                    if (!player.getInventory().add(invitation)) {
                        player.drop(invitation, false);
                    }
                    System.out.println("[KnY-MP Progression] Granted Ubuyashiki's Invitation to " + player.getName().getString());
                } catch (Exception ex) {
                    System.err.println("[KnY-MP Progression] Failed to grant Ubuyashiki's Invitation: " + ex.getMessage());
                }
            } else {
                System.err.println("[KnY-MP Progression] Failed to create training sword for " + player.getName().getString());
                System.err.println("[KnY-MP Progression] ItemStack: " + trainingSword + ", Item: " + trainingSword.getItem());
            }
        } catch (Exception e) {
            // CRITICAL: Never use log4j in exception handlers - causes LinkageError crashes
            System.err.println("[KnY-MP Progression] Error in grantTrainingSword: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void disableBaseAdvancementHandlers() {
        if (baseAdvancementHandlersDisabled) {
            return;
        }

        int disabledCount = 0;

        for (String className : BASE_ADVANCEMENT_HANDLER_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                MinecraftForge.EVENT_BUS.unregister(clazz);
                disabledCount++;

                if (CustomProgressionConfig.enableDebugLogging.get()) {
                    System.out.println("[KnY-MP Progression] Disabled base advancement handler: " + className);
                }
            } catch (ClassNotFoundException ignored) {
                // Different base-mod version; ignore missing classes.
            } catch (Exception e) {
                System.err.println("[KnY-MP Progression] Failed to disable base advancement handler '" +
                    className + "': " + e.getMessage());
            }
        }

        baseAdvancementHandlersDisabled = true;

        if (CustomProgressionConfig.enableDebugLogging.get()) {
            System.out.println("[KnY-MP Progression] Base advancement handlers disabled: " + disabledCount);
        }
    }

    private static void ensureDemonSlayerCorpsAdvancement(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }

        Advancement demonSlayerCorpsAdv = player.server.getAdvancements().getAdvancement(DEMON_SLAYER_CORPS);
        if (demonSlayerCorpsAdv == null) {
            return;
        }

        if (player.getAdvancements().getOrStartProgress(demonSlayerCorpsAdv).isDone()) {
            return;
        }

        List<String> remainingCriteria = new ArrayList<String>();
        for (String criterion : player.getAdvancements().getOrStartProgress(demonSlayerCorpsAdv).getRemainingCriteria()) {
            remainingCriteria.add(criterion);
        }
        for (String criterion : remainingCriteria) {
            player.getAdvancements().award(demonSlayerCorpsAdv, criterion);
        }

        if (CustomProgressionConfig.enableDebugLogging.get()) {
            System.out.println("[KnY-MP Progression] Awarded demon_slayer_corps to " +
                player.getName().getString() + " after kizuki progression advancement");
        }
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player.getServer() == null) {
            return false;
        }

        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return false;
        }

        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void revokeAdvancement(ServerPlayer player, Advancement advancement) {
        List<String> completedCriteria = new ArrayList<String>();
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria()) {
            completedCriteria.add(criterion);
        }
        for (String criterion : completedCriteria) {
            player.getAdvancements().revoke(advancement, criterion);
        }
    }

    /**
     * Called when a player logs out - clean up their tracking data.
     */
    public static void onPlayerLogout(UUID playerUuid) {
        pendingItemRemoval.remove(playerUuid);
        pendingCrowBlock.remove(playerUuid);
    }

    /**
     * Clean up any crows near a player that were spawned by the base mod.
     * Call this if a crow slipped through the EntityJoinLevelEvent check.
     *
     * This removes BOTH tamed and untamed crows near the player, because:
     * - The base mod spawns the crow first, then tames it a few ticks later
     * - If we only check for tamed crows, we miss the window before taming completes
     */
    public static void removeRecentlyTamedCrows(ServerPlayer player) {
        try {
            if (!CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
                return;
            }

            ServerLevel level = player.serverLevel();
            ResourceLocation crowType = ResourceLocation.parse(KASUGAI_CROW);

            // Find all kasugai_crow entities near the player
            // Remove if: tamed to this player OR untamed and very close (within 10 blocks)
            level.getEntities().getAll().forEach(entity -> {
                try {
                    ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    if (entityType != null && entityType.equals(crowType)) {
                        double distanceSq = entity.distanceToSqr(player);

                        // Check if this crow should be removed
                        boolean shouldRemove = false;
                        String reason = "";

                        if (entity instanceof TamableAnimal tamable) {
                            // Remove if tamed to this player (within 20 blocks)
                            if (tamable.getOwnerUUID() != null &&
                                tamable.getOwnerUUID().equals(player.getUUID()) &&
                                distanceSq < 400) {
                                shouldRemove = true;
                                reason = "tamed to player";
                            }
                            // Also remove untamed crows very close to the player (within 10 blocks)
                            // These are likely just-spawned crows that haven't been tamed yet
                            else if (tamable.getOwnerUUID() == null && distanceSq < 100) {
                                shouldRemove = true;
                                reason = "untamed near player";
                            }
                        }

                        if (shouldRemove) {
                            entity.discard();

                            if (CustomProgressionConfig.enableDebugLogging.get()) {
                                System.out.println("[KnY-MP Progression] Removed kasugai_crow (" + reason + ") from " +
                                    player.getName().getString() + " at distance " + Math.sqrt(distanceSq));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Silent fail for individual entities
                }
            });
        } catch (Exception e) {
            // CRITICAL: Never use log4j in exception handlers - causes LinkageError crashes
            System.err.println("[KnY-MP Progression] Error in removeRecentlyTamedCrows: " + e.getMessage());
        }
    }
}
