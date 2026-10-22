package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateBlockDoorPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockDoorScreen extends AbstractContainerScreen<BlockDoorMenu> {
    private EditBox ax;
    private EditBox ay;
    private EditBox az;
    private EditBox bx;
    private EditBox by;
    private EditBox bz;
    private Direction facing;
    private Button directionButton;

    public BlockDoorScreen(BlockDoorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 260;
        imageHeight = 190;
        facing = menu.getFacing();
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 12;
        titleLabelY = 10;
        inventoryLabelY = 1000;

        int x = leftPos + 18;
        int y = topPos + 34;
        int fieldsX = x + 70;
        ax = addField(fieldsX, y, menu.getCornerA().getX());
        ay = addField(fieldsX + 56, y, menu.getCornerA().getY());
        az = addField(fieldsX + 112, y, menu.getCornerA().getZ());
        bx = addField(fieldsX, y + 34, menu.getCornerB().getX());
        by = addField(fieldsX + 56, y + 34, menu.getCornerB().getY());
        bz = addField(fieldsX + 112, y + 34, menu.getCornerB().getZ());

        directionButton = addRenderableWidget(Button.builder(directionLabel(), button -> {
            facing = Direction.values()[(facing.ordinal() + 1) % Direction.values().length];
            directionButton.setMessage(directionLabel());
        }).bounds(leftPos + 18, topPos + 104, 224, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Rotate Region 90 Degrees"), button -> rotateRegion())
            .bounds(leftPos + 18, topPos + 128, 224, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> send())
            .bounds(leftPos + 18, topPos + 152, 224, 20).build());
    }

    private EditBox addField(int x, int y, int value) {
        EditBox field = new EditBox(font, x, y, 42, 18, Component.empty());
        field.setMaxLength(5);
        field.setFilter(text -> text.isEmpty() || text.equals("-") || text.matches("-?\\d+"));
        field.setValue(Integer.toString(value));
        return addRenderableWidget(field);
    }

    private void send() {
        ModNetworking.sendToServer(new UpdateBlockDoorPacket(menu.getBlockPos(),
            new BlockPos(read(ax), read(ay), read(az)),
            new BlockPos(read(bx), read(by), read(bz)), facing));
    }

    private void rotateRegion() {
        BlockPos first = rotateLocal(new BlockPos(read(ax), read(ay), read(az)));
        BlockPos second = rotateLocal(new BlockPos(read(bx), read(by), read(bz)));
        setFields(first, second);
    }

    private void setFields(BlockPos first, BlockPos second) {
        ax.setValue(Integer.toString(first.getX()));
        ay.setValue(Integer.toString(first.getY()));
        az.setValue(Integer.toString(first.getZ()));
        bx.setValue(Integer.toString(second.getX()));
        by.setValue(Integer.toString(second.getY()));
        bz.setValue(Integer.toString(second.getZ()));
    }

    private static BlockPos rotateLocal(BlockPos position) {
        return new BlockPos(-position.getZ(), position.getY(), position.getX());
    }

    private Component directionLabel() {
        return Component.literal("Facing: " + facing.getName());
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
        guiGraphics.fill(leftPos + 12, topPos + 28, leftPos + imageWidth - 12, topPos + 178, 0xFF222222);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal("Corner A"), 18, 38, 0xD8D8D8, false);
        guiGraphics.drawString(font, Component.literal("Corner B"), 18, 72, 0xD8D8D8, false);
        guiGraphics.drawString(font, Component.literal("X"), 104, 24, 0xBEBEBE, false);
        guiGraphics.drawString(font, Component.literal("Y"), 160, 24, 0xBEBEBE, false);
        guiGraphics.drawString(font, Component.literal("Z"), 216, 24, 0xBEBEBE, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
