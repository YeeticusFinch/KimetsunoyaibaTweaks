package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class KnyServersScreen extends Screen {
    private static final int LIST_TOP = 62;
    private static final int FOOTER_HEIGHT = 30;
    private static final int CARD_HEIGHT = 111;
    private static final int CARD_GAP = 12;
    private static final int IMAGE_HEIGHT = 60;

    private static final List<ServerEntry> SERVERS = List.of(
        new ServerEntry(
            "Wib's Verse",
            "Anime-themed action MMO-RPG. Forge your path through anime worlds shaped by power, progression and endless adventure.",
            "play.wibsverse.com",
            "https://wibsverse.com/?utm_source=kny_tweaks",
            "https://discord.wibsverse.com/",
            texture("server_photos/wibsversehero.png"),
            2000,
            667
        ),
        new ServerEntry(
            "Kimetsunoyaiba Ultra",
            "Come join us, explore the world of Demon Slayer, take on quests, become stronger, and play with the community!",
            "kimetsunoyaiba.ultraga.me:19007",
            null,
            "https://discord.gg/9HUucjqT2X",
            texture("server_photos/kny_ultra.png"),
            512,
            512
        )
    );

    private final Screen parent;
    private final List<CardButtons> cardButtons;
    private int scroll;
    private int maxScroll;

    public KnyServersScreen(Screen parent) {
        super(Component.literal("KnY Servers"));
        this.parent = parent;
        this.cardButtons = SERVERS.stream().map(entry -> new CardButtons(entry)).toList();
    }

    @Override
    protected void init() {
        for (CardButtons buttons : cardButtons) {
            if (buttons.website != null) {
                addRenderableWidget(buttons.website);
            }
            addRenderableWidget(buttons.discord);
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(width / 2 - 55, height - 25, 110, 20).build());
        updateScrollBounds();
        repositionWidgets();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = panelLeft();
        int right = panelRight();
        int listBottom = height - FOOTER_HEIGHT;
        updateScrollBounds();
        repositionWidgets();

        guiGraphics.fill(left - 4, 23, right + 4, height - 4, 0xAA08090D);
        guiGraphics.fill(left, 27, right, height - 8, 0xF01C1720);
        guiGraphics.fill(left + 2, 29, right - 2, height - 10, 0xF52A2028);
        guiGraphics.drawCenteredString(font, title, width / 2, 33, 0xFFF2D5A0);
        guiGraphics.drawCenteredString(font, Component.literal("Affiliated Demon Slayer servers"),
            width / 2, 48, 0xFFC8B9AC);

        guiGraphics.enableScissor(left + 8, LIST_TOP, right - 8, listBottom);
        for (int i = 0; i < SERVERS.size(); i++) {
            int cardY = LIST_TOP + i * (CARD_HEIGHT + CARD_GAP) - scroll;
            renderCard(guiGraphics, SERVERS.get(i), left + 10, cardY, right - left - 20);
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = LIST_TOP + 2;
            int trackHeight = Math.max(20, listBottom - LIST_TOP - 4);
            int contentHeight = SERVERS.size() * CARD_HEIGHT + (SERVERS.size() - 1) * CARD_GAP;
            int thumbHeight = Math.max(16, trackHeight * trackHeight / contentHeight);
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll;
            guiGraphics.fill(right - 7, trackTop, right - 4, trackTop + trackHeight, 0xFF1A151B);
            guiGraphics.fill(right - 7, thumbY, right - 4, thumbY + thumbHeight, 0xFFD1A267);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * 30));
        repositionWidgets();
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderCard(GuiGraphics guiGraphics, ServerEntry entry, int x, int y, int cardWidth) {
        int right = x + cardWidth;
        guiGraphics.fill(x, y, right, y + CARD_HEIGHT, 0xE52A2028);
        guiGraphics.fill(x + 2, y + 2, right - 2, y + CARD_HEIGHT - 2, 0xFF342930);

        int imageWidth = Math.min(180, Math.max(110, cardWidth / 3));
        drawTextureFit(guiGraphics, entry.image, x + 10, y + 10, imageWidth, IMAGE_HEIGHT,
            entry.imageWidth, entry.imageHeight);

        int textX = x + imageWidth + 24;
        int textWidth = Math.max(80, right - textX - 12);
        guiGraphics.drawString(font, Component.literal(entry.name), textX, y + 13, 0xFFF0D5A7, false);
        guiGraphics.drawString(font, Component.literal("IP: " + entry.ip), textX, y + 33, 0xFFD2C2B4, false);

        List<FormattedCharSequence> description = font.split(Component.literal(entry.description), textWidth);
        int descriptionY = y + 53;
        for (int i = 0; i < Math.min(3, description.size()); i++) {
            guiGraphics.drawString(font, description.get(i), textX, descriptionY + i * 11, 0xFFB5A6A0);
        }
    }

    private void drawTextureFit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y,
                                int targetWidth, int targetHeight, int textureWidth, int textureHeight) {
        float scale = Math.min(targetWidth / (float) textureWidth, targetHeight / (float) textureHeight);
        float drawWidth = textureWidth * scale;
        float drawHeight = textureHeight * scale;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + (targetWidth - drawWidth) / 2.0F,
            y + (targetHeight - drawHeight) / 2.0F, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.blit(texture, 0, 0, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        guiGraphics.pose().popPose();
    }

    private void repositionWidgets() {
        int left = panelLeft();
        int right = panelRight();
        int listBottom = height - FOOTER_HEIGHT;
        int cardWidth = right - left - 20;
        int imageWidth = Math.min(180, Math.max(110, cardWidth / 3));
        int textX = left + 10 + imageWidth + 24;
        for (int i = 0; i < cardButtons.size(); i++) {
            CardButtons buttons = cardButtons.get(i);
            int cardY = LIST_TOP + i * (CARD_HEIGHT + CARD_GAP) - scroll;
            int buttonY = cardY + CARD_HEIGHT - 25;
            boolean visible = buttonY + 20 >= LIST_TOP && buttonY <= listBottom;
            int widgetY = visible ? buttonY : -100;
            int buttonWidth = Math.max(80, Math.min(100, right - textX - 12));
            if (buttons.website != null) {
                buttons.website.setX(textX);
                buttons.website.setY(widgetY);
                buttons.website.setWidth(buttonWidth);
                buttons.discord.setX(textX + buttonWidth + 6);
            } else {
                buttons.discord.setX(textX);
            }
            buttons.discord.setY(widgetY);
            buttons.discord.setWidth(buttonWidth);
        }
    }

    private void updateScrollBounds() {
        int listHeight = Math.max(0, height - FOOTER_HEIGHT - LIST_TOP);
        int contentHeight = SERVERS.size() * CARD_HEIGHT + (SERVERS.size() - 1) * CARD_GAP;
        maxScroll = Math.max(0, contentHeight - listHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private int panelLeft() {
        return Math.max(8, (width - Math.min(820, width - 16)) / 2);
    }

    private int panelRight() {
        return Math.min(width - 8, panelLeft() + Math.min(820, width - 16));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/gui/" + path);
    }

    private static final class CardButtons {
        private final Button website;
        private final Button discord;

        private CardButtons(ServerEntry entry) {
            this.website = entry.website == null ? null : Button.builder(Component.literal("Website"),
                button -> Util.getPlatform().openUri(entry.website)).bounds(0, -100, 90, 20).build();
            this.discord = Button.builder(Component.literal("Discord"),
                button -> Util.getPlatform().openUri(entry.discord)).bounds(0, -100, 90, 20).build();
        }
    }

    private record ServerEntry(String name, String description, String ip, String website, String discord,
                               ResourceLocation image, int imageWidth, int imageHeight) {
    }
}
