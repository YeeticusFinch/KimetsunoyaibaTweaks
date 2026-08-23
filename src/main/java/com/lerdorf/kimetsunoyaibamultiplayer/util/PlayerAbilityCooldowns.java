package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class PlayerAbilityCooldowns {
    private static final int CREATIVE_MAX_COOLDOWN_TICKS = 40;

    private PlayerAbilityCooldowns() {
    }

    public static int adjusted(Player player, int cooldownTicks) {
        if (player != null && player.isCreative()) {
            return Math.min(cooldownTicks, CREATIVE_MAX_COOLDOWN_TICKS);
        }
        return cooldownTicks;
    }

    public static void addCooldown(Player player, Item item, int cooldownTicks) {
        if (player == null || item == null) {
            return;
        }
        player.getCooldowns().addCooldown(item, adjusted(player, cooldownTicks));
    }
}
