package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BloodDemonArtBuilderActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtFormEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 232;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int MAX_VISIBLE_ROWS = 6;

    private final BloodDemonArtBuilderData data;
    private final BloodDemonArtFormsScreen parent;
    private final int slotIndex;
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private EditBox nameBox;
    private int addMovesScroll = 0;
    private int formMovesScroll = 0;
    private int inventoryScroll = 0;
    private Mode mode = Mode.FORM_MOVES;
    private BinderStage binderStage = BinderStage.MOVE_LIST;
    private String selectedMoveId = "";
    private int selectedCustomEffectSlot = -1;
    private BloodDemonArtBuilderData.MoveView hoveredMove = null;
    private String lastSentName = "";
    private String lastObservedName = "";
    private int nameSyncDelayTicks = 0;

    public BloodDemonArtFormEditorScreen(BloodDemonArtBuilderData data, BloodDemonArtFormsScreen parent, int slotIndex) {
        super(Component.literal("Form Editor"));
        this.data = data;
        this.parent = parent;
        this.slotIndex = slotIndex;
    }

    public Screen parentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        BloodDemonArtBuilderData.FormSlotView slot = currentSlot();
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        nameBox = new EditBox(font, left + 72, top + 24, 164, 16, Component.literal("Form Name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(slot == null ? "" : slot.name());
        nameBox.setCanLoseFocus(true);
        nameBox.setFocused(true);
        setFocused(nameBox);
        lastSentName = nameBox.getValue();
        lastObservedName = nameBox.getValue();
        nameSyncDelayTicks = 0;
        addRenderableWidget(nameBox);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (nameBox == null) {
            return;
        }
        nameBox.tick();
        String current = nameBox.getValue();
        if (!current.equals(lastObservedName)) {
            lastObservedName = current;
            nameSyncDelayTicks = 0;
        }
        if (!current.equals(lastSentName)) {
            nameSyncDelayTicks++;
            if (nameSyncDelayTicks >= 8) {
                sendFormName(current);
                lastSentName = current;
                nameSyncDelayTicks = 0;
            }
        }
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

        BloodDemonArtBuilderData.FormSlotView slot = currentSlot();
        String header = slot == null ? "Invalid Slot" : "Slot " + (slot.index() + 1) + " Editor";
        guiGraphics.drawString(font, header, left + 10, top + 10, 0xF5D18A, false);
        guiGraphics.drawString(font, "Form Name:", left + 10, top + 28, 0xD6E3C5, false);

        guiGraphics.drawString(font, "Auto-saves while typing", left + 242, top + 28, 0x9F978D, false);

        if (slot == null || !slot.filled()) {
            guiGraphics.drawString(font, "This form does not exist yet.", left + 10, top + 56, 0xD9C6A1, false);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        int maxMoves = Math.min(6, Math.max(1, slot.index() + 1));
        guiGraphics.drawString(font, "Moves: " + slot.moves().size() + "/" + maxMoves, left + 10, top + 48, 0xEDE0C1, false);
        guiGraphics.drawString(font, "Cooldown: " + slot.cooldownSeconds() + "s", left + 136, top + 48, 0xD6E3C5, false);

        Rect2i formMovesMode = new Rect2i(left + 10, top + 64, 64, 16);
        Rect2i addMovesMode = new Rect2i(left + 78, top + 64, 66, 16);
        Rect2i catalystsMode = new Rect2i(left + 148, top + 64, 70, 16);
        Rect2i amplifiersMode = new Rect2i(left + 222, top + 64, 74, 16);
        Rect2i bindersMode = new Rect2i(left + 300, top + 64, 50, 16);
        drawModeButton(guiGraphics, formMovesMode, "Moves", mode == Mode.FORM_MOVES);
        drawModeButton(guiGraphics, addMovesMode, "Add", mode == Mode.ADD_MOVES);
        drawModeButton(guiGraphics, catalystsMode, "Catalyst", mode == Mode.CATALYSTS);
        drawModeButton(guiGraphics, amplifiersMode, "Amplifier", mode == Mode.AMPLIFIERS);
        drawModeButton(guiGraphics, bindersMode, "Binders", mode == Mode.BINDERS);
        actionHitboxes.add(new ActionHitbox(formMovesMode, "mode_form_moves", -1));
        actionHitboxes.add(new ActionHitbox(addMovesMode, "mode_add_moves", -1));
        actionHitboxes.add(new ActionHitbox(catalystsMode, "mode_catalysts", -1));
        actionHitboxes.add(new ActionHitbox(amplifiersMode, "mode_amplifiers", -1));
        actionHitboxes.add(new ActionHitbox(bindersMode, "mode_binders", -1));

        if (mode == Mode.FORM_MOVES) {
            renderFormMoves(guiGraphics, slot, left, top);
        } else if (mode == Mode.ADD_MOVES) {
            renderAddMoves(guiGraphics, slot, maxMoves, left, top, mouseX, mouseY);
        } else if (mode == Mode.CATALYSTS) {
            renderInventoryPicker(guiGraphics, left, top, "Catalysts in Inventory", collectCatalystEntries(), "use_catalyst");
        } else if (mode == Mode.AMPLIFIERS) {
            renderInventoryPicker(guiGraphics, left, top, "Amplifiers in Inventory", collectAmplifierEntries(), "use_amplifier");
        } else {
            renderBinderFlow(guiGraphics, slot, left, top, mouseX, mouseY);
        }

        if (hoveredMove != null) {
            guiGraphics.renderTooltip(font, Component.literal(hoveredMove.description()), mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFormMoves(GuiGraphics guiGraphics, BloodDemonArtBuilderData.FormSlotView slot, int left, int top) {
        List<BloodDemonArtBuilderData.MoveView> formMoves = slot.moves();
        formMovesScroll = clampScroll(formMovesScroll, formMoves.size());
        int start = formMovesScroll;
        int end = Math.min(formMoves.size(), start + MAX_VISIBLE_ROWS);
        int y = top + 88;
        if (formMoves.isEmpty()) {
            guiGraphics.drawString(font, "No moves yet. Use Add tab to add moves.", left + 10, y + 6, 0x8C827A, false);
        } else {
            for (int i = start; i < end; i++) {
                BloodDemonArtBuilderData.MoveView move = formMoves.get(i);
                Rect2i row = new Rect2i(left + 10, y, PANEL_WIDTH - 20, LIST_ROW_HEIGHT - 2);
                guiGraphics.fill(row.getX(), row.getY(), row.getX() + row.getWidth(), row.getY() + row.getHeight(), 0xFF3A2D28);
                guiGraphics.drawString(font, move.name(), row.getX() + 6, row.getY() + 5, 0xEDE0C1, false);

                Rect2i upButton = new Rect2i(row.getX() + row.getWidth() - 66, row.getY() + 2, 16, 12);
                Rect2i downButton = new Rect2i(row.getX() + row.getWidth() - 48, row.getY() + 2, 16, 12);
                Rect2i deleteButton = new Rect2i(row.getX() + row.getWidth() - 30, row.getY() + 2, 16, 12);
                drawMiniButton(guiGraphics, upButton, "^");
                drawMiniButton(guiGraphics, downButton, "v");
                drawMiniButton(guiGraphics, deleteButton, "x");
                actionHitboxes.add(new ActionHitbox(upButton, "move_up", i));
                actionHitboxes.add(new ActionHitbox(downButton, "move_down", i));
                actionHitboxes.add(new ActionHitbox(deleteButton, "move_delete", i));
                y += LIST_ROW_HEIGHT;
            }
        }
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 88, MAX_VISIBLE_ROWS * LIST_ROW_HEIGHT - 2, formMovesScroll, formMoves.size());
    }

    private void renderAddMoves(GuiGraphics guiGraphics, BloodDemonArtBuilderData.FormSlotView slot, int maxMoves,
                                int left, int top, int mouseX, int mouseY) {
        List<BloodDemonArtBuilderData.MoveView> moves = data.unlockedMoves();
        addMovesScroll = clampScroll(addMovesScroll, moves.size());
        int start = addMovesScroll;
        int end = Math.min(moves.size(), start + MAX_VISIBLE_ROWS);
        int y = top + 88;

        for (int i = start; i < end; i++) {
            BloodDemonArtBuilderData.MoveView move = moves.get(i);
            boolean canAdd = slot.moves().size() < maxMoves;
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
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 88, MAX_VISIBLE_ROWS * LIST_ROW_HEIGHT - 2, addMovesScroll, moves.size());
    }

    private void renderBinderFlow(GuiGraphics guiGraphics, BloodDemonArtBuilderData.FormSlotView slot, int left, int top, int mouseX, int mouseY) {
        if (binderStage == BinderStage.MOVE_LIST) {
            renderBinderMoveList(guiGraphics, slot, left, top, mouseX, mouseY);
            return;
        }

        Rect2i backButton = new Rect2i(left + 10, top + 88, 44, 12);
        drawMiniButton(guiGraphics, backButton, "<");
        actionHitboxes.add(new ActionHitbox(backButton, "binder_back", -1));

        if (binderStage == BinderStage.PICK_PRIMARY_BINDER) {
            renderInventoryPicker(guiGraphics, left, top, "Pick Binder for Primary", collectBinderEntries(), "bind_primary_inventory");
        } else if (binderStage == BinderStage.PICK_SECONDARY_BINDER) {
            renderInventoryPicker(guiGraphics, left, top, "Pick Binder for Secondary", collectBinderEntries(), "bind_secondary_inventory");
        } else if (binderStage == BinderStage.PICK_CUSTOM_EFFECT) {
            renderInventoryPicker(guiGraphics, left, top, "Pick Custom Effect Item", collectEffectEntries(), "pick_custom_effect");
        } else if (binderStage == BinderStage.PICK_CUSTOM_BINDER) {
            renderInventoryPicker(guiGraphics, left, top, "Pick Binder for Custom Effect", collectBinderEntries(), "bind_custom_inventory");
        }
    }

    private void renderBinderMoveList(GuiGraphics guiGraphics, BloodDemonArtBuilderData.FormSlotView slot, int left, int top, int mouseX, int mouseY) {
        List<BloodDemonArtBuilderData.MoveView> formMoves = slot.moves();
        inventoryScroll = clampScroll(inventoryScroll, formMoves.size());
        guiGraphics.drawString(font, "Per-Move Effect Binding", left + 10, top + 88, 0xD6E3C5, false);

        int start = inventoryScroll;
        int end = Math.min(formMoves.size(), start + MAX_VISIBLE_ROWS);
        int y = top + 102;
        if (formMoves.isEmpty()) {
            guiGraphics.drawString(font, "Add moves before binding effects.", left + 10, y + 6, 0x8C827A, false);
        } else {
            for (int i = start; i < end; i++) {
                BloodDemonArtBuilderData.MoveView move = formMoves.get(i);
                Rect2i row = new Rect2i(left + 10, y, PANEL_WIDTH - 20, LIST_ROW_HEIGHT - 2);
                guiGraphics.fill(row.getX(), row.getY(), row.getX() + row.getWidth(), row.getY() + row.getHeight(), 0xFF3A2D28);
                String binding = move.bindingSource() == null || move.bindingSource().isBlank() ? "none" : move.bindingSource();
                guiGraphics.drawString(font, move.name() + " [" + binding + "]", row.getX() + 6, row.getY() + 5, 0xEDE0C1, false);
                if (contains(row, mouseX, mouseY)) {
                    hoveredMove = move;
                }

                Rect2i primaryButton = new Rect2i(row.getX() + row.getWidth() - 128, row.getY() + 2, 38, 12);
                Rect2i secondaryButton = new Rect2i(row.getX() + row.getWidth() - 86, row.getY() + 2, 38, 12);
                Rect2i customButton = new Rect2i(row.getX() + row.getWidth() - 44, row.getY() + 2, 38, 12);
                drawMiniButton(guiGraphics, primaryButton, "Prim");
                drawMiniButton(guiGraphics, secondaryButton, "Sec");
                drawMiniButton(guiGraphics, customButton, "Custom");
                actionHitboxes.add(new ActionHitbox(primaryButton, "start_bind_primary", i));
                actionHitboxes.add(new ActionHitbox(secondaryButton, "start_bind_secondary", i));
                actionHitboxes.add(new ActionHitbox(customButton, "start_bind_custom", i));
                y += LIST_ROW_HEIGHT;
            }
        }
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 102, MAX_VISIBLE_ROWS * LIST_ROW_HEIGHT - 2, inventoryScroll, formMoves.size());
    }

    private void renderInventoryPicker(GuiGraphics guiGraphics, int left, int top, String title, List<InventoryEntry> entries, String action) {
        inventoryScroll = clampScroll(inventoryScroll, entries.size());
        guiGraphics.drawString(font, title, left + 60, top + 88, 0xD6E3C5, false);

        int start = inventoryScroll;
        int end = Math.min(entries.size(), start + MAX_VISIBLE_ROWS);
        int y = top + 102;
        if (entries.isEmpty()) {
            guiGraphics.drawString(font, "No matching items in inventory.", left + 10, y + 6, 0x8C827A, false);
        } else {
            for (int i = start; i < end; i++) {
                InventoryEntry entry = entries.get(i);
                Rect2i row = new Rect2i(left + 10, y, PANEL_WIDTH - 20, LIST_ROW_HEIGHT - 2);
                guiGraphics.fill(row.getX(), row.getY(), row.getX() + row.getWidth(), row.getY() + row.getHeight(), 0xFF3A2D28);
                guiGraphics.drawString(font, entry.label, row.getX() + 6, row.getY() + 5, 0xEDE0C1, false);
                Rect2i useButton = new Rect2i(row.getX() + row.getWidth() - 52, row.getY() + 2, 44, 12);
                drawMiniButton(guiGraphics, useButton, "Use");
                actionHitboxes.add(new ActionHitbox(useButton, action, entry.inventorySlot));
                y += LIST_ROW_HEIGHT;
            }
        }
        drawScrollBar(guiGraphics, left + PANEL_WIDTH - 12, top + 102, MAX_VISIBLE_ROWS * LIST_ROW_HEIGHT - 2, inventoryScroll, entries.size());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int direction = delta > 0 ? -1 : 1;
        if (mode == Mode.ADD_MOVES) {
            addMovesScroll += direction;
        } else if (mode == Mode.FORM_MOVES) {
            formMovesScroll += direction;
        } else {
            inventoryScroll += direction;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            for (ActionHitbox action : actionHitboxes) {
                if (!contains(action.rect, mouseX, mouseY)) {
                    continue;
                }
                if (handleModeAction(action.action)) {
                    return true;
                }
                if ("add_move".equals(action.action)) {
                    BloodDemonArtBuilderData.MoveView move = data.unlockedMoves().get(action.index);
                    ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket("add_move", slotIndex, move.id(), "form_editor", slotIndex));
                    return true;
                }
                if ("move_up".equals(action.action) || "move_down".equals(action.action) || "move_delete".equals(action.action)) {
                    BloodDemonArtBuilderData.FormSlotView slot = currentSlot();
                    if (slot != null && action.index >= 0 && action.index < slot.moves().size()) {
                        String moveIndex = Integer.toString(action.index);
                        String packetAction = "move_up".equals(action.action) ? "move_form_move_up"
                            : ("move_down".equals(action.action) ? "move_form_move_down" : "remove_form_move");
                        ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(packetAction, slotIndex, moveIndex, "form_editor", slotIndex));
                    }
                    return true;
                }
                if ("use_catalyst".equals(action.action)) {
                    ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                        "unlock_catalyst_inventory", slotIndex, Integer.toString(action.index), "form_editor", slotIndex));
                    return true;
                }
                if ("use_amplifier".equals(action.action)) {
                    ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                        "add_amplifier_inventory", slotIndex, Integer.toString(action.index), "form_editor", slotIndex));
                    return true;
                }
                if (handleBinderAction(action)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleModeAction(String action) {
        if ("mode_form_moves".equals(action)) {
            mode = Mode.FORM_MOVES;
            formMovesScroll = 0;
            return true;
        }
        if ("mode_add_moves".equals(action)) {
            mode = Mode.ADD_MOVES;
            addMovesScroll = 0;
            return true;
        }
        if ("mode_catalysts".equals(action)) {
            mode = Mode.CATALYSTS;
            inventoryScroll = 0;
            return true;
        }
        if ("mode_amplifiers".equals(action)) {
            mode = Mode.AMPLIFIERS;
            inventoryScroll = 0;
            return true;
        }
        if ("mode_binders".equals(action)) {
            mode = Mode.BINDERS;
            binderStage = BinderStage.MOVE_LIST;
            inventoryScroll = 0;
            selectedMoveId = "";
            selectedCustomEffectSlot = -1;
            return true;
        }
        return false;
    }

    private boolean handleBinderAction(ActionHitbox action) {
        if (mode != Mode.BINDERS) {
            return false;
        }
        BloodDemonArtBuilderData.FormSlotView slot = currentSlot();
        if (slot == null) {
            return false;
        }

        if ("binder_back".equals(action.action)) {
            if (binderStage == BinderStage.PICK_CUSTOM_BINDER) {
                binderStage = BinderStage.PICK_CUSTOM_EFFECT;
            } else {
                binderStage = BinderStage.MOVE_LIST;
            }
            return true;
        }

        if ("start_bind_primary".equals(action.action) || "start_bind_secondary".equals(action.action) || "start_bind_custom".equals(action.action)) {
            if (action.index < 0 || action.index >= slot.moves().size()) {
                return true;
            }
            selectedMoveId = slot.moves().get(action.index).id();
            selectedCustomEffectSlot = -1;
            inventoryScroll = 0;
            if ("start_bind_primary".equals(action.action)) {
                binderStage = BinderStage.PICK_PRIMARY_BINDER;
            } else if ("start_bind_secondary".equals(action.action)) {
                binderStage = BinderStage.PICK_SECONDARY_BINDER;
            } else {
                binderStage = BinderStage.PICK_CUSTOM_EFFECT;
            }
            return true;
        }

        if ("bind_primary_inventory".equals(action.action)) {
            ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                "bind_primary_effect_inventory", slotIndex, selectedMoveId + ";" + action.index, "form_editor", slotIndex));
            return true;
        }
        if ("bind_secondary_inventory".equals(action.action)) {
            ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                "bind_secondary_effect_inventory", slotIndex, selectedMoveId + ";" + action.index, "form_editor", slotIndex));
            return true;
        }
        if ("pick_custom_effect".equals(action.action)) {
            selectedCustomEffectSlot = action.index;
            binderStage = BinderStage.PICK_CUSTOM_BINDER;
            inventoryScroll = 0;
            return true;
        }
        if ("bind_custom_inventory".equals(action.action)) {
            ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket(
                "bind_custom_effect_inventory", slotIndex,
                selectedMoveId + ";" + action.index + ";" + selectedCustomEffectSlot,
                "form_editor", slotIndex));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                sendFormName(nameBox.getValue());
                lastSentName = nameBox.getValue();
                return true;
            }
        }
        if (nameBox != null && nameBox.keyPressed(keyCode, scanCode, modifiers)) {
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
        if (nameBox != null && nameBox.isFocused() && nameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (nameBox != null) {
            String current = nameBox.getValue();
            if (!current.equals(lastSentName)) {
                sendFormName(current);
                lastSentName = current;
            }
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void sendFormName(String name) {
        ModNetworking.sendToServer(new BloodDemonArtBuilderActionPacket("set_form_name", slotIndex, name, "form_editor", slotIndex));
    }

    private BloodDemonArtBuilderData.FormSlotView currentSlot() {
        return slotIndex >= 0 && slotIndex < data.slots().size() ? data.slots().get(slotIndex) : null;
    }

    private List<InventoryEntry> collectCatalystEntries() {
        List<InventoryEntry> entries = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return entries;
        }
        List<ItemStack> items = minecraft.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && BloodDemonArtAlchemyCatalog.isCatalyst(stack)) {
                entries.add(new InventoryEntry(i, stack.getHoverName().getString() + " x" + stack.getCount()));
            }
        }
        return entries;
    }

    private List<InventoryEntry> collectAmplifierEntries() {
        List<InventoryEntry> entries = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return entries;
        }
        List<ItemStack> items = minecraft.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && BloodDemonArtAlchemyCatalog.isAmplifier(stack)) {
                entries.add(new InventoryEntry(i, stack.getHoverName().getString() + " x" + stack.getCount()));
            }
        }
        return entries;
    }

    private List<InventoryEntry> collectBinderEntries() {
        List<InventoryEntry> entries = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            return entries;
        }
        List<ItemStack> items = minecraft.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:potion_effect_binder")) {
                entries.add(new InventoryEntry(i, stack.getHoverName().getString() + " x" + stack.getCount()));
            }
        }
        return entries;
    }

    private List<InventoryEntry> collectEffectEntries() {
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
            boolean isPotion = stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
            if (BloodDemonArtAlchemyCatalog.isInfusion(stack) || isPotion) {
                entries.add(new InventoryEntry(i, stack.getHoverName().getString() + " x" + stack.getCount()));
            }
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

    private static void drawMiniButton(GuiGraphics guiGraphics, Rect2i rect, String label) {
        guiGraphics.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0xFF705336);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, rect.getX() + rect.getWidth() / 2, rect.getY() + 2, 0xF7EBDD);
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
        FORM_MOVES,
        ADD_MOVES,
        CATALYSTS,
        AMPLIFIERS,
        BINDERS
    }

    private enum BinderStage {
        MOVE_LIST,
        PICK_PRIMARY_BINDER,
        PICK_SECONDARY_BINDER,
        PICK_CUSTOM_EFFECT,
        PICK_CUSTOM_BINDER
    }

    private record InventoryEntry(int inventorySlot, String label) {
    }

    private record ActionHitbox(Rect2i rect, String action, int index) {
    }
}
