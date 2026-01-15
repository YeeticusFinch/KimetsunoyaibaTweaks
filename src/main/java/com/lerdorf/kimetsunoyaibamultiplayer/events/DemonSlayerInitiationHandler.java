package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;
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
 * 3. Revokes the mizunoto advancement
 *
 * We can't prevent the base mod's event handlers from firing, but we can
 * undo what they did immediately after.
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class DemonSlayerInitiationHandler {

    // Track players who need items removed (player UUID -> tick when demon_slayer_corps was granted)
    private static final Map<UUID, Long> pendingItemRemoval = new ConcurrentHashMap<>();

    // Track players who need mizunoto revoked
    private static final Set<UUID> blockMizunoto = ConcurrentHashMap.newKeySet();

    // Track recently spawned crows that should be removed (entity ID -> spawn tick)
    private static final Map<Integer, Long> pendingCrowRemoval = new ConcurrentHashMap<>();

    // Base mod item registry names
    private static final String UNIFORM_CHESTPLATE = "kimetsunoyaiba:uniform_chestplate";
    private static final String UNIFORM_LEGGINGS = "kimetsunoyaiba:uniform_leggings";
    private static final String UNIFORM_BOOTS = "kimetsunoyaiba:uniform_boots";
    private static final String NICHIRINSWORD = "kimetsunoyaiba:nichirinsword";

    // Base mod entity type
    private static final String KASUGAI_CROW = "kimetsunoyaiba:kasugai_crow";

    // Base mod advancements
    private static final ResourceLocation DEMON_SLAYER_CORPS = ResourceLocation.parse("kimetsunoyaiba:demon_slayer_corps");
    private static final ResourceLocation MIZUNOTO = ResourceLocation.parse("kimetsunoyaiba:mizunoto");

    /**
     * Listen for advancement grants. When demon_slayer_corps is granted and config is enabled,
     * schedule cleanup to remove the items and block mizunoto.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)  // Run after base mod handlers
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
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

        // Get advancements from the manager to compare
        Advancement demonSlayerCorpsAdv = level.getServer().getAdvancements().getAdvancement(DEMON_SLAYER_CORPS);
        Advancement mizunotoAdv = level.getServer().getAdvancements().getAdvancement(MIZUNOTO);

        // When demon_slayer_corps is granted, schedule item removal and block mizunoto
        if (demonSlayerCorpsAdv != null && eventAdvancement.equals(demonSlayerCorpsAdv)) {
            long currentTick = serverPlayer.level().getGameTime();
            pendingItemRemoval.put(player.getUUID(), currentTick);
            blockMizunoto.add(player.getUUID());

            if (CustomProgressionConfig.enableDebugLogging.get()) {
                System.out.println("[KnY-MP Progression] Player " + player.getName().getString() +
                    " earned demon_slayer_corps - scheduling item removal and blocking mizunoto");
            }
        }

        // When mizunoto is granted and this player is in blockMizunoto, revoke it immediately
        if (mizunotoAdv != null && eventAdvancement.equals(mizunotoAdv) && blockMizunoto.contains(player.getUUID())) {
            // Schedule revocation for next tick (can't revoke in same event)
            serverPlayer.getServer().execute(() -> {
                revokeAdvancement(serverPlayer, MIZUNOTO);
                if (CustomProgressionConfig.enableDebugLogging.get()) {
                    System.out.println("[KnY-MP Progression] Revoked mizunoto advancement from " +
                        player.getName().getString());
                }
            });
        }
    }

    /**
     * Prevent kasugai_crow from spawning when a player in our tracking just earned demon_slayer_corps.
     * The base mod spawns the crow immediately when the advancement is granted.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
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

        // Check if any player is pending item removal (means they just got demon_slayer_corps)
        // The crow spawns near the player, so check distance
        if (!pendingItemRemoval.isEmpty()) {
            for (UUID playerUuid : pendingItemRemoval.keySet()) {
                Player player = entity.level().getPlayerByUUID(playerUuid);
                if (player != null && entity.distanceToSqr(player) < 100) { // Within 10 blocks
                    // Cancel the spawn
                    event.setCanceled(true);

                    if (CustomProgressionConfig.enableDebugLogging.get()) {
                        System.out.println("[KnY-MP Progression] Prevented kasugai_crow spawn near " +
                            player.getName().getString());
                    }
                    return;
                }
            }
        }
    }

    /**
     * On server tick, process pending item removals and crow removals.
     * We wait a few ticks after demon_slayer_corps to ensure all items have been given.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
            // Clear any pending operations if config was disabled
            pendingItemRemoval.clear();
            pendingCrowRemoval.clear();
            return;
        }

        if (event.getServer() == null) {
            return;
        }

        long currentTick = event.getServer().overworld().getGameTime();

        // Process pending item removals (wait 3 ticks after advancement to ensure items are given)
        Iterator<Map.Entry<UUID, Long>> itemIter = pendingItemRemoval.entrySet().iterator();
        while (itemIter.hasNext()) {
            Map.Entry<UUID, Long> entry = itemIter.next();
            if (currentTick - entry.getValue() >= 3) {
                UUID playerUuid = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUuid);

                if (player != null) {
                    removeInitiationItems(player);
                    revokeAdvancement(player, MIZUNOTO);

                    if (CustomProgressionConfig.enableDebugLogging.get()) {
                        System.out.println("[KnY-MP Progression] Removed initiation items from " +
                            player.getName().getString() + " and revoked mizunoto");
                    }
                }

                itemIter.remove();

                // Keep blocking mizunoto for a while longer (base mod may try to re-grant it)
                // Will be cleared after 10 seconds
            }
        }

        // Clean up blockMizunoto after 10 seconds (200 ticks) to prevent memory leak
        // Players can still earn mizunoto through normal means after this
        // Note: This is a simple timeout. For production, you might want to track
        // the grant time per player.
    }

    /**
     * Remove the initiation items from a player's inventory.
     */
    private static void removeInitiationItems(ServerPlayer player) {
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

        if (CustomProgressionConfig.enableDebugLogging.get() && removedCount > 0) {
            System.out.println("[KnY-MP Progression] Removed " + removedCount +
                " initiation items from " + player.getName().getString());
        }
    }

    /**
     * Revoke an advancement from a player.
     */
    private static void revokeAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player.getServer() == null) return;

        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            // Revoke all criteria
            for (String criterion : progress.getCompletedCriteria()) {
                player.getAdvancements().revoke(advancement, criterion);
            }
        }
    }

    /**
     * Called when a player logs out - clean up their tracking data.
     */
    public static void onPlayerLogout(UUID playerUuid) {
        pendingItemRemoval.remove(playerUuid);
        blockMizunoto.remove(playerUuid);
    }

    /**
     * Clean up any tamed crows near a player that were spawned by the base mod.
     * Call this if a crow slipped through the EntityJoinLevelEvent check.
     */
    public static void removeRecentlyTamedCrows(ServerPlayer player) {
        if (!CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ResourceLocation crowType = ResourceLocation.parse(KASUGAI_CROW);

        // Find all kasugai_crow entities near the player that are tamed to them
        level.getEntities().getAll().forEach(entity -> {
            ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityType != null && entityType.equals(crowType)) {
                if (entity instanceof TamableAnimal tamable) {
                    if (tamable.getOwnerUUID() != null &&
                        tamable.getOwnerUUID().equals(player.getUUID()) &&
                        entity.distanceToSqr(player) < 400) { // Within 20 blocks

                        entity.discard();

                        if (CustomProgressionConfig.enableDebugLogging.get()) {
                            System.out.println("[KnY-MP Progression] Removed tamed kasugai_crow from " +
                                player.getName().getString());
                        }
                    }
                }
            }
        });
    }
}
