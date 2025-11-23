package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Omen potion item that applies omen effects when consumed.
 * Used as rewards for completing raids.
 */
public class OmenPotionItem extends Item {

    public enum OmenType {
        MUZAN("omen_of_muzan", 0x800080, ModEffects.OMEN_OF_MUZAN),
        UBUYASHIKI("omen_of_ubuyashiki", 0x55FFFF, ModEffects.OMEN_OF_UBUYASHIKI);

        public final String name;
        public final int color;
        public final Supplier<MobEffect> effect;

        OmenType(String name, int color, Supplier<MobEffect> effect) {
            this.name = name;
            this.color = color;
            this.effect = effect;
        }
    }

    private final OmenType omenType;
    private final int level; // 1-5

    public OmenPotionItem(Properties properties, OmenType omenType, int level) {
        super(properties);
        this.omenType = omenType;
        this.level = level;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Apply the omen effect with appropriate amplifier (level - 1)
            MobEffect effect = omenType.effect.get();
            if (effect != null) {
                // Duration of 30 minutes (36000 ticks)
                player.addEffect(new MobEffectInstance(effect, 36000, this.level - 1));

                // Play drink sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);

                // Consume the item
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // Notify player
                String romanLevel = toRoman(this.level);
                String effectName = omenType == OmenType.MUZAN ? "Omen of Muzan" : "Omen of Ubuyashiki";
                player.sendSystemMessage(Component.literal("Applied " + effectName + " " + romanLevel + "!")
                    .withStyle(style -> style.withColor(omenType.color)));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String effectName = omenType == OmenType.MUZAN ? "Omen of Muzan" : "Omen of Ubuyashiki";
        String romanLevel = toRoman(this.level);

        tooltip.add(Component.literal("Applies " + effectName + " " + romanLevel)
            .withStyle(style -> style.withColor(omenType.color)));

        if (omenType == OmenType.MUZAN) {
            tooltip.add(Component.literal("Triggers a demon raid when entering a civilian structure")
                .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal("Triggers a demon slayer raid when entering a civilian structure")
                .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.literal("Difficulty: " + romanLevel)
            .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return level >= 4; // Shiny for higher levels
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }
}
