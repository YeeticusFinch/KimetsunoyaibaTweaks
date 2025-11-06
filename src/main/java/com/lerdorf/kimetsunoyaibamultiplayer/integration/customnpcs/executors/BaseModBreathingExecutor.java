package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.FormSelector;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.NPCCooldownManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for breathing swords from the base KimetsunoYaiba ver3 mod.
 * Uses reflection to call base mod procedures.
 */
public class BaseModBreathingExecutor {

    // Map of breathing style name -> (start ID, end ID, form count, procedure class name)
    private static final Map<String, BreathingStyleInfo> BREATHING_STYLES = new HashMap<>();

    static {
        // Thunder Breathing (101-113)
        BREATHING_STYLES.put("thunder", new BreathingStyleInfo(101, 113, 6, "PlayerBreathThunderProcedure", 8));

        // Hinokami Kagura (201+)
        BREATHING_STYLES.put("hinokami", new BreathingStyleInfo(201, 213, 13, "PlayerBreathHinokamiKaguraProcedure", 10));

        // Flame Breathing (301-309)
        BREATHING_STYLES.put("flame", new BreathingStyleInfo(301, 309, 5, "PlayerBreathFlameProcedure", 8));

        // Mist Breathing (401-407)
        BREATHING_STYLES.put("mist", new BreathingStyleInfo(401, 407, 7, "PlayerBreathMistProcedure", 8));

        // Water Breathing (601-611)
        BREATHING_STYLES.put("water", new BreathingStyleInfo(601, 611, 11, "PlayerBreathWaterProcedure", 8));

        // Wind Breathing
        BREATHING_STYLES.put("wind", new BreathingStyleInfo(701, 710, 8, "PlayerBreathWindProcedure", 8));

        // Stone Breathing
        BREATHING_STYLES.put("stone", new BreathingStyleInfo(801, 810, 5, "PlayerBreathStoneProcedure", 10));

        // Insect Breathing
        BREATHING_STYLES.put("insect", new BreathingStyleInfo(901, 910, 4, "PlayerBreathInsectProcedure", 8));

        // Serpent Breathing
        BREATHING_STYLES.put("serpent", new BreathingStyleInfo(1001, 1010, 5, "PlayerBreathSerpentProcedure", 8));

        // Sound Breathing
        BREATHING_STYLES.put("sound", new BreathingStyleInfo(1101, 1110, 5, "PlayerBreathSoundProcedure", 8));

        // Love Breathing
        BREATHING_STYLES.put("love", new BreathingStyleInfo(1201, 1210, 6, "PlayerBreathesLoveProcedure", 8));

        // Flower Breathing
        BREATHING_STYLES.put("flower", new BreathingStyleInfo(1301, 1310, 7, "PlayerBreathesFlowerProcedure", 8));

        // Beast Breathing
        BREATHING_STYLES.put("beast", new BreathingStyleInfo(1401, 1410, 10, "PlayerBreathBeastProcedure", 8));

        // Moon Breathing
        BREATHING_STYLES.put("moon", new BreathingStyleInfo(1501, 1520, 16, "PlayerBreathMoonProcedure", 10));

        // Sun Breathing
        BREATHING_STYLES.put("sun", new BreathingStyleInfo(1601, 1615, 13, "PlayerBreathSunProcedure", 10));

        // Cherry Blossom Breathing
        BREATHING_STYLES.put("cherry_blossom", new BreathingStyleInfo(1701, 1710, 5, "PlayerBreathCherryBlossomProcedure", 8));

        // Bamboo Breathing
        BREATHING_STYLES.put("bamboo", new BreathingStyleInfo(1801, 1810, 5, "PlayerBreathBambooProcedure", 8));
    }

    // Map of item registry name suffix -> breathing style name
    private static final Map<String, String> ITEM_TO_STYLE = new HashMap<>();

