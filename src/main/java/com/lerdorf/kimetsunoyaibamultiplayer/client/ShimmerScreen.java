package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class ShimmerScreen extends Screen {
    private static final String MODRINTH_URL = "https://modrinth.com/mod/shimmer!/version/1.20.1-0.3.0";
    private static final String CURSEFORGE_URL = "https://www.curseforge.com/minecraft/mc-mods/shimmer";
    private static final List<ConfigFile> CONFIG_FILES = List.of(
        new ConfigFile("shimmer-client.toml", "shimmer-client.toml"),
        new ConfigFile("kimetsunoyaiba.json", "shimmer/kimetsunoyaiba.json"),
        new ConfigFile("minecraft.json", "shimmer/minecraft.json")
    );

    private final Screen parent;
    private final boolean shimmerInstalled;
    private final boolean embeddiumDetected;
    private String actionMessage = "";
    private int actionColor = 0xFFB9AA9D;

    public ShimmerScreen(Screen parent) {
        super(Component.literal("Shimmer"));
        this.parent = parent;
        this.shimmerInstalled = ModList.get().isLoaded("shimmer");
        this.embeddiumDetected = ModList.get().isLoaded("embeddium");
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int width = panelWidth();

        addRenderableWidget(Button.builder(Component.literal("Open on Modrinth"), button -> openUrl(MODRINTH_URL))
            .bounds(left + 24, top + 112, width - 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Open on CurseForge"), button -> openUrl(CURSEFORGE_URL))
            .bounds(left + 24, top + 138, width - 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Setup Shimmer Config"), button -> installConfig())
            .bounds(left + 24, top + 174, width - 48, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(left + 24, top + 222, width - 48, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth();
        int bottom = top + 254;

        guiGraphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xAA08090D);
        guiGraphics.fill(left, top, right, bottom, 0xF01C1720);
        guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xF52A2028);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(width / 2.0F, top + 20.0F, 0.0F);
        guiGraphics.pose().scale(1.55F, 1.55F, 1.0F);
        guiGraphics.drawCenteredString(font, title, 0, 0, 0xFFF2D5A0);
        guiGraphics.pose().popPose();

        guiGraphics.drawCenteredString(font, Component.literal("Shimmer compatibility"), width / 2, top + 42, 0xFFC8B9AC);
        int statusColor = embeddiumDetected ? 0xFFE09A8E : (shimmerInstalled ? 0xFFB9E1B3 : 0xFFE6C887);
        guiGraphics.fill(left + 24, top + 60, right - 24, top + 94, 0xFF211B22);
        guiGraphics.drawCenteredString(font, Component.literal(statusText()), width / 2, top + 73, statusColor);
        guiGraphics.drawCenteredString(font, Component.literal("Install Shimmer before setting up its config."),
            width / 2, top + 99, 0xFF9B8E88);
        if (!actionMessage.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.literal(actionMessage), width / 2, top + 204, actionColor);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String statusText() {
        if (embeddiumDetected) {
            return "embeddium detected, shimmer not compatible";
        }
        return shimmerInstalled ? "shimmer installed" : "shimmer not installed";
    }

    private void openUrl(String url) {
        Util.getPlatform().openUri(url);
    }

    private void installConfig() {
        Path configDirectory = FMLPaths.CONFIGDIR.get();
        try {
            Files.createDirectories(configDirectory.resolve("shimmer"));
            for (ConfigFile configFile : CONFIG_FILES) {
                ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "shimmer_config/" + configFile.sourceName());
                try (InputStream input = Minecraft.getInstance().getResourceManager()
                        .getResource(resourceLocation).orElseThrow(() -> new IOException(
                            "Missing bundled resource " + resourceLocation)).open()) {
                    Path destination = configDirectory.resolve(configFile.destinationName());
                    Files.createDirectories(destination.getParent());
                    Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            actionMessage = "Shimmer config installed. Restart Minecraft to reload it.";
            actionColor = 0xFFB9E1B3;
        } catch (IOException | RuntimeException exception) {
            actionMessage = "Could not install Shimmer config.";
            actionColor = 0xFFE09A8E;
            System.err.println("[Kimetsunoyaiba Tweaks] Shimmer config setup failed: " + exception.getMessage());
        }
    }

    private int panelWidth() {
        return Math.min(430, width - 24);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(24, (height - 254) / 2);
    }

    private record ConfigFile(String sourceName, String destinationName) {
    }
}
