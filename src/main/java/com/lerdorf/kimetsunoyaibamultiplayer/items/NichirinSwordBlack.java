package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.VermilionEyeEffect;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingFormAnnouncementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.LocalizationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.PlayerAbilityCooldowns;
import com.lerdorf.kimetsunoyaibamultiplayer.util.PlayerColorChangeStyleHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunBreathingLevelHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Black nichirin sword that rolls and persists a color-change-eligible style per item stack.
 */
public class NichirinSwordBlack extends BreathingSwordItem {
    private static final String STYLE_ID_TAG = "KnYMPBlackSwordStyleId";
    private static final String STYLE_NAME_TAG = "KnYMPBlackSwordStyleName";
    private static final String PLAYER_BLACK_SWORD_STYLE_TAG = "KnYMPBlackSwordColorChangeStyleId";
    private static final String LORE_PREFIX = "Style: ";
    private static final Set<Integer> THUNDER_BLACK_SWORD_FORMS = Set.of(301, 302, 303, 304, 305, 306);
    private static final Set<Integer> WATER_BLACK_SWORD_FORMS = Set.of(101, 102, 103, 104, 105, 106, 107, 108, 109, 110);
    private static final BreathingTechnique FALLBACK_TECHNIQUE = createTechniqueFromStyleId("water_breathing");

    public NichirinSwordBlack(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FALLBACK_TECHNIQUE;
    }

    public @Nullable BreathingTechnique getEffectiveTechnique(ItemStack stack, @Nullable Entity entity) {
        if (entity == null) {
            return getBreathingTechnique();
        }
        BreathingTechnique resolved = resolveTechnique(stack, entity.level(), entity);
        return resolved != null ? resolved : getBreathingTechnique();
    }

    public static @Nullable String ensureStyleAssigned(ItemStack stack, RandomSource random) {
        CompoundTag tag = stack.getOrCreateTag();
        String currentStyle = tag.getString(STYLE_ID_TAG);
        if (isUsableStyle(currentStyle)) {
            if (!tag.contains(STYLE_NAME_TAG)) {
                tag.putString(STYLE_NAME_TAG, formatStyleName(currentStyle));
                updateStyleLore(stack, formatStyleName(currentStyle));
            }
            return currentStyle;
        }

        List<String> candidates = new ArrayList<>();
        for (StyleMetadataRegistry.StyleMetadata style : StyleMetadataRegistry.getColorChangeEligibleStyles()) {
            String styleId = style.getStyleId();
            if (isUsableStyle(styleId)) {
                candidates.add(styleId);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        String chosenStyle = candidates.get(random.nextInt(candidates.size()));
        String styleName = formatStyleName(chosenStyle);
        tag.putString(STYLE_ID_TAG, chosenStyle);
        tag.putString(STYLE_NAME_TAG, styleName);
        updateStyleLore(stack, styleName);
        return chosenStyle;
    }

    public static @Nullable String resolveOrAssignPlayerStyle(Player player, RandomSource random) {
        if (player == null) {
            return null;
        }

        CompoundTag persistentData = player.getPersistentData();
        String currentStyle = persistentData.getString(PLAYER_BLACK_SWORD_STYLE_TAG);
        if (isUsableStyle(currentStyle)) {
            return currentStyle;
        }

        List<String> candidates = getAvailableStyleIds();
        if (candidates.isEmpty()) {
            return null;
        }

        String chosenStyle = candidates.get(random.nextInt(candidates.size()));
        persistentData.putString(PLAYER_BLACK_SWORD_STYLE_TAG, chosenStyle);
        return chosenStyle;
    }

    public static void rememberPlayerStyle(Player player, String styleId) {
        if (player == null || !isUsableStyle(styleId)) {
            return;
        }
        player.getPersistentData().putString(PLAYER_BLACK_SWORD_STYLE_TAG, styleId);
    }

    public static @Nullable String getRememberedPlayerStyle(Player player) {
        if (player == null) {
            return null;
        }
        String styleId = player.getPersistentData().getString(PLAYER_BLACK_SWORD_STYLE_TAG);
        return isUsableStyle(styleId) ? styleId : null;
    }

    public static void copyRememberedPlayerStyle(Player source, Player target) {
        if (source == null || target == null) {
            return;
        }

        String styleId = source.getPersistentData().getString(PLAYER_BLACK_SWORD_STYLE_TAG);
        if (isUsableStyle(styleId)) {
            target.getPersistentData().putString(PLAYER_BLACK_SWORD_STYLE_TAG, styleId);
        }
    }

    public static @Nullable String assignRememberedStyle(ItemStack stack, Player player, RandomSource random) {
        String styleId = resolveOrAssignPlayerStyle(player, random);
        if (styleId == null) {
            return null;
        }
        assignStyle(stack, styleId);
        return styleId;
    }

    public static @Nullable String getAssignedStyleId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        }
        String styleId = tag.getString(STYLE_ID_TAG);
        return isUsableStyle(styleId) ? styleId : null;
    }

