package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.DissolutionCocoonEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

/** Spawn egg that marks each spawned cocoon as decorative. */
public class DecorationalSilkCocoonSpawnEggItem extends ForgeSpawnEggItem {
    public DecorationalSilkCocoonSpawnEggItem(
        Supplier<? extends EntityType<? extends Mob>> type,
        int backgroundColor,
        int highlightColor,
        Item.Properties properties
    ) {
        super(type, backgroundColor, highlightColor, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return withDecorationTag(context.getItemInHand(), () -> super.useOn(context));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return withDecorationTag(player.getItemInHand(hand), () -> super.use(level, player, hand));
    }

    @Override
    protected DispenseItemBehavior createDispenseBehavior() {
        return (source, stack) -> {
            Direction face = source.getBlockState().getValue(DispenserBlock.FACING);
            EntityType<?> type = getType(stack.getTag());
            try {
                withDecorationTag(stack, () -> {
                    type.spawn(source.getLevel(), stack, null, source.getPos().relative(face),
                        MobSpawnType.DISPENSER, face != Direction.UP, false);
                    return true;
                });
            } catch (Exception exception) {
                System.err.println("Error while dispensing decorative silk cocoon spawn egg from dispenser at "
                    + source.getPos() + ": " + exception.getMessage());
                return ItemStack.EMPTY;
            }

            stack.shrink(1);
            source.getLevel().gameEvent(GameEvent.ENTITY_PLACE, source.getPos(),
                GameEvent.Context.of(source.getBlockState()));
            return stack;
        };
    }

    private static <T> T withDecorationTag(ItemStack stack, Supplier<T> action) {
        CompoundTag originalTag = stack.getTag();
        CompoundTag decoratedTag = originalTag == null ? new CompoundTag() : originalTag.copy();
        CompoundTag entityTag = decoratedTag.contains("EntityTag", Tag.TAG_COMPOUND)
            ? decoratedTag.getCompound("EntityTag") : new CompoundTag();
        entityTag.putBoolean(DissolutionCocoonEntity.DECORATION_TAG, true);
        decoratedTag.put("EntityTag", entityTag);
        stack.setTag(decoratedTag);
        try {
            return action.get();
        } finally {
            stack.setTag(originalTag);
        }
    }
}
