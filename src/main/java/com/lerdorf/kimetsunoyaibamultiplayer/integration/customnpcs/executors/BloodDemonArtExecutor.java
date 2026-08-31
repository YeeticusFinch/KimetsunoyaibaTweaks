package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.FormSelector;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.NPCCooldownManager;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for Blood Demon Art items from the base KimetsunoYaiba ver3 mod.
 * Uses reflection to call base mod demon art procedures.
 */
public class BloodDemonArtExecutor {

    // Map of demon art name -> (start ID, end ID, form count, procedure class name)
    private static final Map<String, DemonArtInfo> DEMON_ARTS = new HashMap<>();

    static {
        // Rui (Spider Thread)
        DEMON_ARTS.put("rui", new DemonArtInfo(200, 210, 5, "PlayerDemonArtRuiProcedure", 8));

        // Nezuko (Blood Explosion)
        DEMON_ARTS.put("nezuko", new DemonArtInfo(300, 310, 4, "PlayerDemonArtNezukoProcedure", 8));

        // Akaza (Destructive Death)
        DEMON_ARTS.put("akaza", new DemonArtInfo(400, 420, 10, "PlayerDemonArtAkazaProcedure", 10));

        // Enmu (Sleep Manipulation)
        DEMON_ARTS.put("enmu", new DemonArtInfo(500, 510, 5, "PlayerDemonArtEnmuProcedure", 10));

        // Gyutaro (Blood Sickles)
        DEMON_ARTS.put("gyutaro", new DemonArtInfo(600, 610, 5, "PlayerDemonArtGyutaroProcedure", 8));

        // Daki (Obi Sashes)
        DEMON_ARTS.put("daki", new DemonArtInfo(700, 710, 5, "PlayerDemonArtDakiProcedure", 8));

        // Gyokko (Pot Art)
        DEMON_ARTS.put("gyokko", new DemonArtInfo(800, 810, 5, "PlayerDemonArtGyokkoProcedure", 10));

        // Hantengu (Emotion Clones)
        DEMON_ARTS.put("hantengu", new DemonArtInfo(900, 910, 6, "PlayerDemonArtHantenguProcedure", 10));

        // Kokushibo (Moon Breathing - Demon Version)
        DEMON_ARTS.put("kokushibo", new DemonArtInfo(1000, 1020, 16, "PlayerDemonArtKokushiboProcedure", 12));

        // Doma (Ice)
        DEMON_ARTS.put("doma", new DemonArtInfo(1100, 1115, 10, "PlayerDemonArtDomaProcedure", 10));

        // Muzan (Biokinesis)
        DEMON_ARTS.put("muzan", new DemonArtInfo(1200, 1215, 10, "PlayerDemonArtMuzanProcedure", 12));
    }

    // Map of item registry name suffix -> demon art name
    private static final Map<String, String> ITEM_TO_ART = new HashMap<>();

    static {
        ITEM_TO_ART.put("blooddemonart_rui", "rui");
        ITEM_TO_ART.put("blooddemonart_nezuko", "nezuko");
        ITEM_TO_ART.put("blooddemonart_akaza", "akaza");
        ITEM_TO_ART.put("blooddemonart_enmu", "enmu");
        ITEM_TO_ART.put("blooddemonart_gyutaro", "gyutaro");
        ITEM_TO_ART.put("blooddemonart_daki", "daki");
        ITEM_TO_ART.put("blooddemonart_gyokko", "gyokko");
        ITEM_TO_ART.put("blooddemonart_hantengu", "hantengu");
        ITEM_TO_ART.put("blooddemonart_kokushibo", "kokushibo");
        ITEM_TO_ART.put("blooddemonart_doma", "doma");
        ITEM_TO_ART.put("blooddemonart_muzan", "muzan");
    }

    /**
     * Execute a blood demon art for an NPC holding a base mod demon art item
     *
     * @param npc The NPC entity
     * @param item The blood demon art item
     * @return true if ability was executed successfully
     */
    public static boolean execute(LivingEntity npc, Item item) {
        if (npc == null || item == null) {
            return false;
        }

        ItemStack heldItem = npc.getMainHandItem();
        if (heldItem.isEmpty() || heldItem.getItem() != item) {
            ItemStack offHand = npc.getOffhandItem();
            if (!offHand.isEmpty() && offHand.getItem() == item) {
                heldItem = offHand;
            }
        }

        return execute(npc, heldItem);
    }

