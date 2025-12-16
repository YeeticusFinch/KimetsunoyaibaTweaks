package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.FormSelector;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.NPCCooldownManager;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Executor for breathing swords from this mod (KimetsunoyaibaMultiplayer).
 * These swords are registered in the SwordRegistry and have BreathingTechnique instances.
 */
public class CustomBreathingExecutor {

    /**
     * Execute a breathing form for an NPC holding a custom breathing sword from this mod
     *
     * @param npc The NPC entity
     * @param heldItem The breathing sword item stack
     * @return true if ability was executed successfully
     */
    public static boolean execute(LivingEntity npc, ItemStack heldItem) {
        if (npc == null || heldItem.isEmpty() || npc.level().isClientSide) {
            return false;
        }

        try {
            Item item = heldItem.getItem();

            // Get registered sword from SwordRegistry
            SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(item);
            if (registeredSword == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Item not in SwordRegistry: " + item.getDescriptionId());
                }
                return false;
            }

            // Get the sword item (should be a BreathingSwordItem)
            BreathingSwordItem swordItem = registeredSword.getSwordItem();
            if (swordItem == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Registered sword has no BreathingSwordItem");
                }
                return false;
            }

            // Get breathing technique
            BreathingTechnique technique = swordItem.getBreathingTechnique();
            if (technique == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Sword has no breathing technique: " + swordItem.getDescriptionId());
                }
                return false;
            }

            int formCount = technique.getFormCount();
            if (formCount == 0) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Breathing technique has no forms");
                }
                return false;
            }

            // Get breathing style ID for cooldown tracking
            String styleId = registeredSword.getStyleId();
            String cooldownKey = "custom_breathing_" + styleId;

            // Check cooldown
            if (!NPCCooldownManager.canUseAbility(npc, cooldownKey)) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    int remaining = NPCCooldownManager.getRemainingCooldown(npc, cooldownKey);
                    Log.debug("[KnY Custom NPCs] On cooldown: " + remaining + " ticks remaining");
                }
                return false;
            }

            // Select a weighted form
            int formIndex = FormSelector.selectWeightedForm(npc.getRandom(), formCount);
            BreathingForm selectedForm = technique.getForm(formIndex);

            if (selectedForm == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Selected form is null at index " + formIndex);
                }
                return false;
            }

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Executing Custom Breathing:");
                Log.debug("  NPC: " + npc.getName().getString());
                Log.debug("  Style: " + technique.getName());
                Log.debug("  Form: " + selectedForm.getName() + " (" + FormSelector.getFormName(formIndex) + ")");
                Log.debug("  Cooldown: " + selectedForm.getCooldownSeconds() + "s");
            }

            // Execute the breathing form effect (formId is auto-injected)
            selectedForm.execute(npc, npc.level());

            // Set cooldown (convert seconds to ticks: seconds * 20)
            int cooldownTicks = selectedForm.getCooldownSeconds() * 20;
            NPCCooldownManager.setCooldown(npc, cooldownKey, cooldownTicks);

            // Remember which form was used
            NPCCooldownManager.setLastFormUsed(npc, styleId, formIndex);

            return true;

        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error executing custom breathing ability:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if an item is a custom breathing sword from this mod
     *
     * @param item The item to check
     * @return true if the item is registered in SwordRegistry
     */
    public static boolean isCustomBreathingSword(Item item) {
        return SwordRegistry.getSword(item) != null;
    }
}
