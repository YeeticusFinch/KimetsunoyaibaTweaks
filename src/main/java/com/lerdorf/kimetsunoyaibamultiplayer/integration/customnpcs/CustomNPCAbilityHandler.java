package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BaseModBreathingExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BloodDemonArtExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.CustomBreathingExecutor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Main event handler for Custom NPCs compatibility.
 * Listens for NPC attacks and triggers appropriate abilities.
 */
@Mod.EventBusSubscriber
public class CustomNPCAbilityHandler {

    // Thread-local flag to prevent recursion
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    /**
     * Handle NPC attacks - trigger breathing forms or demon arts
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onNPCAttack(LivingAttackEvent event) {
        // Prevent recursion
        if (IS_PROCESSING.get()) {
            return;
        }

        try {
            IS_PROCESSING.set(true);

            // Check if compatibility is enabled
            if (!CustomNPCConfig.isEnabled()) {
                return;
            }

            // Only process on server side
            if (event.getEntity().level().isClientSide) {
                return;
            }

            // Get attacker
            DamageSource source = event.getSource();
            if (source.getEntity() == null || !(source.getEntity() instanceof LivingEntity)) {
                return;
            }

            LivingEntity attacker = (LivingEntity) source.getEntity();

            // Check if attacker is a Custom NPC
            if (!CustomNPCHelper.canUseAbilities(attacker)) {
                return;
            }

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] ===== NPC Attack Detected =====");
                Log.debug("[KnY Custom NPCs] Attacker: " + attacker.getName().getString());
                Log.debug("[KnY Custom NPCs] Attacker Class: " + attacker.getClass().getName());
            }

            // Check trigger chance
            double triggerChance = CustomNPCConfig.getTriggerChance();
            double roll = attacker.getRandom().nextDouble();
            if (roll > triggerChance) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Trigger chance check failed: " + roll + " > " + triggerChance);
                }
                return;
            }

            // Get held item
            ItemStack heldItem = attacker.getMainHandItem();
            if (heldItem.isEmpty()) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] No item in main hand");
                }
                return;
            }

            Item item = heldItem.getItem();

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Held Item: " + item.getDescriptionId());
                Log.debug("[KnY Custom NPCs] Item Class: " + item.getClass().getName());
            }

            // Try to execute ability based on item type
            boolean abilityExecuted = false;

            // Debug: Check all detection methods
            if (CustomNPCConfig.isDebugEnabled()) {
                boolean isCustomSword = CustomBreathingExecutor.isCustomBreathingSword(item);
                boolean isBaseModSword = BaseModBreathingExecutor.isBaseModNichirinSword(item);
                boolean isBloodArt = BloodDemonArtExecutor.isBloodDemonArt(item);

                Log.debug("[KnY Custom NPCs] Detection Results:");
                Log.debug("  - Is Custom Breathing Sword: " + isCustomSword);
                Log.debug("  - Is Base Mod Nichirin Sword: " + isBaseModSword);
                Log.debug("  - Is Blood Demon Art: " + isBloodArt);
            }

            // Priority 1: This mod's custom breathing swords
            if (CustomBreathingExecutor.isCustomBreathingSword(item)) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] → Executing custom breathing sword ability");
                }
                abilityExecuted = CustomBreathingExecutor.execute(attacker, heldItem);
            }
            // Priority 2: Base mod nichirin swords
            else if (BaseModBreathingExecutor.isBaseModNichirinSword(item)) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] → Executing base mod nichirin sword ability");
                }
                abilityExecuted = BaseModBreathingExecutor.execute(attacker, item);
            }
            // Priority 3: Blood demon arts
            else if (BloodDemonArtExecutor.isBloodDemonArt(item)) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] → Executing blood demon art ability");
                }
                abilityExecuted = BloodDemonArtExecutor.execute(attacker, item);
            }
            else {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] ✗ Item not recognized as any ability type");
                }
            }

            if (abilityExecuted && CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] ✓ Ability executed successfully");
            } else if (!abilityExecuted && CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] ✗ Ability execution failed or not attempted");
            }

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] ================================");
            }

        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error in ability handler:");
            e.printStackTrace();
        } finally {
            IS_PROCESSING.set(false);
        }
    }

    /**
     * Print debug information about Custom NPCs compatibility
     */
    public static void printDebugInfo() {
        Log.debug("=== KnY Custom NPCs Compatibility Debug Info ===");
        Log.debug("Enabled: " + CustomNPCConfig.isEnabled());
        Log.debug("Custom NPCs Mod Loaded: " + CustomNPCHelper.isCustomNPCsModLoaded());
        Log.debug("Cooldown Multiplier: " + CustomNPCConfig.getCooldownMultiplier());
        Log.debug("Trigger Chance: " + (CustomNPCConfig.getTriggerChance() * 100) + "%");
        Log.debug("Debug Logging: " + CustomNPCConfig.isDebugEnabled());
        Log.debug("\n");
        Log.debug("Form Weight Distribution (7 forms example):");
        FormSelector.printWeightDistribution(7);
        Log.debug("================================================");
    }
}
