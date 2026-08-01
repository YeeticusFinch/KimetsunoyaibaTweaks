package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.FormSelector;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.NPCCooldownManager;
import net.minecraft.server.level.ServerLevel;
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

        // Love Breathing (base mod uses 1501+)
        BREATHING_STYLES.put("love", new BreathingStyleInfo(1501, 1510, 6, "PlayerBreathesLoveProcedure", 8));

        // Flower Breathing (only has 4 forms: 2nd, 4th, 5th, 6th)
        // Base mod breathes values are 1402, 1404, 1405, 1406 => baseId 1401 with select values {1,3,4,5}
        BREATHING_STYLES.put("flower", new BreathingStyleInfo(1401, 1410, 7, "PlayerBreathesFlowerProcedure", 8, new int[]{1, 3, 4, 5}));

        // Beast Breathing
        BREATHING_STYLES.put("beast", new BreathingStyleInfo(1401, 1410, 10, "PlayerBreathBeastProcedure", 8));

        // Moon Breathing (Basic - forms 1, 2, 3, 5, 6 only)
        // Used by nichirinswordmoon - only specific forms available
        BREATHING_STYLES.put("moon_basic", new BreathingStyleInfo(1501, 1520, 16, "PlayerBreathMoonProcedure", 10, new int[]{0, 1, 2, 4, 5})); // 0=1st form, 1=2nd form, etc.

        // Moon Breathing (Full - all 16 forms)
        // Used by sword_kokushibo_1 and sword_kokushibo_2
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
        ITEM_TO_STYLE.put("nichirinsword_flower", "flower"); // Generic flower sword id

        // Beast Breathing
        ITEM_TO_STYLE.put("nichirinsword_inosuke", "beast");

        // Bamboo Breathing
        ITEM_TO_STYLE.put("nichirinsword_bamboo", "bamboo");
        ITEM_TO_STYLE.put("nichirinsword_bamboo_2", "bamboo");

        // Moon Breathing
        // Moon Breathing swords
        ITEM_TO_STYLE.put("nichirinswordmoon", "moon_basic"); // Basic moon sword - only forms 1,2,3,5,6
        ITEM_TO_STYLE.put("sword_kokushibo_1", "moon"); // Kokushibo's swords - all 16 forms
        ITEM_TO_STYLE.put("sword_kokushibo_2", "moon");

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
        if (npc == null || item == null) {
            return false;
        }

        ItemStack mainHand = npc.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() == item) {
            return execute(npc, mainHand);
        }

        ItemStack offHand = npc.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() == item) {
            return execute(npc, offHand);
        }

        return execute(npc, new ItemStack(item));
    }

    /**
     * Execute a breathing form for an NPC holding a base mod nichirin sword stack.
     *
     * @param npc The NPC entity
     * @param heldItem The nichirin sword stack
     * @return true if ability was executed successfully
     */
    public static boolean execute(LivingEntity npc, ItemStack heldItem) {
        if (npc == null || heldItem == null || heldItem.isEmpty() || npc.level().isClientSide) {
            return false;
        }

        try {
            Item item = heldItem.getItem();
            // Get breathing style from item
            String breathingStyle = getBreathingStyleFromItem(item);
            if (breathingStyle == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Unknown base mod nichirin sword: " + item.getDescriptionId());
                }
                return false;
            }

            BreathingStyleInfo styleInfo = BREATHING_STYLES.get(breathingStyle);
            if (styleInfo == null) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] No breathing style info for: " + breathingStyle);
                }
                return false;
            }

            // For Flower: only execute if there is an active target (prevents post-fight idle usage)
            if (breathingStyle.equals("flower") && npc instanceof net.minecraft.world.entity.Mob mob) {
                LivingEntity curTarget = mob.getTarget();
                if (curTarget == null || !curTarget.isAlive()) {
                    if (CustomNPCConfig.isDebugEnabled()) {
                        Log.debug("[KnY Custom NPCs] Skipping Flower execution (no active target)");
                    }
                    return false;
                }
            }

            String cooldownKey = "base_breathing_" + breathingStyle;

            // Check cooldown
            if (!NPCCooldownManager.canUseAbility(npc, cooldownKey)) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    int remaining = NPCCooldownManager.getRemainingCooldown(npc, cooldownKey);
                    Log.debug("[KnY Custom NPCs] On cooldown: " + remaining + " ticks remaining");
                }
                return false;
            }

            // Select weighted form from available forms
            int effectiveFormCount = styleInfo.getEffectiveFormCount();
            int selectionIndex = FormSelector.selectWeightedForm(npc.getRandom(), effectiveFormCount);

            // Map selection index to actual form index (for restricted form sets like moon_basic)
            int formIndex = styleInfo.getFormIndex(selectionIndex);

            // ALWAYS log for flower breathing to debug the issue
            if (CustomNPCConfig.isDebugEnabled() || breathingStyle.equals("flower")) {
                Log.debug("[KnY Custom NPCs] Executing Base Mod Breathing:");
                Log.debug("  NPC: " + npc.getName().getString());
                Log.debug("  Style: " + breathingStyle);
                Log.debug("  Effective Form Count: " + effectiveFormCount);
                Log.debug("  Selection Index: " + selectionIndex);
                Log.debug("  Form Index (select value): " + formIndex);
                Log.debug("  Expected breathes value: " + (styleInfo.startId + formIndex));
                if (styleInfo.allowedFormIndices != null) {
                    Log.debug("  Allowed Form Indices: " + java.util.Arrays.toString(styleInfo.allowedFormIndices));
                    Log.debug("  Selected: " + FormSelector.getFormName(selectionIndex) + " from " + effectiveFormCount + " allowed forms");
                    Log.debug("  Actual Form: " + FormSelector.getFormName(formIndex) + " (Index: " + formIndex + ")");
                } else {
                    Log.debug("  Form: " + FormSelector.getFormName(formIndex) + " (Index: " + formIndex + ")");
                }
                Log.debug("  Using: StartBreathesProcedure");
            }

            // Set itemstack "select" NBT to form index (0-based)
            heldItem.getOrCreateTag().putDouble("select", (double) formIndex);

            // WORKAROUND: Flower and Love Breathing have a bug where the breathes value gets stuck
            // after the first execution, preventing subsequent uses. The base mod doesn't reset
            // breathes back to 0.0, so StartBreathesProcedure refuses to execute when breathes is
            // already set to a form ID. FIX: Always force breathes=0.0 before execution for these styles.
            if (breathingStyle.equals("flower") || breathingStyle.equals("love")) {
                double breathesValue = npc.getPersistentData().getDouble("breathes");

                // ALWAYS reset breathes to 0.0 for these styles to ensure clean execution
                npc.getPersistentData().putDouble("breathes", 0.0);

                // Also reset all counters to ensure clean state
                callResetCounterProcedure(npc);

                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] [" + breathingStyle + " Breathing Fix] Forced reset (breathes was " + breathesValue + ", now 0.0)");
                }
            }

            // Call StartBreathesProcedure (official base mod entry point)
            boolean success = callStartBreathesProcedure(npc.level(), npc, heldItem);
            if (CustomNPCConfig.isDebugEnabled()) {
                double postStart = npc.getPersistentData().getDouble("breathes");
                Log.debug("[KnY Custom NPCs] Post-StartBreathes breathes=" + postStart);
            }

            if (success) {
                // Set cooldown
                int cooldownTicks = styleInfo.baseCooldownSeconds * 20;
                NPCCooldownManager.setCooldown(npc, cooldownKey, cooldownTicks);

                // Remember which form was used
                NPCCooldownManager.setLastFormUsed(npc, breathingStyle, formIndex);

                // CRITICAL FIX for Flower/Love Breathing: Lock the breathes value to prevent base mod from incrementing it
                // The base mod increments breathes over time, moving it out of valid form ranges too quickly
                if (breathingStyle.equals("flower") || breathingStyle.equals("love")) {
                    final int targetBreathes = styleInfo.startId + formIndex;
                    final String styleForLog = breathingStyle;

                    // For Flower: Always force-call the Player procedure after setting breathes to ensure effects play
                    if (breathingStyle.equals("flower")) {
                        npc.getPersistentData().putDouble("breathes", (double) targetBreathes);
                        boolean forcedNow = callPlayerBreathingProcedure("PlayerBreathesFlowerProcedure", npc.level(), npc);
                        if (CustomNPCConfig.isDebugEnabled()) {
                            Log.debug("[KnY Custom NPCs] [Flower Breathing Fallback] Forced PlayerBreathesFlowerProcedure immediate -> " + forcedNow + " (breathes=" + targetBreathes + ")");
                        }
                    }

                    if (npc.level() instanceof ServerLevel serverLevel) {
                        // Lock breathes value for 60 ticks (3 seconds) to allow form to execute
                        for (int i = 1; i <= 60; i++) {
                            final int tick = i;
                            serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                                serverLevel.getServer().getTickCount() + tick,
                                () -> {
                                    if (npc.isAlive() && !npc.isRemoved()) {
                                        double currentBreathes = npc.getPersistentData().getDouble("breathes");
                                        // Only lock if still in the breathing range
                                        if (currentBreathes >= styleInfo.startId && currentBreathes <= styleInfo.endId) {
                                            npc.getPersistentData().putDouble("breathes", (double) targetBreathes);
                                            // For Flower: reinforce call for first few ticks to ensure visual/audio effects spawn
                                            if (breathingStyle.equals("flower") && tick <= 5) {
                                                callPlayerBreathingProcedure("PlayerBreathesFlowerProcedure", npc.level(), npc);
                                            }
                                        }
                                    }
                                }
                            ));
                        }

                        // After 3 seconds, do final cleanup
                        serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                            serverLevel.getServer().getTickCount() + 65,
                            () -> {
                                if (npc.isAlive() && !npc.isRemoved()) {
                                    double oldBreathes = npc.getPersistentData().getDouble("breathes");
                                    npc.getPersistentData().putDouble("breathes", 0.0);
                                    callResetCounterProcedure(npc);

                                    if (CustomNPCConfig.isDebugEnabled()) {
                                        Log.debug("[KnY Custom NPCs] [" + styleForLog + " Breathing Cleanup] Final reset after 3s (breathes was " + oldBreathes + ", now 0.0)");
                                    }
                                }
                            }
                        ));
                    }
                }

                return true;
            } else {
                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] Failed to call breathing procedure");
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
               descriptionId.contains("kimetsunoyaiba.nitirintou") ||
               descriptionId.contains("kimetsunoyaiba.sword_kokushibo");
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
     * Call ResetCounterProcedure using reflection
     * Resets all breathing form counters (cnt1-cnt10) and attack flag
     *
     * @param entity The entity to reset counters for
     */
    private static void callResetCounterProcedure(LivingEntity entity) {
        try {
            // Load ResetCounterProcedure class
            Class<?> procedureClass = Class.forName("net.mcreator.kimetsunoyaiba.procedures.ResetCounterProcedure");

            // Find execute method: execute(Entity entity)
            Method executeMethod = procedureClass.getMethod("execute", net.minecraft.world.entity.Entity.class);

            // Call the method
            executeMethod.invoke(null, entity);

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] ✓ ResetCounterProcedure executed successfully");
            }

        } catch (Exception e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                System.err.println("[KnY Custom NPCs] Failed to call ResetCounterProcedure: " + e.getMessage());
            }
        }
    }

    /**
     * Call a specific PlayerBreath...Procedure using reflection to force execution
     */
    private static boolean callPlayerBreathingProcedure(String simpleClassName, LevelAccessor world, LivingEntity entity) {
        try {
            Class<?> procedureClass = Class.forName("net.mcreator.kimetsunoyaiba.procedures." + simpleClassName);
            Method executeMethod = procedureClass.getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, net.minecraft.world.entity.Entity.class);
            executeMethod.invoke(null, world, entity.getX(), entity.getY(), entity.getZ(), entity);
            return true;
        } catch (ClassNotFoundException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Player breathing procedure not found: " + simpleClassName);
            }
            return false;
        } catch (NoSuchMethodException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Execute method not found in " + simpleClassName);
            }
            return false;
        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error forcing player breathing procedure (" + simpleClassName + "):");
            e.printStackTrace();
            return false;
        }
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
                Log.debug("[KnY Custom NPCs] ✓ StartBreathesProcedure executed successfully");
            }

            return true;

        } catch (ClassNotFoundException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] StartBreathesProcedure class not found");
                Log.debug("[KnY Custom NPCs] Is the base KimetsunoYaiba mod installed?");
            }
            return false;
        } catch (NoSuchMethodException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Execute method not found in StartBreathesProcedure");
                Log.debug("[KnY Custom NPCs] Base mod version may be incompatible");
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
        final int[] allowedFormIndices; // null = all forms allowed, otherwise specific indices (0-based)

        BreathingStyleInfo(int startId, int endId, int formCount, String procedureClassName, int baseCooldownSeconds) {
            this(startId, endId, formCount, procedureClassName, baseCooldownSeconds, null);
        }

        BreathingStyleInfo(int startId, int endId, int formCount, String procedureClassName, int baseCooldownSeconds, int[] allowedFormIndices) {
            this.startId = startId;
            this.endId = endId;
            this.formCount = formCount;
            this.procedureClassName = procedureClassName;
            this.baseCooldownSeconds = baseCooldownSeconds;
            this.allowedFormIndices = allowedFormIndices;
        }

        /**
         * Get actual form count (either all forms or just allowed forms)
         */
        int getEffectiveFormCount() {
            return allowedFormIndices != null ? allowedFormIndices.length : formCount;
        }

        /**
         * Get the form index to use (maps from selection index to actual form index)
         */
        int getFormIndex(int selectionIndex) {
            if (allowedFormIndices != null && selectionIndex >= 0 && selectionIndex < allowedFormIndices.length) {
                return allowedFormIndices[selectionIndex];
            }
            return selectionIndex;
        }
    }
}
