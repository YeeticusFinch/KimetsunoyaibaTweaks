package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BloodDemonArtAxeItem extends AxeItem {
    private static final String FORM_INDEX_TAG = "SelectedBloodDemonArtForm";

    private final String artId;
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public BloodDemonArtAxeItem(String artId, Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, 0.0F, attackSpeed, properties);
        this.artId = artId;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED,
            new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", attackSpeed - 4.0D, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = BloodDemonArtRegistry.getArt(artId);
        if (art == null) {
            return InteractionResultHolder.pass(stack);
        }

        BloodDemonArtTechnique technique = art.getTechnique();
        if (technique.getFormCount() <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        if (!canUseBloodDemonArt(player)) {
            player.displayClientMessage(Component.literal("You must be a demon to use this ability")
                .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleForm(player, stack, 1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        BloodDemonArtForm form = technique.getForm(getSelectedFormIndexInternal(stack));
        if (form == null) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            form.execute(player, level);
            player.getCooldowns().addCooldown(this, Math.max(20, form.getCooldownSeconds() * 20));
        }

        player.displayClientMessage(Component.literal(formatFormName(technique, form))
            .withStyle(ChatFormatting.DARK_GREEN), true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = BloodDemonArtRegistry.getArt(artId);
        if (art == null) {
            return;
        }

        BloodDemonArtTechnique technique = art.getTechnique();
        BloodDemonArtForm selectedForm = technique.getForm(getSelectedFormIndexInternal(stack));
        tooltip.add(Component.literal(technique.getName()).withStyle(ChatFormatting.DARK_GREEN));
        if (selectedForm != null) {
            tooltip.add(Component.literal("Selected: " + formatFormName(technique, selectedForm)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("Sneak-right click: Cycle forms").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Right click: Use selected form").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    public void cycleForm(Player player, ItemStack stack, int direction) {
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = BloodDemonArtRegistry.getArt(artId);
        if (art == null) {
            return;
        }

        BloodDemonArtTechnique technique = art.getTechnique();
        if (technique.getFormCount() <= 0) {
            return;
        }

        int current = getSelectedFormIndex(stack);
        int next = Math.floorMod(current + (direction < 0 ? -1 : 1), technique.getFormCount());
        setSelectedFormIndex(stack, next);

        BloodDemonArtForm form = technique.getForm(next);
        if (form != null) {
            player.sendSystemMessage(Component.literal(formatFormName(technique, form))
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        }
    }

    public int getSelectedFormIndex(ItemStack stack) {
        return getSelectedFormIndexInternal(stack);
    }

    public String getDisplayText(ItemStack stack) {
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = BloodDemonArtRegistry.getArt(artId);
        if (art == null) {
            return null;
        }
        BloodDemonArtForm form = art.getTechnique().getForm(getSelectedFormIndexInternal(stack));
        return form == null ? null : formatFormName(art.getTechnique(), form);
    }

    private static String formatFormName(BloodDemonArtTechnique technique, BloodDemonArtForm form) {
        return technique.getName().replace("Blood Demon Art: ", "") + ": " + form.getName();
    }

    private static int getSelectedFormIndexInternal(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return Math.max(0, tag.getInt(FORM_INDEX_TAG));
    }

    private static boolean canUseBloodDemonArt(Player player) {
        return player.getAbilities().instabuild || Damager.isDemon(player);
    }

    private static void setSelectedFormIndex(ItemStack stack, int index) {
        stack.getOrCreateTag().putInt(FORM_INDEX_TAG, Math.max(0, index));
    }
}
