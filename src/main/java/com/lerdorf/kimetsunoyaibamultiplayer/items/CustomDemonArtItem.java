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
import java.util.UUID;
import java.util.function.Supplier;

public class CustomDemonArtItem extends GeckolibItem {
    public static final String PLAYER_UUID_TAG = "CustomDemonArtPlayerUuid";
    public static final String PLAYER_NAME_TAG = "CustomDemonArtPlayerName";
    private static final String SELECTED_SLOT_TAG = "CustomDemonArtSelectedSlot";

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
        return () -> new com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.CustomDemonArtRenderer(this);
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

    public static ResourceLocation getDefaultTexture() {
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "textures/item/custom_demon_art.png");
    }
}
