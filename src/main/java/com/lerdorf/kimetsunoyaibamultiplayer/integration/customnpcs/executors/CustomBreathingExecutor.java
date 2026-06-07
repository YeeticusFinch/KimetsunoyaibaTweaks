package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.FormSelector;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.NPCCooldownManager;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack;
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
            BreathingTechnique technique = resolveTechnique(npc, heldItem);
            if (technique == null || technique.getFormCount() <= 0) {
                if (BaseModBreathingExecutor.isBaseModNichirinSword(item)) {
                    return BaseModBreathingExecutor.execute(npc, heldItem);
                }

                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Item is not a recognized breathing sword: " + item.getDescriptionId());
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
            String styleId = resolveStyleId(npc, heldItem);
            String cooldownKey = "custom_breathing_" + (styleId != null ? styleId : technique.getName());

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
        if (item == null) {
            return false;
        }

        return item instanceof NichirinSwordBlack
            || item instanceof BreathingSwordItem
            || SwordRegistry.getSword(item) != null
            || SwordMetadataRegistry.getMetadata(item) != null
            || BaseModBreathingExecutor.isBaseModNichirinSword(item);
    }

    private static BreathingTechnique resolveTechnique(LivingEntity npc, ItemStack heldItem) {
        Item item = heldItem.getItem();

        if (item instanceof NichirinSwordBlack blackSword) {
            return blackSword.getEffectiveTechnique(heldItem, npc);
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(item);
        if (registeredSword != null) {
            BreathingStyleRegistry.RegisteredBreathingStyle style =
                BreathingStyleRegistry.getStyle(registeredSword.getStyleId());
            if (style != null && style.getTechnique() != null) {
                return style.getTechnique();
            }

            BreathingSwordItem swordItem = registeredSword.getSwordItem();
            if (swordItem != null) {
                return swordItem.getBreathingTechnique();
            }
        }

        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(item);
        if (metadata != null) {
            BreathingStyleRegistry.RegisteredBreathingStyle style =
                BreathingStyleRegistry.getStyle(metadata.getStyleId());
            if (style != null && style.getTechnique() != null) {
                return style.getTechnique();
            }
        }

        if (item instanceof BreathingSwordItem breathingSword) {
            return breathingSword.getBreathingTechnique();
        }

        return null;
    }

    private static String resolveStyleId(LivingEntity npc, ItemStack heldItem) {
        Item item = heldItem.getItem();

        if (item instanceof NichirinSwordBlack) {
            String assigned = NichirinSwordBlack.getAssignedStyleId(heldItem);
            if (assigned != null && !assigned.isBlank()) {
                return assigned;
            }
            return NichirinSwordBlack.ensureStyleAssigned(heldItem, npc.getRandom());
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(item);
        if (registeredSword != null) {
            return registeredSword.getStyleId();
        }

        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(item);
        if (metadata != null) {
            return metadata.getStyleId();
        }

        if (item instanceof BreathingSwordItem breathingSword) {
            BreathingTechnique technique = breathingSword.getBreathingTechnique();
            if (technique != null) {
                return technique.getName();
            }
        }

        return null;
    }
}
