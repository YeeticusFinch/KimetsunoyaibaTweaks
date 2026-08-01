package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.BridgerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class BridgerPreviewHooks {
    private BridgerPreviewHooks() {
    }

    public static void refresh(BridgerBlockEntity bridger) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BridgerPreviewManager.refresh(bridger));
    }

    public static void clear(BlockPos pos) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BridgerPreviewManager.clear(pos));
    }
}
