package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RequestEnhancedFormCycleDataPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SelectFormPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModBloodDemonArtFormRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModSwordFormRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public final class EnhancedFormCycleHandler {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final float TWO_PI = Mth.TWO_PI;
    private static final float DEAD_ZONE = 15.0F;
    private static final float HYSTERESIS = 0.12F;
    private static final float CAMERA_DEGREES_TO_POINTER = 2.4F;
    private static final float INNER_RADIUS = 22.0F;
    private static final float OUTER_RADIUS = 226.2F;
    private static final float DEFAULT_HUD_DIAMETER = OUTER_RADIUS * 2.0F;
    // Keep the entire wheel (including its selection outline) inside the GUI viewport.
    // At full size, its diameter is at most 90% of the smaller GUI dimension.
    private static final float MAX_SCREEN_DIAMETER_FRACTION = 0.90F;
    private static final float WHEEL_EDGE_PADDING = 5.0F;
    private static final float POINTER_RADIUS = OUTER_RADIUS * 0.80F;
    private static final float NUMBER_RADIUS = 58.0F;
    private static final float LABEL_INNER_RADIUS = NUMBER_RADIUS + 32.0F;
    private static final float LABEL_OUTER_RADIUS = OUTER_RADIUS - 14.0F;
    private static final float LABEL_RADIUS = (LABEL_INNER_RADIUS + LABEL_OUTER_RADIUS) * 0.5F;
    private static final int OPEN_ANIMATION_TICKS = 3;
    private static final Pattern FORM_PREFIX = Pattern.compile(
        "^(?:.*\\s)?(?:\\d+(?:st|nd|rd|th)|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|"
            + "eleventh|twelfth|thirteenth|fourteenth|fifteenth|sixteenth|seventeenth|eighteenth|nineteenth|twentieth)"
            + "\\s+(?:form|fang)\\s*:?\\s*",
        Pattern.CASE_INSENSITIVE);

    private static boolean active;
    private static boolean awaitingCustomData;
    private static boolean ignoreRotationTick;
    private static boolean movedPastDeadZone;
    private static ItemStack heldStack = ItemStack.EMPTY;
    private static List<FormOption> options = List.of();
    private static int selectedOption;
    private static float pointerX;
    private static float pointerY;
    private static int numberInput;
    private static int numberInputTicks;
    private static int lockedHotbarSlot = -1;
    private static float lastYaw;
    private static float lastPitch;
    private static int openingTicks;
    private static boolean cycleHeld;

    private EnhancedFormCycleHandler() {
    }

    public static boolean handleKeyInput(InputEvent.Key event) {
        if (!Config.enhancedFormCycle || MC.player == null) {
            return false;
        }

        if ((active || awaitingCustomData) && event.getAction() == GLFW.GLFW_PRESS) {
            int digit = numberKey(event.getKey());
            if (digit >= 0) {
                if (active && Config.enhancedFormCycleHudEnableNumberSelection) {
                    handleNumberInput(digit);
                }
                suppressHotbarKeyClicks();
                return true;
            }
        }

        var cycleKey = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART;
        if (!cycleKey.matches(event.getKey(), event.getScanCode())) {
            return active || awaitingCustomData;
        }

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (MC.screen != null) {
                return false;
            }
            if (!active && !awaitingCustomData && tryOpen()) {
                suppressBaseCycleState();
                return true;
            }
            return active || awaitingCustomData;
        }

        if (event.getAction() == GLFW.GLFW_RELEASE && (active || awaitingCustomData)) {
            finishSelection();
            return true;
        }
        return active || awaitingCustomData;
    }

    public static boolean handleMouseInput(InputEvent.MouseButton event) {
        if (!Config.enhancedFormCycle || MC.player == null) {
            return false;
        }

        var cycleKey = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART;
        if (!cycleKey.matchesMouse(event.getButton())) {
            return active || awaitingCustomData;
        }

        if (event.getAction() == GLFW.GLFW_PRESS && MC.screen == null && !active && !awaitingCustomData) {
            if (tryOpen()) {
                suppressBaseCycleState();
                return true;
            }
        }
        if (event.getAction() == GLFW.GLFW_RELEASE && (active || awaitingCustomData)) {
            finishSelection();
            return true;
        }
        return active || awaitingCustomData;
    }

    public static boolean handleScroll(InputEvent.MouseScrollingEvent event) {
        if (!Config.enhancedFormCycle || (!active && !awaitingCustomData)) {
            return false;
        }

        event.setCanceled(true);
            if (active && Config.enhancedFormCycleHudEnableScrollwheelSelection && !options.isEmpty()) {
                double delta = event.getScrollDelta();
            if (delta != 0.0D) {
                selectedOption = Math.floorMod(selectedOption + (delta > 0.0D ? 1 : -1), options.size());
                resetPointerForSelection();
            }
        }
        return true;
    }

    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !Config.enhancedFormCycle
            || (!active && !awaitingCustomData)) {
            return;
        }
        suppressHotbarKeyClicks();
        restoreLockedHotbarSlot();
    }

    private static void suppressHotbarKeyClicks() {
        if (MC.options == null) {
            return;
        }
        for (var mapping : MC.options.keyHotbarSlots) {
            while (mapping.consumeClick()) {
                // Keep vanilla number keys from changing the held item while the wheel is open.
            }
        }
    }

    private static void restoreLockedHotbarSlot() {
        if (lockedHotbarSlot < 0 || MC.player == null) {
            return;
        }
        if (MC.player.getInventory().selected != lockedHotbarSlot) {
            MC.player.getInventory().selected = lockedHotbarSlot;
            if (MC.getConnection() != null) {
                MC.getConnection().send(new ServerboundSetCarriedItemPacket(lockedHotbarSlot));
            }
        }
    }

    public static void tick() {
        if (!Config.enhancedFormCycle) {
            cancelSelection();
            return;
        }
        if (!active && !awaitingCustomData) {
            return;
        }

        suppressHotbarKeyClicks();
        restoreLockedHotbarSlot();
        if (MC.player == null || MC.level == null || MC.screen != null
            || !isCycleKeyDown()
            || !ItemStack.isSameItem(heldStack, MC.player.getMainHandItem())) {
            cancelSelection();
            return;
        }

        if (numberInputTicks > 0 && --numberInputTicks == 0) {
            numberInput = 0;
        }
        if (openingTicks > 0) {
            openingTicks--;
        }

        if (!active) {
            return;
        }

        if (!Config.enhancedFormCycleHudEnableMouseRotationSelection
            && !Config.enhancedFormCycleHudShowDirectionLine) {
            resetPointerForSelection();
            ignoreRotationTick = false;
        } else if (ignoreRotationTick) {
            ignoreRotationTick = false;
            lastYaw = MC.player.getYRot();
            lastPitch = MC.player.getXRot();
        } else {
            updatePointerFromCameraRotation();
        }
        suppressBaseCycleClick();
    }

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!active || options.isEmpty() || !event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int centerX = MC.getWindow().getGuiScaledWidth() / 2;
        int centerY = MC.getWindow().getGuiScaledHeight() / 2;
        float openingProgress = 1.0F - openingTicks / (float) OPEN_ANIMATION_TICKS;
        openingProgress = Mth.clamp(openingProgress, 0.0F, 1.0F);
        float easedProgress = 1.0F - (1.0F - openingProgress) * (1.0F - openingProgress)
            * (1.0F - openingProgress);
        float openingScale = 0.12F + easedProgress * 0.88F;

        // Scale everything together: slices, labels, numbers, pointer and outline.
        // Use GUI-scaled dimensions here because GuiGraphics uses GUI coordinates.
        float configuredScale = (float) (Config.enhancedFormCycleHudDiameter / DEFAULT_HUD_DIAMETER);
        float fitScale = wheelFitScale(
            MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight(), configuredScale);
        float totalScale = openingScale * configuredScale * fitScale;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 0.0F);
        guiGraphics.pose().scale(totalScale, totalScale, 1.0F);
        guiGraphics.pose().translate(-centerX, -centerY, 0.0F);

        float sliceAngle = TWO_PI / options.size();
        for (int i = 0; i < options.size(); i++) {
            float start = -Mth.HALF_PI + i * sliceAngle;
            FormOption option = options.get(i);
            int fill = i == selectedOption ? withAlpha(option.color(), 155) : 0xA8000000;
            drawAnnularSlice(guiGraphics, centerX, centerY, start, start + sliceAngle, fill);
        }

        for (int i = 0; i < options.size(); i++) {
            float boundary = -Mth.HALF_PI + i * sliceAngle;
            drawSegment(guiGraphics,
                centerX + Mth.cos(boundary) * INNER_RADIUS,
                centerY + Mth.sin(boundary) * INNER_RADIUS,
                centerX + Mth.cos(boundary) * OUTER_RADIUS,
                centerY + Mth.sin(boundary) * OUTER_RADIUS,
                0xB0000000, 1.0F);
        }

        if (movedPastDeadZone) {
            FormOption selected = options.get(selectedOption);
            float start = -Mth.HALF_PI + selectedOption * sliceAngle;
            drawArc(guiGraphics, centerX, centerY, OUTER_RADIUS + 2.0F,
                start, start + sliceAngle, highlightedEdgeColor(selected.color()));
        }

        for (int i = 0; i < options.size(); i++) {
            FormOption option = options.get(i);
            float angle = -Mth.HALF_PI + (i + 0.5F) * sliceAngle;
            drawNumber(guiGraphics, option, centerX, centerY, angle, i == selectedOption);
            if (Config.enhancedFormCycleHudShowAllNames || i == selectedOption) {
                drawRadialLabel(guiGraphics, option, centerX, centerY, angle, sliceAngle);
            }
        }

        if (Config.enhancedFormCycleHudShowDirectionLine && movedPastDeadZone) {
            drawSegment(guiGraphics, centerX, centerY,
                centerX + pointerX, centerY + pointerY, 0xD0FFFFFF, 1.5F);
        }
        guiGraphics.pose().popPose();
    }

    /**
     * Limit the visible wheel diameter to a fraction of the smaller GUI dimension.
     * The authored radii remain unchanged, so input/selection math still uses the
     * original wheel coordinate system. We only shrink the render transform.
     */
    private static float wheelFitScale(int guiWidth, int guiHeight, float configuredScale) {
        int smallerDimension = Math.min(guiWidth, guiHeight);
        if (smallerDimension <= 0) {
            return 0.0F;
        }
        float maximumVisibleRadius = smallerDimension * MAX_SCREEN_DIAMETER_FRACTION * 0.5F;
        return Math.min(1.0F, maximumVisibleRadius
            / (OUTER_RADIUS * configuredScale + WHEEL_EDGE_PADDING));
    }

    public static void acceptCustomData(List<Integer> indices, List<String> names, int selected, int color) {
        if (!awaitingCustomData || MC.player == null || !isCycleKeyDown()
            || !(MC.player.getMainHandItem().getItem() instanceof CustomDemonArtItem)
            || indices.isEmpty() || indices.size() != names.size()) {
            awaitingCustomData = false;
            cycleHeld = false;
            return;
        }

        options = new ArrayList<>(indices.size());
        for (int i = 0; i < indices.size(); i++) {
            int slotIndex = indices.get(i);
            options.add(new FormOption(slotIndex, slotIndex + 1,
                shortFormName(names.get(i)), opaque(color)));
        }
        selectedOption = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value() == selected) {
                selectedOption = i;
                break;
            }
        }
        awaitingCustomData = false;
        active = true;
        cycleHeld = true;
        heldStack = MC.player.getMainHandItem().copy();
        lockedHotbarSlot = MC.player.getInventory().selected;
        resetPointerForSelection();
        lastYaw = MC.player.getYRot();
        lastPitch = MC.player.getXRot();
        ignoreRotationTick = true;
        openingTicks = OPEN_ANIMATION_TICKS;
    }

    private static boolean tryOpen() {
        ItemStack stack = MC.player.getMainHandItem();
        List<FormOption> resolved = resolveOptions(stack);
        if (resolved == null) {
            if (stack.getItem() instanceof CustomDemonArtItem) {
                awaitingCustomData = true;
                cycleHeld = true;
                lockedHotbarSlot = MC.player.getInventory().selected;
                heldStack = stack.copy();
                ModNetworking.sendToServer(new RequestEnhancedFormCycleDataPacket());
                return true;
            }
            return false;
        }
        if (resolved.isEmpty()) {
            return false;
        }

        options = resolved;
        selectedOption = findCurrentOption(stack, options);
        heldStack = stack.copy();
        active = true;
        cycleHeld = true;
        lockedHotbarSlot = MC.player.getInventory().selected;
        resetPointerForSelection();
        lastYaw = MC.player.getYRot();
        lastPitch = MC.player.getXRot();
        ignoreRotationTick = true;
        openingTicks = OPEN_ANIMATION_TICKS;
        return true;
    }

    private static List<FormOption> resolveOptions(ItemStack stack) {
        if (stack.getItem() instanceof CustomDemonArtItem) {
            return null;
        }

        if (stack.getItem() instanceof BloodDemonArtItem art) {
            BloodDemonArtRegistry.RegisteredBloodDemonArt registered = BloodDemonArtRegistry.getArt(art.getArtId());
            return registered == null || registered.getTechnique() == null
                ? List.of() : bdaOptions(registered.getTechnique());
        }
        if (stack.getItem() instanceof BloodDemonArtAxeItem art) {
            BloodDemonArtRegistry.RegisteredBloodDemonArt registered = BloodDemonArtRegistry.getArt(art.getArtId());
            return registered == null || registered.getTechnique() == null
                ? List.of() : bdaOptions(registered.getTechnique());
        }
        if (stack.getItem() instanceof BreathingSwordItem sword) {
            BreathingTechnique technique = sword instanceof NichirinSwordBlack black
                ? black.getEffectiveTechnique(stack, MC.player)
                : sword.getBreathingTechnique();
            return technique == null ? List.of() : breathingOptions(technique);
        }
        if (BaseModBloodDemonArtFormRegistry.isSupported(stack)) {
            return baseModBloodDemonArtOptions(stack);
        }
        if (BaseModSwordFormRegistry.isSupported(stack)) {
            return baseModSwordOptions(stack);
        }
        if (!BreathingInfoDetector.isNichirinSword(stack)) {
            return List.of();
        }

        double breathes = BreathingFormTracker.getCachedForm(MC.player.getUUID(), stack);
        if (breathes == 0.0D) {
            breathes = MC.player.getPersistentData().getDouble("breathes");
        }
        int currentFormId = VariationEncoder.getFormId(breathes);
        return List.of();
    }

    private static List<FormOption> baseModSwordOptions(ItemStack stack) {
        int[] formIds = BaseModSwordFormRegistry.getFormIds(stack);
        List<FormOption> result = new ArrayList<>(formIds.length);
        for (int i = 0; i < formIds.length; i++) {
            int formId = formIds[i];
            BaseModSwordFormRegistry.FormDefinition form = BaseModSwordFormRegistry.getForm(formId);
            String fullName = form == null ? "Form " + formId : form.name();
            int color = form == null ? 0x55FFFF
                : legacyColor(BaseModSwordFormRegistry.getColor(stack, formId), 0x55FFFF);
            int formNumber = Math.floorMod(formId, 100);
            result.add(new FormOption(formId, formNumber > 0 ? formNumber : i + 1,
                shortFormName(fullName), color));
        }
        return result;
    }

    private static List<FormOption> baseModBloodDemonArtOptions(ItemStack stack) {
        int[] formIds = BaseModBloodDemonArtFormRegistry.getFormIds(stack);
        List<FormOption> result = new ArrayList<>(formIds.length);
        for (int i = 0; i < formIds.length; i++) {
            int formId = formIds[i];
            BaseModBloodDemonArtFormRegistry.FormDefinition form =
                BaseModBloodDemonArtFormRegistry.getForm(formId);
            String fullName = form == null ? "Blood Demon Art " + formId : form.name();
            int color = form == null ? 0x55FFFF : legacyColor(form.color(), 0x55FFFF);
            result.add(new FormOption(formId, formId % 100 + 1, shortFormName(fullName), color));
        }
        return result;
    }

    private static List<FormOption> breathingOptions(BreathingTechnique technique) {
        List<FormOption> result = new ArrayList<>(technique.getFormCount());
        int color = legacyColor(technique.getFormColor(), 0x55FFFF);
        for (int i = 0; i < technique.getFormCount(); i++) {
            BreathingForm form = technique.getForm(i);
            if (form != null) {
                result.add(new FormOption(i, i + 1,
                    shortFormName(form.getDisplayName().getString()), color));
            }
        }
        return result;
    }

    private static List<FormOption> bdaOptions(BloodDemonArtTechnique technique) {
        List<FormOption> result = new ArrayList<>(technique.getFormCount());
        int color = opaque(technique.getDisplayColor());
        for (int i = 0; i < technique.getFormCount(); i++) {
            var form = technique.getForm(i);
            if (form != null) {
                result.add(new FormOption(i, i + 1,
                    shortFormName(form.getDisplayName().getString()), color));
            }
        }
        return result;
    }

    private static int findCurrentOption(ItemStack stack, List<FormOption> resolved) {
        int current;
        if (stack.getItem() instanceof BloodDemonArtItem art) {
            current = art.getSelectedFormIndex(stack);
        } else if (stack.getItem() instanceof BloodDemonArtAxeItem art) {
            current = art.getSelectedFormIndex(stack);
        } else if (stack.getItem() instanceof BreathingSwordItem sword) {
            BreathingTechnique technique = sword instanceof NichirinSwordBlack black
                ? black.getEffectiveTechnique(stack, MC.player)
                : sword.getBreathingTechnique();
            current = PlayerBreathingData.getOrCreate(MC.player)
                .getCurrentFormIndex(PlayerBreathingData.getTechniqueKey(technique.getName()));
        } else if (BaseModBloodDemonArtFormRegistry.isSupported(stack)) {
            current = (int) MC.player.getPersistentData().getDouble("demon_art");
            if (current == 0) {
                current = (int) stack.getOrCreateTag().getDouble("select");
                int[] formIds = BaseModBloodDemonArtFormRegistry.getFormIds(stack);
                current = current >= 0 && current < formIds.length ? formIds[current] : 0;
            }
        } else if (BaseModSwordFormRegistry.isSupported(stack)) {
            double breathes = BreathingFormTracker.getCachedForm(MC.player.getUUID(), stack);
            if (breathes == 0.0D) {
                breathes = MC.player.getPersistentData().getDouble("breathes");
            }
            current = VariationEncoder.getFormId(breathes);
        } else {
            double breathes = BreathingFormTracker.getCachedForm(MC.player.getUUID(), stack);
            if (breathes == 0.0D) {
                breathes = MC.player.getPersistentData().getDouble("breathes");
            }
            current = VariationEncoder.getFormId(breathes);
        }

        for (int i = 0; i < resolved.size(); i++) {
            if (resolved.get(i).value() == current) {
                return i;
            }
        }
        return 0;
    }

    private static void updatePointerFromCameraRotation() {
        float yaw = MC.player.getYRot();
        float pitch = MC.player.getXRot();
        float yawDelta = Mth.wrapDegrees(yaw - lastYaw);
        float pitchDelta = pitch - lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;

        if (Math.abs(yawDelta) > 75.0F || Math.abs(pitchDelta) > 75.0F) {
            return;
        }
        if (Math.abs(yawDelta) < 0.001F && Math.abs(pitchDelta) < 0.001F) {
            return;
        }

        pointerX += yawDelta * CAMERA_DEGREES_TO_POINTER;
        pointerY += pitchDelta * CAMERA_DEGREES_TO_POINTER;
        float distance = (float) Math.hypot(pointerX, pointerY);
        if (distance > POINTER_RADIUS) {
            float factor = POINTER_RADIUS / distance;
            pointerX *= factor;
            pointerY *= factor;
            distance = POINTER_RADIUS;
        }
        if (distance >= DEAD_ZONE) {
            movedPastDeadZone = true;
            if (Config.enhancedFormCycleHudEnableMouseRotationSelection) {
                selectFromPointer();
            }
        }
    }

    private static void selectFromPointer() {
        if (options.size() < 2) {
            selectedOption = 0;
            return;
        }

        float angle = (float) Mth.atan2(pointerY, pointerX);
        float sliceAngle = TWO_PI / options.size();

        selectedOption = optionAtAngle(angle, sliceAngle);
    }

    private static int optionAtAngle(float angle, float sliceAngle) {
        float normalized = Mth.positiveModulo(
            angle + Mth.HALF_PI,
            TWO_PI
        );

        return Mth.floor(normalized / sliceAngle) % options.size();
    }

    private static void handleNumberInput(int digit) {
        int single = digit == 0 ? 10 : digit;
        int combined = numberInputTicks > 0 && numberInput > 0 ? numberInput * 10 + digit : -1;
        int number = hasDisplayNumber(combined) ? combined : single;
        numberInput = number;
        numberInputTicks = 12;

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).number() == number) {
                selectedOption = i;
                resetPointerForSelection();
                return;
            }
        }
    }

    private static boolean hasDisplayNumber(int number) {
        if (number <= 0) {
            return false;
        }
        for (FormOption option : options) {
            if (option.number() == number) {
                return true;
            }
        }
        return false;
    }

    private static int numberKey(int key) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            return key - GLFW.GLFW_KEY_0;
        }
        if (key == GLFW.GLFW_KEY_0) {
            return 0;
        }
        if (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_9) {
            return key - GLFW.GLFW_KEY_KP_0;
        }
        return key == GLFW.GLFW_KEY_KP_0 ? 0 : -1;
    }

    private static void finishSelection() {
        suppressHotbarKeyClicks();
        restoreLockedHotbarSlot();
        if (active && !options.isEmpty()) {
            ModNetworking.sendToServer(new SelectFormPacket(options.get(selectedOption).value()));
        }
        cancelSelection();
    }

    private static void resetPointerForSelection() {
        pointerX = 0.0F;
        pointerY = 0.0F;
        movedPastDeadZone = false;
    }

    private static void cancelSelection() {
        active = false;
        awaitingCustomData = false;
        cycleHeld = false;
        ignoreRotationTick = false;
        heldStack = ItemStack.EMPTY;
        options = List.of();
        selectedOption = 0;
        pointerX = 0.0F;
        pointerY = 0.0F;
        movedPastDeadZone = false;
        numberInput = 0;
        numberInputTicks = 0;
        lockedHotbarSlot = -1;
        openingTicks = 0;
    }

    private static boolean isCycleKeyDown() {
        return cycleHeld;
    }

    private static void suppressBaseCycleClick() {
        var cycleKey = net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings.CHANGE_BREATHES_AND_BLOOD_ART;
        while (cycleKey.consumeClick()) {
            // Prevent the base mod from processing the same press as a normal cycle.
        }
    }

    private static void suppressBaseCycleState() {
        suppressBaseCycleClick();
    }

    private static void drawAnnularSlice(GuiGraphics guiGraphics, int centerX, int centerY,
                                         float start, float end, int color) {
        Matrix4f matrix = guiGraphics.pose().last().pose();
        beginGeometry();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int steps = Math.max(4, (int) (Math.abs(end - start) * 20.0F));
        for (int i = 0; i < steps; i++) {
            float a1 = start + (end - start) * i / steps;
            float a2 = start + (end - start) * (i + 1) / steps;
            vertex(buffer, matrix, centerX + Mth.cos(a1) * INNER_RADIUS,
                centerY + Mth.sin(a1) * INNER_RADIUS, color);
            vertex(buffer, matrix, centerX + Mth.cos(a1) * OUTER_RADIUS,
                centerY + Mth.sin(a1) * OUTER_RADIUS, color);
            vertex(buffer, matrix, centerX + Mth.cos(a2) * OUTER_RADIUS,
                centerY + Mth.sin(a2) * OUTER_RADIUS, color);
            vertex(buffer, matrix, centerX + Mth.cos(a1) * INNER_RADIUS,
                centerY + Mth.sin(a1) * INNER_RADIUS, color);
            vertex(buffer, matrix, centerX + Mth.cos(a2) * OUTER_RADIUS,
                centerY + Mth.sin(a2) * OUTER_RADIUS, color);
            vertex(buffer, matrix, centerX + Mth.cos(a2) * INNER_RADIUS,
                centerY + Mth.sin(a2) * INNER_RADIUS, color);
        }
        BufferUploader.drawWithShader(buffer.end());
        endGeometry();
    }

    private static void drawRadialLabel(GuiGraphics guiGraphics, FormOption option,
                                        int centerX, int centerY, float angle,
                                        float sliceAngle) {
        String[] lines = wrapRadialLabel(option.name(), LABEL_OUTER_RADIUS - LABEL_INNER_RADIUS);
        int widest = 1;
        for (String line : lines) {
            widest = Math.max(widest, MC.font.width(line));
        }
        float radialRoom = LABEL_OUTER_RADIUS - LABEL_INNER_RADIUS;
        float requestedScale = (float) Config.enhancedFormCycleHudNameFontSize;
        float scale = Math.min(requestedScale, radialRoom / widest);
        float perpendicularRoom = 2.0F * LABEL_RADIUS * Mth.sin(sliceAngle * 0.5F) - 16.0F;
        float lineStep = MC.font.lineHeight + 3.0F;
        float twoLineHeight = (lines.length - 1) * lineStep + MC.font.lineHeight;
        scale = Math.min(scale, Math.max(0.25F, perpendicularRoom / twoLineHeight));

        float rotation = angle;
        while (rotation > Mth.PI) rotation -= TWO_PI;
        while (rotation <= -Mth.PI) rotation += TWO_PI;
        if (rotation > Mth.HALF_PI || rotation < -Mth.HALF_PI) {
            rotation += Mth.PI;
        }

        float x = centerX + Mth.cos(angle) * LABEL_RADIUS;
        float y = centerY + Mth.sin(angle) * LABEL_RADIUS;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 20.0F);
        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation(rotation));
        guiGraphics.pose().scale(scale, scale, 1.0F);
        float startY = -twoLineHeight * 0.5F;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            guiGraphics.drawString(MC.font, Component.literal(line), -MC.font.width(line) / 2,
                Math.round(startY + i * lineStep), option.color(), true);
        }
        guiGraphics.pose().popPose();
    }

    private static String[] wrapRadialLabel(String label, float maxWidth) {
        label = label == null ? "" : label.trim().replaceAll("\\s+", " ");
        if (MC.font.width(label) <= maxWidth) {
            return new String[]{label};
        }
        String[] words = label.split(" ");
        if (words.length < 2) {
            return new String[]{label};
        }
        String first = label;
        String second = "";
        int bestWidth = Integer.MAX_VALUE;
        for (int split = 1; split < words.length; split++) {
            String left = String.join(" ", Arrays.copyOfRange(words, 0, split));
            String right = String.join(" ", Arrays.copyOfRange(words, split, words.length));
            int max = Math.max(MC.font.width(left), MC.font.width(right));
            if (max < bestWidth) {
                bestWidth = max;
                first = left;
                second = right;
            }
        }
        return new String[]{first, second};
    }

    private static void drawNumber(GuiGraphics guiGraphics, FormOption option,
                                   int centerX, int centerY, float angle, boolean selected) {
        float x = centerX + Mth.cos(angle) * NUMBER_RADIUS;
        float y = centerY + Mth.sin(angle) * NUMBER_RADIUS;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 10.0F);
        float numberScale = (float) Config.enhancedFormCycleHudNumberFontSize;
        guiGraphics.pose().scale(numberScale, numberScale, 1.0F);
        guiGraphics.drawCenteredString(MC.font, Integer.toString(option.number()),
            0, -MC.font.lineHeight / 2, withAlpha(option.color(), selected ? 215 : 125));
        guiGraphics.pose().popPose();
    }

    private static void drawArc(GuiGraphics guiGraphics, int centerX, int centerY,
                                float radius, float start, float end, int color) {
        int steps = Math.max(5, (int) ((end - start) * 20.0F));
        for (int i = 0; i < steps; i++) {
            float a1 = start + (end - start) * i / steps;
            float a2 = start + (end - start) * (i + 1) / steps;
            drawSegment(guiGraphics,
                centerX + Mth.cos(a1) * radius, centerY + Mth.sin(a1) * radius,
                centerX + Mth.cos(a2) * radius, centerY + Mth.sin(a2) * radius,
                color, 1.8F);
        }
    }

    private static void drawSegment(GuiGraphics guiGraphics, float ax, float ay,
                                    float bx, float by, int color, float width) {
        float dx = bx - ax;
        float dy = by - ay;
        float distance = (float) Math.hypot(dx, dy);
        if (distance < 0.001F) {
            return;
        }
        float normalX = -dy / distance * width / 2.0F;
        float normalY = dx / distance * width / 2.0F;
        Matrix4f matrix = guiGraphics.pose().last().pose();
        beginGeometry();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        vertex(buffer, matrix, ax + normalX, ay + normalY, color);
        vertex(buffer, matrix, ax - normalX, ay - normalY, color);
        vertex(buffer, matrix, bx - normalX, by - normalY, color);
        vertex(buffer, matrix, ax + normalX, ay + normalY, color);
        vertex(buffer, matrix, bx - normalX, by - normalY, color);
        vertex(buffer, matrix, bx + normalX, by + normalY, color);
        BufferUploader.drawWithShader(buffer.end());
        endGeometry();
    }

    private static void beginGeometry() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void endGeometry() {
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        buffer.vertex(matrix, x, y, 0.0F)
            .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
            .endVertex();
    }

    private static String shortFormName(String fullName) {
        String stripped = ChatFormatting.stripFormatting(fullName == null ? "" : fullName);
        if (stripped == null) {
            stripped = "";
        }
        stripped = stripped.trim();
        int colon = stripped.indexOf(':');
        if (colon >= 0 && colon + 1 < stripped.length()) {
            String suffix = stripped.substring(colon + 1).trim();
            if (!suffix.isEmpty()) {
                return suffix;
            }
        }
        Matcher matcher = FORM_PREFIX.matcher(stripped);
        if (matcher.find()) {
            String suffix = stripped.substring(matcher.end()).trim();
            if (!suffix.isEmpty()) {
                return suffix;
            }
            return stripped;
        }
        return stripped.isEmpty() ? "Unnamed" : stripped;
    }

    private static int cachedBaseColor(int formId, int fallback) {
        if (MC.player == null) {
            return fallback;
        }
        String cached = BreathingFormTracker.getDisplayTextForForm(MC.player.getUUID(), formId);
        if (cached == null || cached.isEmpty()) {
            return fallback;
        }
        int delimiter = cached.lastIndexOf(" - ");
        String formPart = delimiter >= 0 ? cached.substring(delimiter + 3) : cached;
        return lastLegacyColor(formPart, fallback);
    }

    private static int lastLegacyColor(String value, int fallback) {
        int result = fallback;
        if (value == null) {
            return result;
        }
        for (int i = 0; i + 1 < value.length(); i++) {
            if (value.charAt(i) != '\u00A7') {
                continue;
            }
            int index = "0123456789abcdef".indexOf(Character.toLowerCase(value.charAt(i + 1)));
            if (index >= 0) {
                result = minecraftColor(index);
            }
            i++;
        }
        return result;
    }

    private static int legacyColor(String code, int fallback) {
        return lastLegacyColor(code, fallback);
    }

    private static int minecraftColor(int index) {
        int[] rgb = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };
        return opaque(rgb[index]);
    }

    private static int highlightedEdgeColor(int rgb) {
        int r = 110 + ((rgb >> 16) & 255) * 145 / 255;
        int g = 110 + ((rgb >> 8) & 255) * 145 / 255;
        int b = 110 + (rgb & 255) * 145 / 255;
        return 0xE0000000 | (r << 16) | (g << 8) | b;
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0xFFFFFF) | ((alpha & 255) << 24);
    }

    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class HotbarGuard {
        private HotbarGuard() {
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onClientTickStart(TickEvent.ClientTickEvent event) {
            EnhancedFormCycleHandler.onClientTickStart(event);
        }
    }

    private record FormOption(int value, int number, String name, int color) {
    }
}
