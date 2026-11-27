package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.BiokinesesTentacleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A live editor for the six configurable Biokineses tentacle rigs. */
public final class BiokinesesTentacleEditorScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 920;
    private static final int MAX_PANEL_HEIGHT = 570;
    private static final int HEADER_HEIGHT = 58;
    private static final int FOOTER_HEIGHT = 38;
    private static final int SECTION_HEIGHT = 30;
    private static final int ROW_HEIGHT = 52;

    private final Screen parent;
    private final List<LayoutRow> rows = new ArrayList<>();
    private final List<FieldControl> fields = new ArrayList<>();
    private int panelWidth;
    private int panelHeight;
    private int leftWidth;
    private int contentHeight;
    private int scrollOffset;
    private int maxScroll;
    private Button resetButton;
    private Button backButton;
    private String status = "Changes apply and save as soon as a valid value is entered.";
    private int statusColor = 0xFFB9AA9D;
    private float orbitYaw = 25.0F;
    private float orbitPitch = 6.0F;
    private float previewZoom = 1.0F;
    private boolean draggingPreview;
    private double lastMouseX;
    private double lastMouseY;
    private boolean updatingFields;

    public BiokinesesTentacleEditorScreen(Screen parent) {
        super(Component.literal("Biokineses Tentacle Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - 16);
        panelHeight = Math.min(MAX_PANEL_HEIGHT, height - 16);
        leftWidth = Math.min(540, Math.max(220, Math.min(panelWidth - 150, panelWidth * 58 / 100)));
        rows.clear();
        fields.clear();
        buildLayout();

        resetButton = addRenderableWidget(Button.builder(Component.literal("Reset All Defaults"), button -> resetAll())
            .bounds(panelLeft() + 12, panelBottom() - 29, 142, 20).build());
        backButton = addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(panelRight() - 112, panelBottom() - 29, 100, 20).build());

        maxScroll = Math.max(0, contentHeight - viewportHeight());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        repositionFields();
    }

    private void buildLayout() {
        contentHeight = 0;
        addSection("Shared geometry and climbing behavior");
        addRow("Widths and bone", -1, BiokinesesTentacleConfig.EditorField.FLESH_WIDTH,
            BiokinesesTentacleConfig.EditorField.BONE_WIDTH,
            BiokinesesTentacleConfig.EditorField.BONE_LENGTH);
        addRow("Spikes", -1, BiokinesesTentacleConfig.EditorField.SPIKE_LENGTH,
            BiokinesesTentacleConfig.EditorField.SPIKE_WIDTH,
            BiokinesesTentacleConfig.EditorField.SPIKE_DEPTH);
        addRow("Blockface weights", -1, BiokinesesTentacleConfig.EditorField.ANCHOR_STRAY_RADIUS,
            BiokinesesTentacleConfig.EditorField.FACE_INWARD_WEIGHT,
            BiokinesesTentacleConfig.EditorField.FACE_TOWARD_USER_WEIGHT);

        for (int arm = 0; arm < BiokinesesTentacleConfig.ARM_COUNT; arm++) {
            addSection(armTitle(arm));
            addRow("Start offset", arm, BiokinesesTentacleConfig.EditorField.START_LEFT,
                BiokinesesTentacleConfig.EditorField.START_UP,
                BiokinesesTentacleConfig.EditorField.START_FORWARD);
            addRow("Start rotation", arm, BiokinesesTentacleConfig.EditorField.START_PITCH,
                BiokinesesTentacleConfig.EditorField.START_YAW,
                BiokinesesTentacleConfig.EditorField.START_ROLL);
            addRow("Default target offset", arm, BiokinesesTentacleConfig.EditorField.TARGET_LEFT,
                BiokinesesTentacleConfig.EditorField.TARGET_UP,
                BiokinesesTentacleConfig.EditorField.TARGET_FORWARD);
            addRow("Default target rotation", arm, BiokinesesTentacleConfig.EditorField.TARGET_PITCH,
                BiokinesesTentacleConfig.EditorField.TARGET_YAW,
                BiokinesesTentacleConfig.EditorField.TARGET_ROLL);
            addRow("Climbing anchor offset", arm, BiokinesesTentacleConfig.EditorField.ANCHOR_LEFT,
                BiokinesesTentacleConfig.EditorField.ANCHOR_UP,
                BiokinesesTentacleConfig.EditorField.ANCHOR_FORWARD);
            addRow("Climbing anchor radius", arm, BiokinesesTentacleConfig.EditorField.ANCHOR_RADIUS);
            addRow("Visual arc", arm, BiokinesesTentacleConfig.EditorField.ARC_LEFT,
                BiokinesesTentacleConfig.EditorField.ARC_UP,
                BiokinesesTentacleConfig.EditorField.ARC_FORWARD);
        }
    }

    private void addSection(String title) {
        rows.add(LayoutRow.section(title, contentHeight));
        contentHeight += SECTION_HEIGHT;
    }

    private void addRow(String title, int arm, BiokinesesTentacleConfig.EditorField... editorFields) {
        List<FieldControl> controls = new ArrayList<>();
        for (BiokinesesTentacleConfig.EditorField field : editorFields) {
            EditBox box = new EditBox(font, 0, -100, 80, 20, Component.literal(field.label()));
            box.setMaxLength(20);
            box.setValue(format(BiokinesesTentacleConfig.editorValue(field, arm)));
            FieldControl control = new FieldControl(field, arm, box);
            box.setResponder(value -> updateField(control));
            controls.add(control);
            fields.add(control);
            addRenderableWidget(box);
        }
        rows.add(LayoutRow.values(title, contentHeight, controls));
        contentHeight += ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();
        int right = panelRight();
        int bottom = panelBottom();
        int listLeft = left + 10;
        int listRight = left + leftWidth - 8;
        int listTop = viewportTop();
        int listBottom = viewportBottom();

        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xAA08090D);
        graphics.fill(left, top, right, bottom, 0xF01C1720);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xF52A2028);
        graphics.drawCenteredString(font, title, left + panelWidth / 2, top + 10, 0xFFF2D5A0);
        graphics.drawString(font, Component.literal("Edit values on the left. Drag the preview to orbit; scroll over it to zoom."),
            left + 14, top + 32, 0xFFC8B9AC, false);
        graphics.fill(left + leftWidth, top + 48, left + leftWidth + 1, bottom - 42, 0x557A6A5B);

        graphics.fill(listLeft, listTop, listRight, listBottom, 0x55110C0A);
        graphics.enableScissor(listLeft, listTop, listRight, listBottom);
        renderRows(graphics, listLeft, listRight);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();

        renderScrollbar(graphics, listRight);
        renderPreview(graphics, mouseX, mouseY);
        graphics.drawCenteredString(font, Component.literal(status), width / 2, bottom - 15, statusColor);

        // Footer widgets are intentionally rendered after the scissored editor area.
        resetButton.render(graphics, mouseX, mouseY, partialTick);
        backButton.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRows(GuiGraphics graphics, int listLeft, int listRight) {
        int width = listRight - listLeft;
        int fieldGap = 7;
        int fieldWidth = Math.max(40, (width - fieldGap * 2 - 12) / 3);
        for (LayoutRow row : rows) {
            int y = viewportTop() + row.contentY - scrollOffset;
            if (row.section) {
                graphics.fill(listLeft + 3, y + 3, listRight - 3, y + SECTION_HEIGHT - 3, 0xFF4A3540);
                graphics.drawString(font, Component.literal(row.title), listLeft + 10, y + 10, 0xFFF0D5A7, false);
                continue;
            }
            graphics.fill(listLeft + 3, y + 2, listRight - 3, y + ROW_HEIGHT - 3, 0xFF30272E);
            graphics.drawString(font, Component.literal(row.title), listLeft + 10, y + 6, 0xFFE7DCC7, false);
            for (int index = 0; index < row.controls.size(); index++) {
                FieldControl control = row.controls.get(index);
                int x = listLeft + 10 + index * (fieldWidth + fieldGap);
                graphics.drawString(font, Component.literal(shortLabel(control.field)), x, y + 20,
                    0xFFC8B9AC, false);
            }
        }
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelLeft() + leftWidth + 12;
        int right = panelRight() - 12;
        int top = panelTop() + 64;
        int bottom = panelBottom() - 48;
        int centerX = (left + right) / 2;
        graphics.drawCenteredString(font, Component.literal("Third-person preview"), centerX, top - 18, 0xFFD4B58D);
        graphics.fill(left, top, right, bottom, 0x66110C0A);
        graphics.fill(centerX, top + 8, centerX + 1, bottom - 8, 0x221F6A5A);
        graphics.fill(left + 8, (top + bottom) / 2, right - 8, (top + bottom) / 2 + 1, 0x221F6A5A);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int playerScale = Math.max(34, Math.min(70, (bottom - top) * 2 / 5));
            int baseY = bottom - 14;
            Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) -Math.toRadians(orbitYaw))
                .rotateX((float) Math.toRadians(orbitPitch));
            InventoryScreen.renderEntityInInventory(graphics, centerX, baseY, playerScale, rotation,
                new Quaternionf(), player);
            BiokinesesArmRenderer.renderPreviewArms(graphics, centerX, baseY,
                playerScale * previewZoom, orbitYaw, orbitPitch);
        } else {
            graphics.drawCenteredString(font, Component.literal("Join a world to preview the rig."), centerX,
                (top + bottom) / 2, 0xFFC8B9AC);
        }
        graphics.drawCenteredString(font, Component.literal("Yaw " + Math.round(orbitYaw) + "°  |  Pitch "
            + Math.round(orbitPitch) + "°  |  Zoom " + String.format("%.1f", previewZoom)), centerX,
            bottom + 8, 0xFFB9AA9D);
        if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
            graphics.fill(left + 6, bottom - 25, right - 6, bottom - 7, 0xAA08090D);
            graphics.drawCenteredString(font, Component.literal("Drag to rotate"), centerX, bottom - 20, 0xFFE7DCC7);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int listRight) {
        if (maxScroll <= 0) {
            return;
        }
        int top = viewportTop() + 2;
        int height = viewportHeight() - 4;
        int thumbHeight = Math.max(20, height * height / contentHeight);
        int thumbY = top + (height - thumbHeight) * scrollOffset / maxScroll;
        graphics.fill(listRight - 5, top, listRight - 2, top + height, 0xFF1A151B);
        graphics.fill(listRight - 5, thumbY, listRight - 2, thumbY + thumbHeight, 0xFFD1A267);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= panelLeft() + 10 && mouseX <= panelLeft() + leftWidth - 8
            && mouseY >= viewportTop() && mouseY <= viewportBottom()) {
            scrollOffset = Math.max(0, Math.min(maxScroll,
                scrollOffset - (int) Math.round(delta * 38.0D)));
            repositionFields();
            return true;
        }
        if (isInsidePreview(mouseX, mouseY)) {
            previewZoom = Math.max(0.65F, Math.min(1.55F, previewZoom + (float) delta * 0.08F));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsidePreview(mouseX, mouseY)) {
            draggingPreview = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview && button == 0) {
            orbitYaw = normalizeDegrees(orbitYaw + (float) (mouseX - lastMouseX) * 0.7F);
            orbitPitch = Math.max(-55.0F, Math.min(55.0F,
                orbitPitch + (float) (mouseY - lastMouseY) * 0.55F));
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPreview && button == 0) {
            draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateField(FieldControl control) {
        if (updatingFields) {
            return;
        }
        try {
            double value = Double.parseDouble(control.box.getValue().trim());
            if (!Double.isFinite(value) || value < control.field.minimum() || value > control.field.maximum()) {
                return;
            }
            BiokinesesTentacleConfig.setEditorValue(control.field, control.arm, value);
            status = control.field.label() + " updated immediately.";
            statusColor = 0xFFB9E1B3;
        } catch (IllegalArgumentException ignored) {
            status = "Enter a value between " + format(control.field.minimum()) + " and "
                + format(control.field.maximum()) + ".";
            statusColor = 0xFFE09A8E;
        }
    }

    private void resetAll() {
        updatingFields = true;
        try {
            BiokinesesTentacleConfig.resetEditorValues();
            for (FieldControl control : fields) {
                double value = BiokinesesTentacleConfig.editorDefault(control.field, control.arm);
                control.box.setValue(format(value));
            }
        } finally {
            updatingFields = false;
        }
        status = "All tentacle values reset to their defaults.";
        statusColor = 0xFFE6C887;
    }

    private void repositionFields() {
        int listLeft = panelLeft() + 10;
        int listRight = panelLeft() + leftWidth - 8;
        int width = listRight - listLeft;
        int fieldGap = 7;
        int fieldWidth = Math.max(40, (width - fieldGap * 2 - 12) / 3);
        for (LayoutRow row : rows) {
            if (row.section) {
                continue;
            }
            int y = viewportTop() + row.contentY - scrollOffset + 28;
            for (int index = 0; index < row.controls.size(); index++) {
                FieldControl control = row.controls.get(index);
                int x = listLeft + 10 + index * (fieldWidth + fieldGap);
                boolean visible = y + 20 >= viewportTop() && y <= viewportBottom();
                control.box.setX(x);
                control.box.setY(visible ? y : -100);
                if (!visible) {
                    control.box.setFocused(false);
                }
            }
        }
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        int left = panelLeft() + leftWidth + 12;
        int right = panelRight() - 12;
        int top = panelTop() + 64;
        int bottom = panelBottom() - 48;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private int panelLeft() {
        return (width - panelWidth) / 2;
    }

    private int panelRight() {
        return panelLeft() + panelWidth;
    }

    private int panelTop() {
        return (height - panelHeight) / 2;
    }

    private int panelBottom() {
        return panelTop() + panelHeight;
    }

    private int viewportTop() {
        return panelTop() + HEADER_HEIGHT;
    }

    private int viewportBottom() {
        return panelBottom() - FOOTER_HEIGHT;
    }

    private int viewportHeight() {
        return Math.max(1, viewportBottom() - viewportTop());
    }

    private String armTitle(int arm) {
        String name = BiokinesesTentacleConfig.ARM_NAMES[arm].replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " tentacle";
    }

    private String shortLabel(BiokinesesTentacleConfig.EditorField field) {
        String name = field.name();
        if (name.endsWith("LEFT")) return "Left";
        if (name.endsWith("UP")) return "Up";
        if (name.endsWith("FORWARD")) return "Forward";
        if (name.endsWith("PITCH")) return "Pitch (deg)";
        if (name.endsWith("YAW")) return "Yaw (deg)";
        if (name.endsWith("ROLL")) return "Roll (deg)";
        return field.label();
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private float normalizeDegrees(float value) {
        return (value % 360.0F + 360.0F) % 360.0F;
    }

    private static final class FieldControl {
        private final BiokinesesTentacleConfig.EditorField field;
        private final int arm;
        private final EditBox box;

        private FieldControl(BiokinesesTentacleConfig.EditorField field, int arm, EditBox box) {
            this.field = field;
            this.arm = arm;
            this.box = box;
        }
    }

    private static final class LayoutRow {
        private final String title;
        private final int contentY;
        private final List<FieldControl> controls;
        private final boolean section;

        private LayoutRow(String title, int contentY, List<FieldControl> controls, boolean section) {
            this.title = title;
            this.contentY = contentY;
            this.controls = controls;
            this.section = section;
        }

        private static LayoutRow section(String title, int contentY) {
            return new LayoutRow(title, contentY, List.of(), true);
        }

        private static LayoutRow values(String title, int contentY, List<FieldControl> controls) {
            return new LayoutRow(title, contentY, List.copyOf(controls), false);
        }
    }
}
