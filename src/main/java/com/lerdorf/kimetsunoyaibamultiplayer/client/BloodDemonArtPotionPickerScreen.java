package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtPotionPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 344;
    private static final int PANEL_HEIGHT = 214;
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 6;

    private final BloodDemonArtBuilderData data;
    private final BloodDemonArtCoreConfigScreen parent;
    private final boolean primary;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private int scroll = 0;
    private boolean selfEffect;

    public BloodDemonArtPotionPickerScreen(BloodDemonArtBuilderData data, BloodDemonArtCoreConfigScreen parent, boolean primary) {
        super(Component.literal(primary ? "Set Primary Effect" : "Set Secondary Effect"));
        this.data = data;
        this.parent = parent;
        this.primary = primary;
        this.selfEffect = primary ? data.primaryPotionSelfEffect() : data.secondaryPotionSelfEffect();
    }

    public Screen parentScreen() {
        return parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll += delta > 0 ? -1 : 1;
        return true;
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

        guiGraphics.drawString(font, title, left + 12, top + 10, 0xF5D18A, false);
        guiGraphics.drawString(font, "Choose potion or infusion to consume", left + 12, top + 24, 0xB8AEA5, false);

        Rect2i modeButton = new Rect2i(left + PANEL_WIDTH - 86, top + 8, 74, 16);
        guiGraphics.fill(modeButton.getX(), modeButton.getY(), modeButton.getX() + modeButton.getWidth(), modeButton.getY() + modeButton.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(font, selfEffect ? "Self" : "Target", modeButton.getX() + modeButton.getWidth() / 2, modeButton.getY() + 4, 0xF7EBDD);
        actionHitboxes.add(new ActionHitbox(modeButton, "toggle_mode", -1));

        List<InventoryEntry> entries = collectEntries();
        scroll = clampScroll(scroll, entries.size());
        int start = scroll;
        int end = Math.min(entries.size(), start + VISIBLE_ROWS);
        int y = top + 44;
        if (entries.isEmpty()) {
            guiGraphics.drawString(font, "No potions/infusions in inventory.", left + 12, y + 8, 0x8C827A, false);
        } else {
            for (int i = start; i < end; i++) {
                InventoryEntry entry = entries.get(i);
                Rect2i row = new Rect2i(left + 10, y, PANEL_WIDTH - 20, ROW_HEIGHT - 2);
                guiGraphics.fill(row.getX(), row.getY(), row.getX() + row.getWidth(), row.getY() + row.getHeight(), 0xFF352A27);
                guiGraphics.drawString(font, entry.label, row.getX() + 6, row.getY() + 6, 0xEDE0C1, false);

                Rect2i useButton = new Rect2i(row.getX() + row.getWidth() - 46, row.getY() + 2, 38, 14);
                guiGraphics.fill(useButton.getX(), useButton.getY(), useButton.getX() + useButton.getWidth(), useButton.getY() + useButton.getHeight(), 0xFF705336);
                guiGraphics.drawCenteredString(font, "Use", useButton.getX() + useButton.getWidth() / 2, useButton.getY() + 3, 0xF7EBDD);
                actionHitboxes.add(new ActionHitbox(useButton, "use_item", entry.slot));
                y += ROW_HEIGHT;
            }
        }
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 10, top + 44, VISIBLE_ROWS * ROW_HEIGHT - 2, scroll, entries.size());

        guiGraphics.drawString(font, "ESC to go back", left + 12, top + PANEL_HEIGHT - 16, 0x9F978D, false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ActionHitbox action : actionHitboxes) {
                if (!contains(action.rect, mouseX, mouseY)) {
                    continue;
                }
                if ("toggle_mode".equals(action.action)) {
                    selfEffect = !selfEffect;
                    return true;
                }
                if ("use_item".equals(action.action)) {
                    String packetAction = primary ? "set_primary_potion_inventory" : "set_secondary_potion_inventory";
                    String value = action.index + ";" + (selfEffect ? "self" : "target");
                    ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(packetAction, -1, value, "core", -1));
                    return true;
                }
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

    private List<InventoryEntry> collectEntries() {
        List<InventoryEntry> entries = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return entries;
        }
        List<ItemStack> stacks = minecraft.player.getInventory().items;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            boolean isPotion = stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
            if (BloodDemonArtAlchemyCatalog.isInfusion(stack) || isPotion) {
                entries.add(new InventoryEntry(i, stack.getHoverName().getString() + " x" + stack.getCount()));
            }
        }
        return entries;
    }

    private static int clampScroll(int value, int totalItems) {
        int max = Math.max(0, totalItems - VISIBLE_ROWS);
        return Math.min(max, Math.max(0, value));
    }

    private static void drawScrollBar(GuiGraphics guiGraphics, int x, int y, int height, int scroll, int totalItems) {
        guiGraphics.fill(x, y, x + 3, y + height, 0x66352A26);
        if (totalItems <= VISIBLE_ROWS) {
            guiGraphics.fill(x, y, x + 3, y + height, 0xAA7A6154);
            return;
        }
        int maxScroll = totalItems - VISIBLE_ROWS;
        int thumbHeight = Math.max(16, (int) (height * (VISIBLE_ROWS / (float) totalItems)));
        int travel = height - thumbHeight;
        int thumbY = y + (int) ((scroll / (float) maxScroll) * travel);
        guiGraphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0xAA7A6154);
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth()
            && mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private record InventoryEntry(int slot, String label) {
    }

    private record ActionHitbox(Rect2i rect, String action, int index) {
    }
}
