package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainingGourdMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.TrainingGourdActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class TrainingGourdScreen extends AbstractContainerScreen<TrainingGourdMenu> {
    private long shakeUntil;

    public TrainingGourdScreen(TrainingGourdMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 280;
        imageHeight = 198;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.keyJump.matches(keyCode, scanCode)) {
            if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                ModNetworking.sendToServer(new TrainingGourdActionPacket(TrainingGourdActionPacket.BREATH));
                shakeUntil = System.currentTimeMillis() + 150L;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyJump.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE51B2028);
        graphics.fill(leftPos + 12, topPos + 12, leftPos + imageWidth - 12, topPos + 154, 0xFF2A313B);

        int progressX = leftPos + 35;
        int progressY = topPos + 118;
        int progressWidth = imageWidth - 70;
        graphics.fill(progressX, progressY, progressX + progressWidth, progressY + 10, 0xFF11151A);
        int filled = menu.getMaxBreaths() <= 0 ? 0
            : (int) ((long) progressWidth * menu.getBreaths() / menu.getMaxBreaths());
        graphics.fill(progressX, progressY, progressX + filled, progressY + 10, 0xFFB95D3B);

        long shakeRemaining = shakeUntil - System.currentTimeMillis();
        int shakeOffset = shakeRemaining > 0 && (shakeRemaining / 35L) % 2L == 0L ? -2 : 0;
        graphics.renderItem(menu.getGourdStack(), leftPos + imageWidth / 2 - 8 + shakeOffset, topPos + 58);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font,
            Component.translatable("gui.kimetsunoyaibamultiplayer.training_gourd.title"),
            imageWidth / 2, 18, 0xFFF3D7A3);
        graphics.drawCenteredString(font,
            Component.translatable("gui.kimetsunoyaibamultiplayer.training_gourd.time",
                menu.getRemainingSeconds()), imageWidth / 2, 30, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
            Component.translatable("gui.kimetsunoyaibamultiplayer.training_gourd.difficulty",
                menu.getDifficulty().getName()), imageWidth / 2, 44, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
            Component.translatable("gui.kimetsunoyaibamultiplayer.training_gourd.breaths",
                menu.getBreaths(), menu.getMaxBreaths()), imageWidth / 2, 132, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
            Component.translatable("gui.kimetsunoyaibamultiplayer.training_gourd.instruction",
                Minecraft.getInstance().options.keyJump.getTranslatedKeyMessage()),
            imageWidth / 2, 146, 0xFFF3D7A3);
    }
}
