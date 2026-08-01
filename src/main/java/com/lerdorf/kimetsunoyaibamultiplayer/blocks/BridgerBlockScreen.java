package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeMovement;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeType;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.EndcapPreviewMode;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.UpdateBridgerBlockPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class BridgerBlockScreen extends AbstractContainerScreen<BridgerBlockMenu> {
    private ResourceLocation bridgeType;
    private BridgeMovement movement;
    private Direction facing;
    private boolean allowEndcap;
    private boolean allowShortEndcap;
    private boolean allowConnectToOpposite;
    private boolean allowMerge;
    private boolean previewEnabled;
    private EndcapPreviewMode endcapPreviewMode;

    private int maxLengthValue;
    private int minLengthValue;
    private int priorityValue;
    private int previewLengthValue;

    private EditBox maxLength;
    private EditBox minLength;
    private EditBox previewLength;
    private EditBox priority;
    private Button typeButton;
    private Button movementButton;
    private Button facingButton;
    private Button endcapButton;
    private Button shortEndcapButton;
    private Button oppositeButton;
    private Button mergeButton;
    private Button previewButton;
    private Button endcapPreviewButton;

    private enum Tab {
        GENERAL,
        GENERATION,
        CONNECTIONS,
        PREVIEW
    }

    private Tab currentTab = Tab.GENERAL;

    public BridgerBlockScreen(BridgerBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 260;
        imageHeight = 300;
        bridgeType = menu.getBridgeType();
        movement = menu.getMovement();
        facing = menu.getFacing();
        allowEndcap = menu.isAllowEndcap();
        allowShortEndcap = menu.isAllowShortEndcap();
        allowConnectToOpposite = menu.isAllowConnectToOpposite();
        allowMerge = menu.isAllowMerge();
        previewEnabled = menu.isPreviewEnabled();
        endcapPreviewMode = menu.getEndcapPreviewMode();
        maxLengthValue = menu.getMaxLength();
        minLengthValue = menu.getMinLength();
        priorityValue = menu.getPriority();
        previewLengthValue = menu.getPreviewLength();
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 12;
        titleLabelY = 10;
        inventoryLabelY = 1000;

        imageWidth = 260;
        imageHeight = 190;

        int x = leftPos + 18;
        int y = topPos + 28;

        addRenderableWidget(Button.builder(Component.literal("General"), b -> {
            currentTab = Tab.GENERAL;
            rebuildWidgets();
        }).bounds(x, y, 54, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Gen"), b -> {
            currentTab = Tab.GENERATION;
            rebuildWidgets();
        }).bounds(x + 58, y, 48, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Conn"), b -> {
            currentTab = Tab.CONNECTIONS;
            rebuildWidgets();
        }).bounds(x + 110, y, 52, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Preview"), b -> {
            currentTab = Tab.PREVIEW;
            rebuildWidgets();
        }).bounds(x + 166, y, 60, 20).build());

        int contentY = y + 34;
        int controlX = x + 112;
        int controlW = 114;
        int rowH = 26;
        int row = 0;

        if (currentTab == Tab.GENERAL) {
            typeButton = addRenderableWidget(Button.builder(Component.empty(), b -> cycleType())
                .bounds(controlX, contentY + row++ * rowH, controlW, 20).build());

            movementButton = addRenderableWidget(Button.builder(Component.empty(), b -> cycleMovement())
                .bounds(controlX, contentY + row++ * rowH, controlW, 20).build());

            facingButton = addRenderableWidget(Button.builder(Component.empty(), b -> cycleFacing())
                .bounds(controlX, contentY + row++ * rowH, controlW, 20).build());
        }

        if (currentTab == Tab.GENERATION) {
            maxLength = addNumberField(controlX, contentY + row++ * rowH, maxLengthValue);
            minLength = addNumberField(controlX, contentY + row++ * rowH, minLengthValue);
            priority = addNumberField(controlX, contentY + row++ * rowH, priorityValue);
            previewLength = addNumberField(controlX, contentY + row++ * rowH, previewLengthValue);
        }

        if (currentTab == Tab.CONNECTIONS) {
            endcapButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                allowEndcap = !allowEndcap;
                updateLabels();
                send();
            }).bounds(x, contentY + row++ * rowH, 226, 20).build());

            shortEndcapButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                allowShortEndcap = !allowShortEndcap;
                updateLabels();
                send();
            }).bounds(x, contentY + row++ * rowH, 226, 20).build());

            oppositeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                allowConnectToOpposite = !allowConnectToOpposite;
                updateLabels();
                send();
            }).bounds(x, contentY + row++ * rowH, 226, 20).build());

            mergeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                allowMerge = !allowMerge;
                updateLabels();
                send();
            }).bounds(x, contentY + row++ * rowH, 226, 20).build());
        }

        if (currentTab == Tab.PREVIEW) {
            previewLength = addNumberField(controlX, contentY + row++ * rowH, menu.getPreviewLength());

            previewButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                previewEnabled = !previewEnabled;
                updateLabels();
                send();
            }).bounds(x, contentY + row++ * rowH, 226, 20).build());

            endcapPreviewButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                endcapPreviewMode = endcapPreviewMode.next();
                updateLabels();
                send();
            }).bounds(x, contentY + row++ * rowH, 226, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> send())
            .bounds(x, topPos + imageHeight - 32, 226, 20).build());

        sanitizeSelections(true);
        updateLabels();
    }

    private EditBox addNumberField(int x, int y, int value) {
        EditBox field = new EditBox(font, x, y, 114, 18, Component.empty());
        field.setMaxLength(5);
        field.setFilter(text -> text.isEmpty() || text.equals("-") || text.matches("-?\\d+"));
        field.setValue(Integer.toString(value));
        return addRenderableWidget(field);
    }

    private void cycleType() {
        List<BridgeType> types = new ArrayList<>(BridgeTypes.all());
        int index = 0;

        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).id().equals(bridgeType)) {
                index = i;
                break;
            }
        }

        BridgeType next = types.get((index + 1) % types.size());

        bridgeType = next.id();
        movement = next.defaultMovement();

        if (maxLength != null) {
            maxLength.setValue(Integer.toString(next.defaultMaxLength()));
        }

        if (minLength != null) {
            minLength.setValue(Integer.toString(next.minLength()));
        }

        sanitizeSelections(false);
        updateLabels();
        send();
    }

    private void cycleMovement() {
        BridgeMovement[] allowed = allowedMovements();
        int index = 0;
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i] == movement) {
                index = i;
                break;
            }
        }
        movement = allowed[(index + 1) % allowed.length];
        sanitizeSelections(false);
        updateLabels();
        send();
    }

    private void cycleFacing() {
        Direction[] allowed = allowedFacings();
        int index = 0;
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i] == facing) {
                index = i;
                break;
            }
        }
        facing = allowed[(index + 1) % allowed.length];
        updateLabels();
        send();
    }

    private void sanitizeSelections(boolean keepExistingFacing) {
        BridgeType type = BridgeTypes.getOrDefault(bridgeType);
        movement = BridgeTypes.sanitizeMovement(type, movement);
        if (movement == BridgeMovement.VERTICAL_UP || movement == BridgeMovement.VERTICAL_STAIR_UP) {
            facing = Direction.UP;
        } else if (movement == BridgeMovement.VERTICAL_DOWN || movement == BridgeMovement.VERTICAL_STAIR_DOWN) {
            facing = Direction.DOWN;
        } else if (!keepExistingFacing || !facing.getAxis().isHorizontal()) {
            facing = Direction.EAST;
        }
    }

    private BridgeMovement[] allowedMovements() {
        return BridgeTypes.allowedMovements(BridgeTypes.getOrDefault(bridgeType));
    }

    private Direction[] allowedFacings() {
        if (movement == BridgeMovement.VERTICAL_UP || movement == BridgeMovement.VERTICAL_STAIR_UP) {
            return new Direction[] { Direction.UP };
        }
        if (movement == BridgeMovement.VERTICAL_DOWN || movement == BridgeMovement.VERTICAL_STAIR_DOWN) {
            return new Direction[] { Direction.DOWN };
        }
        return new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    }

    private void send() {
        sanitizeSelections(true);

        ModNetworking.sendToServer(new UpdateBridgerBlockPacket(
            menu.getBlockPos(),
            bridgeType,
            movement,
            facing,
            readNullable(maxLength, menu.getMaxLength()),
            readNullable(minLength, menu.getMinLength()),
            allowEndcap,
            allowShortEndcap,
            allowConnectToOpposite,
            allowMerge,
            readNullable(priority, menu.getPriority()),
            previewEnabled,
            readNullable(previewLength, menu.getPreviewLength()),
            endcapPreviewMode
        ));
    }

    private int readNullable(EditBox field, int fallback) {
        return field == null ? fallback : read(field, fallback);
    }

    private static int read(EditBox field, int fallback) {
        String value = field.getValue();
        if (value.isBlank() || value.equals("-")) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void updateLabels() {
        if (typeButton != null) typeButton.setMessage(Component.literal(displayId(bridgeType)));
        if (movementButton != null) movementButton.setMessage(Component.literal(displayEnum(movement.name())));
        if (facingButton != null) facingButton.setMessage(Component.literal(displayEnum(facing.name())));
        if (endcapButton != null) endcapButton.setMessage(toggle("Full endcap", allowEndcap));
        if (shortEndcapButton != null) shortEndcapButton.setMessage(toggle("Short endcap", allowShortEndcap));
        if (oppositeButton != null) oppositeButton.setMessage(toggle("Opposite", allowConnectToOpposite));
        if (mergeButton != null) mergeButton.setMessage(toggle("Merge", allowMerge));
        if (previewButton != null) previewButton.setMessage(toggle("Preview", previewEnabled));
        if (endcapPreviewButton != null) endcapPreviewButton.setMessage(Component.literal("End preview: " + displayEnum(endcapPreviewMode.name())));
    }

    private static Component toggle(String label, boolean enabled) {
        return Component.literal(label + ": " + (enabled ? "On" : "Off"));
    }

    private static String displayId(ResourceLocation id) {
        return id.getPath();
    }

    private static String displayEnum(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0202020);
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xE03A3A3A);
        guiGraphics.fill(leftPos + 12, topPos + 24, leftPos + imageWidth - 12, topPos + imageHeight - 12, 0xFF222222);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);

        int y = 68;

        if (currentTab == Tab.GENERAL) {
            guiGraphics.drawString(font, "Bridge type", 18, y, 0xD8D8D8, false);
            guiGraphics.drawString(font, "Movement", 18, y + 26, 0xD8D8D8, false);
            guiGraphics.drawString(font, "Facing", 18, y + 52, 0xD8D8D8, false);
        }

        if (currentTab == Tab.GENERATION) {
            guiGraphics.drawString(font, "Max length", 18, y, 0xD8D8D8, false);
            guiGraphics.drawString(font, "Min length", 18, y + 26, 0xD8D8D8, false);
            guiGraphics.drawString(font, "Priority", 18, y + 52, 0xD8D8D8, false);
        }

        if (currentTab == Tab.PREVIEW) {
            guiGraphics.drawString(font, "Preview len", 18, y, 0xD8D8D8, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
