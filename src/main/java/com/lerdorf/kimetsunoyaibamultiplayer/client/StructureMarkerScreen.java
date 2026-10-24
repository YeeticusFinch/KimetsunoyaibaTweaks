package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.StructureMarkerMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DeleteStructureMarkerPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateStructureMarkerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StructureMarkerScreen extends AbstractContainerScreen<StructureMarkerMenu> {
    private EditBox idField;

    public StructureMarkerScreen(StructureMarkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 105;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        idField = new EditBox(font, leftPos + 15, topPos + 38, 190, 20,
            Component.translatable("menu.kimetsunoyaibamultiplayer.structure_marker.id"));
        idField.setMaxLength(StructureMarkerEntity.MAX_ID_LENGTH);
        idField.setFilter(value -> value.codePoints().noneMatch(Character::isWhitespace));
        idField.setValue(menu.getMarkerId());
        addRenderableWidget(idField);
        setInitialFocus(idField);

        addRenderableWidget(Button.builder(
            Component.translatable("menu.kimetsunoyaibamultiplayer.structure_marker.save"),
            button -> save()).bounds(leftPos + 15, topPos + 72, 90, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("menu.kimetsunoyaibamultiplayer.structure_marker.delete"),
            button -> delete()).bounds(leftPos + 115, topPos + 72, 90, 20).build());
    }

    private void save() {
        ModNetworking.sendToServer(new UpdateStructureMarkerPacket(menu.getMarkerUuid(), idField.getValue()));
        onClose();
    }

    private void delete() {
        ModNetworking.sendToServer(new DeleteStructureMarkerPacket(menu.getMarkerUuid()));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (idField != null && idField.isFocused()
            && Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            // AbstractContainerScreen normally closes on the inventory key before
            // the focused text field gets a chance to handle the character.
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0202020);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xE03A3A3A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 15, 12, 0xFFFFFF, false);
        graphics.drawString(font,
            Component.translatable("menu.kimetsunoyaibamultiplayer.structure_marker.id"),
            15, 27, 0xD8D8D8, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
