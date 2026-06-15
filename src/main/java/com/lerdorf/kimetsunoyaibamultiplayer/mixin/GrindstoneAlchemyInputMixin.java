package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public abstract class GrindstoneAlchemyInputMixin {
    public boolean canGrindstoneRepair(ItemStack stack) {
        return stack.is(Items.BONE) || stack.is(Items.CALCITE);
    }
}
