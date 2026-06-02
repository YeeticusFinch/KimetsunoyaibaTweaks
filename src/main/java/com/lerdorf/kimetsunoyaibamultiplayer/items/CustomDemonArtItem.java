package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.lang.reflect.Constructor;
import java.util.UUID;
import java.util.function.Supplier;

public class CustomDemonArtItem extends GeckolibItem {
    public static final String PLAYER_UUID_TAG = "CustomDemonArtPlayerUuid";
    public static final String PLAYER_NAME_TAG = "CustomDemonArtPlayerName";
    private static final int MIN_MODEL_VARIANT = 1;
    private static final int MAX_MODEL_VARIANT = 5;
    private static final String SELECTED_SLOT_TAG = "CustomDemonArtSelectedSlot";
    private static final String MODEL_VARIANT_TAG = "CustomDemonArtModelVariant";
    private static final String DISPLAY_TEXT_TAG = "CustomDemonArtDisplayText";
    private static final String DISPLAY_COLOR_TAG = "CustomDemonArtDisplayColor";
    private static final String DISPLAY_ART_NAME_TAG = "CustomDemonArtName";
    private static final String DISPLAY_FORM_NAME_TAG = "CustomDemonArtFormName";

    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public CustomDemonArtItem(Properties properties) {
        super(properties.stacksTo(1));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 4.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED,
            new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4D, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    protected Supplier<BlockEntityWithoutLevelRenderer> getRendererSupplier() {
        return () -> {
            try {
                Class<?> rendererClass = Class.forName(
                    "com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.CustomDemonArtRenderer");
                Constructor<?> constructor = rendererClass.getConstructor(CustomDemonArtItem.class);
                return (BlockEntityWithoutLevelRenderer) constructor.newInstance(this);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to create CustomDemonArtRenderer", e);
            }
        };
    }

    @Override
    protected boolean supportsGeckolibItemAnimations(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(Component.literal("Ability on cooldown!").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        return CustomBloodDemonArtRuntime.use(serverPlayer, stack)
            ? InteractionResultHolder.success(stack)
            : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Custom Blood Demon Art").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("Shift+R / R cycle between saved custom forms").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Right click uses the selected custom form").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    public static void setPlayerSkin(ItemStack stack, UUID playerUuid, String playerName) {
        CompoundTag tag = stack.getOrCreateTag();
        if (playerUuid != null) {
            tag.putUUID(PLAYER_UUID_TAG, playerUuid);
        }
        if (playerName != null && !playerName.isBlank()) {
            tag.putString(PLAYER_NAME_TAG, playerName);
        }
    }

    public static UUID getPlayerUuid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(PLAYER_UUID_TAG) ? tag.getUUID(PLAYER_UUID_TAG) : null;
    }

    public static String getPlayerName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(PLAYER_NAME_TAG, Tag.TAG_STRING) ? tag.getString(PLAYER_NAME_TAG) : "";
    }

    public static int getSelectedSlot(ItemStack stack, int fallback) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SELECTED_SLOT_TAG, Tag.TAG_INT)) {
            return tag.getInt(SELECTED_SLOT_TAG);
        }
        return fallback;
    }

    public static void setSelectedSlot(ItemStack stack, int slotIndex) {
        stack.getOrCreateTag().putInt(SELECTED_SLOT_TAG, slotIndex);
    }

    public static void setModelVariant(ItemStack stack, int variant) {
        stack.getOrCreateTag().putInt(MODEL_VARIANT_TAG, clampModelVariant(variant));
    }

    public static int getModelVariant(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MODEL_VARIANT_TAG, Tag.TAG_INT)) {
            return MIN_MODEL_VARIANT;
        }
        return clampModelVariant(tag.getInt(MODEL_VARIANT_TAG));
    }

    public static void setDisplayInfo(ItemStack stack, String artName, String formName, int color) {
        CompoundTag tag = stack.getOrCreateTag();
        String safeArtName = artName == null || artName.isBlank() ? "My Blood Demon Art" : artName;
        String safeFormName = formName == null || formName.isBlank() ? "No Form" : formName;
        tag.putString(DISPLAY_ART_NAME_TAG, safeArtName);
        tag.putString(DISPLAY_FORM_NAME_TAG, safeFormName);
        tag.putString(DISPLAY_TEXT_TAG, safeArtName + ": " + safeFormName);
        tag.putInt(DISPLAY_COLOR_TAG, color);
    }

    public static String getDisplayText(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return "My Blood Demon Art: No Form";
        }
        if (tag.contains(DISPLAY_TEXT_TAG, Tag.TAG_STRING)) {
            return tag.getString(DISPLAY_TEXT_TAG);
        }
        String artName = tag.contains(DISPLAY_ART_NAME_TAG, Tag.TAG_STRING) ? tag.getString(DISPLAY_ART_NAME_TAG) : "My Blood Demon Art";
        String formName = tag.contains(DISPLAY_FORM_NAME_TAG, Tag.TAG_STRING) ? tag.getString(DISPLAY_FORM_NAME_TAG) : "No Form";
        return artName + ": " + formName;
    }

    public static int getDisplayColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(DISPLAY_COLOR_TAG, Tag.TAG_INT)) {
            return 0xAA1E2F;
        }
        return tag.getInt(DISPLAY_COLOR_TAG);
    }

    public static ResourceLocation getDefaultTexture() {
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "textures/item/custom_demon_art.png");
    }

    public static ResourceLocation getGeoModelForVariant(int variant) {
        int clamped = clampModelVariant(variant);
        String path = clamped == 1 ? "geo/custom_demon_art.geo.json" : "geo/custom_demon_art_" + clamped + ".geo.json";
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", path);
    }

    public static int minModelVariant() {
        return MIN_MODEL_VARIANT;
    }

    public static int maxModelVariant() {
        return MAX_MODEL_VARIANT;
    }

    private static int clampModelVariant(int variant) {
        return Math.max(MIN_MODEL_VARIANT, Math.min(MAX_MODEL_VARIANT, variant));
    }
}
