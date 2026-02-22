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
 * Replaces base mod mist breathing swords with enhanced versions
 * when EnhancedBreathingConfig.enhancedMistBreathing is enabled.
 *
 * Replacements:
 * - kimetsunoyaiba:nichirinsword_mist -> kimetsunoyaibamultiplayer:nichirinsword_mist
 * - kimetsunoyaiba:nichirinsword_tokito -> kimetsunoyaibamultiplayer:nichirinsword_muichiro
 *
 * How it works:
 * - Checks player inventory every tick
 * - When a base mod mist sword is detected, replaces it with our enhanced version
 * - Preserves NBT data and stack size
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class MistSwordReplacer {

    // Track per-player tick counters to avoid shared state issues
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounters = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Check player inventory periodically and replace base mod mist swords
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

        // Only replace if enhanced mist breathing is enabled
        if (!EnhancedBreathingConfig.enhancedMistBreathing) {
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

            // Check if this is a base mod mist sword
            ItemStack replacement = null;

            if (itemId.equals(new ResourceLocation("kimetsunoyaiba", "nichirinsword_mist"))) {
                // Replace with our generic mist sword
                replacement = new ItemStack(ModItems.NICHIRINSWORD_MIST.get());
                Log.debug("[Mist Sword Replacer] Replacing kimetsunoyaiba:nichirinsword_mist with our mist sword for " + player.getName().getString());

            } else if (itemId.equals(new ResourceLocation("kimetsunoyaiba", "nichirinsword_tokito"))) {
                // Replace with Muichiro's sword
                replacement = new ItemStack(ModItems.NICHIRINSWORD_MUICHIRO.get());
                Log.debug("[Mist Sword Replacer] Replacing kimetsunoyaiba:nichirinsword_tokito with Muichiro's sword for " + player.getName().getString());
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

                Log.debug("[Mist Sword Replacer] Replacement successful in slot " + i);
            }
        }
    }
}