    public static boolean assignStyle(ItemStack stack, String styleId) {
        if (!isUsableStyle(styleId)) {
            return false;
        }
        String styleName = formatStyleName(styleId);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(STYLE_ID_TAG, styleId);
        tag.putString(STYLE_NAME_TAG, styleName);
        updateStyleLore(stack, styleName);
        return true;
    }

    public static List<String> getAvailableStyleIds() {
        List<String> styles = new ArrayList<>();
        for (StyleMetadataRegistry.StyleMetadata style : StyleMetadataRegistry.getColorChangeEligibleStyles()) {
            String styleId = style.getStyleId();
            if (isUsableStyle(styleId)) {
                styles.add(styleId);
            }
        }
        Collections.sort(styles);
        return styles;
    }

    private static @Nullable BreathingTechnique resolveTechnique(ItemStack stack, Level level, @Nullable Entity entity) {
        CompoundTag tag = stack.getOrCreateTag();
        String styleId = tag.getString(STYLE_ID_TAG);
        if (!isUsableStyle(styleId) && !level.isClientSide) {
            styleId = ensureStyleAssigned(stack, level.getRandom());
        }
        if (!isUsableStyle(styleId)) {
            return null;
        }
        BreathingTechnique baseTechnique = createTechniqueFromStyleId(styleId);
        if (baseTechnique == null || baseTechnique.getFormCount() <= 0) {
            return null;
        }

        int sunLevel = entity != null ? SunBreathingLevelHelper.getSunBreathingLevel(entity) : 0;
        if (sunLevel <= 0) {
            return baseTechnique;
        }

        List<BreathingForm> combined = new ArrayList<>(baseTechnique.getForms());
        combined.addAll(SunBreathingLevelHelper.createUnlockedSunForms(sunLevel));
        return new BreathingTechnique(baseTechnique.getName(), combined, baseTechnique.getTechniqueColor(), baseTechnique.getFormColor());
    }

