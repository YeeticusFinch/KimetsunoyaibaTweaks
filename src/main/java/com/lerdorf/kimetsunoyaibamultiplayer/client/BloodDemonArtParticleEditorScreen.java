package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class BloodDemonArtParticleEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 372;
    private static final int PANEL_HEIGHT = 226;

    private final BloodDemonArtBuilderData data;
    private final BloodDemonArtCoreConfigScreen parent;
    private final boolean primary;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private final List<String> allParticles = new ArrayList<>();
    private final List<String> filteredParticles = new ArrayList<>();
    private final List<String> blockIds = new ArrayList<>();
    private final Random random = new Random();
    private EditBox searchBox;
    private String lastSearch = "";
    private int selectedParticleIndex = 0;
    private int selectedBlockIndex = 0;
    private int color = 0xFFFFFF;
    private float size = 1.0F;

    public BloodDemonArtParticleEditorScreen(BloodDemonArtBuilderData data, BloodDemonArtCoreConfigScreen parent, boolean primary) {
        super(Component.literal(primary ? "Primary Particle Editor" : "Secondary Particle Editor"));
        this.data = data;
        this.parent = parent;
        this.primary = primary;
    }

    public Screen parentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        allParticles.clear();
        ForgeRegistries.PARTICLE_TYPES.getKeys().stream()
            .map(ResourceLocation::toString)
            .sorted(Comparator.naturalOrder())
            .forEach(allParticles::add);
        if (allParticles.isEmpty()) {
            allParticles.add("minecraft:smoke");
        }

        blockIds.clear();
        ForgeRegistries.BLOCKS.getKeys().stream()
            .map(ResourceLocation::toString)
            .sorted(Comparator.naturalOrder())
            .forEach(blockIds::add);
        if (blockIds.isEmpty()) {
            blockIds.add("minecraft:stone");
        }

        String selected = primary ? data.primaryParticle() : data.secondaryParticle();
        if (!allParticles.contains(selected)) {
            allParticles.add(selected);
        }

        color = primary ? data.primaryParticleColor() : data.secondaryParticleColor();
        size = primary ? data.primaryParticleSize() : data.secondaryParticleSize();
        String blockStateId = primary ? data.primaryParticleBlockStateId() : data.secondaryParticleBlockStateId();
        selectedBlockIndex = Math.max(0, blockIds.indexOf(blockStateId));

        selectedParticleIndex = Math.max(0, allParticles.indexOf(selected));
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        searchBox = new EditBox(font, left + 12, top + 30, 182, 16, Component.literal("Search particles"));
        searchBox.setValue("");
        searchBox.setFocused(true);
        addRenderableWidget(searchBox);
        refreshFiltered();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        searchBox.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        actionHitboxes.clear();
        refreshFiltered();

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left - 4, top - 4, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 4, 0xAA09090C);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF1211B19);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + PANEL_HEIGHT - 1, 0xF5382B28);

        guiGraphics.drawString(font, title, left + 12, top + 10, 0xF5D18A, false);
        searchBox.render(guiGraphics, mouseX, mouseY, partialTick);

        String selectedParticle = filteredParticles.get(selectedParticleIndex);
        guiGraphics.drawString(font, "Selected: " + selectedParticle, left + 12, top + 52, 0xF0E3C2, false);
        guiGraphics.drawString(font, "Matches: " + filteredParticles.size(), left + 12, top + 64, 0x8C827A, false);

        Rect2i prevParticle = new Rect2i(left + 12, top + 78, 56, 16);
        Rect2i nextParticle = new Rect2i(left + 74, top + 78, 56, 16);
        drawButton(guiGraphics, prevParticle, "Prev");
        drawButton(guiGraphics, nextParticle, "Next");
        actionHitboxes.add(new ActionHitbox(prevParticle, "prev_particle"));
        actionHitboxes.add(new ActionHitbox(nextParticle, "next_particle"));

        int y = top + 102;
        if (supportsColor(selectedParticle)) {
            guiGraphics.drawString(font, "Color #" + String.format("%06X", color & 0xFFFFFF), left + 12, y, 0xD6E3C5, false);
            addColorButtons(guiGraphics, left + 12, y + 12);
            y += 30;
            guiGraphics.drawString(font, "Size: " + String.format("%.2f", size), left + 12, y, 0xD6E3C5, false);
            Rect2i decSize = new Rect2i(left + 54, y - 1, 18, 14);
            Rect2i incSize = new Rect2i(left + 76, y - 1, 18, 14);
            drawButton(guiGraphics, decSize, "-");
            drawButton(guiGraphics, incSize, "+");
            actionHitboxes.add(new ActionHitbox(decSize, "size_down"));
            actionHitboxes.add(new ActionHitbox(incSize, "size_up"));
            y += 22;
        }

        if (supportsBlock(selectedParticle)) {
            guiGraphics.drawString(font, "Block: " + shortId(blockIds.get(selectedBlockIndex)), left + 12, y, 0xD6E3C5, false);
            Rect2i prevBlock = new Rect2i(left + 12, y + 12, 56, 16);
            Rect2i nextBlock = new Rect2i(left + 74, y + 12, 56, 16);
            drawButton(guiGraphics, prevBlock, "Prev");
            drawButton(guiGraphics, nextBlock, "Next");
            actionHitboxes.add(new ActionHitbox(prevBlock, "prev_block"));
            actionHitboxes.add(new ActionHitbox(nextBlock, "next_block"));
        }

        int previewLeft = left + 206;
        int previewTop = top + 30;
        int previewRight = left + PANEL_WIDTH - 12;
        int previewBottom = top + PANEL_HEIGHT - 42;
        guiGraphics.fill(previewLeft, previewTop, previewRight, previewBottom, 0x552A201C);
        guiGraphics.drawString(font, "Live Preview", previewLeft + 8, previewTop + 8, 0xF5D18A, false);
        renderPreviewParticles(previewLeft + 2, previewTop + 18, previewRight - 2, previewBottom - 2, selectedParticle);

        Rect2i applyButton = new Rect2i(left + PANEL_WIDTH - 104, top + PANEL_HEIGHT - 28, 92, 18);
        drawButton(guiGraphics, applyButton, "Apply (1 XP)");
        actionHitboxes.add(new ActionHitbox(applyButton, "apply"));

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            for (ActionHitbox hitbox : actionHitboxes) {
                if (contains(hitbox.rect, mouseX, mouseY)) {
                    handleAction(hitbox.action);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            selectedParticleIndex = Math.floorMod(selectedParticleIndex - 1, filteredParticles.size());
        } else {
            selectedParticleIndex = Math.floorMod(selectedParticleIndex + 1, filteredParticles.size());
        }
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void handleAction(String action) {
        switch (action) {
            case "prev_particle" -> selectedParticleIndex = Math.floorMod(selectedParticleIndex - 1, filteredParticles.size());
            case "next_particle" -> selectedParticleIndex = Math.floorMod(selectedParticleIndex + 1, filteredParticles.size());
            case "prev_block" -> selectedBlockIndex = Math.floorMod(selectedBlockIndex - 1, blockIds.size());
            case "next_block" -> selectedBlockIndex = Math.floorMod(selectedBlockIndex + 1, blockIds.size());
            case "size_down" -> size = Mth.clamp(size - 0.1F, 0.2F, 4.0F);
            case "size_up" -> size = Mth.clamp(size + 0.1F, 0.2F, 4.0F);
            case "r_down" -> color = adjustColor(color, -16, 0, 0);
            case "r_up" -> color = adjustColor(color, 16, 0, 0);
            case "g_down" -> color = adjustColor(color, 0, -16, 0);
            case "g_up" -> color = adjustColor(color, 0, 16, 0);
            case "b_down" -> color = adjustColor(color, 0, 0, -16);
            case "b_up" -> color = adjustColor(color, 0, 0, 16);
            case "apply" -> {
                String particleId = filteredParticles.get(selectedParticleIndex);
                String blockId = blockIds.get(selectedBlockIndex);
                String serialized = particleId + ";" + color + ";" + size + ";" + blockId;
                String packetAction = primary ? "set_primary_particle" : "set_secondary_particle";
                ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(packetAction, -1, serialized, "core", -1));
            }
            default -> {
            }
        }
    }

    private void addColorButtons(GuiGraphics guiGraphics, int x, int y) {
        Rect2i rDown = new Rect2i(x, y, 20, 14);
        Rect2i rUp = new Rect2i(x + 22, y, 20, 14);
        Rect2i gDown = new Rect2i(x + 50, y, 20, 14);
        Rect2i gUp = new Rect2i(x + 72, y, 20, 14);
        Rect2i bDown = new Rect2i(x + 100, y, 20, 14);
        Rect2i bUp = new Rect2i(x + 122, y, 20, 14);
        drawButton(guiGraphics, rDown, "R-");
        drawButton(guiGraphics, rUp, "R+");
        drawButton(guiGraphics, gDown, "G-");
        drawButton(guiGraphics, gUp, "G+");
        drawButton(guiGraphics, bDown, "B-");
        drawButton(guiGraphics, bUp, "B+");
        actionHitboxes.add(new ActionHitbox(rDown, "r_down"));
        actionHitboxes.add(new ActionHitbox(rUp, "r_up"));
        actionHitboxes.add(new ActionHitbox(gDown, "g_down"));
        actionHitboxes.add(new ActionHitbox(gUp, "g_up"));
        actionHitboxes.add(new ActionHitbox(bDown, "b_down"));
        actionHitboxes.add(new ActionHitbox(bUp, "b_up"));
    }

    private void refreshFiltered() {
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase();
        if (!query.equals(lastSearch)) {
            lastSearch = query;
            selectedParticleIndex = 0;
        }
        filteredParticles.clear();
        for (String particle : allParticles) {
            if (query.isBlank() || particle.toLowerCase().contains(query)) {
                filteredParticles.add(particle);
            }
        }
        if (filteredParticles.isEmpty()) {
            filteredParticles.add("minecraft:smoke");
        }
        selectedParticleIndex = Mth.clamp(selectedParticleIndex, 0, filteredParticles.size() - 1);
    }

    private void renderPreviewParticles(int left, int top, int right, int bottom, String particleId) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        ParticleOptions options = createParticleOptions(particleId);
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 forward = minecraft.player.getLookAngle().normalize();
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 rightAxis = worldUp.cross(forward);
        if (rightAxis.lengthSqr() < 1.0E-5) {
            rightAxis = new Vec3(1.0, 0.0, 0.0);
        } else {
            rightAxis = rightAxis.normalize();
        }
        Vec3 upAxis = forward.cross(rightAxis).normalize();

        for (int i = 0; i < 3; i++) {
            double px = centerX + (random.nextDouble() - 0.5) * 22.0;
            double py = centerY + (random.nextDouble() - 0.5) * 22.0;
            double ndcX = ((px / (double) width) - 0.5) * 2.0;
            double ndcY = (0.5 - (py / (double) height)) * 2.0;
            double depth = 1.35;
            double planeScale = depth * 0.75;

            Vec3 spawn = eye.add(forward.scale(depth))
                .add(rightAxis.scale(ndcX * planeScale))
                .add(upAxis.scale(ndcY * planeScale));
            minecraft.level.addParticle(
                options,
                spawn.x,
                spawn.y,
                spawn.z,
                (random.nextDouble() - 0.5) * 0.01,
                (random.nextDouble() - 0.5) * 0.01,
                (random.nextDouble() - 0.5) * 0.01
            );
        }
    }

    private ParticleOptions createParticleOptions(String particleId) {
        ResourceLocation id = ResourceLocation.tryParse(particleId);
        ParticleType<?> type = id == null ? null : ForgeRegistries.PARTICLE_TYPES.getValue(id);
        if ("minecraft:dust".equals(particleId) || type == ParticleTypes.DUST) {
            return new DustParticleOptions(new Vector3f(red(color), green(color), blue(color)), Mth.clamp(size, 0.2F, 4.0F));
        }
        if ("kimetsunoyaibamultiplayer:energy".equals(particleId)) {
            return new EnergyParticleOptions(new Vector3f(red(color), green(color), blue(color)), Mth.clamp(size, 0.2F, 4.0F));
        }
        if ("minecraft:block".equals(particleId)) {
            return new BlockParticleOption(ParticleTypes.BLOCK, blockForSelection().defaultBlockState());
        }
        if ("minecraft:falling_dust".equals(particleId)) {
            return new BlockParticleOption(ParticleTypes.FALLING_DUST, blockForSelection().defaultBlockState());
        }
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        return ParticleTypes.SMOKE;
    }

    private Block blockForSelection() {
        ResourceLocation id = ResourceLocation.tryParse(blockIds.get(selectedBlockIndex));
        Block block = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        return block == null ? Blocks.STONE : block;
    }

    private static void drawButton(GuiGraphics guiGraphics, Rect2i rect, String label) {
        guiGraphics.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, rect.getX() + rect.getWidth() / 2, rect.getY() + 3, 0xF7EBDD);
    }

    private static boolean supportsColor(String particleId) {
        return "minecraft:dust".equals(particleId) || "kimetsunoyaibamultiplayer:energy".equals(particleId);
    }

    private static boolean supportsBlock(String particleId) {
        return "minecraft:block".equals(particleId) || "minecraft:falling_dust".equals(particleId);
    }

    private static String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "none";
        }
        int split = id.indexOf(':');
        return split >= 0 ? id.substring(split + 1) : id;
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth()
            && mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private static int adjustColor(int current, int dr, int dg, int db) {
        int r = Math.min(255, Math.max(0, ((current >> 16) & 0xFF) + dr));
        int g = Math.min(255, Math.max(0, ((current >> 8) & 0xFF) + dg));
        int b = Math.min(255, Math.max(0, (current & 0xFF) + db));
        return (r << 16) | (g << 8) | b;
    }

    private static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    private record ActionHitbox(Rect2i rect, String action) {
    }
}
