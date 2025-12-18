package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles execution of variations for base KimetsunoYaiba mod swords.
 * When a player right-clicks with a base mod sword that has a variation selected,
 * this handler executes the variation's effect.
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID)
public class BaseModVariationHandler {

    // Track cooldowns per player per form ID
    // Map: Player UUID -> (Form ID -> Cooldown end time in milliseconds)
    private static final Map<UUID, Map<Integer, Long>> formCooldowns = new HashMap<>();

    // Track last execution time to prevent double-triggering
    private static long lastExecutionTime = 0;
    private static final long DOUBLE_TRIGGER_COOLDOWN_MS = 100; // 100ms cooldown to prevent double-triggering

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();

        // Only handle base mod nichirin swords (not our custom BreathingSwordItem)
        if (heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) {
            return; // Our custom swords are handled elsewhere
        }

        if (!BreathingInfoDetector.isNichirinSword(heldItem)) {
            return; // Not a nichirin sword
        }

        // Prevent double-triggering from client/server
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExecutionTime < DOUBLE_TRIGGER_COOLDOWN_MS) {
            return;
        }

        // Only execute on server side
        if (player.level().isClientSide()) {
            return;
        }

        // Prefer the current NBT form for the active sword; overlay/use cache only if same base form
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());
        double nbtBreathes = player.getPersistentData().getDouble("breathes");
        double cachedBreathes = data.getBaseModBreathesValue();
        double breathes = nbtBreathes;

        if (breathes == 0.0 && cachedBreathes > 0.0) {
            breathes = cachedBreathes;
        } else if (breathes > 0.0 && cachedBreathes > 0.0) {
            int nbtFormId = com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping.getFormId(breathes);
            int cachedFormId = com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping.getFormId(cachedBreathes);
            if (nbtFormId == cachedFormId) {
                // Use encoded cached value if it matches the same base form
                breathes = cachedBreathes;
            }
        }

        if (breathes == 0.0) {
            return; // No form selected
        }

        // CRITICAL: Correct out-of-range breathes values (e.g., 602 → 102)
        // Base mod sometimes writes form ID with +500 offset
        int expectedRange = com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping.getExpectedRangeForSword(heldItem);
        int actualRange = ((int)breathes / 100) * 100;
        if (expectedRange > 0 && actualRange == expectedRange + 500) {
            breathes = breathes - 500;
            if (Config.logDebug) {
                Log.debug("[BaseModVariationHandler] Corrected out-of-range breathes: " + (breathes + 500) + " -> " + breathes);
            }
        }

        // Decode base form ID; variation tracked separately
        int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(breathes);
        int variationIndex = data.getCurrentVariationIndex();

        if (formId <= 0) {
            return; // Invalid form ID
        }

        if (variationIndex == 0) {
            return; // Base form, no variation - let base mod handle it
        }

        // CRITICAL: Check if the sword item itself is on cooldown (Minecraft's built-in cooldown system)
        if (player.getCooldowns().isOnCooldown(heldItem.getItem())) {
            if (Config.logDebug) {
                Log.debug("Item " + heldItem.getItem() + " is on Minecraft cooldown, blocking variation execution");
            }
            // Cancel the event to prevent the base mod from executing as well
            event.setCanceled(true);
            return;
        }

        // CRITICAL: Check if form is on cooldown before executing variation (our tracking system)
        if (isFormOnCooldown(player.getUUID(), formId)) {
            if (Config.logDebug) {
                long remainingCooldown = getRemainingCooldown(player.getUUID(), formId);
                Log.debug("Form " + formId + " is on cooldown (remaining: " + remainingCooldown + "ms), blocking variation execution");
            }
            // Cancel the event to prevent the base mod from executing as well
            event.setCanceled(true);
            return;
        }

        // Get the variation for this form
        BreathingFormVariation variation = VariationRegistry.getVariation(formId, variationIndex, null);
        if (variation == null) {
            // Reset bad variation
            data.setCurrentVariationIndex(0);
            if (Config.logDebug) {
                Log.debug("No variation found for form " + formId + " variation " + variationIndex + " -> resetting to base form");
            }
            return;
        }

        // Execute the variation's effect
        lastExecutionTime = currentTime;

        if (Config.logDebug) {
            Log.debug("Executing base mod variation: " + variation.getName() +
                     " (Breathes: " + breathes + ", Form ID: " + formId + ", Variation Index: " + variationIndex + ")");
        }

        try {
            variation.getEffect().execute(player, player.level(), formId);

            // CRITICAL: Set cooldown for this form after execution
            int cooldownSeconds = variation.getCooldownSeconds();
            setFormCooldown(player.getUUID(), formId, cooldownSeconds);

            // CRITICAL: Set Minecraft item cooldown so the sword shows cooldown bar and can't be used
            int cooldownTicks = cooldownSeconds * 20; // Convert seconds to ticks (20 ticks = 1 second)
            player.getCooldowns().addCooldown(heldItem.getItem(), cooldownTicks);

            if (Config.logDebug) {
                Log.debug("Set " + cooldownSeconds + "s (" + cooldownTicks + " ticks) cooldown for form " + formId + " on item " + heldItem.getItem());
            }

            // CRITICAL: Cancel to prevent the base mod from also executing the base form
            // Using HIGHEST priority + canceling should prevent base mod execution
            event.setCanceled(true);
        } catch (Exception e) {
            Log.error("Error executing variation " + variation.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if a form is on cooldown for a player.
     */
    private static boolean isFormOnCooldown(UUID playerUUID, int formId) {
        Map<Integer, Long> playerCooldowns = formCooldowns.get(playerUUID);
        if (playerCooldowns == null) {
            return false;
        }

        Long cooldownEndTime = playerCooldowns.get(formId);
        if (cooldownEndTime == null) {
            return false;
        }

        return System.currentTimeMillis() < cooldownEndTime;
    }

    /**
     * Get remaining cooldown time for a form in milliseconds.
     */
    private static long getRemainingCooldown(UUID playerUUID, int formId) {
        Map<Integer, Long> playerCooldowns = formCooldowns.get(playerUUID);
        if (playerCooldowns == null) {
            return 0;
        }

        Long cooldownEndTime = playerCooldowns.get(formId);
        if (cooldownEndTime == null) {
            return 0;
        }

        long remaining = cooldownEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Set cooldown for a form for a player.
     */
    private static void setFormCooldown(UUID playerUUID, int formId, int cooldownSeconds) {
        long cooldownEndTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        formCooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>())
            .put(formId, cooldownEndTime);
    }

    /**
     * Clear all cooldowns for a player (e.g., when they log out).
     */
    public static void clearPlayerCooldowns(UUID playerUUID) {
        formCooldowns.remove(playerUUID);
    }
}
