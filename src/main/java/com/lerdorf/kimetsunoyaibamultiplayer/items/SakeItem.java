package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class SakeItem extends Item {
    private static final String POTENCY_KEY = "Potency";
    private static final int MIN_POTENCY = 1;
    private static final int MAX_POTENCY = 6;
    private static final int MIN_DURATION_TICKS = 3 * 60 * 20;
    private static final int DURATION_RANGE_TICKS = 5 * 60 * 20;

    public SakeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack withPotency(ItemStack stack, int potency) {
        stack.getOrCreateTag().putInt(POTENCY_KEY, Mth.clamp(potency, MIN_POTENCY, MAX_POTENCY));
        return stack;
    }

    public static int potency(ItemStack stack) {
        if (!stack.hasTag()) {
            return MIN_POTENCY;
        }
        return Mth.clamp(stack.getTag().getInt(POTENCY_KEY), MIN_POTENCY, MAX_POTENCY);
    }

    public static int minDisplayedCourageLevel(ItemStack stack) {
        return potency(stack);
    }

    public static int maxDisplayedCourageLevel(ItemStack stack) {
        return potency(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            int displayedLevel = potency(stack);
            int durationTicks = MIN_DURATION_TICKS + level.random.nextInt(DURATION_RANGE_TICKS + 1);
            entity.addEffect(new MobEffectInstance(ModEffects.COURAGE.get(), durationTicks, displayedLevel - 1, false, false, true));
        }

        if (entity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bowl = new ItemStack(Items.BOWL);
                if (stack.isEmpty()) {
                    return bowl;
                }
                if (!level.isClientSide && !player.getInventory().add(bowl)) {
                    player.drop(bowl, false);
                }
            }
            return stack;
        }

        stack.shrink(1);
        return stack.isEmpty() ? new ItemStack(Items.BOWL) : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int potency = potency(stack);
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.sake.potency", potency).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.sake.courage_level", potency).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
