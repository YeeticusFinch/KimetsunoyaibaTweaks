package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.VermilionEyeEffect;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles execution of variations for base KimetsunoYaiba mod swords.
 * When a player right-clicks with a base mod sword that has a variation selected,
 * this handler executes the variation's effect using substring matching on form names.
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID)
public class BaseModVariationHandler {

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

        // While one of our custom forms is still active, base-mod right-click form usage must be blocked.
        if (GuardStateHelper.isCustomBreathingActive(player)) {
            event.setCanceled(true);
            if (player.level().isClientSide()) {
                player.displayClientMessage(Component.literal("§cYou cannot use base mod forms while a custom breathing form is active."), true);
            }
            return;
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

        // Get current form name and variation index from player data
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());
        String currentFormName = data.getBaseModFormName();
        int variationIndex = data.getCurrentVariationIndex();

        // Fallback: If form name not cached, try to get it from breathes value
        if (currentFormName == null || currentFormName.isEmpty()) {
            double breathes = player.getPersistentData().getDouble("breathes");
            if (breathes > 0) {
                int formId = (int) breathes;
                com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.BaseForm form =
                    com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.forms.get(formId);
                if (form != null) {
                    currentFormName = form.name;
                    data.setBaseModFormName(currentFormName); // Cache it
                    if (Config.logDebug) {
                        Log.debug("[BaseModVariationHandler] Populated form name from breathes: " + formId + " -> " + currentFormName);
                    }
                }
            }
        }

        if (currentFormName == null || currentFormName.isEmpty()) {
            return; // No form selected
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

        // Get the variation using substring matching
        BreathingFormVariation variation = VariationRegistry.getVariationBySubstring(
            currentFormName, variationIndex, null
        );

        if (variation == null) {
            // Reset bad variation
            data.setCurrentVariationIndex(0);
            if (Config.logDebug) {
                Log.debug("No variation found for form '" + currentFormName +
                         "' variation " + variationIndex + " -> resetting to base form");
            }
            return;
        }

        // Execute the variation's effect
        lastExecutionTime = currentTime;

        // Get the form ID from player's breathes value
        double breathes = player.getPersistentData().getDouble("breathes");
        int formId = (int) breathes;

        if (Config.logDebug) {
            Log.debug("Executing base mod variation: " + variation.getName() +
                     " (Form: " + currentFormName + ", Variation Index: " + variationIndex + ", FormID: " + formId + ")");
        }

        try {
            variation.getEffect().execute(player, player.level(), formId);

            // CRITICAL: Restore breathes value after execution
            // The base mod or variation execution might have changed it
            player.getPersistentData().putDouble("breathes", breathes);

            if (Config.logDebug) {
                Log.debug("Restored breathes value to " + breathes + " after variation execution");
            }

            // CRITICAL: Set Minecraft item cooldown so the sword shows cooldown bar and can't be used
            int cooldownSeconds = variation.getCooldownSeconds();
            int baseCooldownTicks = cooldownSeconds * 20; // Convert seconds to ticks (20 ticks = 1 second)

            // Apply Vermilion Eye cooldown reduction if active (40% faster cooldowns)
            int cooldownTicks = VermilionEyeEffect.applyCooldownReductionTicks(player, baseCooldownTicks);

            player.getCooldowns().addCooldown(heldItem.getItem(), cooldownTicks);

            if (Config.logDebug) {
                if (cooldownTicks != baseCooldownTicks) {
                    Log.debug("Set " + (cooldownTicks / 20.0f) + "s (" + cooldownTicks + " ticks) cooldown on item " +
                             heldItem.getItem() + " (reduced from " + cooldownSeconds + "s by Vermilion Eye)");
                } else {
                    Log.debug("Set " + cooldownSeconds + "s (" + cooldownTicks + " ticks) cooldown on item " + heldItem.getItem());
                }
            }

            // CRITICAL: Cancel to prevent the base mod from also executing the base form
            // Using HIGHEST priority + canceling should prevent base mod execution
            event.setCanceled(true);
        } catch (Exception e) {
            Log.error("Error executing variation " + variation.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

}
