package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateGravityBlockPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GravityBlockScreen extends AbstractContainerScreen<GravityBlockMenu> {
    private EditBox sx;
    private EditBox sy;
    private EditBox sz;
    private EditBox dx;
    private EditBox dy;
    private EditBox dz;
    private Direction gravityDirection;
    private Button directionButton;

    public GravityBlockScreen(GravityBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 210;
        imageHeight = 174;
        gravityDirection = menu.getGravityDirection();
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 12;
        titleLabelY = 10;
        inventoryLabelY = 1000;

        int x = leftPos + 18;
        int y = topPos + 34;
        sx = addField(x + 22, y, menu.getStartOffset().getX(), true);
        sy = addField(x + 78, y, menu.getStartOffset().getY(), true);
        sz = addField(x + 134, y, menu.getStartOffset().getZ(), true);
        dx = addField(x + 22, y + 34, menu.getSize().getX(), false);
        dy = addField(x + 78, y + 34, menu.getSize().getY(), false);
        dz = addField(x + 134, y + 34, menu.getSize().getZ(), false);

        directionButton = addRenderableWidget(Button.builder(directionLabel(),
            button -> {
                gravityDirection = Direction.values()[(gravityDirection.ordinal() + 1) % Direction.values().length];
                directionButton.setMessage(directionLabel());
            }).bounds(leftPos + 18, topPos + 104, 174, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("menu.kimetsunoyaibamultiplayer.gravity_block.save"),
            button -> send()).bounds(leftPos + 18, topPos + 138, 174, 20).build());
    }

    private EditBox addField(int x, int y, int value, boolean allowNegative) {
        EditBox field = new EditBox(font, x, y, 42, 18, Component.empty());
        field.setMaxLength(5);
        field.setFilter(text -> text.isEmpty()
            || (allowNegative && text.equals("-"))
            || text.matches(allowNegative ? "-?\\d+" : "\\d+"));
        field.setValue(Integer.toString(value));
        return addRenderableWidget(field);
    }

    private void send() {
        ModNetworking.sendToServer(new UpdateGravityBlockPacket(menu.getBlockPos(),
            new BlockPos(read(sx), read(sy), read(sz)),
            new BlockPos(read(dx), read(dy), read(dz)),
            gravityDirection));
    }

    private Component directionLabel() {
        return Component.literal("Facing / gravity: " + gravityDirection.getName());
    }

    private static int read(EditBox field) {
        String value = field.getValue();
        if (value.isBlank() || value.equals("-")) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0202020);
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xE03A3A3A);
        guiGraphics.fill(leftPos + 12, topPos + 28, leftPos + imageWidth - 12, topPos + 128, 0xFF222222);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal("Start"), 18, 38, 0xD8D8D8, false);
        guiGraphics.drawString(font, Component.literal("Size"), 18, 72, 0xD8D8D8, false);
        guiGraphics.drawString(font, Component.literal("X"), 44, 24, 0xBEBEBE, false);
        guiGraphics.drawString(font, Component.literal("Y"), 100, 24, 0xBEBEBE, false);
        guiGraphics.drawString(font, Component.literal("Z"), 156, 24, 0xBEBEBE, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
