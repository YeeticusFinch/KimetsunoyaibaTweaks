package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles sword switching to prevent interference between custom and base mod swords.
 *
 * PROBLEM:
 * - Custom swords (Mist/Love) set player's "breathes" NBT to custom form IDs (20001-20007, 22001-22006)
 * - Base mod swords expect "breathes" to be in base mod range (101-1806)
 * - Switching from custom to base mod sword leaves invalid "breathes" value
 * - This breaks base mod sword abilities and backstep until you right-click to reset
 *
 * SOLUTION:
 * - Detect when player switches between custom and base mod swords
 * - Cache the "breathes" value for each sword type
 * - Restore appropriate "breathes" value when switching
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID)
public class SwordSwitchHandler {

    // NBT keys for caching breathes values
    private static final String NBT_CUSTOM_SWORD_BREATHES = "CustomSwordBreathesCache";
    private static final String NBT_BASE_MOD_BREATHES = "BaseModBreathesCache";
    private static final String NBT_LAST_SWORD_TYPE = "LastSwordType";

    // Sword type constants
    private static final String SWORD_TYPE_CUSTOM = "custom";
    private static final String SWORD_TYPE_BASE = "base";
    private static final String SWORD_TYPE_NONE = "none";

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        // Only handle players
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only handle main hand changes
        if (event.getSlot() != EquipmentSlot.MAINHAND) {
            return;
        }

        // Only on server
        if (player.level().isClientSide) {
            return;
        }

        ItemStack oldStack = event.getFrom();
        ItemStack newStack = event.getTo();

        String oldSwordType = getSwordType(oldStack);
        String newSwordType = getSwordType(newStack);

        // Only handle cases where sword type changes
        if (oldSwordType.equals(newSwordType)) {
            return;
        }

        if (Config.logDebug) {
            Log.debug("[SwordSwitchHandler] Player {} switching from {} sword to {} sword",
                player.getName().getString(), oldSwordType, newSwordType);
        }

        // Cache current breathes value before switching
        double currentBreathes = player.getPersistentData().getDouble("breathes");

        if (!oldSwordType.equals(SWORD_TYPE_NONE)) {
            // Cache the breathes value from the old sword
            if (oldSwordType.equals(SWORD_TYPE_CUSTOM)) {
                player.getPersistentData().putDouble(NBT_CUSTOM_SWORD_BREATHES, currentBreathes);
                if (Config.logDebug) {
                    Log.debug("[SwordSwitchHandler] Cached custom sword breathes value: {}", currentBreathes);
                }
            } else if (oldSwordType.equals(SWORD_TYPE_BASE)) {
                player.getPersistentData().putDouble(NBT_BASE_MOD_BREATHES, currentBreathes);
                if (Config.logDebug) {
                    Log.debug("[SwordSwitchHandler] Cached base mod breathes value: {}", currentBreathes);
                }
            }
        }

        // Restore appropriate breathes value for new sword
        if (newSwordType.equals(SWORD_TYPE_CUSTOM)) {
            // Switching TO custom sword - restore cached custom breathes or default to first form
            double cachedCustomBreathes = player.getPersistentData().getDouble(NBT_CUSTOM_SWORD_BREATHES);

            if (cachedCustomBreathes > 0) {
                // Restore cached value
                player.getPersistentData().putDouble("breathes", cachedCustomBreathes);
                if (Config.logDebug) {
                    Log.debug("[SwordSwitchHandler] Restored custom sword breathes value: {}", cachedCustomBreathes);
                }
            } else {
                // No cached value - default to first form (20001 for Mist, 22001 for Love)
                // We'll let the natural form cycling handle this, so don't change breathes
                if (Config.logDebug) {
                    Log.debug("[SwordSwitchHandler] No cached custom breathes, letting form cycling handle it");
                }
            }
        } else if (newSwordType.equals(SWORD_TYPE_BASE)) {
            // Switching TO base mod sword - restore cached base mod breathes or default to Water 1st Form
            double cachedBaseBreathes = player.getPersistentData().getDouble(NBT_BASE_MOD_BREATHES);

            if (cachedBaseBreathes > 0 && isValidBaseModFormId((int)cachedBaseBreathes)) {
                // Restore cached value
                player.getPersistentData().putDouble("breathes", cachedBaseBreathes);

                // Also update PlayerBreathingData cache
                PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
                data.setBaseModBreathesValue(cachedBaseBreathes);

                // Update form name cache if we can find it
                BaseKnYForms.BaseForm form = BaseKnYForms.forms.get((int)cachedBaseBreathes);
                if (form != null) {
                    data.setBaseModFormName(form.name);
                }

                if (Config.logDebug) {
                    Log.debug("[SwordSwitchHandler] Restored base mod breathes value: {}", cachedBaseBreathes);
                }
            } else {
                // No cached value or invalid - default to Water Breathing 1st Form (101)
                player.getPersistentData().putDouble("breathes", 101.0);

                // Update PlayerBreathingData cache
                PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
                data.setBaseModBreathesValue(101.0);
                data.setBaseModFormName("Water Breathing 1st Form: Water Surface Slash");

                if (Config.logDebug) {
                    Log.debug("[SwordSwitchHandler] No valid cached base breathes, defaulting to Water 1st Form (101)");
                }
            }
        } else if (newSwordType.equals(SWORD_TYPE_NONE)) {
            // Switching to non-sword - don't change breathes, but clear skill/guard/attack states
            // This ensures clean state when re-equipping a sword
            player.getPersistentData().putBoolean("guard", false);
            player.getPersistentData().putBoolean("attack", false);
            player.getPersistentData().putDouble("Damage", 0.0);

            if (Config.logDebug) {
                Log.debug("[SwordSwitchHandler] Cleared combat states (guard/attack/Damage)");
            }
        }

        // Update last sword type tracker
        player.getPersistentData().putString(NBT_LAST_SWORD_TYPE, newSwordType);
    }

    /**
     * Determine what type of sword the item is.
     * @return "custom" for BreathingSwordItem, "base" for base mod nichirin, "none" otherwise
     */
    private static String getSwordType(ItemStack stack) {
        if (stack.isEmpty()) {
            return SWORD_TYPE_NONE;
        }

        if (stack.getItem() instanceof BreathingSwordItem) {
            return SWORD_TYPE_CUSTOM;
        }

        if (BreathingInfoDetector.isNichirinSword(stack)) {
            return SWORD_TYPE_BASE;
        }

        return SWORD_TYPE_NONE;
    }

    /**
     * Check if a form ID is valid for the base mod.
     * Valid ranges based on BaseKnYForms.java:
     * - Bamboo: 1-6
     * - Water: 101-111
     * - Beast: 201-210
     * - Thunder: 301-307, 309
     * - Flame: 401-405, 409
     * - Wind: 501-509
     * - Stone: 601-609, 611
     * - Mist: 701-707
     * - Serpent: 801-805
     * - Sound: 901-904
     * - Moon: 1101-1110, 1114, 1116
     * - Sun: 1201-1212
     * - Flower: 1301, 1304-1305
     * - Insect: 1401-1402, 1404-1406
     * - Love: 1501-1502, 1505-1506
     * - Sakura: 1801-1802, 1805-1806
     */
    private static boolean isValidBaseModFormId(int formId) {
        return BaseKnYForms.forms.containsKey(formId);
    }
}
