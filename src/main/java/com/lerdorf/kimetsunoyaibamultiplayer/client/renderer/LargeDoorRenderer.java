package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.LargeDoorBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LargeDoorModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class LargeDoorRenderer extends GeoBlockRenderer<LargeDoorBlockEntity> {
    public LargeDoorRenderer(BlockEntityRendererProvider.Context context) {
        super(new LargeDoorModel());
    }
}
