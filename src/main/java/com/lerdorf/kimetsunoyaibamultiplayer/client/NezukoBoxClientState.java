package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NezukoBoxItem;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NezukoBoxClientState {
    private static final Map<UUID, BoxState> STATES = new ConcurrentHashMap<>();

    private NezukoBoxClientState() {
    }

    public static void set(UUID playerId, boolean hasBox, boolean open) {
        if (hasBox) {
            STATES.put(playerId, new BoxState(open));
        } else {
            STATES.remove(playerId);
        }
    }

    public static ItemStack createDisplayStack(UUID playerId) {
        BoxState state = STATES.get(playerId);
        if (state == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(ModItems.NEZUKO_BOX.get());
        if (state.open()) {
            stack.getOrCreateTag().putBoolean(NezukoBoxItem.OPEN_TAG, true);
        }
        return stack;
    }

    private record BoxState(boolean open) {
    }
}
