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
 * Replaces base mod black sword with enhanced version
 * when EnhancedBreathingConfig.enhancedBlackSword is enabled.
 *
 * Replacements:
 * - kimetsunoyaiba:nichirinsword_black -> kimetsunoyaibamultiplayer:nichirinsword_black
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class BlackSwordReplacer {

    // Track per-player tick counters to avoid shared state issues
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounters = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide) {
            return; // Only run on server
        }

        if (!EnhancedBreathingConfig.enhancedBlackSword) {
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

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) {
                continue;
            }

            if (!itemId.equals(new ResourceLocation("kimetsunoyaiba", "nichirinsword_black"))) {
                continue;
            }

            ItemStack replacement = new ItemStack(ModItems.NICHIRINSWORD_BLACK.get());
            replacement.setCount(stack.getCount());
            if (stack.hasTag()) {
                replacement.setTag(stack.getTag().copy());
            }
            if (stack.isDamaged()) {
                replacement.setDamageValue(stack.getDamageValue());
            }

            player.getInventory().setItem(i, replacement);
            Log.debug("[Black Sword Replacer] Replaced base mod black sword for {} in slot {}",
                player.getName().getString(), i);
        }
    }
}
