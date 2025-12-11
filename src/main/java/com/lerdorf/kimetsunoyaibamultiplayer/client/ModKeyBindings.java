package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

/**
 * Key bindings for the mod
 */
public class ModKeyBindings {
    public static final String CATEGORY = "key.categories.kimetsunoyaibamultiplayer";

    /**
     * Optional dedicated reverse cycling keybind (defaults to unbound).
     * Players can bind this to any key/button they want for reverse cycling.
     * If unbound, Shift + base mod cycle key will be used for reverse cycling instead.
     */
    public static final KeyMapping CYCLE_BREATHING_FORM_BACKWARD = new KeyMapping(
        "key.kimetsunoyaibamultiplayer.cycle_breathing_form_backward",
        KeyConflictContext.IN_GAME,
        InputConstants.UNKNOWN, // Unbound by default
        CATEGORY
    );
}