    static {
        // Thunder Breathing
        ITEM_TO_STYLE.put("nichirinsword_thunder", "thunder"); // Generic thunder sword
        ITEM_TO_STYLE.put("nichirinsword_zenitsu", "thunder");
        ITEM_TO_STYLE.put("nichirinsword_kaigaku", "thunder");

        // Hinokami Kagura / Sun Breathing
        ITEM_TO_STYLE.put("nichirinsword_tanjiro_2", "hinokami");
        ITEM_TO_STYLE.put("nichirinsword_yoriichi", "sun");

        // Flame Breathing
        ITEM_TO_STYLE.put("nichirinsword_flame", "flame"); // Generic flame sword
        ITEM_TO_STYLE.put("nichirinsword_rengoku", "flame");

        // Mist Breathing
        ITEM_TO_STYLE.put("nichirinsword_mist", "mist"); // Generic mist sword
        ITEM_TO_STYLE.put("nichirinsword_tokito", "mist");
        ITEM_TO_STYLE.put("nitirintou_tokitou", "mist"); // Alternative spelling

        // Water Breathing
        ITEM_TO_STYLE.put("nichirinsword_water", "water"); // Generic water sword
        ITEM_TO_STYLE.put("nichirinsword_tomioka", "water");

        // Wind Breathing
        ITEM_TO_STYLE.put("nichirinsword_wind", "wind"); // Generic wind sword
        ITEM_TO_STYLE.put("nichirinsword_shinazugawa", "wind");

        // Stone Breathing
        ITEM_TO_STYLE.put("nichirinsword_himejima_1", "stone");
        ITEM_TO_STYLE.put("nichirinsword_himejima_2", "stone");

        // Insect Breathing
        ITEM_TO_STYLE.put("nichirinsword_kocho", "insect");

        // Serpent Breathing
        ITEM_TO_STYLE.put("nichirinsword_iguro", "serpent");

        // Sound Breathing
        ITEM_TO_STYLE.put("nichirinsword_uzui", "sound");

        // Love Breathing
        ITEM_TO_STYLE.put("nichirinsword_kanroji", "love");

        // Flower Breathing
        ITEM_TO_STYLE.put("nichirinsword_kanae", "flower");
        ITEM_TO_STYLE.put("nichirinsword_kanawo", "flower"); // Kanao also uses flower breathing

        // Beast Breathing
        ITEM_TO_STYLE.put("nichirinsword_inosuke", "beast");

        // Bamboo Breathing
        ITEM_TO_STYLE.put("nichirinsword_bamboo", "bamboo");
        ITEM_TO_STYLE.put("nichirinsword_bamboo_2", "bamboo");

        // Moon Breathing
        ITEM_TO_STYLE.put("nichirinswordmoon", "moon");

        // Cherry Blossom Breathing
        ITEM_TO_STYLE.put("nichirinsword_cherry_blossom", "cherry_blossom");

        // Generic/Black sword (usually no breathing style, but adding for completeness)
        ITEM_TO_STYLE.put("nichirinsword_black", "water"); // Default to water for generic black sword
    }

    /**
     * Execute a breathing form for an NPC holding a base mod nichirin sword
     *
     * @param npc The NPC entity
     * @param item The nichirin sword item
     * @return true if ability was executed successfully
     */
    public static boolean execute(LivingEntity npc, Item item) {
        if (npc == null || item == null || npc.level().isClientSide) {
            return false;
        }

        try {
            // Get breathing style from item
            String breathingStyle = getBreathingStyleFromItem(item);
            if (breathingStyle == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    System.out.println("[KnY Custom NPCs] Unknown base mod nichirin sword: " + item.getDescriptionId());
                }
                return false;
            }

            BreathingStyleInfo styleInfo = BREATHING_STYLES.get(breathingStyle);
            if (styleInfo == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    System.out.println("[KnY Custom NPCs] No breathing style info for: " + breathingStyle);
                }
                return false;
            }

            String cooldownKey = "base_breathing_" + breathingStyle;