    private static boolean isUsableStyle(@Nullable String styleId) {
        if (styleId == null || styleId.isEmpty() || "black".equals(styleId)) {
            return false;
        }

        BreathingStyleRegistry.RegisteredBreathingStyle style = BreathingStyleRegistry.getStyle(styleId);
        if (style != null && style.getTechnique() != null && style.getTechnique().getFormCount() > 0) {
            return true;
        }

        int range = BaseModStyleMapping.getBreathesRange(styleId);
        if (range <= 0) {
            return false;
        }
        int[] formIds = getBaseFormIdsForStyle(styleId, range);
        for (int id : formIds) {
            if (BaseKnYForms.forms.get(id) != null) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable BreathingTechnique createTechniqueFromStyleId(@Nullable String styleId) {
        if (!isUsableStyle(styleId)) {
            return null;
        }

        BreathingStyleRegistry.RegisteredBreathingStyle style = BreathingStyleRegistry.getStyle(styleId);
        if (style != null && style.getTechnique() != null && style.getTechnique().getFormCount() > 0) {
            BreathingTechnique registered = style.getTechnique();
            List<BreathingForm> filtered = new ArrayList<>();
            for (BreathingForm form : registered.getForms()) {
                if (isAllowedForBlackSword(styleId, form.getFormId())) {
                    filtered.add(new BreathingForm(
                        form.getFormId(),
                        form.getName(),
                        form.getDescription(),
                        form.getCooldownSeconds(),
                        form.getEffect()
                    ));
                }
            }
            if (filtered.isEmpty()) {
                return null;
            }
            String techniqueColor = "moon_breathing".equals(styleId) ? "§6" : registered.getTechniqueColor();
            String formColor = "moon_breathing".equals(styleId) ? "§6" : registered.getFormColor();
            return new BreathingTechnique(
                registered.getName(),
                filtered,
                techniqueColor,
                formColor
            );
        }

        int styleRange = BaseModStyleMapping.getBreathesRange(styleId);
        if (styleRange <= 0) {
            return null;
        }

        List<BreathingForm> forms = new ArrayList<>();
        String color = "";
        for (int id : getBaseFormIdsForStyle(styleId, styleRange)) {
            if (!isAllowedForBlackSword(styleId, id)) {
                continue;
            }
            BaseKnYForms.BaseForm base = BaseKnYForms.forms.get(id);
            if (base == null) {
                continue;
            }
            if (color.isEmpty() && base.color != null) {
                color = base.color;
            }
            forms.add(new BreathingForm(id, base.name, "", 5, BaseModFormExecutionHelper::executeBaseModForm));
        }

        if (forms.isEmpty()) {
            return null;
        }

        if ("moon_breathing".equals(styleId)) {
            color = "§6";
        }

        String styleName = formatStyleName(styleId);
        return new BreathingTechnique(styleName, forms, color, color);
    }

    private static boolean isAllowedForBlackSword(String styleId, int formId) {
        // Water Breathing on black swords includes forms 1-10 only.
        if ("water_breathing".equals(styleId)) {
            return WATER_BLACK_SWORD_FORMS.contains(formId);
        }

        // Thunder Breathing on black swords includes forms 1-6 only.
        if ("thunder_breathing".equals(styleId)) {
            return THUNDER_BLACK_SWORD_FORMS.contains(formId);
        }

        // Flame Breathing 9th Form (Rengoku) is Hashira-only and excluded for black swords.
        if ("flame_breathing".equals(styleId) && formId == 409) {
            return false;
        }

        // Moon Breathing on black swords is intentionally limited to known usable forms.
        if ("moon_breathing".equals(styleId)) {
            return formId == 1101 || formId == 1102 || formId == 1103 || formId == 1105 || formId == 1106;
        }

        // Stone Breathing on black swords is limited to forms 1-4.
        if ("stone_breathing".equals(styleId)) {
            return formId == 601 || formId == 602 || formId == 603 || formId == 604
                || formId == 1601 || formId == 1602 || formId == 1603 || formId == 1604;
        }

        return true;
    }

    private static int[] getBaseFormIdsForStyle(String styleId, int styleRange) {
        if ("water_breathing".equals(styleId)) {
            return new int[]{101, 102, 103, 104, 105, 106, 107, 108, 109, 110};
        }
        if ("thunder_breathing".equals(styleId)) {
            return new int[]{301, 302, 303, 304, 305, 306};
        }
        return BaseModStyleMapping.getFormsForStyle(styleRange);
    }

    private static String formatStyleName(String styleId) {
        return PlayerColorChangeStyleHelper.formatStyleName(styleId);
    }

    private static void updateStyleLore(ItemStack stack, String styleName) {
        CompoundTag displayTag = stack.getOrCreateTagElement("display");
        ListTag oldLore = displayTag.getList("Lore", Tag.TAG_STRING);
        ListTag newLore = new ListTag();
        String newLine = LORE_PREFIX + styleName;

        for (int i = 0; i < oldLore.size(); i++) {
            String raw = oldLore.getString(i);
            Component parsed = Component.Serializer.fromJson(raw);
            String plain = parsed == null ? "" : parsed.getString();
            if (!plain.startsWith(LORE_PREFIX)) {
                newLore.add(StringTag.valueOf(raw));
            }
        }

        Component loreComponent = Component.literal(newLine)
            .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
        newLore.add(StringTag.valueOf(Component.Serializer.toJson(loreComponent)));
        displayTag.put("Lore", newLore);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BreathingTechnique technique = resolveTechnique(stack, level, player);
        if (technique == null) {
            return InteractionResultHolder.pass(stack);
        }

        if (com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler.isParalyzed(player)) {
            return InteractionResultHolder.fail(stack);
        }

        if (GuardStateHelper.isBaseModBreathingActiveOrOnCooldown(player)) {
            player.displayClientMessage(
                Component.literal("§cYou cannot use a custom breathing form while a base mod form is active or on cooldown."),
                true
            );
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
        String styleKey = PlayerBreathingData.getTechniqueKey(technique.getName());
        int baseFormIndex = data.getCurrentFormIndex(styleKey);
        if (baseFormIndex < 0 || baseFormIndex >= technique.getFormCount()) {
            baseFormIndex = 0;
            data.setCurrentFormIndex(styleKey, 0);
        }
        int variationIndex = data.getCurrentVariationIndex();

        BreathingForm form = technique.getForm(baseFormIndex);
        if (form == null) {
            return InteractionResultHolder.pass(stack);
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(this);
        String swordId = registeredSword != null ? registeredSword.getSwordId() : this.toString();

        int totalVariations = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry
            .getVariationCount(form.getFormId(), swordId);
        if (totalVariations == 0 && variationIndex != 0) {
            variationIndex = 0;
            data.setCurrentVariationIndex(0);
        } else if (variationIndex > totalVariations) {
            variationIndex = 0;
            data.setCurrentVariationIndex(0);
        }

        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation variation = null;
        if (variationIndex > 0) {
            variation = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry.getVariation(
                form.getFormId(), variationIndex, swordId);
        }

        String displayName;
        int cooldownSeconds;
        if (variation != null) {
            displayName = variation.getName();
            cooldownSeconds = variation.getCooldownSeconds();

            if (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.hasCoolTime(player)) {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }

            if (!player.getCooldowns().isOnCooldown(this)) {
                if (!level.isClientSide) {
                    variation.getEffect().execute(player, level, form.getFormId());
                    BreathingFormAnnouncementHelper.announceCustomForm(
                        player, variation.getDisplayName(), technique.getTechniqueColor());
                    int cooldownTicks = VermilionEyeEffect.applyCooldownReductionTicks(player, cooldownSeconds * 20);
                    PlayerAbilityCooldowns.addCooldown(player, this, cooldownTicks);
                }
                player.displayClientMessage(variation.getDisplayName().copy().withStyle(style -> style.withColor(0x55FFFF)), true);
                return InteractionResultHolder.success(stack);
            } else {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        } else {
            displayName = form.getName();
            cooldownSeconds = form.getCooldownSeconds();

            if (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.hasCoolTime(player)) {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }

            if (!player.getCooldowns().isOnCooldown(this)) {
                if (!level.isClientSide) {
                    form.execute(player, level);
                    BreathingFormAnnouncementHelper.announceCustomForm(
                        player, form.getDisplayName(), technique.getTechniqueColor());
                    int cooldownTicks = VermilionEyeEffect.applyCooldownReductionTicks(player, cooldownSeconds * 20);
                    PlayerAbilityCooldowns.addCooldown(player, this, cooldownTicks);
                }
                player.displayClientMessage(form.getDisplayName().copy().withStyle(style -> style.withColor(0x55FFFF)), true);
                return InteractionResultHolder.success(stack);
            } else {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        }
    }

    @Override
    public void cycleForm(Player player, boolean backward) {
        if (com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler.isParalyzed(player)) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        BreathingTechnique technique = (held.getItem() == this)
            ? resolveTechnique(held, player.level(), player)
            : getBreathingTechnique();
        if (technique == null || technique.getFormCount() <= 0) {
            return;
        }

        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
        String styleKey = PlayerBreathingData.getTechniqueKey(technique.getName());
        if (backward) {
            data.cycleFormBackward(styleKey, technique.getFormCount());
        } else {
            data.cycleForm(styleKey, technique.getFormCount());
        }

        int newIndex = data.getCurrentFormIndex(styleKey);
        BreathingForm form = technique.getForm(newIndex);

        if (form != null && !player.level().isClientSide) {
            double formId = form.getFormId();
            player.getPersistentData().putDouble("breathes", formId);
            data.setBaseModBreathesValue(formId);
        }

        if (!player.level().isClientSide) {
            PlayerBreathingData.saveToNBT(player);

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PlayerBreathingData.PlayerData pdata = PlayerBreathingData.getOrCreate(player);
                pdata.setCurrentVariationIndex(0);
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(
                        player.getUUID(), 0),
                    serverPlayer
                );
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket(
                        player.getUUID(),
                        styleKey,
                        newIndex
                    )
                );

                if (form != null) {
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket(
                            player.getUUID(),
                            form.getFormId()
                        ),
                        serverPlayer
                    );
                }
            }
        }

        if (form != null) {
            if (!player.level().isClientSide && !com.lerdorf.kimetsunoyaibamultiplayer.Config.suppressFormCycleChat) {
                player.sendSystemMessage(
                    LocalizationHelper.coloredBreathingSelection(
                        form.getFormId(),
                        technique.getName(),
                        form.getDisplayName(),
                        technique.getTechniqueColor(),
                        technique.getFormColor()
                    )
                );
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(STYLE_NAME_TAG)) {
            tooltip.add(Component.literal(LORE_PREFIX + tag.getString(STYLE_NAME_TAG)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(LORE_PREFIX + "Unassigned").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
