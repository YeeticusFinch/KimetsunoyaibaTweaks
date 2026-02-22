package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Replaces base mod love breathing swords with enhanced versions
 * when EnhancedBreathingConfig.enhancedLoveBreathing is enabled.
 *
 * Replacements:
 * - kimetsunoyaiba:nichirinsword_kanroji -> kimetsunoyaibamultiplayer:nichirinsword_kanroji
 *
 * How it works:
 * - Checks player inventory every tick
 * - When a base mod kanroji sword is detected, replaces it with our enhanced GeckoLib version
 * - Preserves NBT data and stack size
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class LoveSwordReplacer {

    // Track per-player tick counters to avoid shared state issues
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounters = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Check player inventory periodically and replace base mod kanroji swords
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only check at end of tick phase
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide) {
            return; // Only run on server
        }

        // Only replace if enhanced love breathing is enabled
        if (!EnhancedBreathingConfig.enhancedLoveBreathing) {
            return;
        }

        // Per-player tick counter to check every 20 ticks (1 second)
        java.util.UUID playerId = player.getUUID();
        int currentCount = playerTickCounters.getOrDefault(playerId, 0) + 1;
        if (currentCount < 20) {
            playerTickCounters.put(playerId, currentCount);
            return;
        }
        playerTickCounters.put(playerId, 0);

        // Check all inventory slots
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            // Get the item's registry name
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) {
                continue;
            }

            // Check if this is a base mod kanroji sword
            ItemStack replacement = null;

            if (itemId.equals(new ResourceLocation("kimetsunoyaiba", "nichirinsword_kanroji"))) {
                // Replace with our animated kanroji sword
                replacement = new ItemStack(ModItems.NICHIRINSWORD_KANROJI.get());
                Log.debug("[Love Sword Replacer] Replacing kimetsunoyaiba:nichirinsword_kanroji with our kanroji sword for " + player.getName().getString());
            }

            if (replacement != null) {
                // Copy stack size and NBT data
                replacement.setCount(stack.getCount());
                if (stack.hasTag()) {
                    replacement.setTag(stack.getTag().copy());
                }

                // Copy damage value
                if (stack.isDamaged()) {
                    replacement.setDamageValue(stack.getDamageValue());
                }

                // Replace in inventory
                player.getInventory().setItem(i, replacement);

                Log.debug("[Love Sword Replacer] Replacement successful in slot " + i);
            }
        }
    }
}