    /**
     * Execute a blood demon art for an NPC holding a demon art stack.
     *
     * @param npc The NPC entity
     * @param heldItem The demon art item stack
     * @return true if ability was executed successfully
     */
    public static boolean execute(LivingEntity npc, ItemStack heldItem) {
        if (npc == null || heldItem == null || heldItem.isEmpty() || npc.level().isClientSide) {
            return false;
        }
        if (com.lerdorf.kimetsunoyaibamultiplayer.effects.PuppetryHandler.isAbilityUseBlocked(npc)) {
            return false;
        }

        try {
            Item item = heldItem.getItem();
            String demonArt = resolveDemonArtId(heldItem);

            BloodDemonArtRegistry.RegisteredBloodDemonArt registeredArt = demonArt != null
                ? BloodDemonArtRegistry.getArt(demonArt)
                : null;

            if (registeredArt != null && registeredArt.getTechnique() != null
                && registeredArt.getTechnique().getFormCount() > 0) {
                return executeRegisteredArt(npc, heldItem, demonArt, registeredArt.getTechnique());
            }

            if (isBloodDemonArt(item)) {
                return executeLegacyDemonArt(npc, heldItem, item, demonArt);
            }

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Unknown blood demon art item: " + item.getDescriptionId());
            }
            return false;

        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error executing blood demon art:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if an item is a blood demon art item
     *
     * @param item The item to check
     * @return true if the item is a blood demon art
     */
    public static boolean isBloodDemonArt(Item item) {
        if (item == null) {
            return false;
        }

        if (item instanceof BloodDemonArtItem || item instanceof BloodDemonArtAxeItem) {
            return true;
        }

        // Prefer registry path if available
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (key != null) {
            String path = key.getPath();
            if (path.contains("blooddemonart") ||
                path.contains("yahaba") || path.contains("kamanue") || path.contains("enmu") ||
                path.contains("gyokko") || path.contains("doma") || path.contains("akaza") ||
                path.contains("muzan") || path.contains("rui") || path.contains("daki") ||
                path.contains("hantengu") || path.contains("zohakuten") || path.contains("nakime")) {
                return true;
            }
        }

        String descriptionId = item.getDescriptionId().toLowerCase();
        return descriptionId.contains("kimetsunoyaiba.blooddemonart");
    }

    /**
     * Get demon art name from item
     *
     * @param item The blood demon art item
     * @return Demon art name, or null if unknown
     */
    private static String getDemonArtFromItem(Item item) {
        if (item instanceof BloodDemonArtItem bloodItem) {
            return bloodItem.getArtId();
        }
        if (item instanceof BloodDemonArtAxeItem axeItem) {
            return axeItem.getArtId();
        }

        String descriptionId = item.getDescriptionId().toLowerCase();

        for (Map.Entry<String, String> entry : ITEM_TO_ART.entrySet()) {
            if (descriptionId.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static String resolveDemonArtId(ItemStack heldItem) {
        if (heldItem == null || heldItem.isEmpty()) {
            return null;
        }

        String demonArt = getDemonArtFromItem(heldItem.getItem());
        if (demonArt != null && !demonArt.isBlank()) {
            return demonArt;
        }

        return null;
    }

    private static boolean executeRegisteredArt(
            LivingEntity npc,
            ItemStack heldItem,
            String demonArt,
            BloodDemonArtTechnique technique) {
        String cooldownKey = "demon_art_" + demonArt;

        if (!NPCCooldownManager.canUseAbility(npc, cooldownKey)) {
            if (CustomNPCConfig.isDebugEnabled()) {
                int remaining = NPCCooldownManager.getRemainingCooldown(npc, cooldownKey);
                Log.debug("[KnY Custom NPCs] On cooldown: " + remaining + " ticks remaining");
            }
            return false;
        }

        int formIndex = FormSelector.selectWeightedForm(npc.getRandom(), technique.getFormCount());
        BloodDemonArtForm form = technique.getForm(formIndex);
        if (form == null) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Selected blood demon art form was null at index " + formIndex);
            }
            return false;
        }

        if (CustomNPCConfig.isDebugEnabled()) {
            Log.debug("[KnY Custom NPCs] Executing Blood Demon Art:");
            Log.debug("  NPC: " + npc.getName().getString());
            Log.debug("  Art: " + demonArt);
            Log.debug("  Form: " + FormSelector.getFormName(formIndex) + " (Index: " + formIndex + ")");
            Log.debug("  Using: Registered BloodDemonArtTechnique");
        }

        form.execute(npc, npc.level());

        int cooldownTicks = Math.max(20, form.getCooldownSeconds() * 20);
        NPCCooldownManager.setCooldown(npc, cooldownKey, cooldownTicks);
        NPCCooldownManager.setLastFormUsed(npc, demonArt, formIndex);
        return true;
    }

    private static boolean executeLegacyDemonArt(LivingEntity npc, ItemStack heldItem, Item item, String demonArt) {
        boolean genericBda = false;
        String legacyArt = demonArt;

        if (legacyArt == null) {
            if (isBloodDemonArt(item)) {
                genericBda = true;
                legacyArt = "generic";
            } else {
                return false;
            }
        }

        DemonArtInfo artInfo = DEMON_ARTS.get(legacyArt);
        if (artInfo == null && !genericBda) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] No demon art info for: " + legacyArt);
            }
            return false;
        }

