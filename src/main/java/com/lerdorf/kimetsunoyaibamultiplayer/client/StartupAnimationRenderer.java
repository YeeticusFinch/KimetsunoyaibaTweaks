package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class StartupAnimationRenderer {
    private static final int FRAME_COUNT = 8;
    private static final int[] FRAME_WIDTHS = {165, 165, 165, 164, 165, 164, 165, 165};
    private static final int[] FRAME_HEIGHTS = {265, 267, 268, 267, 265, 262, 263, 262};
    private static final long FRAME_DURATION_MILLIS = 100L;
    private static final ResourceLocation[] FRAMES = new ResourceLocation[FRAME_COUNT];
    private static final long ANIMATION_START_TIME = Util.getMillis();

    static {
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            FRAMES[frame] = new ResourceLocation(
                    "kimetsunoyaibamultiplayer",
                    "textures/gui/startup_anim/nezuko_" + frame + ".png");
        }
    }

    private StartupAnimationRenderer() {
    }

    public static void render(GuiGraphics guiGraphics) {
        if (Config.disableStartupAnimation) {
            return;
        }

        int frame = (int) ((Util.getMillis() - ANIMATION_START_TIME) / FRAME_DURATION_MILLIS % FRAME_COUNT);
        int frameWidth = FRAME_WIDTHS[frame];
        int frameHeight = FRAME_HEIGHTS[frame];
        int x = (guiGraphics.guiWidth() - frameWidth) / 2;
        int y = Math.max(0, guiGraphics.guiHeight() - frameHeight);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y + frameHeight, 0.0F);
        guiGraphics.pose().scale(1.0F, -1.0F, 1.0F);
        guiGraphics.blit(FRAMES[frame], 0, 0, 0, 0, frameWidth, frameHeight, frameWidth, frameHeight);
        guiGraphics.pose().popPose();
    }
}
