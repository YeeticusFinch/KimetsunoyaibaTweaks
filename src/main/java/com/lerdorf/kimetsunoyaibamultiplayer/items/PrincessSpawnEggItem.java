package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class PrincessSpawnEggItem extends ForgeSpawnEggItem {
    public PrincessSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor,
                                int highlightColor, Item.Properties properties) {
        super(type, backgroundColor, highlightColor, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Tanzanite's dog"));
    }
}
