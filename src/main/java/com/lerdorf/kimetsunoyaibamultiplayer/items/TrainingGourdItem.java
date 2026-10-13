package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkHooks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainingGourdDifficulty;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainingGourdSize;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrainingGourdItem extends BlockItem {
    private final TrainingGourdSize size;
    private final TrainingGourdDifficulty difficulty;

    public TrainingGourdItem(Block block, Properties properties, TrainingGourdSize size,
                             TrainingGourdDifficulty difficulty) {
        super(block, properties);
        this.size = size;
        this.difficulty = difficulty;
    }

    public TrainingGourdSize getGourdSize() {
        return size;
    }

    public TrainingGourdDifficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                new com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainingGourdMenu.Provider(
                    hand, size, difficulty),
                buffer -> {
                    buffer.writeBoolean(true);
                    buffer.writeVarInt(hand.ordinal());
                    buffer.writeVarInt(size.ordinal());
                    buffer.writeVarInt(difficulty.ordinal());
                });
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), player.level().isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        String key = "item.kimetsunoyaibamultiplayer.training_gourd_"
            + size.getName() + "_" + difficulty.getName();
        ChatFormatting color = switch (difficulty) {
            case EASY -> ChatFormatting.WHITE;
            case HARD -> ChatFormatting.RED;
            case EXTREME -> ChatFormatting.GOLD;
        };
        return Component.translatable(key).withStyle(style -> style
            .withColor(color)
            .withBold(difficulty != TrainingGourdDifficulty.EASY)
            .withUnderlined(difficulty == TrainingGourdDifficulty.EXTREME));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return difficulty == TrainingGourdDifficulty.EXTREME || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.kimetsunoyaibamultiplayer.training_gourd.tooltip")
            .withStyle(ChatFormatting.GRAY));
    }
}
