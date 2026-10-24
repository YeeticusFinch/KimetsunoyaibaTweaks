package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Places a structure marker entity on the face of the clicked block. */
public class StructureMarkerItem extends Item {
    public StructureMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            var pos = context.getClickedPos().relative(context.getClickedFace());
            StructureMarkerEntity marker = ModEntities.STRUCTURE_MARKER.get().create(level);
            if (marker == null) {
                return InteractionResult.FAIL;
            }
            marker.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            level.addFreshEntity(marker);
            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
