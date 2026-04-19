package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonPropositionResponsePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.UUID;

public class DemonPropositionScreen extends Screen {
    private final UUID attackerUuid;
    private final Component attackerName;
    private final long endGameTime;
    private Button acceptButton;
    private Button rejectButton;
    private boolean responded;

    public DemonPropositionScreen(UUID attackerUuid, Component attackerName, long endGameTime) {
        super(Component.literal("Demon Proposition"));
        this.attackerUuid = attackerUuid;
        this.attackerName = attackerName;
        this.endGameTime = endGameTime;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int baseY = this.height / 2 + 24;

        this.acceptButton = addRenderableWidget(Button.builder(Component.literal("Accept"), button -> submit(true))
            .bounds(centerX - 102, baseY, 96, 20)
            .build());
        this.rejectButton = addRenderableWidget(Button.builder(Component.literal("Reject"), button -> submit(false))
            .bounds(centerX + 6, baseY, 96, 20)
            .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null || minecraft.player.level() == null) {
            return;
        }
        if (minecraft.player.level().getGameTime() >= endGameTime) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int left = this.width / 2 - 130;
        int top = this.height / 2 - 60;
        int right = this.width / 2 + 130;
        int bottom = this.height / 2 + 62;

        guiGraphics.fill(left - 3, top - 3, right + 3, bottom + 3, 0xAA050505);
        guiGraphics.fill(left, top, right, bottom, 0xF21A0F12);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 12, 0xFFE8C8);
        guiGraphics.drawCenteredString(this.font, attackerName.copy().append(" offers you demonhood."), this.width / 2, top + 34, 0xFFF3E3);

        int remainingSeconds = 0;
        if (minecraft != null && minecraft.player != null && minecraft.player.level() != null) {
            long remainingTicks = Math.max(0L, endGameTime - minecraft.player.level().getGameTime());
            remainingSeconds = Mth.ceil(remainingTicks / 20.0F);
        }
        guiGraphics.drawCenteredString(this.font, Component.literal("Time remaining: " + remainingSeconds + "s"), this.width / 2, top + 52, 0xFFCAA37A);
        guiGraphics.drawCenteredString(this.font, Component.literal("Accept to become a demon. Reject and die."), this.width / 2, top + 68, 0xFFC9B7A5);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void submit(boolean accept) {
        if (responded) {
            return;
        }
        responded = true;
        acceptButton.active = false;
        rejectButton.active = false;
        ModNetworking.sendToServer(new DemonPropositionResponsePacket(accept));
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
