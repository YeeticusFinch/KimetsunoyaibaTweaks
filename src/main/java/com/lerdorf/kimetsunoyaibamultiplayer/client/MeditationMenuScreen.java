package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuData;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SelectMeditationTargetPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeditationMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 372;
    private static final int PANEL_HEIGHT = 222;
    private static final int INNER_MARGIN = 12;
    private static final int TAB_Y = 30;
    private static final int TAB_HEIGHT = 18;
    private static final int BODY_TOP = 56;
    private static final int BODY_BOTTOM_MARGIN = 12;
    private static final int ROW_HEIGHT = 18;

    private final MeditationMenuData data;
    private Tab activeTab = Tab.INFO;
    private int infoScroll;
    private int questScroll;
    private int locationScroll;
    private int skillsScroll;
    private int detailScroll;
    private final List<Rect2i> tabBounds = new ArrayList<>();
    private final List<InfoHitbox> infoHitboxes = new ArrayList<>();
    private final List<EntryHitbox> questHitboxes = new ArrayList<>();
    private final List<EntryHitbox> locationHitboxes = new ArrayList<>();
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private List<Component> hoveredTooltip = List.of();
    private final Set<String> expandedInfoSections = new HashSet<>(Set.of("demons_killed", "humans_killed"));
    private Rect2i demonEyesButtonBounds;
    private int localDemonEyesIndex;

    public MeditationMenuScreen(MeditationMenuData data) {
        super(Component.literal("Meditation"));
        this.data = data;
        this.localDemonEyesIndex = data.demonEyesIndex();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        hoveredStack = ItemStack.EMPTY;
        hoveredTooltip = List.of();
        tabBounds.clear();
        infoHitboxes.clear();
        questHitboxes.clear();
        locationHitboxes.clear();
        demonEyesButtonBounds = null;

        guiGraphics.fill(left - 4, top - 4, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 4, 0xAA0A0A10);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF1211B19);
        guiGraphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + PANEL_HEIGHT - 1, 0xF5362D28);
        guiGraphics.fill(left + 8, top + 24, left + PANEL_WIDTH - 8, top + 25, 0xFF8A6A3E);

        guiGraphics.drawString(font, title, left + 12, top + 8, 0xF7E5C7, false);
        guiGraphics.drawString(font, font.plainSubstrByWidth(data.role() + " | " + data.rank(), PANEL_WIDTH - 110),
            left + 86, top + 8, 0xD9C6A1, false);

        renderTabs(guiGraphics, left, top, mouseX, mouseY);

        switch (activeTab) {
            case INFO -> renderInfoTab(guiGraphics, left, top);
            case NAVIGATION -> renderNavigationTab(guiGraphics, left, top, mouseX, mouseY);
            case SKILLS -> renderSkillsTab(guiGraphics, left, top);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (!hoveredStack.isEmpty()) {
            guiGraphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
        } else if (!hoveredTooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(font, hoveredTooltip, mouseX, mouseY);
        }
    }

    private void renderTabs(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int tabX = left + 12;
        for (Tab tab : Tab.values()) {
            int tabWidth = 70;
            int tabHeight = TAB_HEIGHT;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= top + TAB_Y && mouseY <= top + TAB_Y + TAB_HEIGHT;
            boolean selected = tab == activeTab;
            int fill = selected ? 0xFFAE8A4F : hovered ? 0xFF6B5741 : 0xFF4A3B32;
            guiGraphics.fill(tabX, top + TAB_Y, tabX + tabWidth, top + TAB_Y + TAB_HEIGHT, fill);
            guiGraphics.drawCenteredString(font, tab.label, tabX + tabWidth / 2, top + TAB_Y + 5, selected ? 0x1D1208 : 0xF3E0C2);
            tabBounds.add(new Rect2i(tabX, top + TAB_Y, tabWidth, tabHeight));
            tabX += tabWidth + 6;
        }
    }

    private void renderInfoTab(GuiGraphics guiGraphics, int left, int top) {
        int bodyLeft = left + INNER_MARGIN;
        int bodyTop = top + BODY_TOP;
        int bodyRight = left + PANEL_WIDTH - INNER_MARGIN;
        int bodyBottom = top + PANEL_HEIGHT - BODY_BOTTOM_MARGIN;
        int statsTop = bodyTop + 48;
        guiGraphics.fill(bodyLeft, statsTop, bodyRight, bodyBottom, 0xAA15100E);

        guiGraphics.drawString(font, "Role", bodyLeft, bodyTop, 0x8FD48C, false);
        guiGraphics.drawString(font, data.role(), bodyLeft + 52, bodyTop, 0xF0E3C2, false);
        guiGraphics.drawString(font, "Rank", bodyLeft, bodyTop + 18, 0x8FD48C, false);
        guiGraphics.drawString(font, data.rank(), bodyLeft + 52, bodyTop + 18, 0xF0E3C2, false);

        List<InfoRow> rows = flattenInfoRows(data.infoSections(), 0);
        int lineHeight = 10;
        int visible = Math.max(1, (bodyBottom - statsTop - 16) / lineHeight);
        infoScroll = clampScroll(infoScroll, rows.size(), visible);

        guiGraphics.enableScissor(bodyLeft + 4, statsTop + 4, bodyRight - 6, bodyBottom - 4);
        int y = statsTop + 8;
        for (int i = infoScroll; i < Math.min(rows.size(), infoScroll + visible); i++) {
            InfoRow row = rows.get(i);
            int rowY = y;
            int rowX = bodyLeft + 8 + row.depth() * 12;
            if (row.section().children().isEmpty()) {
                guiGraphics.drawString(font, row.section().title(), rowX, rowY, 0xEADCC0, false);
            } else {
                String prefix = expandedInfoSections.contains(row.section().id()) ? "[-] " : "[+] ";
                guiGraphics.drawString(font, prefix + row.section().title(), rowX, rowY, 0xF0E3C2, false);
                infoHitboxes.add(new InfoHitbox(new Rect2i(bodyLeft + 6, rowY - 1, bodyRight - bodyLeft - 20, lineHeight + 1), row.section().id()));
            }
            String count = Integer.toString(row.section().count());
            guiGraphics.drawString(font, count, bodyRight - 12 - font.width(count), rowY, 0xCBE7C8, false);
            y += lineHeight;
        }
        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, bodyRight - 8, statsTop + 8, bodyBottom - 8, rows.size(), visible, infoScroll);
    }

    private void renderNavigationTab(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int bodyTop = top + BODY_TOP;
        int bodyBottom = top + PANEL_HEIGHT - BODY_BOTTOM_MARGIN;
        int listLeft = left + INNER_MARGIN;
        int listWidth = 138;
        int listRight = listLeft + listWidth;
        int detailLeft = listRight + 10;
        int detailRight = left + PANEL_WIDTH - INNER_MARGIN;

        int gap = 10;
        int totalListHeight = bodyBottom - bodyTop;
        int questBoxHeight = Math.max(64, (totalListHeight - gap) / 2 - 10);
        int questBoxTop = bodyTop;
        int questBoxBottom = questBoxTop + questBoxHeight;
        int locationBoxTop = questBoxBottom + gap;
        int locationBoxBottom = bodyBottom;

        guiGraphics.fill(listLeft, questBoxTop, listRight, questBoxBottom, 0xAA15100E);
        guiGraphics.fill(listLeft, locationBoxTop, listRight, locationBoxBottom, 0xAA15100E);
        guiGraphics.fill(detailLeft, bodyTop, detailRight, bodyBottom, 0xAA15100E);

        guiGraphics.drawString(font, "Quests", listLeft + 6, questBoxTop + 6, 0xF5D18A, false);
        guiGraphics.drawString(font, "Locations", listLeft + 6, locationBoxTop + 6, 0xF5D18A, false);

        renderQuestList(guiGraphics, listLeft + 4, questBoxTop + 20, listWidth - 12, questBoxBottom - questBoxTop - 24, mouseX, mouseY);
        renderLocationList(guiGraphics, listLeft + 4, locationBoxTop + 20, listWidth - 12, locationBoxBottom - locationBoxTop - 24, mouseX, mouseY);
        renderDetailPane(guiGraphics, detailLeft + 8, bodyTop + 8, detailRight - detailLeft - 16, bodyBottom - bodyTop - 16);
    }

    private void renderQuestList(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        int visible = Math.max(1, height / ROW_HEIGHT);
        questScroll = clampScroll(questScroll, data.quests().size(), visible);

        guiGraphics.enableScissor(x, y, x + width + 2, y + height);
        for (int i = 0; i < visible && i + questScroll < data.quests().size(); i++) {
            MeditationMenuData.QuestEntry quest = data.quests().get(i + questScroll);
            int rowTop = y + i * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowTop && mouseY <= rowTop + 16;
            int fill = quest.selected() ? 0xFF9B7443 : hovered ? 0xFF604A33 : 0xFF392D24;
            guiGraphics.fill(x, rowTop, x + width, rowTop + 16, fill);
            guiGraphics.renderItem(new ItemStack(ModItems.QUEST_SCROLL.get()), x + 1, rowTop);
            guiGraphics.drawString(font, font.plainSubstrByWidth(quest.name(), width - 24), x + 20, rowTop + 4,
                quest.completed() ? 0xA4FFB1 : 0xF3E0C2, false);
            questHitboxes.add(new EntryHitbox(new Rect2i(x, rowTop, width, 16), MeditationMenuService.SELECTED_TYPE_QUEST, quest.id()));
            if (hovered) {
                hoveredStack = new ItemStack(ModItems.QUEST_SCROLL.get());
                hoveredTooltip = buildQuestTooltip(quest);
            }
        }
        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, x + width + 2, y, y + height, data.quests().size(), visible, questScroll);
    }

    private void renderLocationList(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        int visible = Math.max(1, height / ROW_HEIGHT);
        locationScroll = clampScroll(locationScroll, data.locations().size(), visible);

        guiGraphics.enableScissor(x, y, x + width + 2, y + height);
        for (int i = 0; i < visible && i + locationScroll < data.locations().size(); i++) {
            MeditationMenuData.LocationEntry location = data.locations().get(i + locationScroll);
            int rowTop = y + i * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowTop && mouseY <= rowTop + 16;
            int fill = location.selected() ? 0xFF688B72 : hovered ? 0xFF475F4E : 0xFF2D3A31;
            guiGraphics.fill(x, rowTop, x + width, rowTop + 16, fill);
            guiGraphics.renderItem(new ItemStack(ModItems.NAV_PIN_WAYPOINT.get()), x + 1, rowTop);
            guiGraphics.drawString(font, font.plainSubstrByWidth(location.name(), width - 24), x + 20, rowTop + 4, 0xE5F2DD, false);
            locationHitboxes.add(new EntryHitbox(new Rect2i(x, rowTop, width, 16), MeditationMenuService.SELECTED_TYPE_LOCATION, location.id()));
            if (hovered) {
                hoveredStack = new ItemStack(ModItems.NAV_PIN_WAYPOINT.get());
                hoveredTooltip = List.of(
                    Component.literal(location.name()).withStyle(ChatFormatting.WHITE),
                    Component.literal(location.description()).withStyle(ChatFormatting.GRAY)
                );
            }
        }
        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, x + width + 2, y, y + height, data.locations().size(), visible, locationScroll);
    }

    private void renderDetailPane(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        SelectedEntry selected = getSelectedEntry();
        if (selected == null) {
            guiGraphics.drawString(font, "Select a quest or location.", x, y, 0xF0E3C2, false);
            return;
        }

        // Pre-build all detail lines to calculate total height
        List<DetailLine> detailLines = new ArrayList<>();
        
        // Title
        for (FormattedCharSequence sequence : font.split(Component.literal(selected.title()), width)) {
            detailLines.add(new DetailLine(sequence, 0xF5D18A));
        }
        detailLines.add(new DetailLine(null, 0)); // spacer

        if (selected.quest != null) {
            // Progress text
            for (FormattedCharSequence seq : font.split(Component.literal(selected.quest.progressText()), width - 26)) {
                detailLines.add(new DetailLine(seq, 0xD6E3C5, 22));
            }
            detailLines.add(new DetailLine(null, 0)); // spacer

            // Description
            List<String> descriptionLines = new ArrayList<>();
            String currentQuestName = null;
            for (String line : selected.quest.description()) {
                if (line.startsWith("Current Quest: ")) {
                    currentQuestName = line.substring("Current Quest: ".length());
                } else {
                    descriptionLines.add(line);
                }
            }
            for (FormattedCharSequence seq : wrapLines(descriptionLines, width)) {
                detailLines.add(new DetailLine(seq, 0xEADCC0));
            }

            if (currentQuestName != null && !currentQuestName.isBlank()) {
                detailLines.add(new DetailLine(null, 0)); // spacer
                detailLines.add(new DetailLine(Component.literal("Current Quest").getVisualOrderText(), 0x8FD48C));
                for (FormattedCharSequence seq : font.split(Component.literal(currentQuestName), width)) {
                    detailLines.add(new DetailLine(seq, 0xEADCC0));
                }
            }

            // Rewards
            detailLines.add(new DetailLine(null, 0)); // spacer
            detailLines.add(new DetailLine(Component.literal("Rewards").getVisualOrderText(), 0x8FD48C));
            for (FormattedCharSequence seq : prefixWrappedLines(selected.quest.rewards(), "- ", width)) {
                detailLines.add(new DetailLine(seq, 0xCBE7C8));
            }
        } else {
            detailLines.add(new DetailLine(null, 0)); // spacer
            for (FormattedCharSequence seq : font.split(Component.literal(selected.location.description()), width - 24)) {
                detailLines.add(new DetailLine(seq, 0xEADCC0, 22));
            }
            detailLines.add(new DetailLine(null, 0)); // spacer
            for (FormattedCharSequence seq : wrapLines(List.of(
                    "Selection persistence is wired in.",
                    "Navigation behavior comes next."
                ), width)) {
                detailLines.add(new DetailLine(seq, 0xCBE7C8));
            }
        }

        int lineHeight = 10;
        int totalLines = detailLines.size();
        int visible = Math.max(1, height / lineHeight);
        detailScroll = clampScroll(detailScroll, totalLines, visible);

        guiGraphics.enableScissor(x, y, x + width, y + height);
        int scrollOffset = detailScroll * lineHeight;
        for (int i = detailScroll; i < Math.min(detailLines.size(), detailScroll + visible + 1); i++) {
            DetailLine line = detailLines.get(i);
            int lineY = y + (i - detailScroll) * lineHeight;
            if (line.sequence() != null) {
                int drawX = line.indent() > 0 ? x + line.indent() : x;
                guiGraphics.drawString(font, line.sequence(), drawX, lineY, line.color(), false);
            }
        }
        guiGraphics.disableScissor();

        // Render scrollbar
        renderDetailScrollbar(guiGraphics, x + width + 2, y, y + height, totalLines, visible);
    }

    private void renderDetailScrollbar(GuiGraphics guiGraphics, int x, int top, int bottom, int total, int visible) {
        int height = bottom - top;
        guiGraphics.fill(x, top, x + 4, bottom, 0x55281F1A);
        if (total <= visible) {
            guiGraphics.fill(x, top, x + 4, bottom, 0xAA8A6A3E);
            return;
        }
        int knobHeight = Math.max(10, height * visible / total);
        int travel = height - knobHeight;
        int maxScroll = Math.max(1, total - visible);
        int knobTop = top + (travel * detailScroll / maxScroll);
        guiGraphics.fill(x, knobTop, x + 4, knobTop + knobHeight, 0xFFB08D55);
    }

    private record DetailLine(FormattedCharSequence sequence, int color, int indent) {
        DetailLine(FormattedCharSequence sequence, int color) {
            this(sequence, color, 0);
        }
    }

    private void renderSkillsTab(GuiGraphics guiGraphics, int left, int top) {
        int bodyLeft = left + INNER_MARGIN;
        int bodyTop = top + BODY_TOP;
        int bodyRight = left + PANEL_WIDTH - INNER_MARGIN;
        int bodyBottom = top + PANEL_HEIGHT - BODY_BOTTOM_MARGIN;
        guiGraphics.fill(bodyLeft, bodyTop, bodyRight, bodyBottom, 0xAA15100E);

        if (data.demonPlayer()) {
            int cardLeft = bodyLeft + 10;
            int cardTop = bodyTop + 12;
            int cardRight = bodyRight - 10;
            int buttonTop = cardTop + 46;
            int buttonHeight = 18;
            guiGraphics.fill(cardLeft, cardTop, cardRight, cardTop + 86, 0x88312420);
            guiGraphics.drawString(font, "Demon Customization", cardLeft + 8, cardTop + 8, 0xF5D18A, false);
            guiGraphics.drawString(font, "Adjust the glowing demon-eyes overlay on your player skin.", cardLeft + 8, cardTop + 24, 0xF0E3C2, false);
            guiGraphics.drawString(font, "Current style: " + localDemonEyesIndex, cardLeft + 8, cardTop + 36, 0xCBE7C8, false);

            demonEyesButtonBounds = new Rect2i(cardLeft + 8, buttonTop, cardRight - cardLeft - 16, buttonHeight);
            guiGraphics.fill(demonEyesButtonBounds.getX(), demonEyesButtonBounds.getY(),
                demonEyesButtonBounds.getX() + demonEyesButtonBounds.getWidth(),
                demonEyesButtonBounds.getY() + demonEyesButtonBounds.getHeight(), 0xFF8A6A3E);
            guiGraphics.drawCenteredString(font, "Change Demon Eyes",
                demonEyesButtonBounds.getX() + demonEyesButtonBounds.getWidth() / 2,
                demonEyesButtonBounds.getY() + 5, 0x1D1208);

            guiGraphics.drawString(font, "Other demon skills can be added here later.", cardLeft + 8, cardTop + 72, 0xB8A48B, false);
            return;
        }

        List<String> lines = List.of(
            "Skills",
            "",
            "No demon eye customization is available for non-demon players.",
            "This tab can later hold role-specific skills and passive systems."
        );

        List<FormattedCharSequence> wrappedLines = wrapLines(lines, bodyRight - bodyLeft - 20);
        int lineHeight = 10;
        int visible = Math.max(1, (bodyBottom - bodyTop - 16) / lineHeight);
        skillsScroll = clampScroll(skillsScroll, wrappedLines.size(), visible);

        guiGraphics.enableScissor(bodyLeft + 4, bodyTop + 4, bodyRight - 6, bodyBottom - 4);
        int y = bodyTop + 8;
        for (int i = skillsScroll; i < Math.min(wrappedLines.size(), skillsScroll + visible); i++) {
            guiGraphics.drawString(font, wrappedLines.get(i), bodyLeft + 8, y, 0xF0E3C2, false);
            y += lineHeight;
        }
        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, bodyRight - 8, bodyTop + 8, bodyBottom - 8, wrappedLines.size(), visible, skillsScroll);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int x, int top, int bottom, int total, int visible, int scroll) {
        guiGraphics.fill(x, top, x + 4, bottom, 0x55281F1A);
        if (total <= visible) {
            guiGraphics.fill(x, top, x + 4, bottom, 0xAA8A6A3E);
            return;
        }
        int height = Math.max(10, (bottom - top) * visible / total);
        int travel = (bottom - top) - height;
        int maxScroll = Math.max(1, total - visible);
        int knobTop = top + (travel * scroll / maxScroll);
        guiGraphics.fill(x, knobTop, x + 4, knobTop + height, 0xFFB08D55);
    }

    private List<Component> buildQuestTooltip(MeditationMenuData.QuestEntry quest) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(quest.name()).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal(quest.categoryLabel()).withStyle(style -> style.withColor(quest.categoryColor())));
        for (String line : quest.description()) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal(quest.progressText()).withStyle(ChatFormatting.GREEN));
        return tooltip;
    }

    private SelectedEntry getSelectedEntry() {
        if (MeditationMenuService.SELECTED_TYPE_QUEST.equals(data.selectedType())) {
            for (MeditationMenuData.QuestEntry quest : data.quests()) {
                if (quest.id().equals(data.selectedId())) {
                    return new SelectedEntry(quest, null);
                }
            }
        }
        if (MeditationMenuService.SELECTED_TYPE_LOCATION.equals(data.selectedType())) {
            for (MeditationMenuData.LocationEntry location : data.locations()) {
                if (location.id().equals(data.selectedId())) {
                    return new SelectedEntry(null, location);
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < tabBounds.size(); i++) {
                Rect2i rect = tabBounds.get(i);
                if (contains(rect, mouseX, mouseY)) {
                    activeTab = Tab.values()[i];
                    return true;
                }
            }
            if (activeTab == Tab.INFO) {
                for (InfoHitbox hitbox : infoHitboxes) {
                    if (contains(hitbox.rect, mouseX, mouseY)) {
                        toggleInfoSection(hitbox.id);
                        return true;
                    }
                }
            }
            if (activeTab == Tab.NAVIGATION) {
                for (EntryHitbox hitbox : questHitboxes) {
                    if (contains(hitbox.rect, mouseX, mouseY)) {
                        select(hitbox.type, hitbox.id);
                        return true;
                    }
                }
                for (EntryHitbox hitbox : locationHitboxes) {
                    if (contains(hitbox.rect, mouseX, mouseY)) {
                        select(hitbox.type, hitbox.id);
                        return true;
                    }
                }
            }
            if (activeTab == Tab.SKILLS && demonEyesButtonBounds != null && contains(demonEyesButtonBounds, mouseX, mouseY)) {
                minecraft.setScreen(new DemonEyesCustomizationScreen(this, localDemonEyesIndex));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        int amount = scrollY > 0 ? -1 : 1;
        if (activeTab == Tab.INFO) {
            int left = (width - PANEL_WIDTH) / 2;
            int top = (height - PANEL_HEIGHT) / 2;
            int bodyLeft = left + INNER_MARGIN;
            int bodyTop = top + BODY_TOP + 48;
            int bodyRight = left + PANEL_WIDTH - INNER_MARGIN;
            int bodyBottom = top + PANEL_HEIGHT - BODY_BOTTOM_MARGIN;
            if (mouseX >= bodyLeft && mouseX <= bodyRight && mouseY >= bodyTop && mouseY <= bodyBottom) {
                List<InfoRow> rows = flattenInfoRows(data.infoSections(), 0);
                int visible = Math.max(1, (bodyBottom - bodyTop - 16) / 10);
                infoScroll = clampScroll(infoScroll + amount, rows.size(), visible);
            }
        } else if (activeTab == Tab.NAVIGATION) {
            int left = (width - PANEL_WIDTH) / 2;
            int top = (height - PANEL_HEIGHT) / 2;
            int bodyTop = top + BODY_TOP;
            int bodyBottom = top + PANEL_HEIGHT - BODY_BOTTOM_MARGIN;
            int listLeft = left + INNER_MARGIN;
            int listRight = listLeft + 138;
            int gap = 10;
            int totalListHeight = bodyBottom - bodyTop;
            int questBoxHeight = Math.max(64, (totalListHeight - gap) / 2 - 10);
            int questBoxTop = bodyTop;
            int questBoxBottom = questBoxTop + questBoxHeight;
            int locationBoxTop = questBoxBottom + gap;

            if (mouseX >= listLeft && mouseX <= listRight) {
                if (mouseY >= questBoxTop && mouseY <= questBoxBottom) {
                    int visible = Math.max(1, (questBoxBottom - questBoxTop - 24) / ROW_HEIGHT);
                    questScroll = clampScroll(questScroll + amount, data.quests().size(), visible);
                } else if (mouseY >= locationBoxTop && mouseY <= bodyBottom) {
                    int visible = Math.max(1, (bodyBottom - locationBoxTop - 24) / ROW_HEIGHT);
                    locationScroll = clampScroll(locationScroll + amount, data.locations().size(), visible);
                }
            } else {
                // Detail pane scrolling
                int detailLeft = listRight + 10 + 8;
                int detailRight = left + PANEL_WIDTH - INNER_MARGIN;
                if (mouseX >= detailLeft && mouseX <= detailRight && mouseY >= bodyTop && mouseY <= bodyBottom) {
                    // Recalculate detail lines count for scroll clamping
                    SelectedEntry selected = getSelectedEntry();
                    if (selected != null) {
                        int lineCount = countDetailLines(selected, detailRight - detailLeft - 16);
                        int lineHeight = 10;
                        int detailHeight = bodyBottom - bodyTop - 16;
                        int visible = Math.max(1, detailHeight / lineHeight);
                        detailScroll = clampScroll(detailScroll + amount, lineCount, visible);
                    }
                }
            }
        } else {
            int left = (width - PANEL_WIDTH) / 2;
            int top = (height - PANEL_HEIGHT) / 2;
            int bodyLeft = left + INNER_MARGIN;
            int bodyTop = top + BODY_TOP;
            int bodyRight = left + PANEL_WIDTH - INNER_MARGIN;
            int bodyBottom = top + PANEL_HEIGHT - BODY_BOTTOM_MARGIN;
            if (mouseX >= bodyLeft && mouseX <= bodyRight && mouseY >= bodyTop && mouseY <= bodyBottom) {
                List<FormattedCharSequence> wrappedLines = wrapLines(List.of(
                    "Skills tab framework",
                    "",
                    "This page is intentionally lightweight for now.",
                    "We can later split it into breathing, blood demon arts, passives, and unlock trees.",
                    "The scrolling behavior is already in place so this can grow without changing the shell."
                ), bodyRight - bodyLeft - 20);
                int visible = Math.max(1, (bodyBottom - bodyTop - 16) / 10);
                skillsScroll = clampScroll(skillsScroll + amount, wrappedLines.size(), visible);
            }
        }
        return true;
    }

    public void updateLocalDemonEyesIndex(int demonEyesIndex) {
        this.localDemonEyesIndex = demonEyesIndex;
    }

    private void toggleInfoSection(String id) {
        if (!expandedInfoSections.add(id)) {
            expandedInfoSections.remove(id);
        }
    }

    private List<InfoRow> flattenInfoRows(List<MeditationMenuData.InfoSection> sections, int depth) {
        List<InfoRow> rows = new ArrayList<>();
        for (MeditationMenuData.InfoSection section : sections) {
            rows.add(new InfoRow(section, depth));
            if (!section.children().isEmpty() && expandedInfoSections.contains(section.id())) {
                rows.addAll(flattenInfoRows(section.children(), depth + 1));
            }
        }
        return rows;
    }

    private List<FormattedCharSequence> wrapLines(List<String> lines, int width) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                wrapped.add(FormattedCharSequence.EMPTY);
                continue;
            }
            wrapped.addAll(font.split(Component.literal(line), width));
        }
        return wrapped;
    }

    private List<FormattedCharSequence> prefixWrappedLines(List<String> lines, String prefix, int width) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (String line : lines) {
            wrapped.addAll(font.split(Component.literal(prefix + line), width));
        }
        return wrapped;
    }

    private int drawWrappedBlock(GuiGraphics guiGraphics, List<FormattedCharSequence> lines, int x, int y, int width, int bottom, int color) {
        int cursorY = y;
        for (FormattedCharSequence line : lines) {
            if (cursorY + 10 > bottom) {
                break;
            }
            guiGraphics.drawString(font, line, x, cursorY, color, false);
            cursorY += 10;
        }
        return cursorY;
    }

    private int countDetailLines(SelectedEntry selected, int width) {
        int count = 0;
        // Title
        count += font.split(Component.literal(selected.title()), width).size() + 1; // +1 for spacer

        if (selected.quest != null) {
            // Progress
            count += font.split(Component.literal(selected.quest.progressText()), width - 26).size() + 1;
            
            // Description
            List<String> desc = new ArrayList<>();
            String currentQuestName = null;
            for (String line : selected.quest.description()) {
                if (line.startsWith("Current Quest: ")) {
                    currentQuestName = line.substring("Current Quest: ".length());
                } else {
                    desc.add(line);
                }
            }
            count += wrapLines(desc, width).size();
            
            if (currentQuestName != null && !currentQuestName.isBlank()) {
                count += 1 + font.split(Component.literal(currentQuestName), width).size(); // header + lines
            }
            
            // Rewards header + lines
            count += 1 + prefixWrappedLines(selected.quest.rewards(), "- ", width).size();
        } else {
            count += font.split(Component.literal(selected.location.description()), width - 24).size();
            count += 1 + wrapLines(List.of(
                "Selection persistence is wired in.",
                "Navigation behavior comes next."
            ), width).size();
        }
        return count;
    }

    private int clampScroll(int value, int total, int visible) {
        return Math.max(0, Math.min(value, Math.max(0, total - visible)));
    }

    private void select(String type, String id) {
        ModNetworking.sendToServer(new SelectMeditationTargetPacket(type, id));
        MeditationMenuScreen newScreen = new MeditationMenuScreen(new MeditationMenuData(
            data.role(),
            data.rank(),
            data.muzanBlood(),
            data.kizukiRank(),
            data.infoSections(),
            data.quests().stream().map(quest -> new MeditationMenuData.QuestEntry(
                quest.id(),
                quest.name(),
                quest.categoryLabel(),
                quest.categoryColor(),
                quest.description(),
                quest.rewards(),
                quest.progressText(),
                quest.completed(),
                MeditationMenuService.SELECTED_TYPE_QUEST.equals(type) && quest.id().equals(id)
            )).toList(),
            data.locations().stream().map(location -> new MeditationMenuData.LocationEntry(
                location.id(),
                location.name(),
                location.description(),
                MeditationMenuService.SELECTED_TYPE_LOCATION.equals(type) && location.id().equals(id)
            )).toList(),
            type,
            id,
            data.demonPlayer(),
            localDemonEyesIndex
        ));
        newScreen.activeTab = Tab.NAVIGATION;
        minecraft.setScreen(newScreen);
    }

    private boolean contains(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX <= rect.getX() + rect.getWidth() &&
            mouseY >= rect.getY() && mouseY <= rect.getY() + rect.getHeight();
    }

    private enum Tab {
        INFO("Info"),
        NAVIGATION("Navigation"),
        SKILLS("Skills");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private record EntryHitbox(Rect2i rect, String type, String id) {
    }

    private record InfoHitbox(Rect2i rect, String id) {
    }

    private record InfoRow(MeditationMenuData.InfoSection section, int depth) {
    }

    private record SelectedEntry(MeditationMenuData.QuestEntry quest, MeditationMenuData.LocationEntry location) {
        String title() {
            return quest != null ? quest.name() : location.name();
        }
    }
}
