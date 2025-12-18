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
 * Favor potion item that applies protective favor effects when consumed.
 * Used as rewards for completing raids.
 *
 * - Favor of Ubuyashiki: Spawns demon slayers when attacked by demons/players
 * - Favor of Muzan: Spawns demons when attacked by non-demons
 */
public class FavorPotionItem extends Item {

    public enum FavorType {
        MUZAN("favor_of_muzan", 0x964B00, ModEffects.FAVOR_OF_MUZAN),
        UBUYASHIKI("favor_of_ubuyashiki", 0x00FF00, ModEffects.FAVOR_OF_UBUYASHIKI);

        public final String name;
        public final int color;
        public final Supplier<MobEffect> effect;

        FavorType(String name, int color, Supplier<MobEffect> effect) {
            this.name = name;
            this.color = color;
            this.effect = effect;
        }
    }

    private final FavorType favorType;
    private final int level; // 1-3

    public FavorPotionItem(Properties properties, FavorType favorType, int level) {
        super(properties);
        this.favorType = favorType;
        this.level = level;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Apply the favor effect with appropriate amplifier (level - 1)
            MobEffect effect = favorType.effect.get();
            if (effect != null) {
                // Duration of 20 minutes (24000 ticks)
                player.addEffect(new MobEffectInstance(effect, 24000, this.level - 1));

                // Play drink sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);

                // Consume the item
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // Notify player
                String romanLevel = toRoman(this.level);
                String effectName = favorType == FavorType.MUZAN ? "Favor of Muzan" : "Favor of Ubuyashiki";
                player.sendSystemMessage(Component.literal("Applied " + effectName + " " + romanLevel + "!")
                    .withStyle(style -> style.withColor(favorType.color)));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String effectName = favorType == FavorType.MUZAN ? "Favor of Muzan" : "Favor of Ubuyashiki";
        String romanLevel = toRoman(this.level);

        tooltip.add(Component.literal("Applies " + effectName + " " + romanLevel)
            .withStyle(style -> style.withColor(favorType.color)));

        if (favorType == FavorType.MUZAN) {
            tooltip.add(Component.literal("Summons demons when attacked by non-demons")
                .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Level " + romanLevel + ": Summons " + getSpawnCount() + " demon" + (getSpawnCount() > 1 ? "s" : ""))
                .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Summons demon slayers when attacked by demons or players")
                .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Level " + romanLevel + ": Summons " + getSpawnCount() + " slayer" + (getSpawnCount() > 1 ? "s" : ""))
                .withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.literal("Protection: " + romanLevel)
            .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return level >= 3; // Shiny for level 3
    }

    /**
     * Get number of entities to spawn based on level.
     * Level 1: 2 entities
     * Level 2: 3 entities
     * Level 3: 4 entities
     */
    private int getSpawnCount() {
        return switch (level) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 2;
        };
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(num);
        };
    }
}
