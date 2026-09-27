package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.config.ConfigEditorRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ConfigEditPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class TweaksConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 29;
    private static final int LIST_TOP = 80;
    private static final int FOOTER_HEIGHT = 35;

    private final Screen parent;
    private final ConfigEditorRegistry.EditorTab tab;
    private final List<LayoutItem> items = new ArrayList<>();
    private final List<List<LayoutItem>> pages = new ArrayList<>();
    private final List<String> pageLabels = new ArrayList<>();
    private final List<ValueRow> rows = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private int pageIndex;
    private Button previousPage;
    private Button nextPage;
    private String status = "Changes are staged until Apply is pressed.";
    private int statusColor = 0xFFB9AA9D;

    public TweaksConfigScreen(Screen parent, ConfigEditorRegistry.EditorTab tab) {
        super(Component.literal(screenTitle(tab)));
        this.parent = parent;
        this.tab = tab;
    }

    @Override
    protected void init() {
        items.clear();
        pages.clear();
        pageLabels.clear();
        rows.clear();
        for (ConfigEditorRegistry.SpecEntry spec : ConfigEditorRegistry.specs(tab)) {
            List<LayoutItem> page = new ArrayList<>();
            pageLabels.add(spec.label());
            page.add(LayoutItem.header(spec.label()));
            for (ConfigEditorRegistry.ValueEntry value : ConfigEditorRegistry.values(spec)) {
                ValueRow row = new ValueRow(value);
                rows.add(row);
                page.add(LayoutItem.value(row));
            }
            pages.add(page);
        }
        pageIndex = Math.max(0, Math.min(pageIndex, Math.max(0, pages.size() - 1)));
        selectPage(false);

        for (ValueRow row : rows) {
            row.defaultButton = addRenderableWidget(Button.builder(Component.literal("Default"), button -> reset(row))
                .bounds(0, -100, 52, 20).build());
            if (row.booleanValue()) {
                row.valueButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggle(row))
                    .bounds(0, -100, 82, 20).build());
            } else {
                row.editBox = new EditBox(font, 0, -100, 142, 20, Component.literal(row.value.label()));
                row.editBox.setMaxLength(4096);
                row.editBox.setValue(row.text);
                addRenderableWidget(row.editBox);
            }
        }
        int left = panelLeft();
        int right = panelRight();
        previousPage = addRenderableWidget(Button.builder(Component.literal("< Previous"), button -> changePage(-1))
            .bounds(left + 8, 40, 66, 20).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal("Next >"), button -> changePage(1))
            .bounds(right - 74, 40, 66, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), button -> apply())
            .bounds(width / 2 - 112, height - 29, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(width / 2 + 6, height - 29, 106, 20).build());
        repositionWidgets();
        updatePageButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int left = panelLeft();
        int right = panelRight();
        int listBottom = height - FOOTER_HEIGHT;
        int contentHeight = items.size() * ROW_HEIGHT;
        maxScroll = Math.max(0, contentHeight - (listBottom - LIST_TOP));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        repositionWidgets();

        guiGraphics.fill(left - 4, 27, right + 4, height - 5, 0xAA08090D);
        guiGraphics.fill(left, 31, right, height - 9, 0xF01C1720);
        guiGraphics.fill(left + 2, 33, right - 2, height - 11, 0xF52A2028);
        guiGraphics.drawCenteredString(font, title, width / 2, 28, 0xFFF2D5A0);
        if (!pageLabels.isEmpty()) {
            guiGraphics.drawCenteredString(font,
                Component.literal(pageLabels.get(pageIndex) + "  |  Page " + (pageIndex + 1) + "/" + pages.size()),
                width / 2, 51, 0xFFF0D5A7);
        }
        String description = switch (tab) {
            case SERVER -> "Server values require operator permissions";
            case KNY_WORLDS -> "KnY Worlds common and client values";
            case CLIENT -> "Client and common values";
        };
        guiGraphics.drawString(font, Component.literal(description), left + 12, 66, 0xFFC8B9AC, false);

        guiGraphics.enableScissor(left + 7, LIST_TOP, right - 7, listBottom);
        int y = LIST_TOP - scroll;
        for (LayoutItem item : items) {
            if (item.header != null) {
                guiGraphics.fill(left + 8, y + 3, right - 8, y + ROW_HEIGHT - 3, 0xFF4A3540);
                guiGraphics.drawString(font, Component.literal(item.header), left + 14, y + 11, 0xFFF0D5A7, false);
            } else {
                ValueRow row = item.row;
                guiGraphics.fill(left + 8, y + 2, right - 8, y + ROW_HEIGHT - 3, 0xFF30272E);
                String label = row.value.label();
                String path = row.value.path();
                guiGraphics.drawString(font, Component.literal(label), left + 14, y + 5, 0xFFE7DCC7, false);
                guiGraphics.drawString(font, Component.literal(path), left + 14, y + 16, 0xFF9B8E88, false);
            }
            y += ROW_HEIGHT;
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = LIST_TOP + 2;
            int trackHeight = Math.max(20, listBottom - LIST_TOP - 4);
            int thumbHeight = Math.max(16, trackHeight * trackHeight / contentHeight);
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll;
            guiGraphics.fill(right - 6, trackTop, right - 3, trackTop + trackHeight, 0xFF1A151B);
            guiGraphics.fill(right - 6, thumbY, right - 3, thumbY + thumbHeight, 0xFFD1A267);
        }
        guiGraphics.drawCenteredString(font, Component.literal(status), width / 2, height - 42, statusColor);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * ROW_HEIGHT * 3));
            repositionWidgets();
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void repositionWidgets() {
        for (ValueRow row : rows) {
            row.defaultButton.setY(-100);
            if (row.booleanValue()) {
                row.valueButton.setY(-100);
            } else {
                row.editBox.setY(-100);
            }
        }
        int y = LIST_TOP - scroll;
        int listBottom = height - FOOTER_HEIGHT;
        for (LayoutItem item : items) {
            if (item.header != null) {
                y += ROW_HEIGHT;
                continue;
            }
            ValueRow row = item.row;
            int controlY = y + 5;
            boolean visible = controlY + 20 >= LIST_TOP && controlY <= listBottom;
            int widgetY = visible ? controlY : -100;
            row.defaultButton.setX(panelRight() - 67);
            row.defaultButton.setY(widgetY);
            if (row.booleanValue()) {
                row.valueButton.setX(panelRight() - 156);
                row.valueButton.setY(widgetY);
            } else {
                row.editBox.setX(panelRight() - 215);
                row.editBox.setY(widgetY);
                if (!visible) {
                    row.editBox.setFocused(false);
                }
            }
            y += ROW_HEIGHT;
        }
        for (ValueRow row : rows) {
            if (row.booleanValue()) {
                row.valueButton.setMessage(Component.literal(Boolean.parseBoolean(row.text) ? "Enabled" : "Disabled"));
            }
        }
    }

    private void changePage(int direction) {
        int next = pageIndex + direction;
        if (next < 0 || next >= pages.size()) {
            return;
        }
        pageIndex = next;
        selectPage(true);
        repositionWidgets();
    }

    private void selectPage(boolean resetScroll) {
        items.clear();
        if (!pages.isEmpty()) {
            items.addAll(pages.get(pageIndex));
        }
        if (resetScroll) {
            scroll = 0;
        }
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (previousPage != null) {
            previousPage.active = pageIndex > 0;
        }
        if (nextPage != null) {
            nextPage.active = pageIndex + 1 < pages.size();
        }
    }

    private void toggle(ValueRow row) {
        row.text = Boolean.toString(!Boolean.parseBoolean(row.text));
        row.valueButton.setMessage(Component.literal(Boolean.parseBoolean(row.text) ? "Enabled" : "Disabled"));
    }

    private void reset(ValueRow row) {
        row.text = ConfigEditorRegistry.format(row.value.value().getDefault());
        if (row.editBox != null) {
            row.editBox.setValue(row.text);
        }
        if (row.valueButton != null) {
            row.valueButton.setMessage(Component.literal(Boolean.parseBoolean(row.text) ? "Enabled" : "Disabled"));
        }
        status = row.value.label() + " reset to its default. Press Apply to keep it.";
        statusColor = 0xFFE6C887;
    }

    private void apply() {
        int applied = 0;
        int invalid = 0;
        for (ValueRow row : rows) {
            if (row.editBox != null) {
                row.text = row.editBox.getValue();
            }
            try {
                Object parsed = ConfigEditorRegistry.parse(row.value, row.text);
                if (!row.value.valueSpec().test(parsed)) {
                    invalid++;
                    continue;
                }
                if (tab == ConfigEditorRegistry.EditorTab.SERVER) {
                    ModNetworking.sendToServer(new ConfigEditPacket(row.value.specEntry().id(), row.value.path(), row.text));
                    applied++;
                } else if (ConfigEditorRegistry.apply(row.value, parsed)) {
                    applied++;
                } else {
                    invalid++;
                }
            } catch (RuntimeException exception) {
                invalid++;
            }
        }
        if (invalid == 0) {
            status = applied + " setting(s) applied immediately.";
            statusColor = 0xFFB9E1B3;
        } else {
            status = applied + " applied; " + invalid + " invalid value(s) were skipped.";
            statusColor = 0xFFE09A8E;
        }
    }

    private int panelLeft() {
        return Math.max(8, (width - Math.min(720, width - 16)) / 2);
    }

    private int panelRight() {
        return Math.min(width - 8, panelLeft() + Math.min(720, width - 16));
    }

    private static String screenTitle(ConfigEditorRegistry.EditorTab tab) {
        return switch (tab) {
            case SERVER -> "Server Config";
            case KNY_WORLDS -> "KnY Worlds Config";
            case CLIENT -> "Client Config";
        };
    }

    private static final class ValueRow {
        private final ConfigEditorRegistry.ValueEntry value;
        private String text;
        private EditBox editBox;
        private Button valueButton;
        private Button defaultButton;

        private ValueRow(ConfigEditorRegistry.ValueEntry value) {
            this.value = value;
            this.text = ConfigEditorRegistry.format(value.value().get());
        }

        private boolean booleanValue() {
            return value.value().getDefault() instanceof Boolean;
        }
    }

    private static final class LayoutItem {
        private final String header;
        private final ValueRow row;

        private LayoutItem(String header, ValueRow row) {
            this.header = header;
            this.row = row;
        }

        private static LayoutItem header(String header) {
            return new LayoutItem(header, null);
        }

        private static LayoutItem value(ValueRow row) {
            return new LayoutItem(null, row);
        }
    }
}
