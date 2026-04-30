package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Applies a shared cooldown across every nichirin sword item so swapping swords
 * cannot bypass breathing-form cooldowns.
 */
public final class NichirinCooldownHelper {

    private NichirinCooldownHelper() {
    }

    public static void applyCooldownToAllNichirinSwords(Player player, int cooldownTicks) {
        if (player == null || cooldownTicks <= 0) {
            return;
        }

        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item == null) {
                continue;
            }

            ItemStack probeStack = new ItemStack(item);
            if (probeStack.isEmpty() || !BreathingInfoDetector.isNichirinSword(probeStack)) {
                continue;
            }

            player.getCooldowns().addCooldown(item, cooldownTicks);
        }
    }
}
