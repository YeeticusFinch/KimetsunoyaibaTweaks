package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Key bindings for the mod
 */
public class ModKeyBindings {
    public static final String CATEGORY = "key.categories.kimetsunoyaibamultiplayer";

    public static final KeyMapping CYCLE_BREATHING_FORM = new KeyMapping(
        "key.kimetsunoyaibamultiplayer.cycle_breathing_form",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );
}
