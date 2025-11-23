package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BaseModBreathingExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BloodDemonArtExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.CustomBreathingExecutor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles ranged ability triggering for Custom NPCs.
 * NPCs can use breathing forms when they have a target within range, not just on melee attacks.
 */
@Mod.EventBusSubscriber
public class CustomNPCRangedAbilityHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Only check during END phase to avoid double processing
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Check if ranged abilities are enabled
        if (!CustomNPCConfig.isEnabled() || !CustomNPCConfig.isRangedAbilitiesEnabled()) {
            return;
        }

        // Increment tick counter
        tickCounter++;

        // Only check periodically based on config
        int checkInterval = CustomNPCConfig.getRangedAbilityCheckInterval();
        if (tickCounter < checkInterval) {
            return;
        }

        // Reset counter
        tickCounter = 0;

        try {
            // Check all server levels
            for (ServerLevel level : event.getServer().getAllLevels()) {
                // Check all entities in the level
                for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                    if (!(entity instanceof Mob)) {
                        continue;
                    }

                    Mob mob = (Mob) entity;

                    // Check if this is a Custom NPC
                    if (!CustomNPCHelper.canUseAbilities(mob)) {
                        continue;
                    }

                    // Determine target strictly from AI target to prevent post-fight spam
                    LivingEntity target = mob.getTarget();

                    // Only proceed if there is an active, living target
                    if (target == null || !target.isAlive()) {
                        continue; // idle state: do not trigger ranged abilities
                    }

                    // Get the held item
                    ItemStack heldItem = mob.getMainHandItem();
                    if (heldItem.isEmpty()) {
                        continue;
                    }

                    // Range check against the active target only (prevents idle spam)
                    LivingEntity primary = target;
                    double range = CustomNPCConfig.getRangedAbilityRange();
                    double distanceSq = mob.distanceToSqr(primary);
                    if (distanceSq > range * range) {
                        continue;
                    }

                    // Check trigger chance (respect config for all swords including flower)
                    double triggerChance = CustomNPCConfig.getTriggerChance();
                    double roll = mob.getRandom().nextDouble();
                    if (roll > triggerChance) {
                        continue;
                    }

                    Item item = heldItem.getItem();

                    if (CustomNPCConfig.isDebugEnabled()) {
                        double dist = Math.sqrt(distanceSq);
                        Log.debug("[KnY Custom NPCs] [Ranged] Checking NPC: " + mob.getName().getString() +
                                         " (distance: " + String.format("%.1f", dist) + " blocks)");
                    }

                    // Try to execute ability based on item type
                    boolean abilityExecuted = false;

                    // Priority 1: This mod's custom breathing swords
                    if (CustomBreathingExecutor.isCustomBreathingSword(item)) {
                        if (CustomNPCConfig.isDebugEnabled()) {
                            Log.debug("[KnY Custom NPCs] [Ranged] → Executing custom breathing sword ability");
                        }
                        abilityExecuted = CustomBreathingExecutor.execute(mob, heldItem);
                    }
                    // Priority 2: Base mod nichirin swords
                    else if (BaseModBreathingExecutor.isBaseModNichirinSword(item)) {
                        if (CustomNPCConfig.isDebugEnabled()) {
                            Log.debug("[KnY Custom NPCs] [Ranged] → Executing base mod nichirin sword ability");
                        }
                        abilityExecuted = BaseModBreathingExecutor.execute(mob, item);
                    }
                    // Priority 3: Blood demon arts
                    else if (BloodDemonArtExecutor.isBloodDemonArt(item)) {
                        if (CustomNPCConfig.isDebugEnabled()) {
                            Log.debug("[KnY Custom NPCs] [Ranged] → Executing blood demon art ability");
                        }
                        abilityExecuted = BloodDemonArtExecutor.execute(mob, item);
                    }

                    if (abilityExecuted && CustomNPCConfig.isDebugEnabled()) {
                        Log.debug("[KnY Custom NPCs] [Ranged] ✓ Ability executed successfully");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error in ranged ability handler:");
            e.printStackTrace();
        }
    }
}
