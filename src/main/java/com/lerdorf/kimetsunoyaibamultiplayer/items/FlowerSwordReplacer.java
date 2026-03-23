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
 * Replaces base mod flower breathing swords with enhanced versions
 * when EnhancedBreathingConfig.enhancedFlowerBreathing is enabled.
 *
 * Replacements:
 * - kimetsunoyaiba:nichirinsword_kanawo -> kimetsunoyaibamultiplayer:nichirinsword_kanawo
 * - kimetsunoyaiba:nichirinsword_kanae -> kimetsunoyaibamultiplayer:nichirinsword_kanae
 *
 * How it works:
 * - Checks player inventory every 20 ticks (1 second)
 * - When a base mod flower sword is detected, replaces it with our enhanced version
 * - Preserves NBT data and stack size
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class FlowerSwordReplacer {

    // Track per-player tick counters to avoid shared state issues
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounters = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Check player inventory periodically and replace base mod flower swords
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only check at end of tick phase
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Log.startupProbeOnce("FlowerSwordReplacer.onPlayerTick");

        Player player = event.player;
        if (player.level().isClientSide) {
            return; // Only run on server
        }

        // Only replace if enhanced flower breathing is enabled
        if (!EnhancedBreathingConfig.enhancedFlowerBreathing) {
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

            // Check if this is a base mod flower sword
            ItemStack replacement = null;

            if (itemId.equals(new ResourceLocation("kimetsunoyaiba", "nichirinsword_kanawo"))) {
                // Replace with our enhanced Kanao sword
                replacement = new ItemStack(ModItems.NICHIRINSWORD_KANAWO.get());
                Log.debug("[Flower Sword Replacer] Replacing kimetsunoyaiba:nichirinsword_kanawo with our Kanao sword for " + player.getName().getString());

            } else if (itemId.equals(new ResourceLocation("kimetsunoyaiba", "nichirinsword_kanae"))) {
                // Replace with our enhanced Kanae sword
                replacement = new ItemStack(ModItems.NICHIRINSWORD_KANAE.get());
                Log.debug("[Flower Sword Replacer] Replacing kimetsunoyaiba:nichirinsword_kanae with our Kanae sword for " + player.getName().getString());
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

                Log.debug("[Flower Sword Replacer] Replacement successful in slot " + i);
            }
        }
    }
}
