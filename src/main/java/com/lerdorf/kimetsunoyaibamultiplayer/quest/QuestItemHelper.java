package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class QuestItemHelper {
    private QuestItemHelper() {
    }

    public static boolean addQuestItem(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }

        ItemStack remaining = stack.copy();
        boolean fullyAdded = player.getInventory().add(remaining);
        player.getInventory().setChanged();

        if (!remaining.isEmpty()) {
            ItemEntity dropped = player.drop(remaining.copy(), false);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
                dropped.setTarget(player.getUUID());
            }
            return false;
        }

        return fullyAdded;
    }
}