        String cooldownKey = "demon_art_" + legacyArt;
        if (!NPCCooldownManager.canUseAbility(npc, cooldownKey)) {
            if (CustomNPCConfig.isDebugEnabled()) {
                int remaining = NPCCooldownManager.getRemainingCooldown(npc, cooldownKey);
                Log.debug("[KnY Custom NPCs] On cooldown: " + remaining + " ticks remaining");
            }
            return false;
        }

        int formIndex = genericBda ? 0 : FormSelector.selectWeightedForm(npc.getRandom(), artInfo.formCount);

        if (CustomNPCConfig.isDebugEnabled()) {
            Log.debug("[KnY Custom NPCs] Executing Legacy Blood Demon Art:");
            Log.debug("  NPC: " + npc.getName().getString());
            Log.debug("  Art: " + legacyArt);
            Log.debug("  Form: " + FormSelector.getFormName(formIndex) + " (Index: " + formIndex + ")");
            Log.debug("  Using: StartKekkizyutuProcedure");
        }

        heldItem.getOrCreateTag().putDouble("select", (double) formIndex);
        boolean success = callStartKekkizyutuProcedure(npc.level(), npc, heldItem);

        if (success) {
            int cooldownTicks = (genericBda ? 8 : artInfo.baseCooldownSeconds) * 20;
            NPCCooldownManager.setCooldown(npc, cooldownKey, cooldownTicks);
            NPCCooldownManager.setLastFormUsed(npc, legacyArt, formIndex);
            return true;
        }

        if (CustomNPCConfig.isDebugEnabled()) {
            Log.debug("[KnY Custom NPCs] Failed to call demon art procedure");
        }
        return false;
    }

    /**
     * Call StartKekkizyutuProcedure using reflection
     * This is the official base mod entry point for blood demon arts.
     *
     * @param world The world/level
     * @param entity The entity using the demon art
     * @param itemstack The demon art item (with "select" NBT set)
     * @return true if procedure was called successfully
     */
    private static boolean callStartKekkizyutuProcedure(LevelAccessor world, LivingEntity entity, ItemStack itemstack) {
        try {
            // Load StartKekkizyutuProcedure class
            Class<?> procedureClass = Class.forName("net.mcreator.kimetsunoyaiba.procedures.StartKekkizyutuProcedure");

            // Find execute method: execute(LevelAccessor world, Entity entity, ItemStack itemstack)
            Method executeMethod = procedureClass.getMethod("execute", LevelAccessor.class,
                net.minecraft.world.entity.Entity.class, ItemStack.class);

            // Call the method
            executeMethod.invoke(null, world, entity, itemstack);

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] ✓ StartKekkizyutuProcedure executed successfully");
            }

            return true;

        } catch (ClassNotFoundException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] StartKekkizyutuProcedure class not found");
                Log.debug("[KnY Custom NPCs] Is the base KimetsunoYaiba mod installed?");
            }
            return false;
        } catch (NoSuchMethodException e) {
            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Execute method not found in StartKekkizyutuProcedure");
                Log.debug("[KnY Custom NPCs] Base mod version may be incompatible");
            }
            return false;
        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error calling StartKekkizyutuProcedure:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inner class to store demon art information
     */
    private static class DemonArtInfo {
        final int startId;
        final int endId;
        final int formCount;
        final String procedureClassName;
        final int baseCooldownSeconds;

        DemonArtInfo(int startId, int endId, int formCount, String procedureClassName, int baseCooldownSeconds) {
            this.startId = startId;
            this.endId = endId;
            this.formCount = formCount;
            this.procedureClassName = procedureClassName;
            this.baseCooldownSeconds = baseCooldownSeconds;
        }
    }
}
