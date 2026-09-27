package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import net.minecraft.world.item.Item;

import java.util.UUID;

/** Temporary render override used only while the sheath editor previews a player. */
public final class SheathPreviewState {
    private static UUID playerId;
    private static Preview preview;

    private SheathPreviewState() {
    }

    public static void set(UUID id, Item sheathItem, Item swordItem, SwordDisplayConfig.SwordDisplayPosition position) {
        playerId = id;
        preview = new Preview(sheathItem, swordItem, position);
    }

    public static Preview get(UUID id) {
        return id != null && id.equals(playerId) ? preview : null;
    }

    public static void clear() {
        playerId = null;
        preview = null;
    }

    public record Preview(Item sheathItem, Item swordItem, SwordDisplayConfig.SwordDisplayPosition position) {
    }
}
