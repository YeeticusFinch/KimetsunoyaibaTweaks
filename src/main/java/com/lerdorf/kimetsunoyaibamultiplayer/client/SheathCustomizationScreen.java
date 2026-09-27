package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetSheathCosmeticsPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class SheathCustomizationScreen extends Screen {
    private static final int PREVIEW_IDLE_LAYER = 90;
    private static final int MAX_PANEL_WIDTH = 500;
    private static final int MAX_PANEL_HEIGHT = 360;
    private static final int SCREEN_MARGIN = 8;
    private static final int HEADER_HEIGHT = 30;
    private static final int BOTTOM_PADDING = 10;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Screen parent;
    private final MeditationMenuScreen meditationScreen;

    private SwordDisplayConfig.SwordDisplayPosition position;
    private int textureIndex;
    private double translateX;
    private double translateY;
    private double translateZ;
    private double rotateX;
    private double rotateY;
    private double rotateZ;
    private float playerRotation;

    private final List<EditBox> fields = new ArrayList<>();
    private final List<ScrollableWidget> scrollableWidgets = new ArrayList<>();

    private int panelWidth;
    private int panelHeight;
    private int contentHeight;
    private int scrollOffset;
    private int maxScroll;

    // Content-space positions. Y values are relative to the top of the scrollable area.
    private int translationLabelX;
    private int translationLabelY;
    private int rotationLabelX;
    private int rotationLabelY;
    private int positionLabelX;
    private int positionLabelY;
    private int textureTextX;
    private int textureTextY;
    private int previewLabelX;
    private int previewLabelY;
    private int previewLeft;
    private int previewTop;
    private int previewRight;
    private int previewBottom;
    private int rotatePlayerLabelX;
    private int rotatePlayerLabelY;
    private int columnDividerX;
    private RotationSlider rotationSlider;

    // A separate entity is used for the preview so the real player's meditation/sitting
    // animation cannot leak into this screen.
    private PreviewPlayer previewPlayer;
    private ModifierLayer<IAnimation> previewIdleLayer;

    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public SheathCustomizationScreen(MeditationMenuScreen parent, SwordDisplayConfig.SwordDisplayPosition position,
                                     int textureIndex, double translateX, double translateY, double translateZ,
                                     double rotateX, double rotateY, double rotateZ) {
        super(Component.literal("Sheath Editor"));
        this.parent = parent;
        this.meditationScreen = parent;
        this.position = position;
        this.textureIndex = Math.max(0, textureIndex);
        this.translateX = translateX;
        this.translateY = translateY;
        this.translateZ = translateZ;
        this.rotateX = rotateX;
        this.rotateY = rotateY;
        this.rotateZ = rotateZ;
        this.playerRotation = defaultPlayerRotationForPosition(position);
    }

    @Override
    protected void init() {
        fields.clear();
        scrollableWidgets.clear();
        draggingScrollbar = false;

        panelWidth = Math.max(220, Math.min(MAX_PANEL_WIDTH, width - SCREEN_MARGIN * 2));
        panelHeight = Math.max(170, Math.min(MAX_PANEL_HEIGHT, height - SCREEN_MARGIN * 2));

        buildTwoColumnLayout();

        maxScroll = Math.max(0, contentHeight - getViewportHeight());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        applyScrollToWidgets();
        ensurePreviewPlayer();
    }

    private void buildTwoColumnLayout() {
        int left = getPanelLeft();
        int innerLeft = left + 14;

        // Reserve the scrollbar strip all the time so the two columns remain exactly equal
        // whether or not scrolling is currently necessary.
        int innerWidth = panelWidth - 28 - SCROLLBAR_WIDTH;
        int columnGap = 12;
        int columnWidth = Math.max(1, (innerWidth - columnGap) / 2);
        int leftColumnLeft = innerLeft;
        int rightColumnLeft = innerLeft + columnWidth + columnGap;
        columnDividerX = leftColumnLeft + columnWidth + columnGap / 2;

        int fieldGap = 6;
        int fieldWidth = Math.max(24, (columnWidth - fieldGap * 2) / 3);

        // LEFT COLUMN ---------------------------------------------------------
        translationLabelX = leftColumnLeft;
        translationLabelY = 8;
        addField(leftColumnLeft, 22, fieldWidth, "Translate X", translateX, 5.0);
        addField(leftColumnLeft + fieldWidth + fieldGap, 22, fieldWidth, "Translate Y", translateY, 5.0);
        addField(leftColumnLeft + (fieldWidth + fieldGap) * 2, 22, fieldWidth, "Translate Z", translateZ, 5.0);

        rotationLabelX = leftColumnLeft;
        rotationLabelY = 56;
        addField(leftColumnLeft, 70, fieldWidth, "Rotate X", rotateX, 360.0);
        addField(leftColumnLeft + fieldWidth + fieldGap, 70, fieldWidth, "Rotate Y", rotateY, 360.0);
        addField(leftColumnLeft + (fieldWidth + fieldGap) * 2, 70, fieldWidth, "Rotate Z", rotateZ, 360.0);

        positionLabelX = leftColumnLeft;
        positionLabelY = 106;
        Button positionButton = Button.builder(Component.literal(position.name()), button -> {
            position = position == SwordDisplayConfig.SwordDisplayPosition.HIP
                ? SwordDisplayConfig.SwordDisplayPosition.BACK
                : SwordDisplayConfig.SwordDisplayPosition.HIP;
            button.setMessage(Component.literal(position.name()));

            // Each sheath position gets a useful default viewing angle. The slider remains
            // fully interactive after this reset.
            setPlayerRotation(defaultPlayerRotationForPosition(position));
            sendCurrent();
        }).bounds(leftColumnLeft, 120, Math.min(116, columnWidth), 20).build();
        addScrollableWidget(positionButton, 120);

        int textureControlY = 164;
        int previousTextureX = leftColumnLeft;
        int nextTextureX = leftColumnLeft + columnWidth - 20;
        Button previousTexture = Button.builder(Component.literal("<"), button -> cycleTexture(-1))
            .bounds(previousTextureX, textureControlY, 20, 20).build();
        Button nextTexture = Button.builder(Component.literal(">"), button -> cycleTexture(1))
            .bounds(nextTextureX, textureControlY, 20, 20).build();
        addScrollableWidget(previousTexture, textureControlY);
        addScrollableWidget(nextTexture, textureControlY);
        textureTextX = leftColumnLeft + columnWidth / 2;
        textureTextY = textureControlY + 6;

        Button doneButton = Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(leftColumnLeft, 206, Math.min(116, columnWidth), 20).build();
        addScrollableWidget(doneButton, 206);

        // Reuse positionLabelX for the left-aligned Texture label in render().
        // The texture label's Y is fixed at 150 below.

        // RIGHT COLUMN --------------------------------------------------------
        previewLabelX = rightColumnLeft + columnWidth / 2;
        previewLabelY = 8;
        previewLeft = rightColumnLeft;
        previewTop = 22;
        previewRight = rightColumnLeft + columnWidth;
        previewBottom = 174;

        rotatePlayerLabelX = rightColumnLeft;
        rotatePlayerLabelY = 182;
        rotationSlider = new RotationSlider(rightColumnLeft, 196, columnWidth, 20);
        addScrollableWidget(rotationSlider, 196);

        contentHeight = 238;
    }

    private void addField(int x, int contentY, int fieldWidth, String label, double value, double max) {
        EditBox field = new EditBox(font, x, getViewportTop() + contentY, fieldWidth, 20, Component.literal(label));
        field.setMaxLength(32);
        field.setValue(Double.toString(value));
        field.setResponder(ignored -> updateFields());
        field.setTooltip(Tooltip.create(Component.literal(label + " (-" + max + " to " + max + ")")));
        fields.add(field);
        addScrollableWidget(field, contentY);
    }

    private void addScrollableWidget(AbstractWidget widget, int contentY) {
        scrollableWidgets.add(new ScrollableWidget(widget, contentY));
        addRenderableWidget(widget);
    }

    private void applyScrollToWidgets() {
        int viewportTop = getViewportTop();
        for (ScrollableWidget entry : scrollableWidgets) {
            entry.widget.setY(viewportTop + entry.contentY - scrollOffset);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int left = getPanelLeft();
        int top = getPanelTop();
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xAA050505);
        graphics.fill(left, top, right, bottom, 0xF11B1412);
        graphics.drawCenteredString(font, title, left + panelWidth / 2, top + 10, 0xFFE8C8);

        int viewportLeft = left + 6;
        int viewportTop = getViewportTop();
        int viewportRight = right - 6 - (maxScroll > 0 ? SCROLLBAR_WIDTH : 0);
        int viewportBottom = getViewportBottom();

        graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);

        graphics.drawString(font, "Translation offsets", translationLabelX,
            contentToScreenY(translationLabelY), 0xFFC9B7A5, false);
        graphics.drawString(font, "Rotation offsets (degrees)", rotationLabelX,
            contentToScreenY(rotationLabelY), 0xFFC9B7A5, false);
        graphics.drawString(font, "Position", positionLabelX,
            contentToScreenY(positionLabelY), 0xFFC9B7A5, false);

        graphics.drawString(font, "Texture", positionLabelX,
            contentToScreenY(150), 0xFFC9B7A5, false);

        // Visual bisection between the equal-width editor and preview columns.
        graphics.fill(columnDividerX, viewportTop + 2, columnDividerX + 1, viewportBottom - 2, 0x557A6A5B);

        graphics.drawCenteredString(font,
            Component.literal("Texture " + normalizedTextureIndex() + " / " + textureVariantCount()),
            textureTextX, contentToScreenY(textureTextY), 0xFFC9B7A5);

        graphics.drawCenteredString(font, Component.literal("Player preview"), previewLabelX,
            contentToScreenY(previewLabelY), 0xFFD4B58D);

        int previewScreenTop = contentToScreenY(previewTop);
        int previewScreenBottom = contentToScreenY(previewBottom);
        graphics.fill(previewLeft, previewScreenTop, previewRight, previewScreenBottom, 0x55110C0A);

        graphics.drawString(font, "Rotate player", rotatePlayerLabelX,
            contentToScreenY(rotatePlayerLabelY), 0xFFC9B7A5, false);

        renderPreviewPlayer(graphics);

        // Widgets need to be rendered while the scissor is active so off-screen controls are clipped.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();

        if (maxScroll > 0) {
            renderScrollbar(graphics);
        }
    }

    private void renderPreviewPlayer(GuiGraphics graphics) {
        Player realPlayer = Minecraft.getInstance().player;
        ItemStack previewSword = findPreviewSword(realPlayer);
        Item sheath = SwordSheathRegistry.getSheathItem(previewSword);

        if (realPlayer == null || sheath == null) {
            return;
        }

        ensurePreviewPlayer();
        if (previewPlayer == null) {
            return;
        }

        previewPlayer.setAppearanceSource(realPlayer);
        copyPreviewArmor(realPlayer);

        // This is a separate RemotePlayer, so it does not inherit the real player's
        // active Player Animator meditation/sitting layer. Force the remaining vanilla state
        // to standing as well.
        previewPlayer.setPose(Pose.STANDING);
        previewPlayer.setXRot(0.0F);
        previewPlayer.setYRot(0.0F);
        previewPlayer.yBodyRot = 0.0F;
        previewPlayer.yHeadRot = 0.0F;

        SheathPreviewState.set(previewPlayer.getUUID(), sheath,
            previewSword.isEmpty() ? null : previewSword.getItem(), position);

        int previewHeight = previewBottom - previewTop;
        int scale = Math.max(32, Math.min(56, previewHeight * 2 / 5));
        int centerX = (previewLeft + previewRight) / 2;
        int baseY = contentToScreenY(previewBottom - 8);

        try {
            Quaternionf modelRotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) -Math.toRadians(playerRotation));
            InventoryScreen.renderEntityInInventory(
                graphics,
                centerX,
                baseY,
                scale,
                modelRotation,
                new Quaternionf(),
                previewPlayer
            );
        } finally {
            SheathPreviewState.clear();
        }
    }

    private void ensurePreviewPlayer() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            clearPreviewIdleAnimation();
            previewPlayer = null;
            return;
        }

        if (previewPlayer == null || !previewPlayer.getUUID().equals(minecraft.player.getUUID())) {
            clearPreviewIdleAnimation();
            previewPlayer = new PreviewPlayer(minecraft.level, minecraft.player.getGameProfile());
            previewPlayer.setPose(Pose.STANDING);
        }

        ensurePreviewIdleAnimation();
    }

    private void ensurePreviewIdleAnimation() {
        if (previewPlayer == null || previewIdleLayer != null) {
            return;
        }

        try {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(previewPlayer);
            KeyframeAnimation idleAnimation = findPreviewIdleAnimation();
            if (animationStack == null || idleAnimation == null) {
                return;
            }

            animationStack.removeLayer(PREVIEW_IDLE_LAYER);
            ModifierLayer<IAnimation> idleLayer = new ModifierLayer<>();
            idleLayer.setAnimation(new KeyframeAnimationPlayer(idleAnimation));
            animationStack.addAnimLayer(PREVIEW_IDLE_LAYER, idleLayer);
            previewIdleLayer = idleLayer;
        } catch (Exception ignored) {
            // Player Animator is optional at runtime; the vanilla standing preview remains usable.
        }
    }

    private KeyframeAnimation findPreviewIdleAnimation() {
        ResourceLocation[] candidates = {
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "idle"),
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "idle"),
            ResourceLocation.fromNamespaceAndPath("playeranimator", "idle"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "idle")
        };
        for (ResourceLocation candidate : candidates) {
            try {
                KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(candidate);
                if (animation != null) {
                    return animation;
                }
            } catch (Exception ignored) {
                // Try the next known animation namespace.
            }
        }
        return null;
    }

    private void clearPreviewIdleAnimation() {
        if (previewPlayer != null) {
            try {
                AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(previewPlayer);
                if (animationStack != null) {
                    animationStack.removeLayer(PREVIEW_IDLE_LAYER);
                }
            } catch (Exception ignored) {
                // The preview entity may already be detached during screen teardown.
            }
        }
        previewIdleLayer = null;
    }

    private void copyPreviewArmor(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor()) {
                previewPlayer.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
        }
        previewPlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        previewPlayer.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int trackX = getScrollbarX();
        int top = getViewportTop();
        int bottom = getViewportBottom();
        int viewportHeight = bottom - top;

        graphics.fill(trackX, top, trackX + 3, bottom, 0x553A302B);

        int thumbHeight = getScrollbarThumbHeight();
        int thumbY = getScrollbarThumbY();
        graphics.fill(trackX - 1, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF9A8877);
    }

    private int getScrollbarX() {
        return getPanelLeft() + panelWidth - 8;
    }

    private int getScrollbarThumbHeight() {
        int viewportHeight = getViewportHeight();
        if (contentHeight <= 0) {
            return viewportHeight;
        }
        return Math.max(20, viewportHeight * viewportHeight / contentHeight);
    }

    private int getScrollbarThumbY() {
        int top = getViewportTop();
        int travel = getViewportHeight() - getScrollbarThumbHeight();
        if (maxScroll <= 0 || travel <= 0) {
            return top;
        }
        return top + Math.round((float) scrollOffset / (float) maxScroll * travel);
    }

    private void setScrollFromThumbY(double thumbTopY) {
        int travel = getViewportHeight() - getScrollbarThumbHeight();
        if (travel <= 0 || maxScroll <= 0) {
            setScrollOffset(0);
            return;
        }

        double relative = thumbTopY - getViewportTop();
        relative = Math.max(0.0D, Math.min(travel, relative));
        setScrollOffset((int) Math.round(relative / travel * maxScroll));
    }

    private void setScrollOffset(int value) {
        scrollOffset = Math.max(0, Math.min(maxScroll, value));
        applyScrollToWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0 && isInsidePanel(mouseX, mouseY)) {
            setScrollOffset(scrollOffset - (int) Math.round(delta * 24.0D));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll > 0) {
            int barX = getScrollbarX();
            if (mouseX >= barX - 4 && mouseX <= barX + 7
                && mouseY >= getViewportTop() && mouseY <= getViewportBottom()) {

                int thumbY = getScrollbarThumbY();
                int thumbHeight = getScrollbarThumbHeight();
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    scrollbarGrabOffset = (int) mouseY - thumbY;
                } else {
                    scrollbarGrabOffset = thumbHeight / 2;
                    setScrollFromThumbY(mouseY - scrollbarGrabOffset);
                }

                draggingScrollbar = true;
                return true;
            }
        }

        if (isInsideViewport(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            setScrollFromThumbY(mouseY - scrollbarGrabOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar && button == 0) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsidePanel(double mouseX, double mouseY) {
        int left = getPanelLeft();
        int top = getPanelTop();
        return mouseX >= left && mouseX <= left + panelWidth
            && mouseY >= top && mouseY <= top + panelHeight;
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        return mouseX >= getPanelLeft() + 6
            && mouseX <= getPanelLeft() + panelWidth - 6
            && mouseY >= getViewportTop()
            && mouseY <= getViewportBottom();
    }

    private int contentToScreenY(int contentY) {
        return getViewportTop() + contentY - scrollOffset;
    }

    private int getViewportTop() {
        return getPanelTop() + HEADER_HEIGHT;
    }

    private int getViewportBottom() {
        return getPanelTop() + panelHeight - BOTTOM_PADDING;
    }

    private int getViewportHeight() {
        return Math.max(1, getViewportBottom() - getViewportTop());
    }

    @Override
    public void onClose() {
        SheathPreviewState.clear();
        clearPreviewIdleAnimation();
        previewPlayer = null;
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        ensurePreviewPlayer();
        if (previewIdleLayer != null) {
            previewIdleLayer.tick();
        }
    }

    private void updateFields() {
        if (fields.size() < 6) {
            return;
        }

        Double[] values = new Double[6];
        for (int i = 0; i < fields.size(); i++) {
            try {
                double value = Double.parseDouble(fields.get(i).getValue());
                if (!Double.isFinite(value)) {
                    return;
                }
                values[i] = value;
            } catch (NumberFormatException exception) {
                return;
            }
        }

        translateX = clamp(values[0], 5.0);
        translateY = clamp(values[1], 5.0);
        translateZ = clamp(values[2], 5.0);
        rotateX = clamp(values[3], 360.0);
        rotateY = clamp(values[4], 360.0);
        rotateZ = clamp(values[5], 360.0);
        sendCurrent();
    }

    private float defaultPlayerRotationForPosition(SwordDisplayConfig.SwordDisplayPosition position) {
        return position == SwordDisplayConfig.SwordDisplayPosition.BACK ? 325.0F : 235.0F;
    }

    private void setPlayerRotation(float degrees) {
        playerRotation = (degrees % 360.0F + 360.0F) % 360.0F;
        if (rotationSlider != null) {
            rotationSlider.setDegrees(playerRotation);
        }
    }

    private void cycleTexture(int direction) {
        int count = textureVariantCount();
        textureIndex = count <= 0 ? 0 : Math.floorMod(textureIndex + direction, count);
        sendCurrent();
    }

    private void sendCurrent() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            SheathCosmeticsClientState.set(player.getUUID(), position, textureIndex, translateX, translateY, translateZ,
                rotateX, rotateY, rotateZ);
        }

        meditationScreen.updateLocalSheathCosmetics(position, textureIndex, translateX, translateY, translateZ,
            rotateX, rotateY, rotateZ);
        ModNetworking.sendToServer(new SetSheathCosmeticsPacket(position, textureIndex, translateX, translateY, translateZ,
            rotateX, rotateY, rotateZ));
    }

    private ItemStack findPreviewSword(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (SwordParticleMapping.isKimetsunoyaibaSword(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private int textureVariantCount() {
        Item sheath = SwordSheathRegistry.getSheathItem(findPreviewSword(Minecraft.getInstance().player));
        return Math.max(1, SwordSheathRegistry.getSheathTextureVariantCount(sheath));
    }

    private int normalizedTextureIndex() {
        return Math.floorMod(textureIndex, textureVariantCount());
    }

    private double clamp(double value, double max) {
        return Math.max(-max, Math.min(max, value));
    }

    private int getPanelLeft() {
        return (width - panelWidth) / 2;
    }

    private int getPanelTop() {
        return (height - panelHeight) / 2;
    }

    private static final class PreviewPlayer extends RemotePlayer {
        private Player appearanceSource;

        private PreviewPlayer(net.minecraft.client.multiplayer.ClientLevel level,
                              com.mojang.authlib.GameProfile profile) {
            super(level, profile);
        }

        private void setAppearanceSource(Player player) {
            appearanceSource = player;
        }

        @Override
        public boolean isModelPartShown(PlayerModelPart part) {
            return appearanceSource == null
                ? super.isModelPartShown(part)
                : appearanceSource.isModelPartShown(part);
        }
    }

    private static final class ScrollableWidget {
        private final AbstractWidget widget;
        private final int contentY;

        private ScrollableWidget(AbstractWidget widget, int contentY) {
            this.widget = widget;
            this.contentY = contentY;
        }
    }

    private class RotationSlider extends AbstractSliderButton {
        RotationSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), playerRotation / 360.0D);
            updateMessage();
        }

        void setDegrees(float degrees) {
            playerRotation = (degrees % 360.0F + 360.0F) % 360.0F;
            this.value = playerRotation / 360.0D;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Player rotation: " + Math.round(playerRotation) + "°"));
        }

        @Override
        protected void applyValue() {
            playerRotation = (float) (this.value * 360.0D);
            updateMessage();
        }
    }
}