            // Check cooldown
            if (!NPCCooldownManager.canUseAbility(npc, cooldownKey)) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    int remaining = NPCCooldownManager.getRemainingCooldown(npc, cooldownKey);
                    System.out.println("[KnY Custom NPCs] On cooldown: " + remaining + " ticks remaining");
                }
                return false;
            }

            // Select weighted form
            int formIndex = FormSelector.selectWeightedForm(npc.getRandom(), styleInfo.formCount);

            if (CustomNPCConfig.isDebugEnabled()) {
                System.out.println("[KnY Custom NPCs] Executing Base Mod Breathing:");
                System.out.println("  NPC: " + npc.getName().getString());
                System.out.println("  Style: " + breathingStyle);
                System.out.println("  Form: " + FormSelector.getFormName(formIndex) + " (Index: " + formIndex + ")");
                System.out.println("  Using: StartBreathesProcedure");
            }

            // Get held item and set "select" NBT (like the 1.16.5 script example)
            ItemStack heldItem = npc.getMainHandItem();
            if (heldItem.isEmpty()) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    System.out.println("[KnY Custom NPCs] NPC has no item in main hand");
                }
                return false;
            }

            // Set itemstack "select" NBT to form index (0-based)
            heldItem.getOrCreateTag().putDouble("select", (double) formIndex);

            // Call StartBreathesProcedure (official base mod entry point)
            boolean success = callStartBreathesProcedure(npc.level(), npc, heldItem);

            if (success) {
                // Set cooldown
                int cooldownTicks = styleInfo.baseCooldownSeconds * 20;
                NPCCooldownManager.setCooldown(npc, cooldownKey, cooldownTicks);

                // Remember which form was used
                NPCCooldownManager.setLastFormUsed(npc, breathingStyle, formIndex);

                return true;
            } else {
                if (CustomNPCConfig.isDebugEnabled()) {
                    System.out.println("[KnY Custom NPCs] Failed to call breathing procedure");
                }
                return false;
            }

        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error executing base mod breathing ability:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if an item is a base mod nichirin sword
     *
     * @param item The item to check
     * @return true if the item is a base mod nichirin sword
     */
    public static boolean isBaseModNichirinSword(Item item) {
        if (item == null) {
            return false;
        }

        String descriptionId = item.getDescriptionId();
        return descriptionId.contains("kimetsunoyaiba.nichirinsword") ||
               descriptionId.contains("kimetsunoyaiba.nitirintou");
    }

    /**
     * Get breathing style name from item
     *
     * @param item The nichirin sword item
     * @return Breathing style name, or null if unknown
     */
    private static String getBreathingStyleFromItem(Item item) {
        String descriptionId = item.getDescriptionId().toLowerCase();

        for (Map.Entry<String, String> entry : ITEM_TO_STYLE.entrySet()) {
            if (descriptionId.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Call StartBreathesProcedure using reflection
     * This is the official base mod entry point for breathing techniques.
     *
     * @param world The world/level
     * @param entity The entity using the breathing form
     * @param itemstack The nichirin sword item (with "select" NBT set)
     * @return true if procedure was called successfully
     */
    private static boolean callStartBreathesProcedure(LevelAccessor world, LivingEntity entity, ItemStack itemstack) {
        try {
            // Load StartBreathesProcedure class
            Class<?> procedureClass = Class.forName("net.mcreator.kimetsunoyaiba.procedures.StartBreathesProcedure");

            // Find execute method: execute(LevelAccessor world, Entity entity, ItemStack itemstack)
            Method executeMethod = procedureClass.getMethod("execute", LevelAccessor.class,
                net.minecraft.world.entity.Entity.class, ItemStack.class);

            // Call the method
            executeMethod.invoke(null, world, entity, itemstack);

            if (CustomNPCConfig.isDebugEnabled()) {
                System.out.println("[KnY Custom NPCs] ✓ StartBreathesProcedure executed successfully");
            }

            return true;

        } catch (ClassNotFoundException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                System.out.println("[KnY Custom NPCs] StartBreathesProcedure class not found");
                System.out.println("[KnY Custom NPCs] Is the base KimetsunoYaiba mod installed?");
            }
            return false;
        } catch (NoSuchMethodException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                System.out.println("[KnY Custom NPCs] Execute method not found in StartBreathesProcedure");
                System.out.println("[KnY Custom NPCs] Base mod version may be incompatible");
            }
            return false;
        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error calling StartBreathesProcedure:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inner class to store breathing style information
     */
    private static class BreathingStyleInfo {
        final int startId;
        final int endId;
        final int formCount;
        final String procedureClassName;
        final int baseCooldownSeconds;

        BreathingStyleInfo(int startId, int endId, int formCount, String procedureClassName, int baseCooldownSeconds) {
            this.startId = startId;
            this.endId = endId;
            this.formCount = formCount;
            this.procedureClassName = procedureClassName;
            this.baseCooldownSeconds = baseCooldownSeconds;
        }
    }
}
