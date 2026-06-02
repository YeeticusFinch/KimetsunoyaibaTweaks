package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtCoreConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 238;

    private final BloodDemonArtBuilderData data;
    private final BloodDemonArtBuilderScreen parent;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();

    public BloodDemonArtCoreConfigScreen(BloodDemonArtBuilderData data, BloodDemonArtBuilderScreen parent) {
        super(Component.literal("Core Configuration"));
        this.data = data;
        this.parent = parent;
    }

    public Screen parentScreen() {
        return parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        actionHitboxes.clear();

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left - 4, top - 4, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 4, 0xAA09090C);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF1211B19);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + PANEL_HEIGHT - 1, 0xF5382B28);

        guiGraphics.drawString(font, title, left + 12, top + 12, 0xF5D18A, false);
        guiGraphics.drawString(font, "Particles", left + 14, top + 36, 0xF0E3C2, false);
        drawParticleRow(guiGraphics, left + 14, top + 52, "Primary", data.primaryParticle(), true);
        drawParticleRow(guiGraphics, left + 14, top + 80, "Secondary", data.secondaryParticle(), false);

        guiGraphics.drawString(font, "Primary and Secondary Effects", left + 14, top + 124, 0xD6E3C5, false);
        drawPotionRow(guiGraphics, left + 14, top + 140, true);
        drawPotionRow(guiGraphics, left + 14, top + 168, false);

        guiGraphics.drawString(font, "Press ESC to return", left + 14, top + PANEL_HEIGHT - 18, 0x9F978D, false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ActionHitbox action : actionHitboxes) {
                if (!contains(action.rect, mouseX, mouseY)) {
                    continue;
                }
                if (minecraft == null) {
                    return true;
                }
                switch (action.action) {
                    case "edit_primary_particle" -> minecraft.setScreen(new BloodDemonArtParticleEditorScreen(data, this, true));
                    case "edit_secondary_particle" -> minecraft.setScreen(new BloodDemonArtParticleEditorScreen(data, this, false));
                    case "set_primary_effect" -> minecraft.setScreen(new BloodDemonArtPotionPickerScreen(data, this, true));
                    case "set_secondary_effect" -> minecraft.setScreen(new BloodDemonArtPotionPickerScreen(data, this, false));
                    case "toggle_primary_targeting" -> ModNetworking.sendToServer(
                        new BloodDemonArtBuilderActionPacket("toggle_primary_potion_target", -1, "", "core", -1));
                    case "toggle_secondary_targeting" -> ModNetworking.sendToServer(
                        new BloodDemonArtBuilderActionPacket("toggle_secondary_potion_target", -1, "", "core", -1));
                    default -> {
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void drawParticleRow(GuiGraphics guiGraphics, int x, int y, String label, String particleId, boolean primary) {
        guiGraphics.fill(x, y, x + PANEL_WIDTH - 28, y + 22, 0xFF2D2421);
        guiGraphics.drawString(font, label + ": " + readableId(particleId), x + 8, y + 7, 0xF0E3C2, false);
        Rect2i editButton = new Rect2i(x + PANEL_WIDTH - 128, y + 3, 94, 16);
        guiGraphics.fill(editButton.getX(), editButton.getY(), editButton.getX() + editButton.getWidth(), editButton.getY() + editButton.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(font, "Edit Particle", editButton.getX() + editButton.getWidth() / 2, editButton.getY() + 4, 0xF7EBDD);
        actionHitboxes.add(new ActionHitbox(editButton, primary ? "edit_primary_particle" : "edit_secondary_particle"));
    }

    private void drawPotionRow(GuiGraphics guiGraphics, int x, int y, boolean primary) {
        String effectId = primary ? data.primaryPotion() : data.secondaryPotion();
        boolean selfEffect = primary ? data.primaryPotionSelfEffect() : data.secondaryPotionSelfEffect();
        int duration = primary ? data.primaryPotionDurationSeconds() : data.secondaryPotionDurationSeconds();
        int amplifier = primary ? data.primaryPotionAmplifier() : data.secondaryPotionAmplifier();
        String label = primary ? "Primary" : "Secondary";

        guiGraphics.fill(x, y, x + PANEL_WIDTH - 28, y + 24, 0xFF2D2421);
        guiGraphics.drawString(font, label + ": " + potionSummary(effectId, selfEffect, duration, amplifier), x + 8, y + 8, 0xD6E3C5, false);

        Rect2i setButton = new Rect2i(x + PANEL_WIDTH - 198, y + 4, 72, 16);
        guiGraphics.fill(setButton.getX(), setButton.getY(), setButton.getX() + setButton.getWidth(), setButton.getY() + setButton.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(font, "Set Effect", setButton.getX() + setButton.getWidth() / 2, setButton.getY() + 4, 0xF7EBDD);
        actionHitboxes.add(new ActionHitbox(setButton, primary ? "set_primary_effect" : "set_secondary_effect"));

        Rect2i targetButton = new Rect2i(x + PANEL_WIDTH - 120, y + 4, 64, 16);
        guiGraphics.fill(targetButton.getX(), targetButton.getY(), targetButton.getX() + targetButton.getWidth(), targetButton.getY() + targetButton.getHeight(), 0xFF60492F);
        guiGraphics.drawCenteredString(font, selfEffect ? "Self" : "Target", targetButton.getX() + targetButton.getWidth() / 2, targetButton.getY() + 4, 0xF7EBDD);
        actionHitboxes.add(new ActionHitbox(targetButton, primary ? "toggle_primary_targeting" : "toggle_secondary_targeting"));
    }

    private static String readableId(String id) {
        if (id == null || id.isBlank()) {
            return Component.literal("None").withStyle(ChatFormatting.GRAY).getString();
        }
        return id;
    }

    private static String potionSummary(String effectId, boolean selfEffect, int durationSeconds, int amplifier) {
        if (effectId == null || effectId.isBlank()) {
            return "None";
        }
        return readableId(effectId) + " -> " + (selfEffect ? "Self" : "Target") + ", " + durationSeconds + "s, amp " + amplifier;
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth()
            && mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private record ActionHitbox(Rect2i rect, String action) {
    }
}
