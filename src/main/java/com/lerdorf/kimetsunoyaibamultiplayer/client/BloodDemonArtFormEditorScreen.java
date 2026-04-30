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

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtFormEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 216;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int MAX_VISIBLE_ROWS = 6;

    private final BloodDemonArtBuilderData data;
    private final Screen parent;
    private final int slotIndex;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private int moveScroll = 0;
    private int inventoryScroll = 0;
    private Mode mode = Mode.MOVES;
    private BloodDemonArtBuilderData.MoveView hoveredMove = null;

    public BloodDemonArtFormEditorScreen(BloodDemonArtBuilderData data, Screen parent, int slotIndex) {
        super(Component.literal("Form Editor"));
        this.data = data;
        this.parent = parent;
        this.slotIndex = slotIndex;
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
        hoveredMove = null;

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left - 4, top - 4, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 4, 0xAA09090C);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF1211B19);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + PANEL_HEIGHT - 1, 0xF5382B28);

        BloodDemonArtBuilderData.FormSlotView slot = slotIndex >= 0 && slotIndex < data.slots().size() ? data.slots().get(slotIndex) : null;
        String header = slot == null ? "Invalid Slot" : "Slot " + (slot.index() + 1) + " - " + slot.name();
        guiGraphics.drawString(font, header, left + 10, top + 10, 0xF5D18A, false);

        if (slot == null || !slot.filled()) {
            guiGraphics.drawString(font, "This form does not exist yet.", left + 10, top + 28, 0xD9C6A1, false);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        int maxMoves = Math.min(6, Math.max(1, slot.index() + 1));
        guiGraphics.drawString(font, "Moves: " + slot.moves().size() + "/" + maxMoves, left + 10, top + 26, 0xEDE0C1, false);
        guiGraphics.drawString(font, "Cooldown: " + slot.cooldownSeconds() + "s", left + 136, top + 26, 0xD6E3C5, false);

        Rect2i movesMode = new Rect2i(left + 10, top + 44, 110, 16);
        Rect2i catalystsMode = new Rect2i(left + 126, top + 44, 72, 16);
        Rect2i amplifiersMode = new Rect2i(left + 202, top + 44, 78, 16);
        Rect2i bindersMode = new Rect2i(left + 284, top + 44, 66, 16);
        drawModeButton(guiGraphics, movesMode, "Add Moves", mode == Mode.MOVES);
        drawModeButton(guiGraphics, catalystsMode, "Add Catalyst", mode == Mode.CATALYSTS);
        drawModeButton(guiGraphics, amplifiersMode, "Add Amplifier", mode == Mode.AMPLIFIERS);
        drawModeButton(guiGraphics, bindersMode, "Binders", mode == Mode.BINDERS);
        actionHitboxes.add(new ActionHitbox(movesMode, "mode_moves", -1));
        actionHitboxes.add(new ActionHitbox(catalystsMode, "mode_catalysts", -1));
        actionHitboxes.add(new ActionHitbox(amplifiersMode, "mode_amplifiers", -1));
        actionHitboxes.add(new ActionHitbox(bindersMode, "mode_binders", -1));

        if (mode == Mode.MOVES) {
            renderMoves(guiGraphics, slot, maxMoves, left, top, mouseX, mouseY);
        } else if (mode == Mode.CATALYSTS) {
            renderInventoryPicker(guiGraphics, left, top, "Catalysts in Inventory", true);
        } else if (mode == Mode.AMPLIFIERS) {
            renderInventoryPicker(guiGraphics, left, top, "Amplifiers in Inventory", false);
        } else {
            renderBinderPicker(guiGraphics, left, top);
        }

        if (hoveredMove != null) {
            guiGraphics.renderTooltip(font, Component.literal(hoveredMove.description()), mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderMoves(GuiGraphics guiGraphics, BloodDemonArtBuilderData.FormSlotView slot, int maxMoves,
                             int left, int top, int mouseX, int mouseY) {
        List<BloodDemonArtBuilderData.MoveView> moves = data.unlockedMoves();
        moveScroll = clampScroll(moveScroll, moves.size());
        int start = moveScroll;
        int end = Math.min(moves.size(), start + MAX_VISIBLE_ROWS);
        int y = top + 68;

        for (int i = start; i < end; i++) {
            BloodDemonArtBuilderData.MoveView move = moves.get(i);
            boolean alreadyInForm = slot.moves().stream().anyMatch(existing -> existing.id().equals(move.id()));
            boolean canAdd = slot.moves().size() < maxMoves && !alreadyInForm;
            Rect2i row = new Rect2i(left + 10, y, PANEL_WIDTH - 20, LIST_ROW_HEIGHT - 2);
            guiGraphics.fill(row.getX(), row.getY(), row.getX() + row.getWidth(), row.getY() + row.getHeight(), canAdd ? 0xFF3A2D28 : 0xFF2A2321);
            guiGraphics.drawString(font, move.name(), row.getX() + 6, row.getY() + 5, canAdd ? 0xEDE0C1 : 0x8C827A, false);

            Rect2i addButton = new Rect2i(row.getX() + row.getWidth() - 52, row.getY() + 2, 44, 12);
            guiGraphics.fill(addButton.getX(), addButton.getY(), addButton.getX() + addButton.getWidth(), addButton.getY() + addButton.getHeight(), canAdd ? 0xFF705336 : 0xFF3F342D);
            guiGraphics.drawCenteredString(font, "Add", addButton.getX() + addButton.getWidth() / 2, addButton.getY() + 2, canAdd ? 0xF7EBDD : 0x8C827A);
            if (canAdd) {
                actionHitboxes.add(new ActionHitbox(addButton, "add_move", i));
            }

            if (contains(row, mouseX, mouseY)) {
                hoveredMove = move;
            }
            y += LIST_ROW_HEIGHT;
        }
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 68, MAX_VISIBLE_ROWS * LIST_ROW_HEIGHT - 2, moveScroll, moves.size());
    }

    private void renderInventoryPicker(GuiGraphics guiGraphics, int left, int top, String title, boolean catalysts) {
        List<InventoryEntry> entries = collectInventoryEntries(catalysts);
        inventoryScroll = clampScroll(inventoryScroll, entries.size());
        guiGraphics.drawString(font, title, left + 10, top + 68, 0xD6E3C5, false);

        int start = inventoryScroll;
        int end = Math.min(entries.size(), start + MAX_VISIBLE_ROWS);
        int y = top + 82;
        if (entries.isEmpty()) {
            guiGraphics.drawString(font, "No matching items in inventory.", left + 10, y + 6, 0x8C827A, false);
        } else {
            for (int i = start; i < end; i++) {
                InventoryEntry entry = entries.get(i);
                Rect2i row = new Rect2i(left + 10, y, PANEL_WIDTH - 20, LIST_ROW_HEIGHT - 2);
                guiGraphics.fill(row.getX(), row.getY(), row.getX() + row.getWidth(), row.getY() + row.getHeight(), 0xFF3A2D28);
                guiGraphics.drawString(font, entry.label, row.getX() + 6, row.getY() + 5, 0xEDE0C1, false);

                Rect2i useButton = new Rect2i(row.getX() + row.getWidth() - 52, row.getY() + 2, 44, 12);
                guiGraphics.fill(useButton.getX(), useButton.getY(), useButton.getX() + useButton.getWidth(), useButton.getY() + useButton.getHeight(), 0xFF705336);
                guiGraphics.drawCenteredString(font, "Use", useButton.getX() + useButton.getWidth() / 2, useButton.getY() + 2, 0xF7EBDD);
                actionHitboxes.add(new ActionHitbox(useButton, catalysts ? "use_catalyst" : "use_amplifier", entry.inventorySlot));
                y += LIST_ROW_HEIGHT;
            }
        }
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 82, MAX_VISIBLE_ROWS * LIST_ROW_HEIGHT - 2, inventoryScroll, entries.size());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int direction = delta > 0 ? -1 : 1;
        if (mode == Mode.MOVES) {
            moveScroll += direction;
        } else {
            inventoryScroll += direction;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ActionHitbox action : actionHitboxes) {
                if (contains(action.rect, mouseX, mouseY)) {
                    switch (action.action) {
                        case "mode_moves" -> mode = Mode.MOVES;
                        case "mode_catalysts" -> mode = Mode.CATALYSTS;
                        case "mode_amplifiers" -> mode = Mode.AMPLIFIERS;
                        case "mode_binders" -> mode = Mode.BINDERS;
                        case "add_move" -> {
                            BloodDemonArtBuilderData.MoveView move = data.unlockedMoves().get(action.index);
                            ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket("add_move", slotIndex, move.id(), "form_editor", slotIndex));
                        }
                        case "use_catalyst" -> ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                            "unlock_catalyst_inventory", slotIndex, Integer.toString(action.index), "form_editor", slotIndex));
                        case "use_amplifier" -> ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                            "add_amplifier_inventory", slotIndex, Integer.toString(action.index), "form_editor", slotIndex));
                        case "use_binder" -> ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                            "consume_binder_inventory", slotIndex, Integer.toString(action.index), "form_editor", slotIndex));
                        default -> {
                        }
                    }
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

    private List<InventoryEntry> collectInventoryEntries(boolean catalysts) {
        List<InventoryEntry> entries = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return entries;
        }
        List<ItemStack> items = minecraft.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            boolean matches = catalysts ? BloodDemonArtAlchemyCatalog.isCatalyst(stack) : BloodDemonArtAlchemyCatalog.isAmplifier(stack);
            if (!matches) {
                continue;
            }
            entries.add(new InventoryEntry(i, stack.getHoverName().getString() + " x" + stack.getCount()));
        }
        return entries;
    }

    private void renderBinderPicker(GuiGraphics guiGraphics, int left, int top) {
        List<InventoryEntry> entries = collectBinderEntries();
        inventoryScroll = clampScroll(inventoryScroll, entries.size());
        guiGraphics.drawString(font, "Binders in Inventory", left + 10, top + 68, 0xD6E3C5, false);
        guiGraphics.drawString(font, "Selecting one consumes it.", left + 10, top + 80, 0x8C827A, false);
        int start = inventoryScroll;
        int end = Math.min(entries.size(), start + MAX_VISIBLE_ROWS);
        int y = top + 94;
        if (entries.isEmpty()) {
            guiGraphics.drawString(font, "No binders in inventory.", left + 10, y + 6, 0x8C827A, false);
        } else {
            for (int i = start; i < end; i++) {
                InventoryEntry entry = entries.get(i);
                Rect2i row = new Rect2i(left + 10, y, PANEL_WIDTH - 20, LIST_ROW_HEIGHT - 2);
                guiGraphics.fill(row.getX(), row.getY(), row.getX() + row.getWidth(), row.getY() + row.getHeight(), 0xFF3A2D28);
                guiGraphics.drawString(font, entry.label, row.getX() + 6, row.getY() + 5, 0xEDE0C1, false);
                Rect2i useButton = new Rect2i(row.getX() + row.getWidth() - 52, row.getY() + 2, 44, 12);
                guiGraphics.fill(useButton.getX(), useButton.getY(), useButton.getX() + useButton.getWidth(), useButton.getY() + useButton.getHeight(), 0xFF705336);
                guiGraphics.drawCenteredString(font, "Use", useButton.getX() + useButton.getWidth() / 2, useButton.getY() + 2, 0xF7EBDD);
                actionHitboxes.add(new ActionHitbox(useButton, "use_binder", entry.inventorySlot));
                y += LIST_ROW_HEIGHT;
            }
        }
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 94, MAX_VISIBLE_ROWS * LIST_ROW_HEIGHT - 2, inventoryScroll, entries.size());
    }

    private List<InventoryEntry> collectBinderEntries() {
        List<InventoryEntry> entries = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return entries;
        }
        List<ItemStack> items = minecraft.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || !BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:potion_effect_binder")) {
                continue;
            }
            entries.add(new InventoryEntry(i, stack.getHoverName().getString() + " x" + stack.getCount()));
        }
        return entries;
    }

    private static int clampScroll(int value, int totalItems) {
        int max = Math.max(0, totalItems - MAX_VISIBLE_ROWS);
        return Math.min(max, Math.max(0, value));
    }

    private static void drawModeButton(GuiGraphics guiGraphics, Rect2i rect, String label, boolean active) {
        int fill = active ? 0xFF705336 : 0xFF3A2D28;
        guiGraphics.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), fill);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, rect.getX() + rect.getWidth() / 2, rect.getY() + 4, active ? 0xF7EBDD : 0xC4B8A9);
    }

    private static void drawScrollBar(GuiGraphics guiGraphics, int x, int y, int height, int scroll, int totalItems) {
        guiGraphics.fill(x, y, x + 4, y + height, 0x66352A26);
        if (totalItems <= MAX_VISIBLE_ROWS) {
            guiGraphics.fill(x, y, x + 4, y + height, 0xAA7A6154);
            return;
        }
        int maxScroll = totalItems - MAX_VISIBLE_ROWS;
        int thumbHeight = Math.max(18, (int) (height * (MAX_VISIBLE_ROWS / (float) totalItems)));
        int travel = height - thumbHeight;
        int thumbY = y + (int) ((scroll / (float) maxScroll) * travel);
        guiGraphics.fill(x, thumbY, x + 4, thumbY + thumbHeight, 0xAA7A6154);
    }

    private static boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth()
            && mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private enum Mode {
        MOVES,
        CATALYSTS,
        AMPLIFIERS,
        BINDERS
    }

    private record InventoryEntry(int inventorySlot, String label) {
    }

    private record ActionHitbox(Rect2i rect, String action, int index) {
    }
}
