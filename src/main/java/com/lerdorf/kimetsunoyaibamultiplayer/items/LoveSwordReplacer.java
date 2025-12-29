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

    // Throttle checks to every 20 ticks (1 second) for performance
    private static int tickCounter = 0;

    /**
     * Check player inventory periodically and replace base mod kanroji swords
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only replace if enhanced love breathing is enabled
        if (!EnhancedBreathingConfig.enhancedLoveBreathing) {
            return;
        }

        // Only check once per second (20 ticks)
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        Player player = event.player;
        if (player.level().isClientSide) {
            return; // Only run on server
        }

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
